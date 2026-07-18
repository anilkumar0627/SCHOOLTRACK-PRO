package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.example.data.*
import com.example.ui.components.GlassCard
import com.example.ui.components.SchoolLogoImage
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
    viewModel: AppViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val schools by viewModel.schools.collectAsState()
    val allAudits by viewModel.allAuditLogs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Dashboard", "Schools Registry", "Global Broadcast", "Audit & GPS")

    // Subscription rates
    val planRates = mapOf(
        "FREE_TRIAL" to 0,
        "BASIC" to 49,
        "STANDARD" to 99,
        "PREMIUM" to 249,
        "ENTERPRISE" to 499
    )

    // Compute metrics
    val totalSchools = schools.size
    val activeSchools = schools.count { it.status == "ACTIVE" && it.subscriptionExpiryDate > System.currentTimeMillis() }
    val pendingSchools = schools.count { it.status == "PENDING" }
    val suspendedSchools = schools.count { it.status == "SUSPENDED" }
    val expiredSchools = schools.count { it.subscriptionExpiryDate <= System.currentTimeMillis() && it.status == "ACTIVE" }

    // Multi-tenant simulated counts
    val simulatedStudentsCount = totalSchools * 124
    val simulatedDriversCount = totalSchools * 8
    val simulatedParentsCount = totalSchools * 115
    val simulatedBusesCount = totalSchools * 6
    val runningBusesCount = if (totalSchools > 0) totalSchools * 3 + 1 else 0
    val offlineBusesCount = maxOf(0, simulatedBusesCount - runningBusesCount)

    val todayTrips = totalSchools * 12
    val completedTrips = (todayTrips * 0.85).toInt()
    val cancelledTrips = (todayTrips * 0.05).toInt()

    // Revenue computations
    val monthlyRevenue = schools.filter { it.status == "ACTIVE" }.sumOf { planRates[it.subscriptionPlan] ?: 0 }
    val yearlyRevenue = monthlyRevenue * 12
    val totalRevenue = monthlyRevenue * 4 // simulated cumulative revenue (4 months)
    val pendingPayments = pendingSchools * 99 // simulated pending verification billing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Enterprise Header
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SCHOOLTRACK PRO SaaS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Platform Super Admin System",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Text(
                        text = currentUser?.fullName ?: "Super Admin",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("super_admin_logout")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> SuperAdminMainDashboard(
                    totalSchools = totalSchools,
                    activeSchools = activeSchools,
                    pendingSchools = pendingSchools,
                    suspendedSchools = suspendedSchools,
                    expiredSchools = expiredSchools,
                    totalStudents = simulatedStudentsCount,
                    totalDrivers = simulatedDriversCount,
                    totalParents = simulatedParentsCount,
                    totalBuses = simulatedBusesCount,
                    runningBuses = runningBusesCount,
                    offlineBuses = offlineBusesCount,
                    todayTrips = todayTrips,
                    completedTrips = completedTrips,
                    cancelledTrips = cancelledTrips,
                    monthlyRevenue = monthlyRevenue,
                    yearlyRevenue = yearlyRevenue,
                    totalRevenue = totalRevenue,
                    pendingPayments = pendingPayments
                )
                1 -> SuperAdminSchoolsRegistry(
                    schools = schools,
                    viewModel = viewModel,
                    planRates = planRates
                )
                2 -> SuperAdminGlobalBroadcast(viewModel = viewModel)
                3 -> SuperAdminAuditAndGps(
                    allAudits = allAudits,
                    runningBuses = runningBusesCount,
                    offlineBuses = offlineBusesCount,
                    schoolsCount = totalSchools
                )
            }
        }
    }
}

@Composable
fun SuperAdminMainDashboard(
    totalSchools: Int,
    activeSchools: Int,
    pendingSchools: Int,
    suspendedSchools: Int,
    expiredSchools: Int,
    totalStudents: Int,
    totalDrivers: Int,
    totalParents: Int,
    totalBuses: Int,
    runningBuses: Int,
    offlineBuses: Int,
    todayTrips: Int,
    completedTrips: Int,
    cancelledTrips: Int,
    monthlyRevenue: Int,
    yearlyRevenue: Int,
    totalRevenue: Int,
    pendingPayments: Int
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Analytics Banners
        Text(
            text = "SaaS Analytics & KPI Summary",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RevenueKPI(
                title = "Monthly Revenue",
                value = "$$monthlyRevenue",
                color = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.MonetizationOn,
                modifier = Modifier.weight(1f)
            )
            RevenueKPI(
                title = "Total Revenue",
                value = "$$totalRevenue",
                color = Color(0xFF4CAF50),
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RevenueKPI(
                title = "Pending Payments",
                value = "$$pendingPayments",
                color = Color(0xFFFF9800),
                icon = Icons.Default.PendingActions,
                modifier = Modifier.weight(1f)
            )
            RevenueKPI(
                title = "Yearly Revenue",
                value = "$$yearlyRevenue",
                color = MaterialTheme.colorScheme.secondary,
                icon = Icons.Default.TrendingUp,
                modifier = Modifier.weight(1f)
            )
        }

        // Schools Status Grid
        Text(
            text = "Multi-School Registry Metrics",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(title = "Total", value = "$totalSchools", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            MetricCard(title = "Active", value = "$activeSchools", color = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
            MetricCard(title = "Pending", value = "$pendingSchools", color = Color(0xFFFF9800), modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(title = "Suspended", value = "$suspendedSchools", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            MetricCard(title = "Expired", value = "$expiredSchools", color = Color.Gray, modifier = Modifier.weight(1f))
        }

        // Fleet and Operations Summary
        Text(
            text = "Global Fleet & Telemetry Status",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Global Students", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalStudents", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("Global Drivers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalDrivers", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("Global Parents", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalParents", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Fleet Buses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalBuses", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Running Buses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$runningBuses", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                    Column {
                        Text("Offline Buses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$offlineBuses", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Today's Trips", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$todayTrips", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Completed Trips", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$completedTrips", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                    Column {
                        Text("Cancelled Trips", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$cancelledTrips", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RevenueKPI(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SuperAdminSchoolsRegistry(
    schools: List<SchoolEntity>,
    viewModel: AppViewModel,
    planRates: Map<String, Int>
) {
    var searchSchoolQuery by remember { mutableStateOf("") }
    val filteredSchools = schools.filter {
        it.name.contains(searchSchoolQuery, ignoreCase = true) ||
                it.principalName.contains(searchSchoolQuery, ignoreCase = true)
    }

    var selectedSchoolForPlanUpgrade by remember { mutableStateOf<SchoolEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchSchoolQuery,
            onValueChange = { searchSchoolQuery = it },
            label = { Text("Search Schools / Principals") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredSchools.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No schools found in registry.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSchools) { school ->
                    SchoolRegistrationRow(
                        school = school,
                        rate = planRates[school.subscriptionPlan] ?: 0,
                        onApprove = { viewModel.updateSchoolStatus(school.id, "ACTIVE") },
                        onReject = { viewModel.updateSchoolStatus(school.id, "REJECTED") },
                        onSuspend = { viewModel.updateSchoolStatus(school.id, "SUSPENDED") },
                        onReactivate = { viewModel.updateSchoolStatus(school.id, "ACTIVE") },
                        onDelete = { viewModel.deleteSchool(school.id) },
                        onUpgradePlan = { selectedSchoolForPlanUpgrade = school }
                    )
                }
            }
        }

        // Drop-down Dialog/Sheet to Change/Upgrade Plan
        if (selectedSchoolForPlanUpgrade != null) {
            val targetSchool = selectedSchoolForPlanUpgrade!!
            AlertDialog(
                onDismissRequest = { selectedSchoolForPlanUpgrade = null },
                title = { Text("Manage Subscription Plan") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("School: ${targetSchool.name}")
                        Text("Current Plan: ${targetSchool.subscriptionPlan}")
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Select New SaaS Subscription Plan:", fontWeight = FontWeight.SemiBold)

                        viewModel.SubscriptionPlans.forEach { plan ->
                            Button(
                                onClick = {
                                    viewModel.upgradeSubscription(targetSchool.id, plan.name, durationDays = 30)
                                    selectedSchoolForPlanUpgrade = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (targetSchool.subscriptionPlan == plan.name) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(plan.displayName)
                                    Text("$$${planRates[plan.name]}/mo")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedSchoolForPlanUpgrade = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SchoolRegistrationRow(
    school: SchoolEntity,
    rate: Int,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
    onUpgradePlan: () -> Unit
) {
    val expiryStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(school.subscriptionExpiryDate))
    val isExpired = school.subscriptionExpiryDate <= System.currentTimeMillis()

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SchoolLogoImage(logoUri = school.logoUri, schoolName = school.name, size = 44.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = school.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "Principal: ${school.principalName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (school.status) {
                                "ACTIVE" -> if (isExpired) Color.Gray.copy(alpha = 0.2f) else Color(0xFFE8F5E9)
                                "PENDING" -> Color(0xFFFFF3E0)
                                "SUSPENDED" -> Color(0xFFFFEBEE)
                                else -> Color.LightGray
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (school.status == "ACTIVE" && isExpired) "EXPIRED" else school.status,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = when (school.status) {
                            "ACTIVE" -> if (isExpired) Color.DarkGray else Color(0xFF2E7D32)
                            "PENDING" -> Color(0xFFE65100)
                            "SUSPENDED" -> Color(0xFFC62828)
                            else -> Color.DarkGray
                        }
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Plan & Billing", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${school.subscriptionPlan} ($$rate/mo)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Subscription Expiry", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(expiryStr, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
            }

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (school.status == "PENDING") {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", fontSize = 12.sp)
                    }
                }

                if (school.status == "ACTIVE") {
                    Button(
                        onClick = onSuspend,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Suspend", fontSize = 12.sp)
                    }
                }

                if (school.status == "SUSPENDED") {
                    Button(
                        onClick = onReactivate,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Activate", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onUpgradePlan,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Plan", fontSize = 12.sp)
                }

                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete School", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SuperAdminGlobalBroadcast(viewModel: AppViewModel) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var alertSent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Broadcast Global Announcement",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Send a push alert and administrative notification to every registered school, visible to all Principals, Drivers and Parents on the platform.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Announcement Title / Heading") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Detailed Notification Body") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )

        Button(
            onClick = {
                if (title.isNotEmpty() && message.isNotEmpty()) {
                    viewModel.sendGlobalAnnouncement(title, message)
                    title = ""
                    message = ""
                    alertSent = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Text("Broadcast Announcement")
            }
        }

        if (alertSent) {
            Text(
                text = "Success: Global announcement has been successfully broadcast to all registered school nodes.",
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun SuperAdminAuditAndGps(
    allAudits: List<AuditLogEntity>,
    runningBuses: Int,
    offlineBuses: Int,
    schoolsCount: Int
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Global Audit Log") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("GPS Device Monitoring") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            if (allAudits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No actions logged in the global audit repository.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allAudits.sortedByDescending { it.id }) { log ->
                        AuditLogRow(log = log)
                    }
                }
            }
        } else {
            GpsDeviceMonitoring(runningBuses = runningBuses, offlineBuses = offlineBuses, schoolsCount = schoolsCount)
        }
    }
}

@Composable
fun AuditLogRow(log: AuditLogEntity) {
    val dateStr = SimpleDateFormat("HH:mm:ss yyyy-MM-dd", Locale.getDefault()).format(Date(log.timestamp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.actionType.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = dateStr, fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = log.details, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "By: ${log.actorName} (${log.actorRole})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "School ID: ${if (log.schoolId == -1) "Global" else log.schoolId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun GpsDeviceMonitoring(
    runningBuses: Int,
    offlineBuses: Int,
    schoolsCount: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Active GPS Telemetry Stream", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ONLINE DEVICES", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                    Text("$runningBuses", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text("GPS Signal active", fontSize = 11.sp, color = Color.DarkGray)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OFFLINE DEVICES", fontSize = 10.sp, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                    Text("$offlineBuses", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    Text("No signal received", fontSize = 11.sp, color = Color.DarkGray)
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("GPS Frequency & Performance", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Standard GPS ping interval is configured at 6 seconds for accurate geofencing. Active buses report live speed (25 km/h) and real-time ETAs dynamically. Operating with custom Leaflet OSM layer mapping.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Average Ping Latency", fontSize = 12.sp)
                    Text("182ms", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SaaS Tenancy Scope", fontSize = 12.sp)
                    Text("$schoolsCount Database Scopes Isolated", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
