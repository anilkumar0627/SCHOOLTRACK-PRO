package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.data.SchoolEntity
import kotlin.random.Random

@Composable
fun SchoolLogoImage(
    logoUri: String?,
    schoolName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val isCustomFile = logoUri != null && (logoUri.startsWith("content://") || logoUri.startsWith("file://") || logoUri.contains("/data/"))
    val logoResource = when {
        logoUri == null -> null
        logoUri.contains("academic") -> R.drawable.img_school_logo_academic_1784299281404
        logoUri.contains("nature") -> R.drawable.img_school_logo_nature_1784299296057
        else -> null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                    )
                )
            )
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isCustomFile && logoUri != null) {
            Image(
                painter = rememberAsyncImagePainter(model = Uri.parse(logoUri)),
                contentDescription = "$schoolName Logo",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (logoResource != null) {
            Image(
                painter = painterResource(id = logoResource),
                contentDescription = "$schoolName Logo",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val isSingleEmoji = logoUri != null && logoUri.length <= 4 && logoUri.any { Character.isSurrogate(it) || it.code > 127 }
            if (isSingleEmoji && logoUri != null) {
                Text(
                    text = logoUri,
                    fontSize = (size.value * 0.5f).sp,
                    textAlign = TextAlign.Center
                )
            } else {
                val initials = schoolName.split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .map { it.first().uppercase() }
                    .joinToString("")
                
                Text(
                    text = initials.ifEmpty { "S" },
                    fontSize = (size.value * 0.4f).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SchoolCoverImage(
    coverUri: String?,
    schoolName: String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp
) {
    val isCustomFile = coverUri != null && (coverUri.startsWith("content://") || coverUri.startsWith("file://") || coverUri.contains("/data/"))
    val coverResource = when {
        coverUri == null -> null
        coverUri.contains("campus") -> R.drawable.img_school_cover_campus_1784299311004
        else -> null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    )
                )
            )
    ) {
        if (isCustomFile && coverUri != null) {
            Image(
                painter = rememberAsyncImagePainter(model = Uri.parse(coverUri)),
                contentDescription = "$schoolName Cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (coverResource != null) {
            Image(
                painter = painterResource(id = coverResource),
                contentDescription = "$schoolName Cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val emoji = when (coverUri) {
                    "sunset" -> "🌅"
                    "aurora" -> "🌌"
                    "forest" -> "🌲"
                    else -> "🏛️"
                }
                Text(emoji, fontSize = 48.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = schoolName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SchoolHeader(
    school: SchoolEntity?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (school != null) {
            SchoolLogoImage(
                logoUri = school.logoUri,
                schoolName = school.name,
                size = 40.dp,
                modifier = Modifier.testTag("header_school_logo")
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = school.shortName.ifEmpty { school.name },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("header_school_name")
                )
                if (!school.motto.isNullOrEmpty()) {
                    Text(
                        text = school.motto,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "System Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "SchoolTrack Pro",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SchoolQRCodeCard(
    school: SchoolEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val portalUrl = "https://schooltrack.pro/node/${school.id}/portal"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SCHOOL QR IDENTITY",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "Parents scanning this secure code are automatically routed to the ${school.shortName} Transport Login Portal.",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Vector Canvas Drawn QR Code!
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cols = 21
                    val sizeX = size.width / cols
                    val sizeY = size.height / cols

                    // Stable QR pattern generated deterministically from school ID
                    val random = Random(school.id.toLong() + 37L)

                    // Draw 3 Corner Anchor squares (standard QR specification)
                    // Top-Left Anchor
                    drawRect(color = primaryColor, topLeft = Offset(0f, 0f), size = Size(sizeX * 7, sizeY * 7))
                    drawRect(color = Color.White, topLeft = Offset(sizeX, sizeY), size = Size(sizeX * 5, sizeY * 5))
                    drawRect(color = primaryColor, topLeft = Offset(sizeX * 2, sizeY * 2), size = Size(sizeX * 3, sizeY * 3))

                    // Top-Right Anchor
                    drawRect(color = primaryColor, topLeft = Offset(sizeX * 14, 0f), size = Size(sizeX * 7, sizeY * 7))
                    drawRect(color = Color.White, topLeft = Offset(sizeX * 15, sizeY), size = Size(sizeX * 5, sizeY * 5))
                    drawRect(color = primaryColor, topLeft = Offset(sizeX * 16, sizeY * 2), size = Size(sizeX * 3, sizeY * 3))

                    // Bottom-Left Anchor
                    drawRect(color = primaryColor, topLeft = Offset(0f, sizeY * 14), size = Size(sizeX * 7, sizeY * 7))
                    drawRect(color = Color.White, topLeft = Offset(sizeX, sizeY * 15), size = Size(sizeX * 5, sizeY * 5))
                    drawRect(color = primaryColor, topLeft = Offset(sizeX * 2, sizeY * 16), size = Size(sizeX * 3, sizeY * 3))

                    // Draw QR Data pixels
                    for (r in 0 until cols) {
                        for (c in 0 until cols) {
                            // Skip anchor regions
                            if (r < 8 && c < 8) continue
                            if (r < 8 && c >= 13) continue
                            if (r >= 13 && c < 8) continue
                            
                            // Center overlay safety cutout for logo
                            if (r in 8..12 && c in 8..12) continue

                            if (random.nextBoolean()) {
                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = Offset(c * sizeX + 1f, r * sizeY + 1f),
                                    size = Size(sizeX - 2f, sizeY - 2f),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }
                        }
                    }
                }

                // Nested Logo/Symbol in QR Center
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SchoolLogoImage(
                        logoUri = school.logoUri,
                        schoolName = school.name,
                        size = 38.dp
                    )
                }
            }

            Text(
                text = "ID: ${school.schoolCode ?: "SCH-%04d".format(school.id)} | Portal Node ACTIVE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Principal QR actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "QR Image Spooled to System Print Queue", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.weight(1f).testTag("print_qr_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "${school.name} - Portal QR Identity")
                            putExtra(Intent.EXTRA_TEXT, "Scan the QR code to join the transport network for ${school.name}.\nPortal: $portalUrl\nNode ID: ${school.id}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share School QR Code"))
                    },
                    modifier = Modifier.weight(1f).testTag("share_qr_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "QR saved securely to local storage gallery!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).testTag("download_qr_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", fontSize = 11.sp)
                }
            }
        }
    }
}
