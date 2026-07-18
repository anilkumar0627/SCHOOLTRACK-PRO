package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val shortName: String,
    val logoUri: String, // Path, base64 or predefined design identifier
    val coverPhotoUri: String?,
    val motto: String?,
    val address: String,
    val city: String,
    val district: String,
    val state: String,
    val country: String,
    val pinCode: String,
    val mobileNumber: String,
    val email: String,
    val principalName: String,
    val principalEmail: String,
    
    // Advanced Profile Information
    val schoolCode: String? = null,
    val board: String? = null,
    val village: String? = null,
    val principalPhotoUri: String? = null,
    val schoolPhone: String? = null,
    val website: String? = null,
    val transportHelpline: String? = null,
    val emergencyContact: String? = null,

    // Theme Engine Customization Properties
    val primaryColorHex: String = "#3F51B5",
    val secondaryColorHex: String = "#303F9F",
    val accentColorHex: String = "#FF4081",
    val buttonStyle: String = "ROUNDED", // "ROUNDED", "SHARP", "CUT"
    val cardStyle: String = "GLASS", // "GLASS", "FLAT", "ELEVATED", "OUTLINED"
    val headerStyle: String = "COVER", // "COVER", "MINIMAL", "GRADIENT"
    val navigationStyle: String = "BOTTOM", // "BOTTOM", "DRAWER", "RAIL"
    val themeMode: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    
    // SaaS Multi-School Status and Subscriptions
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED", "SUSPENDED"
    val subscriptionPlan: String = "FREE_TRIAL", // "FREE_TRIAL", "BASIC", "STANDARD", "PREMIUM", "ENTERPRISE"
    val subscriptionStartDate: Long = System.currentTimeMillis(),
    val subscriptionExpiryDate: Long = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L // 30-Day Free Trial by default
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val schoolId: Int, // Data isolation key
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val role: String, // "PRINCIPAL", "PARENT", "DRIVER"
    val phone: String = "",
    val isActive: Boolean = true,
    val avatarUri: String = ""
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val schoolId: Int, // Data isolation key
    val fullName: String,
    val rollNumber: String,
    val className: String,
    val parentUserId: Int, // Links to UserEntity (PARENT)
    val busId: Int? = null,
    val routeId: Int? = null,
    val pickupStop: String = "",
    val dropStop: String = "",
    // Extended fields
    val tripId: Int? = null,
    val pickupStopId: Int? = null,
    val dropStopId: Int? = null,
    val seatNumber: String? = null
)

@Entity(tableName = "buses")
data class BusEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val schoolId: Int, // Data isolation key
    val busPhotoUri: String = "",
    val busNumber: String,
    val vehicleRegNumber: String,
    val driverUserId: Int? = null, // Links to UserEntity (DRIVER)
    val capacity: Int,
    val routeId: Int? = null,
    val tripId: Int? = null,
    val gpsStatus: String = "OFFLINE", // "OFFLINE", "ACTIVE", "COMPLETED"
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Double = 0.0,
    val etaMinutes: Int = 0,
    val lastUpdate: Long = System.currentTimeMillis()
)

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val schoolId: Int, // Data isolation key
    val routeName: String,
    val startPoint: String,
    val endPoint: String,
    val waypointsJson: String, // Stringified coordinates / names
    // Extended fields
    val busId: Int? = null,
    val distanceKm: Double = 0.0,
    val estimatedDurationMinutes: Int = 0
)

@Entity(tableName = "stops")
data class StopEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val schoolId: Int, // Data isolation key
    val routeId: Int, // Links to RouteEntity
    val stopName: String,
    val villageArea: String,
    val latitude: Double,
    val longitude: Double,
    val arrivalTime: String, // e.g. "07:30 AM"
    val stopSequence: Int = 0
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val schoolId: Int, // Data isolation key
    val tripName: String,
    val routeId: Int,
    val busId: Int,
    // Extended fields
    val driverUserId: Int = 0, // Links to UserEntity (DRIVER)
    val tripType: String = "Pickup", // "Pickup" or "Drop"
    val startTime: String,
    val endTime: String,
    val status: String = "Scheduled" // "Scheduled", "Active", "Completed", "Cancelled"
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val schoolId: Int, // Data isolation key
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val senderRole: String = "SYSTEM",
    val targetUserId: Int? = null // Specific recipient, null for broadcast
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val schoolId: Int,
    val actionType: String, // e.g. "School Registration", "Approval", "Logo Change", "Theme Change", "Bus Creation", "Route Creation", "Trip Creation", "Student Assignment", "Password Reset", "Account Disable"
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actorName: String,
    val actorRole: String
)
