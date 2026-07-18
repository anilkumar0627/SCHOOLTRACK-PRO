package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchool(school: SchoolEntity): Long

    @Query("SELECT * FROM schools WHERE id = :id")
    fun getSchoolById(id: Int): Flow<SchoolEntity?>

    @Query("SELECT * FROM schools WHERE id = :id")
    suspend fun getSchoolByIdSync(id: Int): SchoolEntity?

    @Query("SELECT * FROM schools WHERE email = :email LIMIT 1")
    suspend fun getSchoolByEmail(email: String): SchoolEntity?

    @Query("SELECT * FROM schools")
    fun getAllSchools(): Flow<List<SchoolEntity>>

    @Query("UPDATE schools SET status = :status WHERE id = :id")
    suspend fun updateSchoolStatus(id: Int, status: String)

    @Query("UPDATE schools SET subscriptionPlan = :plan, subscriptionExpiryDate = :expiryDate WHERE id = :id")
    suspend fun updateSubscription(id: Int, plan: String, expiryDate: Long)

    @Query("DELETE FROM schools WHERE id = :id")
    suspend fun deleteSchool(id: Int)
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserByIdSync(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE schoolId = :schoolId")
    fun getUsersBySchool(schoolId: Int): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE schoolId = :schoolId AND role = :role")
    fun getUsersBySchoolAndRole(schoolId: Int, role: String): Flow<List<UserEntity>>

    @Query("UPDATE users SET isActive = :isActive WHERE id = :id")
    suspend fun updateUserStatus(id: Int, isActive: Boolean)

    @Query("UPDATE users SET email = :email, passwordHash = :passwordHash WHERE id = :id")
    suspend fun updateUserEmailAndPassword(id: Int, email: String, passwordHash: String)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Int)
}

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Query("SELECT * FROM students WHERE schoolId = :schoolId")
    fun getStudentsBySchool(schoolId: Int): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id")
    fun getStudentById(id: Int): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE schoolId = :schoolId AND parentUserId = :parentUserId")
    fun getStudentsByParent(schoolId: Int, parentUserId: Int): Flow<List<StudentEntity>>

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudent(id: Int)
}

@Dao
interface BusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBus(bus: BusEntity): Long

    @Query("SELECT * FROM buses WHERE schoolId = :schoolId")
    fun getBusesBySchool(schoolId: Int): Flow<List<BusEntity>>

    @Query("SELECT * FROM buses WHERE id = :id")
    fun getBusById(id: Int): Flow<BusEntity?>

    @Query("SELECT * FROM buses WHERE id = :id")
    suspend fun getBusByIdSync(id: Int): BusEntity?

    @Query("UPDATE buses SET latitude = :lat, longitude = :lng, speed = :speed, gpsStatus = :status, etaMinutes = :etaMinutes, lastUpdate = :timestamp WHERE id = :id")
    suspend fun updateGps(id: Int, lat: Double, lng: Double, speed: Double, status: String, etaMinutes: Int, timestamp: Long)

    @Query("DELETE FROM buses WHERE id = :id")
    suspend fun deleteBus(id: Int)
}

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity): Long

    @Query("SELECT * FROM routes WHERE schoolId = :schoolId")
    fun getRoutesBySchool(schoolId: Int): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id")
    fun getRouteById(id: Int): Flow<RouteEntity?>

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteRoute(id: Int)
}

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Query("SELECT * FROM trips WHERE schoolId = :schoolId")
    fun getTripsBySchool(schoolId: Int): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun getTripById(id: Int): Flow<TripEntity?>

    @Query("UPDATE trips SET status = :status WHERE id = :id")
    suspend fun updateTripStatus(id: Int, status: String)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTrip(id: Int)
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications WHERE schoolId = :schoolId ORDER BY timestamp DESC")
    fun getNotificationsBySchool(schoolId: Int): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE schoolId = :schoolId AND (targetUserId IS NULL OR targetUserId = :targetUserId) ORDER BY timestamp DESC")
    fun getNotificationsBySchoolAndUser(schoolId: Int, targetUserId: Int): Flow<List<NotificationEntity>>
}

@Dao
interface StopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: StopEntity): Long

    @Query("SELECT * FROM stops WHERE schoolId = :schoolId")
    fun getStopsBySchool(schoolId: Int): Flow<List<StopEntity>>

    @Query("SELECT * FROM stops WHERE routeId = :routeId ORDER BY stopSequence ASC")
    fun getStopsByRoute(routeId: Int): Flow<List<StopEntity>>

    @Query("SELECT * FROM stops WHERE id = :id")
    fun getStopById(id: Int): Flow<StopEntity?>

    @Query("SELECT * FROM stops WHERE id = :id")
    suspend fun getStopByIdSync(id: Int): StopEntity?

    @Query("DELETE FROM stops WHERE id = :id")
    suspend fun deleteStop(id: Int)

    @Query("DELETE FROM stops WHERE routeId = :routeId")
    suspend fun deleteStopsByRoute(routeId: Int)
}

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    @Query("SELECT * FROM audit_logs WHERE schoolId = :schoolId ORDER BY timestamp DESC")
    fun getAuditLogsBySchool(schoolId: Int): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>
}
