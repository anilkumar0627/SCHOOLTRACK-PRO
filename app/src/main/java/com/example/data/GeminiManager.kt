package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiThinkingConfig(
    val thinkingLevel: String // "high"
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val thinkingConfig: GeminiThinkingConfig? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.1-pro-preview:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiManager {
    suspend fun generateRouteOptimization(
        routeName: String,
        stopsCount: Int,
        activeStudents: Int,
        systemStatus: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "AI Optimization is operating in Offline Sandbox. To enable live reasoning, please configure your actual GEMINI_API_KEY in the AI Studio Secrets panel.\n\n" +
                   "Offline Suggestion for Route '$routeName':\n" +
                   "- Reduce idle stops to minimize fuel burn.\n" +
                   "- Schedule pickup starting at 07:15 AM to achieve the optimal ETA of 38 minutes."
        }

        val prompt = """
            You are 'SchoolTrack Pro' - an advanced Transport Safety & Route Optimization AI.
            Analyze the following bus route data and provide a highly concise, professional optimization summary, safety warnings, and student pickup sequence suggestions.
            
            Route Details:
            - Route Name: $routeName
            - Number of Bus Stops: $stopsCount
            - Registered Students: $activeStudents
            - Map System: OpenStreetMap (OSM) Live GPS Active
            - Real-Time Geofence status: $systemStatus
            
            Please provide:
            1. Optimized Pickup Sequence
            2. Safety Alerts (e.g. wet conditions, high traffic hours)
            3. Operational recommendations to reduce fuel by 15%.
            
            Keep the response formatting clean, utilizing markdown bullets and bold text.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(
                thinkingConfig = GeminiThinkingConfig(thinkingLevel = "high"), // Enable high reasoning capacity!
                temperature = 0.4f
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = "You are a professional school transport operations specialist. Keep answers actionable, professional, and clear."))
            )
        )

        return try {
            val response = GeminiClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Failed to analyze route parameters."
        } catch (e: Exception) {
            Log.e("SchoolTrackPro", "Gemini API Call Failed: ${e.localizedMessage}")
            "Error analyzing route details. Operating in local safe mode.\nReason: ${e.localizedMessage}"
        }
    }
}
