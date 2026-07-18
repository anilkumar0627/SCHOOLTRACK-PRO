package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import com.example.data.*
import com.example.ui.components.GlassCard
import com.example.ui.components.SchoolLogoImage
import com.example.ui.components.SchoolCoverImage
import com.example.ui.components.SchoolHeader
import com.example.ui.components.SchoolQRCodeCard
import com.example.ui.viewmodel.AppViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.utils.ImageUtils
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalDashboardScreen(
    viewModel: AppViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val school by viewModel.currentSchool.collectAsState()
    val principal by viewModel.currentUser.collectAsState()

    val usersList by viewModel.users.collectAsState()
    val studentsList by viewModel.students.collectAsState()
    val busesList by viewModel.buses.collectAsState()
    val routesList by viewModel.routes.collectAsState()
    val tripsList by viewModel.trips.collectAsState()
    val notificationsList by viewModel.notifications.collectAsState()
    val simulationLogs by viewModel.simulationLogs.collectAsState()

    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiSuggestion by viewModel.aiSuggestion.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Home, 1: Accounts, 2: Fleet & Routes, 3: AI Assistant

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SchoolLogoImage(
                            logoUri = school?.logoUri,
                            schoolName = school?.name ?: "School",
                            size = 48.dp,
                            modifier = Modifier.testTag("app_bar_school_logo")
                        )
                        Column {
                            Text(
                                text = school?.shortName?.ifEmpty { school?.name } ?: school?.name ?: "SchoolTrack Pro",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("app_bar_school_name")
                            )
                            Text(
                                text = "Principal: ${principal?.fullName ?: "Super Admin"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("logout_button")) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Overview") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("Directory") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Navigation, contentDescription = null) },
                    label = { Text("Transit") }
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    label = { Text("AI Insights") }
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                slideInHorizontally { width -> if (targetState > initialState) width else -width } togetherWith
                        slideOutHorizontally { width -> if (targetState > initialState) -width else width }
            },
            label = "tab_navigation"
        ) { tab ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (tab) {
                    0 -> PrincipalHomeTab(
                        viewModel = viewModel,
                        studentsList = studentsList,
                        busesList = busesList,
                        routesList = routesList,
                        notificationsList = notificationsList,
                        simulationLogs = simulationLogs
                    )
                    1 -> PrincipalDirectoryTab(
                        viewModel = viewModel,
                        usersList = usersList,
                        studentsList = studentsList
                    )
                    2 -> PrincipalFleetTab(
                        viewModel = viewModel,
                        busesList = busesList,
                        routesList = routesList,
                        tripsList = tripsList,
                        usersList = usersList
                    )
                    3 -> PrincipalAiTab(
                        viewModel = viewModel,
                        routesList = routesList,
                        studentsList = studentsList,
                        isAiLoading = isAiLoading,
                        aiSuggestion = aiSuggestion
                    )
                    4 -> PrincipalBrandingSettingsTab(
                        viewModel = viewModel,
                        school = school
                    )
                }
            }
        }
    }
}

// ==========================
// OVERVIEW HOME TAB
// ==========================
@Composable
fun PrincipalHomeTab(
    viewModel: AppViewModel,
    studentsList: List<StudentEntity>,
    busesList: List<BusEntity>,
    routesList: List<RouteEntity>,
    notificationsList: List<NotificationEntity>,
    simulationLogs: List<String>
) {
    val scrollState = rememberScrollState()
    var alertTitle by remember { mutableStateOf("") }
    var alertMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stat Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Students",
                value = studentsList.size.toString(),
                icon = Icons.Default.School,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Buses",
                value = busesList.size.toString(),
                icon = Icons.Default.Navigation,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Routes",
                value = routesList.size.toString(),
                icon = Icons.Default.AltRoute,
                modifier = Modifier.weight(1f)
            )
        }

        // Live GPS Simulation Controller
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Live GPS Simulation Controller", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Simulates active telemetry broadcasts from drivers, triggering dynamic geofencing alerts for parents on approach.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (busesList.isNotEmpty()) {
                    var selectedBusId by remember { mutableStateOf(busesList.first().id) }
                    val activeBus = busesList.find { it.id == selectedBusId }
                    val isTracking = activeBus?.gpsStatus == "ACTIVE"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Simulating: ${activeBus?.busNumber ?: "No Bus selected"}",
                            fontWeight = FontWeight.SemiBold
                        )

                        Button(
                            onClick = {
                                if (isTracking) {
                                    viewModel.stopBusTracking()
                                    viewModel.resetBusGps(selectedBusId)
                                } else {
                                    viewModel.startBusTracking(selectedBusId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (isTracking) "Stop Simulator" else "Start Simulator")
                        }
                    }
                } else {
                    Text("Register a Bus in the 'Transit' tab to enable simulation.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }

                // Mini Console Logs
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .padding(8.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(simulationLogs) { log ->
                            Text(log, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Broadcast Alert Form
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Broadcast Campus Alert", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = alertTitle,
                    onValueChange = { alertTitle = it },
                    label = { Text("Alert Title (e.g. Weather Delay)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = alertMsg,
                    onValueChange = { alertMsg = it },
                    label = { Text("Message details...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (alertTitle.isNotEmpty() && alertMsg.isNotEmpty()) {
                            viewModel.broadcastAlert(alertTitle, alertMsg)
                            alertTitle = ""
                            alertMsg = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Broadcast Notification")
                }
            }
        }

        // Display Notifications
        Text("Sent Notifications History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (notificationsList.isEmpty()) {
            Text("No alerts logged.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            notificationsList.take(5).forEach { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(alert.title, fontWeight = FontWeight.Bold)
                            Text(
                                text = "By ${alert.senderRole}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(alert.message, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==========================
// DIRECTORY TAB (ACCOUNTS / STUDENTS)
// ==========================
@Composable
fun PrincipalDirectoryTab(
    viewModel: AppViewModel,
    usersList: List<UserEntity>,
    studentsList: List<StudentEntity>
) {
    val routesList by viewModel.routes.collectAsState()
    val busesList by viewModel.buses.collectAsState()
    val tripsList by viewModel.trips.collectAsState()
    val stopsList by viewModel.stops.collectAsState()

    var section by remember { mutableStateOf(0) } // 0: Parents, 1: Drivers, 2: Students

    // Create account dialog inputs
    var showAddDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var inputPhone by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }

    // Student Dialog inputs
    var showStudentDialog by remember { mutableStateOf(false) }
    var studName by remember { mutableStateOf("") }
    var studRoll by remember { mutableStateOf("") }
    var studClass by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf(-1) }
    var pickupStop by remember { mutableStateOf("") }
    var dropStop by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = section) {
            Tab(selected = section == 0, onClick = { section = 0 }) {
                Text("Parents", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = section == 1, onClick = { section = 1 }) {
                Text("Drivers", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = section == 2, onClick = { section = 2 }) {
                Text("Students", modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (section) {
                    0 -> "Parent Accounts"
                    1 -> "Driver Accounts"
                    else -> "Student Profiles"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    if (section == 2) showStudentDialog = true else showAddDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add New")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (section == 0 || section == 1) {
                val targetRole = if (section == 0) "PARENT" else "DRIVER"
                val filteredUsers = usersList.filter { it.role == targetRole }

                items(filteredUsers) { account ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(account.fullName, fontWeight = FontWeight.Bold)
                                Text(account.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(account.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Switch(
                                    checked = account.isActive,
                                    onCheckedChange = { viewModel.changeAccountStatus(account.id, it) }
                                )
                                IconButton(
                                    onClick = { viewModel.deleteAccount(account.id) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            } else {
                items(studentsList) { student ->
                    val sBus = busesList.find { it.id == student.busId }
                    val sRoute = routesList.find { it.id == student.routeId }
                    val sTrip = tripsList.find { it.id == student.tripId }
                    val sPickupStop = stopsList.find { it.id == student.pickupStopId }
                    val sDropStop = stopsList.find { it.id == student.dropStopId }

                    Card(modifier = Modifier.fillMaxWidth().testTag("student_card_${student.id}")) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Class: ${student.className} | Roll: ${student.rollNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                if (sBus != null || sRoute != null) {
                                    Text(
                                        text = "Bus: ${sBus?.busNumber ?: "N/A"} | Route: ${sRoute?.routeName ?: "N/A"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                if (sTrip != null) {
                                    Text(
                                        text = "Trip: ${sTrip.tripName} (${sTrip.startTime})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = "Pickup Stop: ${sPickupStop?.stopName ?: student.pickupStop}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "Drop Stop: ${sDropStop?.stopName ?: student.dropStop}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (student.seatNumber != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = student.seatNumber!!,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteStudent(student.id) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue parent / driver creation
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (section == 0) "Create Parent Profile" else "Create Driver Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = inputName, onValueChange = { inputName = it }, label = { Text("Full Name") })
                    OutlinedTextField(value = inputEmail, onValueChange = { inputEmail = it }, label = { Text("Email Address") })
                    OutlinedTextField(value = inputPhone, onValueChange = { inputPhone = it }, label = { Text("Mobile Number") })
                    OutlinedTextField(value = inputPassword, onValueChange = { inputPassword = it }, label = { Text("Password") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotEmpty() && inputEmail.isNotEmpty() && inputPassword.isNotEmpty()) {
                            if (section == 0) {
                                viewModel.createParent(inputName, inputEmail, inputPassword, inputPhone)
                            } else {
                                viewModel.createDriver(inputName, inputEmail, inputPassword, inputPhone)
                            }
                            // Reset
                            inputName = ""
                            inputEmail = ""
                            inputPhone = ""
                            inputPassword = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal student creation
    if (showStudentDialog) {
        val parents = usersList.filter { it.role == "PARENT" }

        var selectedBusId by remember { mutableStateOf<Int?>(null) }
        var selectedRouteId by remember { mutableStateOf<Int?>(null) }
        var selectedTripId by remember { mutableStateOf<Int?>(null) }
        var selectedPickupStopId by remember { mutableStateOf<Int?>(null) }
        var selectedDropStopId by remember { mutableStateOf<Int?>(null) }
        var seatNumText by remember { mutableStateOf("") }

        val activePickupStop = stopsList.find { it.id == selectedPickupStopId }
        val activeDropStop = stopsList.find { it.id == selectedDropStopId }

        AlertDialog(
            onDismissRequest = { showStudentDialog = false },
            title = { Text("Create Student Profile") },
            text = {
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier.verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(value = studName, onValueChange = { studName = it }, label = { Text("Student Full Name") })
                    OutlinedTextField(value = studRoll, onValueChange = { studRoll = it }, label = { Text("Roll/Admission ID") })
                    OutlinedTextField(value = studClass, onValueChange = { studClass = it }, label = { Text("Class / Grade") })
                    
                    Text("Assign Parent Account:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (parents.isEmpty()) {
                        Text("Please create a parent account first.", color = MaterialTheme.colorScheme.error)
                    } else {
                        var expandedParents by remember { mutableStateOf(false) }
                        val activeParent = parents.find { it.id == selectedParentId }
                        Box {
                            OutlinedButton(onClick = { expandedParents = true }) {
                                Text(activeParent?.fullName ?: "Select Parent Account")
                            }
                            DropdownMenu(expanded = expandedParents, onDismissRequest = { expandedParents = false }) {
                                parents.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.fullName) },
                                        onClick = {
                                            selectedParentId = p.id
                                            expandedParents = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Assigned Bus
                    Text("Assign Bus Node:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    var expandedBuses by remember { mutableStateOf(false) }
                    val activeBus = busesList.find { it.id == selectedBusId }
                    Box {
                        OutlinedButton(onClick = { expandedBuses = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(activeBus?.busNumber ?: "Select Bus (Optional)")
                        }
                        DropdownMenu(expanded = expandedBuses, onDismissRequest = { expandedBuses = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { selectedBusId = null; expandedBuses = false })
                            busesList.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.busNumber) },
                                    onClick = { selectedBusId = b.id; expandedBuses = false }
                                )
                            }
                        }
                    }

                    // Assigned Route
                    Text("Assign Route Node:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    var expandedRoutes by remember { mutableStateOf(false) }
                    val activeRoute = routesList.find { it.id == selectedRouteId }
                    Box {
                        OutlinedButton(onClick = { expandedRoutes = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(activeRoute?.routeName ?: "Select Route (Optional)")
                        }
                        DropdownMenu(expanded = expandedRoutes, onDismissRequest = { expandedRoutes = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { selectedRouteId = null; expandedRoutes = false })
                            routesList.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.routeName) },
                                    onClick = { selectedRouteId = r.id; expandedRoutes = false }
                                )
                            }
                        }
                    }

                    // Assigned Trip (Multi-Trip Engine)
                    Text("Assign Daily Trip (Multi-Trip):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    var expandedTrips by remember { mutableStateOf(false) }
                    val activeTrip = tripsList.find { it.id == selectedTripId }
                    Box {
                        OutlinedButton(onClick = { expandedTrips = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(activeTrip?.tripName ?: "Select Trip (Optional)")
                        }
                        DropdownMenu(expanded = expandedTrips, onDismissRequest = { expandedTrips = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { selectedTripId = null; expandedTrips = false })
                            tripsList.filter { selectedBusId == null || it.busId == selectedBusId }.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("${t.tripName} (${t.startTime})") },
                                    onClick = { selectedTripId = t.id; expandedTrips = false }
                                )
                            }
                        }
                    }

                    // Pickup Stop
                    Text("Select Pickup Stop:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    var expandedPickupStops by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expandedPickupStops = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(activePickupStop?.stopName ?: "Select Pickup Stop (Optional)")
                        }
                        DropdownMenu(expanded = expandedPickupStops, onDismissRequest = { expandedPickupStops = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { selectedPickupStopId = null; expandedPickupStops = false })
                            stopsList.filter { selectedRouteId == null || it.routeId == selectedRouteId }.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.stopSequence}. ${s.stopName} (${s.arrivalTime})") },
                                    onClick = { selectedPickupStopId = s.id; expandedPickupStops = false }
                                )
                            }
                        }
                    }

                    // Drop Stop
                    Text("Select Drop Stop:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    var expandedDropStops by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expandedDropStops = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(activeDropStop?.stopName ?: "Select Drop Stop (Optional)")
                        }
                        DropdownMenu(expanded = expandedDropStops, onDismissRequest = { expandedDropStops = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { selectedDropStopId = null; expandedDropStops = false })
                            stopsList.filter { selectedRouteId == null || it.routeId == selectedRouteId }.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.stopSequence}. ${s.stopName} (${s.arrivalTime})") },
                                    onClick = { selectedDropStopId = s.id; expandedDropStops = false }
                                )
                            }
                        }
                    }

                    // Seat Number
                    OutlinedTextField(
                        value = seatNumText,
                        onValueChange = { seatNumText = it },
                        label = { Text("Seat Number (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (studName.isNotEmpty() && selectedParentId != -1) {
                            viewModel.createStudent(
                                fullName = studName,
                                rollNumber = studRoll,
                                className = studClass,
                                parentUserId = selectedParentId,
                                busId = selectedBusId,
                                routeId = selectedRouteId,
                                pickup = activePickupStop?.stopName ?: pickupStop.ifEmpty { "Main Point" },
                                drop = activeDropStop?.stopName ?: dropStop.ifEmpty { "Campus Gate" },
                                tripId = selectedTripId,
                                pickupStopId = selectedPickupStopId,
                                dropStopId = selectedDropStopId,
                                seatNumber = seatNumText.ifEmpty { null }
                            )
                            studName = ""
                            studRoll = ""
                            studClass = ""
                            selectedParentId = -1
                            pickupStop = ""
                            dropStop = ""
                            showStudentDialog = false
                        }
                    }
                ) {
                    Text("Register Student")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStudentDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ==========================
// FLEET & TRANSIT TAB
// ==========================
@Composable
fun PrincipalFleetTab(
    viewModel: AppViewModel,
    busesList: List<BusEntity>,
    routesList: List<RouteEntity>,
    tripsList: List<TripEntity>,
    usersList: List<UserEntity>
) {
    var subTab by remember { mutableStateOf(0) } // 0: Buses, 1: Routes, 2: Trips

    // Bus Dialog inputs
    var showBusDialog by remember { mutableStateOf(false) }
    var busNo by remember { mutableStateOf("") }
    var busReg by remember { mutableStateOf("") }
    var busCap by remember { mutableStateOf("40") }
    var selectedDriverId by remember { mutableStateOf(-1) }

    // Route Dialog inputs
    var showRouteDialog by remember { mutableStateOf(false) }
    var routeName by remember { mutableStateOf("") }
    var startPt by remember { mutableStateOf("") }
    var endPt by remember { mutableStateOf("") }
    var stop1Name by remember { mutableStateOf("") }
    var stop2Name by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }) {
                Text("Buses", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = subTab == 1, onClick = { subTab = 1 }) {
                Text("Routes", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = subTab == 2, onClick = { subTab = 2 }) {
                Text("Trips", modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (subTab) {
                    0 -> "Active Fleet"
                    1 -> "Service Routes"
                    else -> "Scheduled Journeys"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    if (subTab == 0) showBusDialog = true
                    if (subTab == 1) showRouteDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add New")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (subTab == 0) {
                items(busesList) { bus ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🚌", fontSize = 20.sp)
                                }
                                Column {
                                    Text(bus.busNumber, fontWeight = FontWeight.Bold)
                                    Text("Reg: ${bus.vehicleRegNumber} | Cap: ${bus.capacity}", fontSize = 12.sp)
                                    Text(
                                        text = "GPS Status: ${bus.gpsStatus}",
                                        fontSize = 11.sp,
                                        color = if (bus.gpsStatus == "ACTIVE") Color.Green else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteBus(bus.id) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            } else if (subTab == 1) {
                items(routesList) { route ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(route.routeName, fontWeight = FontWeight.Bold)
                                Text("Transit: ${route.startPoint} ➔ ${route.endPoint}", fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = { viewModel.deleteRoute(route.id) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            } else {
                items(tripsList) { trip ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(trip.tripName, fontWeight = FontWeight.Bold)
                                Text("Timings: ${trip.startTime} - ${trip.endTime}", fontSize = 12.sp)
                                Text("Status: ${trip.status}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }

                            IconButton(
                                onClick = { viewModel.deleteTrip(trip.id) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Bus Modal
    if (showBusDialog) {
        val drivers = usersList.filter { it.role == "DRIVER" }
        AlertDialog(
            onDismissRequest = { showBusDialog = false },
            title = { Text("Configure New Fleet Bus") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = busNo, onValueChange = { busNo = it }, label = { Text("Bus Number (e.g. Bus-05)") })
                    OutlinedTextField(value = busReg, onValueChange = { busReg = it }, label = { Text("Vehicle Registration ID") })
                    OutlinedTextField(
                        value = busCap,
                        onValueChange = { busCap = it },
                        label = { Text("Bus Seating Capacity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text("Assign Driver Profile:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (drivers.isEmpty()) {
                        Text("No drivers registered. Create a Driver in Directory first.", color = MaterialTheme.colorScheme.error)
                    } else {
                        var expandedDrivers by remember { mutableStateOf(false) }
                        val activeDriver = drivers.find { it.id == selectedDriverId }
                        Box {
                            OutlinedButton(onClick = { expandedDrivers = true }) {
                                Text(activeDriver?.fullName ?: "Select Driver Profile")
                            }
                            DropdownMenu(expanded = expandedDrivers, onDismissRequest = { expandedDrivers = false }) {
                                drivers.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d.fullName) },
                                        onClick = {
                                            selectedDriverId = d.id
                                            expandedDrivers = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (busNo.isNotEmpty() && busReg.isNotEmpty()) {
                            viewModel.createBus(
                                busNo,
                                busReg,
                                if (selectedDriverId != -1) selectedDriverId else null,
                                busCap.toIntOrNull() ?: 40,
                                null
                            )
                            busNo = ""
                            busReg = ""
                            selectedDriverId = -1
                            showBusDialog = false
                        }
                    }
                ) {
                    Text("Register Bus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBusDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Route Modal
    if (showRouteDialog) {
        AlertDialog(
            onDismissRequest = { showRouteDialog = false },
            title = { Text("Establish New Transit Route") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = routeName, onValueChange = { routeName = it }, label = { Text("Route Name") })
                    OutlinedTextField(value = startPt, onValueChange = { startPt = it }, label = { Text("Start Terminal / Location") })
                    OutlinedTextField(value = endPt, onValueChange = { endPt = it }, label = { Text("End Terminal") })
                    
                    Text("Pre-configure Stops (OSM Geo JSON):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = stop1Name, onValueChange = { stop1Name = it }, label = { Text("Stop 1 Name (e.g. Central Park)") })
                    OutlinedTextField(value = stop2Name, onValueChange = { stop2Name = it }, label = { Text("Stop 2 Name (e.g. Sunset Blvd)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routeName.isNotEmpty() && startPt.isNotEmpty() && endPt.isNotEmpty()) {
                            val stopsJson = """[
                                {"name": "${stop1Name.ifEmpty { "Stop A" }}", "lat": 37.7749, "lng": -122.4194},
                                {"name": "${stop2Name.ifEmpty { "Stop B" }}", "lat": 37.7649, "lng": -122.4294}
                            ]"""
                            viewModel.createRoute(routeName, startPt, endPt, stopsJson)
                            routeName = ""
                            startPt = ""
                            endPt = ""
                            stop1Name = ""
                            stop2Name = ""
                            showRouteDialog = false
                        }
                    }
                ) {
                    Text("Generate Route")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRouteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ==========================
// AI INSIGHTS HIGH THINKING TAB
// ==========================
@Composable
fun PrincipalAiTab(
    viewModel: AppViewModel,
    routesList: List<RouteEntity>,
    studentsList: List<StudentEntity>,
    isAiLoading: Boolean,
    aiSuggestion: String?
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Hero Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Text("Gemini Pro AI Insights Wizard", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "Evaluate school route variables using Deepmind's high thinking model. Designs safer passenger pickup sequences, calculates fuel-efficient layouts, and predicts weather proximity hazards.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (routesList.isEmpty()) {
            Text("Please configure a route in the Transit tab to enable optimization insights.", color = MaterialTheme.colorScheme.error)
        } else {
            var selectedRouteId by remember { mutableStateOf(routesList.first().id) }
            val activeRoute = routesList.find { it.id == selectedRouteId }

            Text("Select Route to Optimize:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(activeRoute?.routeName ?: "Select Route Profile")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    routesList.forEach { rt ->
                        DropdownMenuItem(
                            text = { Text(rt.routeName) },
                            onClick = {
                                selectedRouteId = rt.id
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (activeRoute != null) {
                        viewModel.optimizeRouteAi(
                            routeName = activeRoute.routeName,
                            stopsCount = 3,
                            studentsCount = studentsList.size
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAiLoading
            ) {
                if (isAiLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reasoning Level: HIGH thinking activated...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run AI Route Optimization")
                }
            }
        }

        // Show AI Suggestion Output
        if (aiSuggestion != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gemini Optimization Output", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Thinking Mode: High", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    Text(
                        text = aiSuggestion!!,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PrincipalBrandingSettingsTab(
    viewModel: AppViewModel,
    school: SchoolEntity?
) {
    if (school == null) return

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Textual Brand Identity
    var name by remember(school) { mutableStateOf(school.name) }
    var shortName by remember(school) { mutableStateOf(school.shortName) }
    var motto by remember(school) { mutableStateOf(school.motto ?: "") }
    
    // Core Visual Assets (Custom & Presets)
    var selectedLogo by remember(school) { mutableStateOf(school.logoUri) }
    var selectedCover by remember(school) { mutableStateOf(school.coverPhotoUri ?: "none") }

    // Advanced Profile Information
    var schoolCode by remember(school) { mutableStateOf(school.schoolCode ?: "") }
    var board by remember(school) { mutableStateOf(school.board ?: "") }
    var village by remember(school) { mutableStateOf(school.village ?: "") }
    var principalPhotoUri by remember(school) { mutableStateOf(school.principalPhotoUri ?: "") }
    var schoolPhone by remember(school) { mutableStateOf(school.schoolPhone ?: "") }
    var website by remember(school) { mutableStateOf(school.website ?: "") }
    var transportHelpline by remember(school) { mutableStateOf(school.transportHelpline ?: "") }
    var emergencyContact by remember(school) { mutableStateOf(school.emergencyContact ?: "") }

    // Theme Customizer Engine state
    var primaryColorHex by remember(school) { mutableStateOf(school.primaryColorHex) }
    var secondaryColorHex by remember(school) { mutableStateOf(school.secondaryColorHex) }
    var accentColorHex by remember(school) { mutableStateOf(school.accentColorHex) }
    var buttonStyle by remember(school) { mutableStateOf(school.buttonStyle) }
    var cardStyle by remember(school) { mutableStateOf(school.cardStyle) }
    var headerStyle by remember(school) { mutableStateOf(school.headerStyle) }
    var navigationStyle by remember(school) { mutableStateOf(school.navigationStyle) }
    var themeMode by remember(school) { mutableStateOf(school.themeMode) }

    // Cropper & Compressor custom config
    var compressionQuality by remember { mutableStateOf(85f) }
    var autoCropSquare by remember { mutableStateOf(true) }
    var transparentPNGSupport by remember { mutableStateOf(true) }
    var saveSuccess by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localPath = ImageUtils.processAndSaveImage(
                context = context,
                uri = it,
                fileName = "custom_logo_${school.id}.png",
                cropToSquare = autoCropSquare,
                compressQuality = compressionQuality.toInt()
            )
            if (localPath != null) {
                selectedLogo = localPath
                Toast.makeText(context, "Logo uploaded, cropped, and optimized successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localPath = ImageUtils.processAndSaveImage(
                context = context,
                uri = it,
                fileName = "custom_cover_${school.id}.png",
                cropToSquare = false,
                compressQuality = compressionQuality.toInt()
            )
            if (localPath != null) {
                selectedCover = localPath
                Toast.makeText(context, "Cover photo updated and compressed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.filesDir, "custom_logo_camera_${school.id}.png")
                val out = FileOutputStream(file)
                val format = if (transparentPNGSupport) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                bitmap.compress(format, compressionQuality.toInt(), out)
                out.flush()
                out.close()
                selectedLogo = Uri.fromFile(file).toString()
                Toast.makeText(context, "Camera capture saved and optimized!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Camera photo processing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val logos = listOf(
        "🏫" to "Classic Academy",
        "🍎" to "Early Years",
        "🎨" to "Creative Arts",
        "🔬" to "Science Lab",
        "🛡️" to "Shield Crest",
        "img_school_logo_academic_crest" to "Academic Crest",
        "img_school_logo_nature_shield" to "Nature Shield"
    )

    val covers = listOf(
        "img_school_cover_campus_view" to "Campus Gate",
        "sunset" to "Crimson Sunset",
        "aurora" to "Cosmic Aurora",
        "forest" to "Pine Forest",
        "none" to "Standard Solid Color"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "School Settings & Branding",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("settings_header")
        )

        Text(
            text = "Upgrade and customize your school portal node visual theme engine, official legal profiles, live brand identity system, and secure QR portal entry card.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Live App Branding Preview Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Live App Branding Preview",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val tempSchool = SchoolEntity(
                    id = school.id,
                    name = name,
                    shortName = shortName,
                    logoUri = selectedLogo,
                    coverPhotoUri = selectedCover,
                    motto = motto.ifEmpty { null },
                    address = school.address,
                    city = school.city,
                    district = school.district,
                    state = school.state,
                    country = school.country,
                    pinCode = school.pinCode,
                    mobileNumber = school.mobileNumber,
                    email = school.email,
                    principalName = school.principalName,
                    principalEmail = school.principalEmail,
                    schoolCode = schoolCode.ifEmpty { null },
                    board = board.ifEmpty { null },
                    village = village.ifEmpty { null },
                    principalPhotoUri = principalPhotoUri.ifEmpty { null },
                    schoolPhone = schoolPhone.ifEmpty { null },
                    website = website.ifEmpty { null },
                    transportHelpline = transportHelpline.ifEmpty { null },
                    emergencyContact = emergencyContact.ifEmpty { null },
                    primaryColorHex = primaryColorHex,
                    secondaryColorHex = secondaryColorHex,
                    accentColorHex = accentColorHex,
                    buttonStyle = buttonStyle,
                    cardStyle = cardStyle,
                    headerStyle = headerStyle,
                    navigationStyle = navigationStyle,
                    themeMode = themeMode
                )
                SchoolHeader(
                    school = tempSchool,
                    modifier = Modifier.testTag("settings_about_preview")
                )
            }
        }

        // Active QR Code card
        val qrTempSchool = SchoolEntity(
            id = school.id,
            name = name,
            shortName = shortName,
            logoUri = selectedLogo,
            coverPhotoUri = selectedCover,
            motto = motto.ifEmpty { null },
            address = school.address,
            city = school.city,
            district = school.district,
            state = school.state,
            country = school.country,
            pinCode = school.pinCode,
            mobileNumber = school.mobileNumber,
            email = school.email,
            principalName = school.principalName,
            principalEmail = school.principalEmail,
            schoolCode = schoolCode.ifEmpty { null }
        )
        SchoolQRCodeCard(school = qrTempSchool)

        // Custom Logo / Cover Image Uploader Workspace
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Upload Visual Assets Workspace",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { logoPickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).testTag("upload_logo_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload Logo", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { coverPickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).testTag("upload_cover_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload Cover", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f).testTag("camera_logo_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera Logo", fontSize = 11.sp)
                    }
                }

                // Image compressor controls
                Text("Auto Resizing & Compression Suite", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Compression Quality (${compressionQuality.toInt()}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Size Estimate: ${"%.2f".format(0.12 * (compressionQuality / 100f) * 2.1)} MB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = compressionQuality,
                        onValueChange = { compressionQuality = it },
                        valueRange = 50f..100f,
                        modifier = Modifier.testTag("compression_quality_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = autoCropSquare,
                                onCheckedChange = { autoCropSquare = it },
                                modifier = Modifier.testTag("autocrop_checkbox")
                            )
                            Text("Image Crop (1:1 Square)", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = transparentPNGSupport,
                                onCheckedChange = { transparentPNGSupport = it },
                                modifier = Modifier.testTag("png_transparent_checkbox")
                            )
                            Text("Transparent PNG Mode", fontSize = 11.sp)
                        }
                    }
                }

                Divider()

                // Logo Presets fallback list
                Text("Or select Preset Logos", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    logos.forEach { (logoValue, logoLabel) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedLogo == logoValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedLogo = logoValue }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SchoolLogoImage(logoUri = logoValue, schoolName = "School", size = 18.dp)
                                Text(
                                    text = logoLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedLogo == logoValue) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Cover presets fallback
                Text("Or select Preset Covers", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    covers.forEach { (coverValue, coverLabel) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedCover == coverValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedCover = coverValue }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = coverLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCover == coverValue) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Custom Theme Customizer Engine
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Theme Customization Engine",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Color Swatches / Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = primaryColorHex,
                        onValueChange = { primaryColorHex = it },
                        label = { Text("Primary (Hex)") },
                        modifier = Modifier.weight(1f).testTag("primary_color_input"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )

                    OutlinedTextField(
                        value = secondaryColorHex,
                        onValueChange = { secondaryColorHex = it },
                        label = { Text("Secondary (Hex)") },
                        modifier = Modifier.weight(1f).testTag("secondary_color_input"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )

                    OutlinedTextField(
                        value = accentColorHex,
                        onValueChange = { accentColorHex = it },
                        label = { Text("Accent (Hex)") },
                        modifier = Modifier.weight(1f).testTag("accent_color_input"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )
                }

                // Preset Swatches for fast custom themes
                Text("Quick Preset Brand Swatches", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        Triple("#3F51B5", "#303F9F", "#FF4081") to "Royal Navy",
                        Triple("#2E7D32", "#1B5E20", "#FFEB3B") to "Forest Gold",
                        Triple("#C62828", "#8E24AA", "#00E676") to "Crimson Spark",
                        Triple("#00838F", "#006064", "#00E5FF") to "Teal Cyber",
                        Triple("#E65100", "#FF6D00", "#2979FF") to "Amber Accent"
                    )
                    presets.forEach { (colors, name) ->
                        Button(
                            onClick = {
                                primaryColorHex = colors.first
                                secondaryColorHex = colors.second
                                accentColorHex = colors.third
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.testTag("preset_$name"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Button Style
                Text("Button Border Style", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val styles = listOf("ROUNDED", "SHARP", "CUT")
                    styles.forEach { style ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (buttonStyle == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { buttonStyle = style }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(style, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (buttonStyle == style) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Theme Mode Selector (Light, Dark, System)
                Text("Theme Dark/Light Mode Preference", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf("LIGHT", "DARK", "SYSTEM")
                    modes.forEach { mode ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (themeMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { themeMode = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (themeMode == mode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // School Legal & Profile Details
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "School Official Profile Registry",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = schoolCode,
                    onValueChange = { schoolCode = it },
                    label = { Text("School Unique Identification Code") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_school_code")
                )

                OutlinedTextField(
                    value = board,
                    onValueChange = { board = it },
                    label = { Text("Educational Board Affiliation (e.g., CBSE)") },
                    leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_board")
                )

                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    label = { Text("Village / Locality / Sub-area") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_village")
                )

                OutlinedTextField(
                    value = schoolPhone,
                    onValueChange = { schoolPhone = it },
                    label = { Text("Primary School Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_phone")
                )

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Official Website URL") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_website")
                )

                OutlinedTextField(
                    value = transportHelpline,
                    onValueChange = { transportHelpline = it },
                    label = { Text("Dedicated Transport Helpline") },
                    leadingIcon = { Icon(Icons.Default.SupportAgent, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_transport_helpline")
                )

                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("Emergency Response Direct Line") },
                    leadingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_emergency_contact")
                )
            }
        }

        // Core Textual Brand Identity
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Textual Brand Identity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("School Full Name (Required)") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("settings_full_name_input")
                )

                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("School Short Name (Required)") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("settings_short_name_input")
                )

                OutlinedTextField(
                    value = motto,
                    onValueChange = { motto = it },
                    label = { Text("School Motto / Tagline (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("settings_motto_input")
                )

                if (saveSuccess) {
                    Text(
                        text = "Branding & Theme engine options applied across school node successfully!",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.testTag("branding_success_msg")
                    )
                }

                Button(
                    onClick = {
                        if (name.isNotEmpty() && shortName.isNotEmpty()) {
                            viewModel.updateSchoolBranding(
                                schoolId = school.id,
                                fullName = name,
                                shortName = shortName,
                                logoUri = selectedLogo,
                                coverPhotoUri = selectedCover,
                                motto = motto.ifEmpty { null },
                                schoolCode = schoolCode,
                                board = board,
                                village = village,
                                principalPhotoUri = principalPhotoUri,
                                schoolPhone = schoolPhone,
                                website = website,
                                transportHelpline = transportHelpline,
                                emergencyContact = emergencyContact,
                                primaryColorHex = primaryColorHex,
                                secondaryColorHex = secondaryColorHex,
                                accentColorHex = accentColorHex,
                                buttonStyle = buttonStyle,
                                cardStyle = cardStyle,
                                headerStyle = headerStyle,
                                navigationStyle = navigationStyle,
                                themeMode = themeMode
                            )
                            saveSuccess = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_branding_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Apply Branding Studio")
                }
            }
        }

        // SaaS Subscription & Billing Engine
        val currentPlan = viewModel.getPlanForSchool(school)
        val isTrial = school.subscriptionPlan == "FREE_TRIAL"
        val expiryDate = java.util.Date(school.subscriptionExpiryDate)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val remainingDays = maxOf(0L, (school.subscriptionExpiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000))
        val isExpired = viewModel.isSubscriptionExpired(school)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardMembership,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SaaS Subscription & Billing Engine",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Manage your enterprise subscription, billing history and plan limits.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Plan KPI Highlights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURRENT PLAN", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(currentPlan.displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isExpired) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isExpired) "EXPIRED (READ-ONLY)" else if (isTrial) "FREE TRIAL STATUS" else "ACTIVE SUBSCRIPTION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("EXPIRY DATE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(sdf.format(expiryDate), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("REMAINING DAYS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("$remainingDays Days left", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (remainingDays < 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Plan limits table
                Text("Plan Resources & Limits", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LimitRow(label = "Max Allowed Students", limit = currentPlan.maxStudents)
                    LimitRow(label = "Max Allowed Drivers", limit = currentPlan.maxDrivers)
                    LimitRow(label = "Max Allowed Parents", limit = currentPlan.maxParents)
                    LimitRow(label = "Max Allowed Buses", limit = currentPlan.maxBuses)
                    LimitRow(label = "Max Allowed Routes", limit = currentPlan.maxRoutes)
                    LimitRow(label = "Max Allowed Trips", limit = currentPlan.maxTrips)
                    LimitRow(label = "Cloud Storage Allocation", value = "${currentPlan.cloudStorageLimitGb} GB")
                    FeatureRow(label = "GPS Tracking Support", enabled = currentPlan.gpsTracking)
                    FeatureRow(label = "Enterprise Reports", enabled = currentPlan.reports)
                    FeatureRow(label = "Premium Visual Extras", enabled = currentPlan.premiumFeatures)
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Actions: Upgrade Plan & Billing List
                var showUpgradeSheet by remember { mutableStateOf(false) }
                var showInvoicesSheet by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showUpgradeSheet = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upgrade/Renew", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showInvoicesSheet = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invoices", fontSize = 12.sp)
                    }
                }

                if (showUpgradeSheet) {
                    AlertDialog(
                        onDismissRequest = { showUpgradeSheet = false },
                        title = { Text("Choose Subscription Plan") },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                Text("Upgrade your plan to unlock more students, buses, and telemetry features instantly:")
                                viewModel.SubscriptionPlans.forEach { p ->
                                    val price = when(p.name) {
                                        "FREE_TRIAL" -> 0
                                        "BASIC" -> 49
                                        "STANDARD" -> 99
                                        "PREMIUM" -> 249
                                        "ENTERPRISE" -> 499
                                        else -> 0
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.upgradeSubscription(school.id, p.name, durationDays = 30)
                                            showUpgradeSheet = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (school.subscriptionPlan == p.name) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(p.displayName, fontWeight = FontWeight.Bold)
                                            Text("$$price/mo")
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showUpgradeSheet = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (showInvoicesSheet) {
                    val invoices = listOf(
                        Triple("2026-07-01", "INV-2026-004", "$99.00"),
                        Triple("2026-06-01", "INV-2026-003", "$99.00"),
                        Triple("2026-05-01", "INV-2026-002", "$99.00"),
                        Triple("2026-04-01", "INV-2026-001", "$49.00")
                    )
                    AlertDialog(
                        onDismissRequest = { showInvoicesSheet = false },
                        title = { Text("Invoice and Payment History") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Below is your record of completed billing transactions:")
                                invoices.forEach { inv ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(inv.second, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Date: ${inv.first}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(inv.third, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                                            IconButton(
                                                onClick = {
                                                    Toast.makeText(context, "Downloading PDF for ${inv.second}...", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = "Download Invoice", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showInvoicesSheet = false }) {
                                Text("Done")
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun LimitRow(label: String, limit: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (limit == Int.MAX_VALUE || limit >= 10000) "UNLIMITED" else "$limit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LimitRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FeatureRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(
            imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (enabled) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(16.dp)
        )
    }
}

