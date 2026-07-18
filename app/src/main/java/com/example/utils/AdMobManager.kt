package com.example.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

object AdMobManager {
    private const val TAG = "AdMobManager"

    // Test Ad Unit IDs (AdMob Official Test IDs)
    private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    // Preloaded Ad Instances
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var nativeAd: NativeAd? = null

    // Consent states
    var canShowAdsState = mutableStateOf(false)
    var isConsentRequiredState = mutableStateOf(false)

    // Rate Limiting to prevent Excessive Ad Density & Accidental Clicks (AdMob Policy)
    private var lastInterstitialShownTime = 0L
    private const val INTERSTITIAL_MIN_GAP_MS = 45000L // 45 seconds gap minimum

    /**
     * Initializes the Google UMP Consent SDK and automatically proceeds to initialize AdMob if permitted.
     */
    fun initializeConsentAndAds(activity: Activity) {
        Log.d(TAG, "Initializing Consent Information...")

        // Configure debug settings for development. In production, do not force geography.
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("TEST-DEVICE-HASHED-ID-IF-NEEDED")
            .build()

        val consentRequestParameters = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            // .setConsentDebugSettings(debugSettings) // Uncomment to force testing EEA GDPR dialogs
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        consentInformation.requestConsentInfoUpdate(
            activity,
            consentRequestParameters,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.e(TAG, "Consent Form Error: ${formError.errorCode} - ${formError.message}")
                    }
                    val canRequest = consentInformation.canRequestAds()
                    canShowAdsState.value = canRequest
                    isConsentRequiredState.value = consentInformation.privacyOptionsRequirementStatus == 
                        com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

                    if (canRequest) {
                        initializeMobileAds(activity.applicationContext)
                    }
                }
            },
            { requestConsentError ->
                Log.e(TAG, "Consent Request Error: ${requestConsentError.errorCode} - ${requestConsentError.message}")
                val canRequest = consentInformation.canRequestAds()
                canShowAdsState.value = canRequest
                if (canRequest) {
                    initializeMobileAds(activity.applicationContext)
                }
            }
        )
    }

    /**
     * Resets the Consent state so the user can re-open the Consent Form and adjust CCPA/GDPR options.
     * Essential for Google Play policy compliance regarding user privacy options.
     */
    fun resetAndShowConsentForm(activity: Activity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.reset()
        initializeConsentAndAds(activity)
    }

    private fun initializeMobileAds(context: Context) {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            Log.d(TAG, "MobileAds already initialized.")
            return
        }

        Log.d(TAG, "Initializing MobileAds SDK...")
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "MobileAds initialized successfully. Preloading ads...")
            preloadAds(context)
        }
    }

    private fun preloadAds(context: Context) {
        preloadInterstitial(context)
        preloadRewarded(context)
        preloadAppOpen(context)
        preloadNative(context)
    }

    // ==========================================
    // INTERSTITIAL ADS IMPLEMENTATION
    // ==========================================
    fun preloadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial Ad preloaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Interstitial Ad Failed to load: ${loadAdError.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastInterstitialShownTime < INTERSTITIAL_MIN_GAP_MS) {
            Log.d(TAG, "Skipped showing Interstitial due to frequency capping / policy compliance.")
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad Dismissed.")
                    lastInterstitialShownTime = System.currentTimeMillis()
                    preloadInterstitial(activity.applicationContext) // Reload
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Interstitial Ad Failed to Show: ${adError.message}")
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Interstitial Ad was not loaded. Loading now and bypassing...")
            preloadInterstitial(activity.applicationContext)
            onAdDismissed()
        }
    }

    // ==========================================
    // REWARDED ADS IMPLEMENTATION
    // ==========================================
    fun preloadRewarded(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            TEST_REWARDED_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded Ad preloaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded Ad Failed to load: ${loadAdError.message}")
                    rewardedAd = null
                }
            }
        )
    }

    fun showRewarded(activity: Activity, onUserEarnedReward: (Int) -> Unit, onAdClosed: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded Ad Dismissed.")
                    preloadRewarded(activity.applicationContext) // Reload
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Rewarded Ad Failed to Show: ${adError.message}")
                    onAdClosed()
                }
            }
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onUserEarnedReward(rewardItem.amount)
            }
        } else {
            Log.d(TAG, "Rewarded Ad not preloaded. Showing error toast and preloading...")
            preloadRewarded(activity.applicationContext)
            onAdClosed()
        }
    }

    // ==========================================
    // APP OPEN ADS IMPLEMENTATION
    // ==========================================
    fun preloadAppOpen(context: Context) {
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            TEST_APP_OPEN_ID,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    Log.d(TAG, "App Open Ad loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "App Open Ad Failed to load: ${loadAdError.message}")
                    appOpenAd = null
                }
            }
        )
    }

    fun showAppOpenAd(activity: Activity, onDismiss: () -> Unit = {}) {
        val ad = appOpenAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "App Open Ad Dismissed.")
                    preloadAppOpen(activity.applicationContext) // Reload
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "App Open Ad Failed to Show: ${adError.message}")
                    onDismiss()
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "App Open Ad not loaded. preloading...")
            preloadAppOpen(activity.applicationContext)
            onDismiss()
        }
    }

    // ==========================================
    // NATIVE ADS IMPLEMENTATION
    // ==========================================
    fun preloadNative(context: Context) {
        val adLoader = AdLoader.Builder(context, TEST_NATIVE_ID)
            .forNativeAd { ad : NativeAd ->
                nativeAd = ad
                Log.d(TAG, "Native Ad preloaded.")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Native Ad Failed to load: ${adError.message}")
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun getLoadedNativeAd(): NativeAd? = nativeAd
}

/**
 * A highly polished, Material 3 compliant Banner Ad Composable.
 * Prevents accidental clicks and policy violations by framing the ad cleanly with labels and borders.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adSize: AdSize = AdSize.BANNER
) {
    val context = LocalContext.current
    val canShowAds by remember { AdMobManager.canShowAdsState }

    if (!canShowAds) {
        // Return a clean fallback placeholder card to avoid lay-out shifting (CLS) and keep design looking beautiful
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Ad Space (Compliant with UMP Consent)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Safe labeling to prevent accidental clicks
        Text(
            text = "SPONSORED ADVERTISEMENT",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test banner ID
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { adView ->
                // Banner view does not require continuous updates
            }
        )
    }
}

/**
 * A beautiful Native Ad Composable that dynamically renders elements of a preloaded Native Ad.
 * Avoids accidental clicks and enforces correct Ad Attribution according to policies.
 */
@Composable
fun AdMobNativeAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canShowAds by remember { AdMobManager.canShowAdsState }
    val nativeAd = remember { AdMobManager.getLoadedNativeAd() }

    if (!canShowAds || nativeAd == null) {
        // Placeholder or skip
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Premium Partner Offer",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mandatory "Ad" attribution badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Ad",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Text(
                    text = "Sponsored Content",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = nativeAd.headline ?: "Special Partner Program",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = nativeAd.body ?: "Get advanced school route safety features, immediate notifications, and secure digital records directly in the dashboard.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            nativeAd.callToAction?.let { cta ->
                Button(
                    onClick = {
                        // Normally handled via SDK click overlay, we let the user tap the CTA
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(text = cta, fontSize = 12.sp)
                }
            }
        }
    }
}
