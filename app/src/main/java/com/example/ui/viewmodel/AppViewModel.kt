package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val repository = Repository(application)
    private val gemini = GeminiManager()

    // --- Session States ---
    private val _currentSchool = MutableStateFlow<SchoolEntity?>(null)
    val currentSchool: StateFlow<SchoolEntity?> = _currentSchool.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _registrationSuccess = MutableStateFlow(false)
    val registrationSuccess: StateFlow<Boolean> = _registrationSuccess.asStateFlow()

    // --- Administrative Lists (Flows from Room) ---
    val schools: StateFlow<List<SchoolEntity>> = repository.schoolDao.getAllSchools()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserEntity>> = _currentSchool
        .flatMapLatest { school ->
            if (school != null) repository.userDao.getUsersBySchool(school.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val students: StateFlow<List<StudentEntity>> = _currentSchool
        .flatMapLatest { school ->
            if (school != null) repository.studentDao.getStudentsBySchool(school.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val buses: StateFlow<List<BusEntity>> = _currentSchool
        .flatMapLatest { school ->
            if (school != null) repository.busDao.getBusesBySchool(school.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routes: StateFlow<List<RouteEntity>> = _currentSchool
        .flatMapLatest { school ->
            if (school != null) repository.routeDao.getRoutesBySchool(school.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trips: StateFlow<List<TripEntity>> = _currentSchool
        .flatMapLatest { school ->
            if (school != null) repository.tripDao.getTripsBySchool(school.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stops: StateFlow<List<StopEntity>> = _currentSchool
        .flatMapLatest { school ->
            if (school != null) repository.stopDao.getStopsBySchool(school.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _parentSelectedStudentId = MutableStateFlow<Int?>(null)
    val parentSelectedStudentId: StateFlow<Int?> = _parentSelectedStudentId.asStateFlow()

    fun selectParentStudent(studentId: Int) {
        _parentSelectedStudentId.value = studentId
    }

    val notifications: StateFlow<List<NotificationEntity>> = _currentSchool
        .flatMapLatest { school ->
            val user = _currentUser.value
            if (school != null && user != null) {
                // Return isolated notifications for current school and user
                repository.notificationDao.getNotificationsBySchoolAndUser(school.id, user.id)
            } else if (school != null) {
                repository.notificationDao.getNotificationsBySchool(school.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- GPS Telemetry Simulation & Geofencing ---
    private var trackingJob: Job? = null
    private val _simulationLogs = MutableStateFlow<List<String>>(listOf("Telemetry engine offline."))
    val simulationLogs: StateFlow<List<String>> = _simulationLogs.asStateFlow()

    // Predefined simulated route coordinates (Stop 3 -> Stop 2 -> Stop 1 -> Greenwood High School)
    private val pathCoords = listOf(
        Pair(37.7549, -122.4394), // Stop 3: Richmond Library Plaza (approx 4.5 km)
        Pair(37.7600, -122.4344), // Waypoint
        Pair(37.7649, -122.4294), // Stop 2: Sunset Boulevard Corner (approx 3.0 km)
        Pair(37.7700, -122.4244), // Waypoint
        Pair(37.7749, -122.4194), // Stop 1: Central Park Bus Stop (approx 1.5 km)
        Pair(37.7800, -122.4144), // Waypoint
        Pair(37.7850, -122.4100)  // Greenwood High School (Base Campus)
    )

    // --- AI Assistant States ---
    private val _aiSuggestion = MutableStateFlow<String?>(null)
    val aiSuggestion: StateFlow<String?> = _aiSuggestion.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogDao.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _creationError = MutableStateFlow<String?>(null)
    val creationError: StateFlow<String?> = _creationError.asStateFlow()

    fun clearCreationError() {
        _creationError.value = null
    }

    init {
        // Pre-fill session with a demonstration school if exists
        viewModelScope.launch {
            val firstSchool = schools.firstOrNull()?.firstOrNull()
            if (firstSchool != null) {
                // Don't auto log in to let user experience registration and clean multi-tenant profiles.
            }
        }
        // Prepopulate the Platform Super Admin account if it doesn't exist
        viewModelScope.launch {
            try {
                val admin = repository.userDao.getUserByEmail("admin@schooltrack.pro")
                if (admin == null) {
                    val defaultAdmin = UserEntity(
                        schoolId = -1, // No specific school
                        fullName = "Platform Super Admin",
                        email = "admin@schooltrack.pro",
                        passwordHash = "admin123",
                        role = "SUPER_ADMIN",
                        phone = "0000000000",
                        avatarUri = "avatar_principal",
                        isActive = true
                    )
                    repository.userDao.insertUser(defaultAdmin)
                    Log.d("SchoolTrackPro", "Platform Super Admin account auto-prepopulated.")
                }
            } catch (e: Exception) {
                Log.e("SchoolTrackPro", "Failed to prepopulate Super Admin: ${e.localizedMessage}")
            }
        }
    }

    // --- Actions ---

    fun registerNewSchool(
        name: String,
        shortName: String,
        logoUri: String,
        coverPhotoUri: String?,
        motto: String?,
        address: String,
        city: String,
        district: String,
        state: String,
        country: String,
        pinCode: String,
        mobileNumber: String,
        email: String,
        principalName: String,
        principalEmail: String,
        principalPasswordPlain: String
    ) {
        viewModelScope.launch {
            try {
                val school = SchoolEntity(
                    name = name,
                    shortName = shortName,
                    logoUri = logoUri.ifEmpty { "🎓" },
                    coverPhotoUri = coverPhotoUri,
                    motto = motto,
                    address = address,
                    city = city,
                    district = district,
                    state = state,
                    country = country,
                    pinCode = pinCode,
                    mobileNumber = mobileNumber,
                    email = email,
                    principalName = principalName,
                    principalEmail = principalEmail
                )
                val (_, principalId) = repository.registerSchool(
                    school = school,
                    principalEmail = principalEmail,
                    passwordPlain = principalPasswordPlain
                )
                _registrationSuccess.value = true
                _loginError.value = null
                // Auto log in as Principal
                loginUser(principalEmail, principalPasswordPlain)
            } catch (e: Exception) {
                Log.e("SchoolTrackPro", "School Registration Failed: ${e.localizedMessage}")
            }
        }
    }

    fun updateSchoolBranding(
        schoolId: Int,
        fullName: String,
        shortName: String,
        logoUri: String,
        coverPhotoUri: String?,
        motto: String?,
        schoolCode: String? = null,
        board: String? = null,
        village: String? = null,
        principalPhotoUri: String? = null,
        schoolPhone: String? = null,
        website: String? = null,
        transportHelpline: String? = null,
        emergencyContact: String? = null,
        primaryColorHex: String = "#3F51B5",
        secondaryColorHex: String = "#303F9F",
        accentColorHex: String = "#FF4081",
        buttonStyle: String = "ROUNDED",
        cardStyle: String = "GLASS",
        headerStyle: String = "COVER",
        navigationStyle: String = "BOTTOM",
        themeMode: String = "SYSTEM"
    ) {
        viewModelScope.launch {
            try {
                val current = repository.schoolDao.getSchoolByIdSync(schoolId)
                if (current != null) {
                    val updated = current.copy(
                        name = fullName,
                        shortName = shortName,
                        logoUri = logoUri,
                        coverPhotoUri = coverPhotoUri,
                        motto = motto,
                        schoolCode = schoolCode ?: current.schoolCode,
                        board = board ?: current.board,
                        village = village ?: current.village,
                        principalPhotoUri = principalPhotoUri ?: current.principalPhotoUri,
                        schoolPhone = schoolPhone ?: current.schoolPhone,
                        website = website ?: current.website,
                        transportHelpline = transportHelpline ?: current.transportHelpline,
                        emergencyContact = emergencyContact ?: current.emergencyContact,
                        primaryColorHex = primaryColorHex,
                        secondaryColorHex = secondaryColorHex,
                        accentColorHex = accentColorHex,
                        buttonStyle = buttonStyle,
                        cardStyle = cardStyle,
                        headerStyle = headerStyle,
                        navigationStyle = navigationStyle,
                        themeMode = themeMode
                    )
                    repository.schoolDao.insertSchool(updated)
                    _currentSchool.value = updated
                    addLog("Branding System & Theme Engine updated.")
                    sendNotificationAlert("Branding & Theme Updated", "Your school brand identity system has been updated successfully.")
                }
            } catch (e: Exception) {
                Log.e("SchoolTrackPro", "Failed to update school branding: ${e.localizedMessage}")
            }
        }
    }

    fun loginUser(email: String, passwordPlain: String) {
        viewModelScope.launch {
            _loginError.value = null
            try {
                // Find user in local database
                val user = repository.userDao.getUserByEmail(email)
                if (user != null && user.passwordHash == passwordPlain) {
                    if (!user.isActive) {
                        _loginError.value = "This account is currently deactivated."
                        return@launch
                    }
                    // Fetch corresponding school profile
                    val school = repository.schoolDao.getSchoolByIdSync(user.schoolId)
                    if (school != null && user.role != "SUPER_ADMIN") {
                        if (school.status == "PENDING") {
                            _loginError.value = "Your school registration is pending approval from the Platform Super Admin."
                            return@launch
                        } else if (school.status == "REJECTED") {
                            _loginError.value = "Your school registration was rejected. Please contact support."
                            return@launch
                        } else if (school.status == "SUSPENDED") {
                            _loginError.value = "Your school account has been suspended. Please contact support."
                            return@launch
                        }
                    }
                    _currentUser.value = user
                    _currentSchool.value = school
                    addLog("Welcome back, ${user.fullName}!")
                } else {
                    _loginError.value = "Invalid email address or password."
                }
            } catch (e: Exception) {
                _loginError.value = "Authentication error: ${e.localizedMessage}"
            }
        }
    }

    fun logout() {
        stopBusTracking()
        _currentUser.value = null
        _currentSchool.value = null
        _registrationSuccess.value = false
        _simulationLogs.value = listOf("Telemetry engine offline.")
        _aiSuggestion.value = null
    }

    // --- Admin Operations (Principal only) ---

    fun createParent(fullName: String, email: String, passwordPlain: String, phone: String) {
        val school = _currentSchool.value ?: return
        if (!canCreateEntity("PARENT") { _creationError.value = it }) return
        viewModelScope.launch {
            val user = UserEntity(
                schoolId = school.id,
                fullName = fullName,
                email = email,
                passwordHash = passwordPlain,
                role = "PARENT",
                phone = phone,
                avatarUri = "avatar_parent"
            )
            repository.createAccount(user)
            sendNotificationAlert("New Parent Registered", "Parent account for '$fullName' has been configured successfully.")
            repository.insertAuditLog(
                schoolId = school.id,
                actionType = "Student Assignment",
                details = "Parent account for '$fullName' registered.",
                actorName = _currentUser.value?.fullName ?: "Principal",
                actorRole = "PRINCIPAL"
            )
        }
    }

    fun createDriver(fullName: String, email: String, passwordPlain: String, phone: String) {
        val school = _currentSchool.value ?: return
        if (!canCreateEntity("DRIVER") { _creationError.value = it }) return
        viewModelScope.launch {
            val user = UserEntity(
                schoolId = school.id,
                fullName = fullName,
                email = email,
                passwordHash = passwordPlain,
                role = "DRIVER",
                phone = phone,
                avatarUri = "avatar_driver"
            )
            repository.createAccount(user)
            sendNotificationAlert("New Driver Registered", "Driver account for '$fullName' has been configured successfully.")
            repository.insertAuditLog(
                schoolId = school.id,
                actionType = "Driver Assigned",
                details = "Driver account for '$fullName' created and assigned.",
                actorName = _currentUser.value?.fullName ?: "Principal",
                actorRole = "PRINCIPAL"
            )
        }
    }

    fun createStudent(
        fullName: String,
        rollNumber: String,
        className: String,
        parentUserId: Int,
        busId: Int?,
        routeId: Int?,
        pickup: String,
        drop: String,
        tripId: Int? = null,
        pickupStopId: Int? = null,
        dropStopId: Int? = null,
        seatNumber: String? = null
    ) {
        val school = _currentSchool.value ?: return
        if (!canCreateEntity("STUDENT") { _creationError.value = it }) return
        viewModelScope.launch {
            val student = StudentEntity(
                schoolId = school.id,
                fullName = fullName,
                rollNumber = rollNumber,
                className = className,
                parentUserId = parentUserId,
                busId = busId,
                routeId = routeId,
                pickupStop = pickup,
                dropStop = drop,
                tripId = tripId,
                pickupStopId = pickupStopId,
                dropStopId = dropStopId,
                seatNumber = seatNumber
            )
            repository.addStudent(student)
            sendNotificationAlert("New Student Record", "Student record for '$fullName' (Class $className) is active.")
            repository.insertAuditLog(
                schoolId = school.id,
                actionType = "Student Assignment",
                details = "Student '$fullName' assigned to school registry.",
                actorName = _currentUser.value?.fullName ?: "Principal",
                actorRole = "PRINCIPAL"
            )
        }
    }

    fun createBus(busNumber: String, vehicleRegNumber: String, driverUserId: Int?, capacity: Int, routeId: Int?) {
        val school = _currentSchool.value ?: return
        if (!canCreateEntity("BUS") { _creationError.value = it }) return
        viewModelScope.launch {
            val bus = BusEntity(
                schoolId = school.id,
                busNumber = busNumber,
                vehicleRegNumber = vehicleRegNumber,
                driverUserId = driverUserId,
                capacity = capacity,
                routeId = routeId,
                gpsStatus = "OFFLINE"
            )
            repository.addBus(bus)
            sendNotificationAlert("New Bus Configured", "Vehicle registration '$vehicleRegNumber' assigned to $busNumber.")
            repository.insertAuditLog(
                schoolId = school.id,
                actionType = "Bus Creation",
                details = "Bus '$busNumber' ($vehicleRegNumber) created.",
                actorName = _currentUser.value?.fullName ?: "Principal",
                actorRole = "PRINCIPAL"
            )
        }
    }

    fun createRoute(
        routeName: String,
        start: String,
        end: String,
        waypoints: String,
        busId: Int? = null,
        distanceKm: Double = 0.0,
        estimatedDurationMinutes: Int = 0
    ) {
        val school = _currentSchool.value ?: return
        if (!canCreateEntity("ROUTE") { _creationError.value = it }) return
        viewModelScope.launch {
            val route = RouteEntity(
                schoolId = school.id,
                routeName = routeName,
                startPoint = start,
                endPoint = end,
                waypointsJson = waypoints,
                busId = busId,
                distanceKm = distanceKm,
                estimatedDurationMinutes = estimatedDurationMinutes
            )
            repository.addRoute(route)
            sendNotificationAlert("Route Operationalized", "New transit route '$routeName' added to the network.")
            repository.insertAuditLog(
                schoolId = school.id,
                actionType = "Route Creation",
                details = "Route '$routeName' was created.",
                actorName = _currentUser.value?.fullName ?: "Principal",
                actorRole = "PRINCIPAL"
            )
        }
    }

    fun createStop(
        routeId: Int,
        stopName: String,
        villageArea: String,
        latitude: Double,
        longitude: Double,
        arrivalTime: String,
        stopSequence: Int
    ) {
        val school = _currentSchool.value ?: return
        viewModelScope.launch {
            val stop = StopEntity(
                schoolId = school.id,
                routeId = routeId,
                stopName = stopName,
                villageArea = villageArea,
                latitude = latitude,
                longitude = longitude,
                arrivalTime = arrivalTime,
                stopSequence = stopSequence
            )
            repository.addStop(stop)
            sendNotificationAlert("New Stop Configured", "Stop '$stopName' is operational on route.")
        }
    }

    fun deleteStop(id: Int) {
        viewModelScope.launch {
            repository.deleteStop(id)
        }
    }

    fun createTrip(
        tripName: String,
        routeId: Int,
        busId: Int,
        driverUserId: Int = 0,
        tripType: String = "Pickup",
        startTime: String,
        endTime: String
    ) {
        val school = _currentSchool.value ?: return
        if (!canCreateEntity("TRIP") { _creationError.value = it }) return
        viewModelScope.launch {
            val trip = TripEntity(
                schoolId = school.id,
                tripName = tripName,
                routeId = routeId,
                busId = busId,
                driverUserId = driverUserId,
                tripType = tripType,
                startTime = startTime,
                endTime = endTime,
                status = "Scheduled"
            )
            repository.addTrip(trip)
            sendNotificationAlert("Trip Scheduled", "Trip schedule '$tripName' configured.")
            repository.insertAuditLog(
                schoolId = school.id,
                actionType = "Trip Creation",
                details = "Trip schedule '$tripName' was configured.",
                actorName = _currentUser.value?.fullName ?: "Principal",
                actorRole = "PRINCIPAL"
            )
        }
    }

    fun startTripForDriver(tripId: Int) {
        val school = _currentSchool.value ?: return
        viewModelScope.launch {
            // Find current trip
            val selectedTrip = repository.tripDao.getTripById(tripId).firstOrNull() ?: return@launch
            
            // Find all trips for the same bus
            val allTrips = repository.tripDao.getTripsBySchool(school.id).firstOrNull() ?: emptyList()
            val sameBusTrips = allTrips.filter { it.busId == selectedTrip.busId }
            
            // Deactivate other trips for the same bus
            for (t in sameBusTrips) {
                if (t.status == "Active" && t.id != selectedTrip.id) {
                    repository.updateTripStatus(t.id, "Completed")
                }
            }
            
            // Activate selected trip
            repository.updateTripStatus(selectedTrip.id, "Active")
            
            // Start GPS Telemetry
            startBusTracking(selectedTrip.busId)
            
            sendNotificationAlert("Trip Started", "Trip '${selectedTrip.tripName}' is now ACTIVE.")
        }
    }

    fun updateTripStatus(tripId: Int, status: String) {
        viewModelScope.launch {
            repository.updateTripStatus(tripId, status)
            sendNotificationAlert("Trip $status", "Trip status updated to $status.")
        }
    }

    fun changeAccountStatus(userId: Int, isActive: Boolean) {
        val schoolId = _currentSchool.value?.id ?: 0
        val actorName = _currentUser.value?.fullName ?: "Admin"
        val actorRole = _currentUser.value?.role ?: "PRINCIPAL"
        viewModelScope.launch {
            repository.updateAccountStatus(userId, isActive)
            repository.insertAuditLog(
                schoolId = schoolId,
                actionType = "Account Disable",
                details = "User ID $userId status changed to ${if (isActive) "Active" else "Disabled"}.",
                actorName = actorName,
                actorRole = actorRole
            )
        }
    }

    fun resetCredentials(userId: Int, email: String, passwordPlain: String) {
        val schoolId = _currentSchool.value?.id ?: 0
        val actorName = _currentUser.value?.fullName ?: "Admin"
        val actorRole = _currentUser.value?.role ?: "PRINCIPAL"
        viewModelScope.launch {
            repository.resetAccountCredentials(userId, email, passwordPlain)
            repository.insertAuditLog(
                schoolId = schoolId,
                actionType = "Password Reset",
                details = "Credentials reset for User ID $userId.",
                actorName = actorName,
                actorRole = actorRole
            )
        }
    }

    fun deleteAccount(userId: Int) {
        viewModelScope.launch {
            repository.deleteUserAccount(userId)
        }
    }

    fun deleteStudent(id: Int) {
        viewModelScope.launch {
            repository.deleteStudent(id)
        }
    }

    fun deleteBus(id: Int) {
        val schoolId = _currentSchool.value?.id ?: 0
        val actorName = _currentUser.value?.fullName ?: "Principal"
        viewModelScope.launch {
            repository.deleteBus(id)
            repository.insertAuditLog(
                schoolId = schoolId,
                actionType = "Bus Deleted",
                details = "Bus with ID $id was deleted from the fleet.",
                actorName = actorName,
                actorRole = "PRINCIPAL"
            )
        }
    }

    fun deleteRoute(id: Int) {
        val schoolId = _currentSchool.value?.id ?: 0
        val actorName = _currentUser.value?.fullName ?: "Principal"
        viewModelScope.launch {
            repository.deleteRoute(id)
            repository.insertAuditLog(
                schoolId = schoolId,
                actionType = "Route Deleted",
                details = "Route with ID $id was deleted.",
                actorName = actorName,
                actorRole = "PRINCIPAL"
            )
        }
    }

    fun deleteTrip(id: Int) {
        val schoolId = _currentSchool.value?.id ?: 0
        val actorName = _currentUser.value?.fullName ?: "Principal"
        viewModelScope.launch {
            repository.deleteTrip(id)
            repository.insertAuditLog(
                schoolId = schoolId,
                actionType = "Trip Deleted",
                details = "Trip with ID $id was deleted.",
                actorName = actorName,
                actorRole = "PRINCIPAL"
            )
        }
    }

    // --- Notification Helper ---
    private suspend fun sendNotificationAlert(title: String, message: String) {
        val school = _currentSchool.value ?: return
        val user = _currentUser.value ?: return
        repository.sendNotification(
            NotificationEntity(
                schoolId = school.id,
                title = title,
                message = message,
                senderRole = user.role
            )
        )
    }

    fun broadcastAlert(title: String, message: String) {
        viewModelScope.launch {
            sendNotificationAlert(title, message)
        }
    }

    // --- Live GPS Simulation & Telemetry Engine ---

    fun startBusTracking(busId: Int) {
        stopBusTracking() // prevent multiple jobs
        val school = _currentSchool.value ?: return
        addLog("GPS engine initializing...")

        trackingJob = viewModelScope.launch {
            var step = 0
            while (true) {
                val coords = pathCoords[step % pathCoords.size]
                val currentLat = coords.first
                val currentLng = coords.second

                // School is our destination target
                val destLat = 37.7850
                val destLng = -122.4100
                val distance = calculateDistance(currentLat, currentLng, destLat, destLng)
                val speed = 25.0 // simulated 25 km/h
                val eta = if (speed > 0) ((distance / speed) * 60).toInt() else 0

                // Update database
                repository.updateBusLiveGps(
                    id = busId,
                    lat = currentLat,
                    lng = currentLng,
                    speed = speed,
                    status = "ACTIVE",
                    etaMinutes = eta
                )

                addLog("Bus GPS updated. Lat: $currentLat, Lng: $currentLng. Speed: 25 km/h. ETA to campus: $eta mins.")

                // Geofencing trigger alerts
                evaluateGeofencing(distance)

                delay(6000) // update every 6 seconds
                step++
            }
        }
    }

    fun stopBusTracking() {
        trackingJob?.cancel()
        trackingJob = null
        addLog("GPS Telemetry engine halted.")
    }

    fun resetBusGps(busId: Int) {
        viewModelScope.launch {
            repository.updateBusLiveGps(busId, 0.0, 0.0, 0.0, "OFFLINE", 0)
        }
    }

    private fun evaluateGeofencing(distanceKm: Double) {
        val school = _currentSchool.value ?: return
        viewModelScope.launch {
            if (distanceKm <= 0.1) {
                repository.sendNotification(
                    NotificationEntity(
                        schoolId = school.id,
                        title = "🚨 Geofence: Arrival Alert",
                        message = "Transport Alert: Bus 04 is arriving at Greenwood High Base Campus. Parents prepare for student pickup.",
                        senderRole = "SYSTEM"
                    )
                )
                addLog("🚨 Geofence Alert: Bus ARRIVED at School campus.")
            } else if (distanceKm <= 1.0) {
                repository.sendNotification(
                    NotificationEntity(
                        schoolId = school.id,
                        title = "⚠️ Geofence: 1 KM Radius",
                        message = "Transport Warning: Bus 04 entered the 1 KM high proximity zone to Greenwood High.",
                        senderRole = "SYSTEM"
                    )
                )
                addLog("⚠️ Geofence warning: Bus is within 1 KM radius.")
            } else if (distanceKm <= 3.0) {
                repository.sendNotification(
                    NotificationEntity(
                        schoolId = school.id,
                        title = "ℹ️ Geofence: 3 KM Radius",
                        message = "Transport Update: Bus 04 has entered the 3 KM outer perimeter geofence.",
                        senderRole = "SYSTEM"
                    )
                )
                addLog("ℹ️ Geofence info: Bus is within 3 KM radius.")
            }
        }
    }

    private fun addLog(message: String) {
        val currentLogs = _simulationLogs.value.toMutableList()
        currentLogs.add(0, "[${System.currentTimeMillis() % 100000}] $message")
        if (currentLogs.size > 20) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _simulationLogs.value = currentLogs
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    // --- Gemini high reasoning query trigger ---
    fun optimizeRouteAi(routeName: String, stopsCount: Int, studentsCount: Int) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiSuggestion.value = null
            try {
                val recommendation = gemini.generateRouteOptimization(
                    routeName = routeName,
                    stopsCount = stopsCount,
                    activeStudents = studentsCount,
                    systemStatus = "Geofence perimeter 3 KM active. GPS frequency 6s."
                )
                _aiSuggestion.value = recommendation
            } catch (e: Exception) {
                _aiSuggestion.value = "Error calling reasoning engine: ${e.localizedMessage}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // --- SaaS Multi-School & Subscription Operations ---

    val SubscriptionPlans = listOf(
        SubscriptionPlan(
            name = "FREE_TRIAL",
            displayName = "30-Day Free Trial",
            maxStudents = 50,
            maxDrivers = 5,
            maxBuses = 5,
            maxParents = 50,
            maxRoutes = 5,
            maxTrips = 10,
            cloudStorageLimitGb = 5,
            gpsTracking = true,
            reports = true,
            premiumFeatures = false
        ),
        SubscriptionPlan(
            name = "BASIC",
            displayName = "Basic Plan",
            maxStudents = 100,
            maxDrivers = 10,
            maxBuses = 10,
            maxParents = 100,
            maxRoutes = 10,
            maxTrips = 20,
            cloudStorageLimitGb = 10,
            gpsTracking = false,
            reports = false,
            premiumFeatures = false
        ),
        SubscriptionPlan(
            name = "STANDARD",
            displayName = "Standard Plan",
            maxStudents = 300,
            maxDrivers = 30,
            maxBuses = 30,
            maxParents = 300,
            maxRoutes = 30,
            maxTrips = 60,
            cloudStorageLimitGb = 50,
            gpsTracking = true,
            reports = true,
            premiumFeatures = false
        ),
        SubscriptionPlan(
            name = "PREMIUM",
            displayName = "Premium Plan",
            maxStudents = 1000,
            maxDrivers = 100,
            maxBuses = 100,
            maxParents = 1000,
            maxRoutes = 100,
            maxTrips = 200,
            cloudStorageLimitGb = 200,
            gpsTracking = true,
            reports = true,
            premiumFeatures = true
        ),
        SubscriptionPlan(
            name = "ENTERPRISE",
            displayName = "Enterprise Plan",
            maxStudents = 10000,
            maxDrivers = 1000,
            maxBuses = 1000,
            maxParents = 10000,
            maxRoutes = 1000,
            maxTrips = 1000,
            cloudStorageLimitGb = 1000,
            gpsTracking = true,
            reports = true,
            premiumFeatures = true
        )
    )

    fun getPlanForSchool(school: SchoolEntity?): SubscriptionPlan {
        val planName = school?.subscriptionPlan ?: "FREE_TRIAL"
        return SubscriptionPlans.find { it.name == planName } ?: SubscriptionPlans[0]
    }

    fun isSubscriptionExpired(school: SchoolEntity?): Boolean {
        if (school == null) return false
        return System.currentTimeMillis() > school.subscriptionExpiryDate
    }

    fun canCreateEntity(type: String, onFail: (String) -> Unit): Boolean {
        val school = _currentSchool.value
        if (school == null) return true // Super Admin is exempt
        
        // Expiry check
        if (isSubscriptionExpired(school)) {
            onFail("Your subscription has expired! The platform is now in Read-Only Mode. Please renew/upgrade your plan to continue.")
            return false
        }
        
        val plan = getPlanForSchool(school)
        val currentStudentsCount = students.value.size
        val currentBusesCount = buses.value.size
        val currentRoutesCount = routes.value.size
        val currentTripsCount = trips.value.size
        
        val currentDriversCount = users.value.count { it.role == "DRIVER" }
        val currentParentsCount = users.value.count { it.role == "PARENT" }
        
        when (type) {
            "STUDENT" -> {
                if (currentStudentsCount >= plan.maxStudents) {
                    onFail("SaaS Limit Exceeded: You have reached the maximum allowed students (${plan.maxStudents}) for your current '${plan.displayName}'. Please upgrade your subscription.")
                    return false
                }
            }
            "DRIVER" -> {
                if (currentDriversCount >= plan.maxDrivers) {
                    onFail("SaaS Limit Exceeded: You have reached the maximum allowed drivers (${plan.maxDrivers}) for your current '${plan.displayName}'. Please upgrade your subscription.")
                    return false
                }
            }
            "PARENT" -> {
                if (currentParentsCount >= plan.maxParents) {
                    onFail("SaaS Limit Exceeded: You have reached the maximum allowed parents (${plan.maxParents}) for your current '${plan.displayName}'. Please upgrade your subscription.")
                    return false
                }
            }
            "BUS" -> {
                if (currentBusesCount >= plan.maxBuses) {
                    onFail("SaaS Limit Exceeded: You have reached the maximum allowed buses (${plan.maxBuses}) for your current '${plan.displayName}'. Please upgrade your subscription.")
                    return false
                }
            }
            "ROUTE" -> {
                if (currentRoutesCount >= plan.maxRoutes) {
                    onFail("SaaS Limit Exceeded: You have reached the maximum allowed routes (${plan.maxRoutes}) for your current '${plan.displayName}'. Please upgrade your subscription.")
                    return false
                }
            }
            "TRIP" -> {
                if (currentTripsCount >= plan.maxTrips) {
                    onFail("SaaS Limit Exceeded: You have reached the maximum allowed trips (${plan.maxTrips}) for your current '${plan.displayName}'. Please upgrade your subscription.")
                    return false
                }
            }
        }
        return true
    }

    fun updateSchoolStatus(schoolId: Int, status: String) {
        viewModelScope.launch {
            try {
                repository.schoolDao.updateSchoolStatus(schoolId, status)
                if (_currentSchool.value?.id == schoolId) {
                    _currentSchool.value = repository.schoolDao.getSchoolByIdSync(schoolId)
                }
                
                // Add Audit Log
                val adminName = _currentUser.value?.fullName ?: "Super Admin"
                repository.insertAuditLog(
                    schoolId = schoolId,
                    actionType = "School Status Change",
                    details = "School status updated to '$status' by $adminName",
                    actorName = adminName,
                    actorRole = "SUPER_ADMIN"
                )
            } catch (e: Exception) {
                Log.e("SchoolTrackPro", "Failed to update school status: ${e.localizedMessage}")
            }
        }
    }

    fun deleteSchool(schoolId: Int) {
        viewModelScope.launch {
            try {
                repository.schoolDao.deleteSchool(schoolId)
                val adminName = _currentUser.value?.fullName ?: "Super Admin"
                repository.insertAuditLog(
                    schoolId = schoolId,
                    actionType = "School Deleted",
                    details = "School was deleted by $adminName",
                    actorName = adminName,
                    actorRole = "SUPER_ADMIN"
                )
            } catch (e: Exception) {
                Log.e("SchoolTrackPro", "Failed to delete school: ${e.localizedMessage}")
            }
        }
    }

    fun upgradeSubscription(schoolId: Int, plan: String, durationDays: Int = 30) {
        viewModelScope.launch {
            try {
                val currentExpiry = repository.schoolDao.getSchoolByIdSync(schoolId)?.subscriptionExpiryDate ?: System.currentTimeMillis()
                val newExpiry = maxOf(System.currentTimeMillis(), currentExpiry) + durationDays.toLong() * 24L * 60L * 60L * 1000L
                repository.schoolDao.updateSubscription(schoolId, plan, newExpiry)
                
                if (_currentSchool.value?.id == schoolId) {
                    _currentSchool.value = repository.schoolDao.getSchoolByIdSync(schoolId)
                }
                
                // Add Audit Log
                val actorName = _currentUser.value?.fullName ?: "Principal"
                val actorRole = _currentUser.value?.role ?: "PRINCIPAL"
                repository.insertAuditLog(
                    schoolId = schoolId,
                    actionType = "Subscription Plan Upgraded",
                    details = "Subscription updated to '$plan'. Expiry: ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(newExpiry)}",
                    actorName = actorName,
                    actorRole = actorRole
                )
            } catch (e: Exception) {
                Log.e("SchoolTrackPro", "Failed to update subscription: ${e.localizedMessage}")
            }
        }
    }

    fun sendGlobalAnnouncement(title: String, message: String) {
        viewModelScope.launch {
            try {
                val schoolList = schools.value
                for (sch in schoolList) {
                    repository.sendNotification(
                        NotificationEntity(
                            schoolId = sch.id,
                            title = "GLOBAL ANNOUNCEMENT: $title",
                            message = message,
                            senderRole = "SYSTEM"
                        )
                    )
                }
                val adminName = _currentUser.value?.fullName ?: "Super Admin"
                repository.insertAuditLog(
                    schoolId = -1,
                    actionType = "Global Announcement",
                    details = "Sent global announcement: '$title'",
                    actorName = adminName,
                    actorRole = "SUPER_ADMIN"
                )
            } catch (e: Exception) {
                Log.e("SchoolTrackPro", "Failed to send global announcement: ${e.localizedMessage}")
            }
        }
    }
}

data class SubscriptionPlan(
    val name: String,
    val displayName: String,
    val maxStudents: Int,
    val maxDrivers: Int,
    val maxBuses: Int,
    val maxParents: Int,
    val maxRoutes: Int,
    val maxTrips: Int,
    val cloudStorageLimitGb: Int,
    val gpsTracking: Boolean,
    val reports: Boolean,
    val premiumFeatures: Boolean
)
