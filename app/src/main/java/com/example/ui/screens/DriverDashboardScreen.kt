package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.example.ui.components.SchoolLogoImage
import com.example.ui.components.SchoolCoverImage
import com.example.ui.components.SchoolHeader
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(
    viewModel: AppViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val school by viewModel.currentSchool.collectAsState()
    val driverUser by viewModel.currentUser.collectAsState()

    val busesList by viewModel.buses.collectAsState()
    val routesList by viewModel.routes.collectAsState()
    val tripsList by viewModel.trips.collectAsState()
    val simulationLogs by viewModel.simulationLogs.collectAsState()

    // Find assigned bus for this driver
    val myBus = remember(busesList, driverUser) {
        busesList.find { it.driverUserId == (driverUser?.id ?: -1) } ?: busesList.firstOrNull()
    }

    val myRoute = remember(routesList, myBus) {
        routesList.find { it.id == myBus?.routeId } ?: routesList.firstOrNull()
    }

    val isTransmitting = myBus?.gpsStatus == "ACTIVE"
    val scrollState = rememberScrollState()

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
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Large Avatar representation
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("👨‍✈️", fontSize = 48.sp)
            }

            Text(
                text = "Welcome back, ${driverUser?.fullName ?: "Driver"}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (myBus != null) {
                // Large GPS Telemetry controller card (Frosted Glass!)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Telemetry Broadcast", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    text = if (isTransmitting) "GPS TRANSMITTING (LIVE)" else "GPS BROADCAST OFFLINE",
                                    fontSize = 11.sp,
                                    color = if (isTransmitting) Color.Green else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Switch(
                                checked = isTransmitting,
                                onCheckedChange = { active ->
                                    if (active) {
                                        viewModel.startBusTracking(myBus.id)
                                    } else {
                                        viewModel.stopBusTracking()
                                        viewModel.resetBusGps(myBus.id)
                                    }
                                }
                            )
                        }

                        Divider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Vehicle No", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(myBus.busNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("License Plate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(myBus.vehicleRegNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                // Today's Scheduled Trips (Multi-Trip Engine)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("trips_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Today's Trip Schedule",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                        
                        val myTrips = remember(tripsList, driverUser, myBus) {
                            tripsList.filter { it.driverUserId == (driverUser?.id ?: -1) || (it.busId == myBus.id) }
                                .sortedBy { it.startTime }
                        }
                        
                        val currentlyScheduledTrip = remember(myTrips) {
                            myTrips.filter { it.status == "Scheduled" }
                                .minByOrNull { it.startTime }
                        }

                        if (myTrips.isEmpty()) {
                            Text(
                                "No trips scheduled for today.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        } else {
                            myTrips.forEach { trip ->
                                val tripRoute = routesList.find { it.id == trip.routeId }
                                val isActive = trip.status == "Active"
                                val isCurrentlyScheduled = currentlyScheduledTrip?.id == trip.id
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("trip_item_${trip.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surface
                                    )
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (trip.tripType == "Pickup") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = trip.tripType,
                                                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                                )
                                                Column {
                                                    Text(
                                                        text = trip.tripName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = "Route: ${tripRoute?.routeName ?: "Unknown"} (${trip.tripType})",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            
                                            // Status Badge
                                            Surface(
                                                color = when (trip.status) {
                                                    "Active" -> MaterialTheme.colorScheme.primary
                                                    "Completed" -> MaterialTheme.colorScheme.tertiaryContainer
                                                    "Cancelled" -> MaterialTheme.colorScheme.errorContainer
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = trip.status,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    color = when (trip.status) {
                                                        "Active" -> MaterialTheme.colorScheme.onPrimary
                                                        "Completed" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                        "Cancelled" -> MaterialTheme.colorScheme.onErrorContainer
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Time: ${trip.startTime} - ${trip.endTime}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            // Action Buttons
                                            when (trip.status) {
                                                "Scheduled" -> {
                                                    if (isCurrentlyScheduled) {
                                                        Button(
                                                            onClick = {
                                                                viewModel.startTripForDriver(trip.id)
                                                            },
                                                            modifier = Modifier.testTag("start_trip_btn_${trip.id}").height(32.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.primary
                                                            )
                                                        ) {
                                                            Text("Start Trip", fontSize = 11.sp)
                                                        }
                                                    } else {
                                                        Text(
                                                            text = "Locked (In Queue)",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.error,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                "Active" -> {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Button(
                                                            onClick = {
                                                                viewModel.updateTripStatus(trip.id, "Completed")
                                                                viewModel.stopBusTracking()
                                                                viewModel.resetBusGps(myBus.id)
                                                            },
                                                            modifier = Modifier.testTag("complete_trip_btn_${trip.id}").height(32.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.tertiary
                                                            )
                                                        ) {
                                                            Text("Complete", fontSize = 11.sp)
                                                        }
                                                        Button(
                                                            onClick = {
                                                                viewModel.updateTripStatus(trip.id, "Cancelled")
                                                                viewModel.stopBusTracking()
                                                                viewModel.resetBusGps(myBus.id)
                                                            },
                                                            modifier = Modifier.testTag("cancel_trip_btn_${trip.id}").height(32.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.error
                                                            )
                                                        ) {
                                                            Text("Cancel", fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Mini simulated GPS telemetry output logs
                Text("Simulated Broadcast Stream", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(simulationLogs) { log ->
                            Text(log, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Text("No bus has been assigned to your driver account. Please contact your administrator.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // School branding - About School page!
            school?.let { sch ->
                Text("About My School", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
                SchoolHeader(
                    school = sch,
                    modifier = Modifier.testTag("driver_about_school")
                )
            }

            // Read-only settings safety notice card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Driver accounts are strictly managed. Drivers are prohibited from changing email addresses, passwords, or deleting accounts self-service. Contact school Principal for credential resets.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
