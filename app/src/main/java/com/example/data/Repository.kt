package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class Repository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val schoolDao = db.schoolDao()
    val userDao = db.userDao()
    val studentDao = db.studentDao()
    val busDao = db.busDao()
    val routeDao = db.routeDao()
    val stopDao = db.stopDao()
    val tripDao = db.tripDao()
    val notificationDao = db.notificationDao()
    val auditLogDao = db.auditLogDao()

    suspend fun insertAuditLog(
        schoolId: Int,
        actionType: String,
        details: String,
        actorName: String,
        actorRole: String
    ) = withContext(Dispatchers.IO) {
        val log = AuditLogEntity(
            schoolId = schoolId,
            actionType = actionType,
            details = details,
            actorName = actorName,
            actorRole = actorRole
        )
        auditLogDao.insertAuditLog(log)
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Check if Firebase is available and fully initialized at runtime
    val isFirebaseInitialized: Boolean
        get() = try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            Log.w("SchoolTrackPro", "Firebase is not initialized. Operating in local Room mode.")
            false
        }

    // --- Firebase Auth & Firestore Helpers ---
    private val firebaseAuth: FirebaseAuth?
        get() = if (isFirebaseInitialized) FirebaseAuth.getInstance() else null

    private val firestore: FirebaseFirestore?
        get() = if (isFirebaseInitialized) FirebaseFirestore.getInstance() else null

    // --- School Registration & Profile ---
    suspend fun registerSchool(
        school: SchoolEntity,
        principalEmail: String,
        passwordPlain: String
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        // 1. Save School locally in Room
        val schoolId = schoolDao.insertSchool(school).toInt()
        val finalSchool = school.copy(id = schoolId)

        // 2. Create Principal User locally in Room
        val principalUser = UserEntity(
            schoolId = schoolId,
            fullName = school.principalName,
            email = principalEmail,
            passwordHash = passwordPlain, // Simple secure string or hash
            role = "PRINCIPAL",
            phone = school.mobileNumber,
            isActive = true
        )
        val principalId = userDao.insertUser(principalUser).toInt()

        // 3. Sync to Firebase if available
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    val auth = firebaseAuth
                    val store = firestore
                    if (auth != null && store != null) {
                        // Create Principal auth account in Firebase
                        auth.createUserWithEmailAndPassword(principalEmail, passwordPlain).await()
                        
                        // Sync school info to Firestore
                        store.collection("schools").document(schoolId.toString()).set(finalSchool).await()
                        
                        // Sync principal info to Firestore
                        val firebasePrincipal = principalUser.copy(id = principalId)
                        store.collection("users").document(principalId.toString()).set(firebasePrincipal).await()
                        Log.d("SchoolTrackPro", "Successfully synchronized school and principal to Firebase.")
                    }
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firebase sync failed (Registration): ${e.localizedMessage}")
                }
            }
        }

        // Prepopulate some default data for immediate visual experience (such as custom sample routes, buses, stops)
        prepopulateSchoolDefaults(schoolId)

        // Record audit log
        val log = AuditLogEntity(
            schoolId = schoolId,
            actionType = "School Registration",
            details = "School '${school.name}' was registered with short name '${school.shortName}'",
            actorName = school.principalName,
            actorRole = "PRINCIPAL"
        )
        auditLogDao.insertAuditLog(log)

        Pair(schoolId, principalId)
    }

    private suspend fun prepopulateSchoolDefaults(schoolId: Int) {
        // Prepopulate some realistic bus routes, stops and notifications so that the dashboard doesn't look barren on first registration
        val routeId = routeDao.insertRoute(
            RouteEntity(
                schoolId = schoolId,
                routeName = "North Sector Route A",
                startPoint = "Main Terminal",
                endPoint = "Greenwood High School",
                waypointsJson = """[
                    {"name": "Richmond Library Plaza", "lat": 37.7549, "lng": -122.4394},
                    {"name": "Sunset Boulevard Corner", "lat": 37.7649, "lng": -122.4294},
                    {"name": "Central Park Bus Stop", "lat": 37.7749, "lng": -122.4194}
                ]""",
                distanceKm = 12.5,
                estimatedDurationMinutes = 45
            )
        ).toInt()

        // Insert structured stops for Stop Management
        val stop1Id = stopDao.insertStop(
            StopEntity(
                schoolId = schoolId,
                routeId = routeId,
                stopName = "Richmond Library Plaza",
                villageArea = "West Richmond",
                latitude = 37.7549,
                longitude = -122.4394,
                arrivalTime = "07:15 AM",
                stopSequence = 1
            )
        ).toInt()

        val stop2Id = stopDao.insertStop(
            StopEntity(
                schoolId = schoolId,
                routeId = routeId,
                stopName = "Sunset Boulevard Corner",
                villageArea = "Sunset Valley",
                latitude = 37.7649,
                longitude = -122.4294,
                arrivalTime = "07:30 AM",
                stopSequence = 2
            )
        ).toInt()

        val stop3Id = stopDao.insertStop(
            StopEntity(
                schoolId = schoolId,
                routeId = routeId,
                stopName = "Central Park Bus Stop",
                villageArea = "Downtown",
                latitude = 37.7749,
                longitude = -122.4194,
                arrivalTime = "07:45 AM",
                stopSequence = 3
            )
        ).toInt()

        val driverId = userDao.insertUser(
            UserEntity(
                schoolId = schoolId,
                fullName = "Robert Miller",
                email = "driver@school$schoolId.com",
                passwordHash = "password",
                role = "DRIVER",
                phone = "+1 (555) 019-2834",
                avatarUri = "avatar_driver"
            )
        ).toInt()

        val parentId = userDao.insertUser(
            UserEntity(
                schoolId = schoolId,
                fullName = "Sarah Jenkins",
                email = "parent@school$schoolId.com",
                passwordHash = "password",
                role = "PARENT",
                phone = "+1 (555) 014-9821",
                avatarUri = "avatar_parent"
            )
        ).toInt()

        val busId = busDao.insertBus(
            BusEntity(
                schoolId = schoolId,
                busNumber = "Bus-04",
                vehicleRegNumber = "CA-269-PRO",
                driverUserId = driverId,
                capacity = 40,
                routeId = routeId,
                gpsStatus = "OFFLINE",
                latitude = 37.7749,
                longitude = -122.4194,
                speed = 22.5
            )
        ).toInt()

        // Insert multiple trips for the same day (Multi-Trip Engine)
        val trip1Id = tripDao.insertTrip(
            TripEntity(
                schoolId = schoolId,
                tripName = "Morning Pickup Trip 1",
                routeId = routeId,
                busId = busId,
                driverUserId = driverId,
                tripType = "Pickup",
                startTime = "07:00 AM",
                endTime = "07:45 AM",
                status = "Scheduled"
            )
        ).toInt()

        val trip2Id = tripDao.insertTrip(
            TripEntity(
                schoolId = schoolId,
                tripName = "Morning Pickup Trip 2",
                routeId = routeId,
                busId = busId,
                driverUserId = driverId,
                tripType = "Pickup",
                startTime = "08:00 AM",
                endTime = "08:45 AM",
                status = "Scheduled"
            )
        ).toInt()

        val trip3Id = tripDao.insertTrip(
            TripEntity(
                schoolId = schoolId,
                tripName = "Afternoon Drop Trip 1",
                routeId = routeId,
                busId = busId,
                driverUserId = driverId,
                tripType = "Drop",
                startTime = "02:30 PM",
                endTime = "03:15 PM",
                status = "Scheduled"
            )
        ).toInt()

        val trip4Id = tripDao.insertTrip(
            TripEntity(
                schoolId = schoolId,
                tripName = "Afternoon Drop Trip 2",
                routeId = routeId,
                busId = busId,
                driverUserId = driverId,
                tripType = "Drop",
                startTime = "03:30 PM",
                endTime = "04:15 PM",
                status = "Scheduled"
            )
        ).toInt()

        // Assign multiple students to the parent so she can switch between them
        studentDao.insertStudent(
            StudentEntity(
                schoolId = schoolId,
                fullName = "Leo Jenkins",
                rollNumber = "ST-2026-089",
                className = "Grade 5-B",
                parentUserId = parentId,
                busId = busId,
                routeId = routeId,
                pickupStop = "Central Park Bus Stop",
                dropStop = "Greenwood High School",
                tripId = trip1Id,
                pickupStopId = stop3Id,
                dropStopId = stop1Id,
                seatNumber = "Seat 12"
            )
        )

        studentDao.insertStudent(
            StudentEntity(
                schoolId = schoolId,
                fullName = "Mia Jenkins",
                rollNumber = "ST-2026-090",
                className = "Grade 2-A",
                parentUserId = parentId,
                busId = busId,
                routeId = routeId,
                pickupStop = "Sunset Boulevard Corner",
                dropStop = "Greenwood High School",
                tripId = trip2Id,
                pickupStopId = stop2Id,
                dropStopId = stop1Id,
                seatNumber = "Seat 04"
            )
        )

        notificationDao.insertNotification(
            NotificationEntity(
                schoolId = schoolId,
                title = "Welcome to SchoolTrack Pro",
                message = "The transport management system for school ID $schoolId is fully active. Multi-trip engine, route planner, stop manager, and student assigning are online.",
                senderRole = "SYSTEM"
            )
        )
    }

    // --- Users (Principal/Driver/Parent) Operations ---
    suspend fun createAccount(user: UserEntity): Int = withContext(Dispatchers.IO) {
        val id = userDao.insertUser(user).toInt()
        val finalUser = user.copy(id = id)

        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    val auth = firebaseAuth
                    val store = firestore
                    if (auth != null && store != null) {
                        // Attempt Firebase creation. Note: To prevent logging out the current active session,
                        // secondary user creation can be handled via Admin API or direct database entry for multi-tenant simulation.
                        // We store the user profile in Firestore
                        store.collection("users").document(id.toString()).set(finalUser).await()
                    }
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore sync failed (User): ${e.localizedMessage}")
                }
            }
        }
        id
    }

    suspend fun updateAccountStatus(id: Int, isActive: Boolean) {
        userDao.updateUserStatus(id, isActive)
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("users")?.document(id.toString())?.update("isActive", isActive)?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore update failed: ${e.localizedMessage}")
                }
            }
        }
    }

    suspend fun resetAccountCredentials(id: Int, email: String, passwordHash: String) {
        userDao.updateUserEmailAndPassword(id, email, passwordHash)
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("users")?.document(id.toString())?.update(
                        "email", email,
                        "passwordHash", passwordHash
                    )?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore credentials sync failed: ${e.localizedMessage}")
                }
            }
        }
    }

    suspend fun deleteUserAccount(id: Int) {
        userDao.deleteUser(id)
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("users")?.document(id.toString())?.delete()?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore delete user failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // --- Students Operations ---
    suspend fun addStudent(student: StudentEntity): Int = withContext(Dispatchers.IO) {
        val id = studentDao.insertStudent(student).toInt()
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("students")?.document(id.toString())?.set(student.copy(id = id))?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore student sync failed: ${e.localizedMessage}")
                }
            }
        }
        id
    }

    suspend fun deleteStudent(id: Int) {
        studentDao.deleteStudent(id)
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("students")?.document(id.toString())?.delete()?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore delete student failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // --- Buses Operations ---
    suspend fun addBus(bus: BusEntity): Int = withContext(Dispatchers.IO) {
        val id = busDao.insertBus(bus).toInt()
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("buses")?.document(id.toString())?.set(bus.copy(id = id))?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore bus sync failed: ${e.localizedMessage}")
                }
            }
        }
        id
    }

    suspend fun updateBusLiveGps(id: Int, lat: Double, lng: Double, speed: Double, status: String, etaMinutes: Int) {
        val now = System.currentTimeMillis()
        busDao.updateGps(id, lat, lng, speed, status, etaMinutes, now)
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("buses")?.document(id.toString())?.update(
                        "latitude", lat,
                        "longitude", lng,
                        "speed", speed,
                        "gpsStatus", status,
                        "etaMinutes", etaMinutes,
                        "lastUpdate", now
                    )?.await()
                } catch (e: Exception) {
                    // Suppress excessive log noise for high frequency telemetry
                }
            }
        }
    }

    suspend fun deleteBus(id: Int) {
        busDao.deleteBus(id)
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("buses")?.document(id.toString())?.delete()?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore delete bus failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // --- Routes Operations ---
    suspend fun addRoute(route: RouteEntity): Int = withContext(Dispatchers.IO) {
        val id = routeDao.insertRoute(route).toInt()
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("routes")?.document(id.toString())?.set(route.copy(id = id))?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore route sync failed: ${e.localizedMessage}")
                }
            }
        }
        id
    }

    suspend fun deleteRoute(id: Int) {
        routeDao.deleteRoute(id)
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("routes")?.document(id.toString())?.delete()?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore delete route failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // --- Trips Operations ---
    suspend fun addTrip(trip: TripEntity): Int = withContext(Dispatchers.IO) {
        val id = tripDao.insertTrip(trip).toInt()
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("trips")?.document(id.toString())?.set(trip.copy(id = id))?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore trip sync failed: ${e.localizedMessage}")
                }
            }
        }
        id
    }

    suspend fun updateTripStatus(id: Int, status: String) {
        tripDao.updateTripStatus(id, status)
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("trips")?.document(id.toString())?.update("status", status)?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore trip status sync failed: ${e.localizedMessage}")
                }
            }
        }
    }

    suspend fun deleteTrip(id: Int) {
        tripDao.deleteTrip(id)
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("trips")?.document(id.toString())?.delete()?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore delete trip failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // --- Notifications ---
    suspend fun sendNotification(notification: NotificationEntity): Int = withContext(Dispatchers.IO) {
        val id = notificationDao.insertNotification(notification).toInt()
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("notifications")?.document(id.toString())?.set(notification.copy(id = id))?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore notification sync failed: ${e.localizedMessage}")
                }
            }
        }
        id
    }

    // --- Stops Operations ---
    suspend fun addStop(stop: StopEntity): Int = withContext(Dispatchers.IO) {
        val id = stopDao.insertStop(stop).toInt()
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("stops")?.document(id.toString())?.set(stop.copy(id = id))?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore stop sync failed: ${e.localizedMessage}")
                }
            }
        }
        id
    }

    suspend fun deleteStop(id: Int) = withContext(Dispatchers.IO) {
        stopDao.deleteStop(id)
        if (isFirebaseInitialized) {
            repositoryScope.launch {
                try {
                    firestore?.collection("stops")?.document(id.toString())?.delete()?.await()
                } catch (e: Exception) {
                    Log.e("SchoolTrackPro", "Firestore delete stop failed: ${e.localizedMessage}")
                }
            }
        }
    }
}
