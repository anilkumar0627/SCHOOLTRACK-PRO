package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.GlassCard
import com.example.ui.components.OSMMap
import com.example.ui.components.SchoolLogoImage
import com.example.ui.components.SchoolCoverImage
import com.example.ui.components.SchoolHeader
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    viewModel: AppViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val school by viewModel.currentSchool.collectAsState()
    val parentUser by viewModel.currentUser.collectAsState()

    val studentsList by viewModel.students.collectAsState()
    val busesList by viewModel.buses.collectAsState()
    val routesList by viewModel.routes.collectAsState()
    val tripsList by viewModel.trips.collectAsState()
    val stopsList by viewModel.stops.collectAsState()
    val alertsList by viewModel.notifications.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Live Tracker Map, 1: Bus & Driver, 2: Alerts, 3: Profile

    // Find children linked to this parent profile
    val myChildren = remember(studentsList, parentUser) {
        studentsList.filter { it.parentUserId == (parentUser?.id ?: -1) }
    }
    var selectedChildIndex by remember { mutableStateOf(0) }
    val activeChild = myChildren.getOrNull(selectedChildIndex)

    // Find assigned bus for selected child
    val assignedBus = remember(busesList, activeChild) {
        // Fallback to first active simulated bus if none is linked, ensuring elegant first-launch demo
        busesList.find { it.id == activeChild?.busId || it.routeId == activeChild?.routeId } 
            ?: busesList.firstOrNull { it.gpsStatus == "ACTIVE" }
            ?: busesList.firstOrNull()
    }

    val assignedRoute = remember(routesList, assignedBus, activeChild) {
        routesList.find { it.id == activeChild?.routeId || it.id == assignedBus?.routeId } ?: routesList.firstOrNull()
    }

    val assignedTrip = remember(tripsList, activeChild) {
        tripsList.find { it.id == activeChild?.tripId }
    }

    val pickupStop = remember(stopsList, activeChild) {
        stopsList.find { it.id == activeChild?.pickupStopId }
    }

    val dropStop = remember(stopsList, activeChild) {
        stopsList.find { it.id == activeChild?.dropStopId }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SchoolLogoImage(
                            logoUri = school?.logoUri,
                            schoolName = school?.name ?: "School",
                            size = 36.dp,
                            modifier = Modifier.testTag("app_bar_school_logo")
                        )
                        Text(
                            text = school?.shortName?.ifEmpty { school?.name } ?: school?.name ?: "SchoolTrack Pro",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("app_bar_school_name")
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("logout_button")) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("Tracker") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Navigation, contentDescription = null) },
                    label = { Text("Bus Info") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                    label = { Text("Alerts") }
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    label = { Text("Profile") }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                0 -> ParentTrackerTab(
                    viewModel = viewModel,
                    children = myChildren,
                    selectedIndex = selectedChildIndex,
                    onChildSelected = { selectedChildIndex = it },
                    assignedBus = assignedBus,
                    assignedRoute = assignedRoute,
                    assignedTrip = assignedTrip,
                    pickupStop = pickupStop,
                    dropStop = dropStop,
                    schoolName = school?.name ?: "Campus Base"
                )
                1 -> ParentBusInfoTab(
                    assignedBus = assignedBus,
                    assignedRoute = assignedRoute,
                    assignedTrip = assignedTrip,
                    pickupStop = pickupStop,
                    dropStop = dropStop,
                    activeChild = activeChild,
                    viewModel = viewModel
                )
                2 -> ParentAlertsTab(
                    alerts = alertsList
                )
                3 -> ParentProfileTab(
                    parentUser = parentUser,
                    school = school,
                    children = myChildren
                )
            }
        }
    }
}

// ==========================
// PARENT LIVE MAP TRACKER
// ==========================
@Composable
fun ParentTrackerTab(
    viewModel: AppViewModel,
    children: List<StudentEntity>,
    selectedIndex: Int,
    onChildSelected: (Int) -> Unit,
    assignedBus: BusEntity?,
    assignedRoute: RouteEntity?,
    assignedTrip: TripEntity?,
    pickupStop: StopEntity?,
    dropStop: StopEntity?,
    schoolName: String
) {
    val activeChild = children.getOrNull(selectedIndex)

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. OpenStreetMap Leaflet Engine
        if (assignedBus != null) {
            OSMMap(
                busLatitude = assignedBus.latitude,
                busLongitude = assignedBus.longitude,
                busAngle = 0f,
                busName = assignedBus.busNumber,
                schoolLatitude = 37.7850,
                schoolLongitude = -122.4100,
                schoolName = schoolName,
                waypointsJson = assignedRoute?.waypointsJson ?: "[]",
                isDarkMode = false // Light Map tile matches Light mode
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Map offline. Waiting for active simulated GPS feed from driver...", textAlign = TextAlign.Center)
            }
        }

        // Overlapping Child Selector Panel (Glassmorphism!)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (children.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    children.forEachIndexed { idx, child ->
                        val isSelected = idx == selectedIndex
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onChildSelected(idx) }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = child.fullName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text("Class: ${child.className}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Real-Time Distance & ETA Overlay Panel (Glassmorphic HUD)
            if (assignedBus != null) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Active Fleet: ${assignedBus.busNumber}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Speed: ${assignedBus.speed} km/h", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("ETA", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${assignedBus.etaMinutes} mins", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        // Assigned Trip & Stops Info
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Trip: ${assignedTrip?.tripName ?: "Active Route"}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Pickup Stop: ${pickupStop?.stopName ?: activeChild?.pickupStop ?: "Not Assigned"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Drop Stop: ${dropStop?.stopName ?: activeChild?.dropStop ?: "Not Assigned"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (activeChild?.seatNumber != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = activeChild.seatNumber!!,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Simulation Running Alert
        if (assignedBus?.gpsStatus != "ACTIVE") {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Simulation Idle. Ask Principal to trigger 'Start Simulator' on Overview to test Live OSM tracking.", fontSize = 12.sp)
                }
            }
        }
    }
}

// ==========================
// BUS BRANDING AND DETAILS TAB
// ==========================
@Composable
fun ParentBusInfoTab(
    assignedBus: BusEntity?,
    assignedRoute: RouteEntity?,
    assignedTrip: TripEntity?,
    pickupStop: StopEntity?,
    dropStop: StopEntity?,
    activeChild: StudentEntity?,
    viewModel: AppViewModel
) {
    val scrollState = rememberScrollState()
    val usersList by viewModel.users.collectAsState()

    val assignedDriver = remember(usersList, assignedBus) {
        usersList.find { it.id == assignedBus?.driverUserId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Your Child's Bus Assignment", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        if (assignedBus != null) {
            // Bus Card (Branded with photo placeholder!)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Decorative Bus Banner representation (High end illustration preset)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚌", fontSize = 72.sp)
                    }

                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(assignedBus.busNumber, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Capacity: ${assignedBus.capacity}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("Registration Number: ${assignedBus.vehicleRegNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Divider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👨‍✈️", fontSize = 24.sp)
                            }

                            Column {
                                Text(assignedDriver?.fullName ?: "Driver Not Assigned", fontWeight = FontWeight.SemiBold)
                                Text("Contact: ${assignedDriver?.phone ?: "No phone listed"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Route Stops
            Text("Assigned Transit Schedule", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("transit_schedule_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Trip: ${assignedTrip?.tripName ?: "Regular School Commute"}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                    
                    Text("Trip Service Type: ${assignedTrip?.tripType ?: "Pickup & Drop"}", fontSize = 13.sp)
                    Text("Daily Slot: ${assignedTrip?.startTime ?: "07:30 AM"} - ${assignedTrip?.endTime ?: "08:15 AM"}", fontSize = 13.sp)
                    Text("Status: ${assignedTrip?.status ?: "Scheduled"}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    
                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Designated Pickup Stop", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(pickupStop?.stopName ?: activeChild?.pickupStop ?: "Main Gate/Assigned Point", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Area/Village: ${pickupStop?.villageArea ?: "Campus Neighborhood"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Arrival Window", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(pickupStop?.arrivalTime ?: "07:30 AM", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Designated Drop Stop", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dropStop?.stopName ?: activeChild?.dropStop ?: "Main Gate/Assigned Point", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Area/Village: ${dropStop?.villageArea ?: "Campus Neighborhood"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Arrival Window", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dropStop?.arrivalTime ?: "03:45 PM", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (activeChild?.seatNumber != null) {
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reserved Cabin Seat", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = activeChild.seatNumber!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text("No active bus is assigned currently. Please contact the administrator.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==========================
// ALERTS TAB
// ==========================
@Composable
fun ParentAlertsTab(
    alerts: List<NotificationEntity>
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Campus Alert Broadcast Feed", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No announcements at this moment.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alerts) { alert ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(alert.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "System",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(alert.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

// ==========================
// READ-ONLY PROFILE SETTINGS
// ==========================
@Composable
fun ParentProfileTab(
    parentUser: UserEntity?,
    school: SchoolEntity?,
    children: List<StudentEntity>
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Large Profile Avatar
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("👩", fontSize = 54.sp)
        }

        Text(parentUser?.fullName ?: "Parent Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Email: ${parentUser?.email}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Tenant Node: School #${school?.id ?: "00"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

        school?.let { sch ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "About My School",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            SchoolHeader(
                school = sch,
                modifier = Modifier.testTag("parent_about_school")
            )
        }

        Divider()

        // Read-only info warning card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Enterprise-grade safety protocol. Parent credentials can only be reconfigured or deleted by your School Principal (Super Administrator).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Child Details
        Text("Registered Children Records", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
        children.forEach { child ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(child.fullName, fontWeight = FontWeight.Bold)
                    Text("Admission ID: ${child.rollNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Class Group: ${child.className}", fontSize = 12.sp)
                }
            }
        }
    }
}
