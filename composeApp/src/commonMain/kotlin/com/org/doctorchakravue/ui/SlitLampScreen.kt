package com.org.doctorchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.model.SlitLampImage
import com.org.doctorchakravue.ui.theme.DoctorBlue
import com.org.doctorchakravue.ui.theme.DoctorGreen
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * SlitLampScreen - Displays all slit lamp images from the collection.
 * Shows list view, tap to see detail with full image and notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlitLampScreen(
    onBack: () -> Unit = {}
) {
    val repository = remember { ApiRepository() }
    var images by remember { mutableStateOf<List<SlitLampImage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedImage by remember { mutableStateOf<SlitLampImage?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        try {
            images = repository.getAllSlitLampImages()
            println("Loaded ${images.size} slit lamp images")
        } catch (e: Exception) {
            error = "Failed to load images: ${e.message}"
            println("Error loading slit lamp images: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Detail dialog when image is selected
    if (selectedImage != null) {
        SlitLampDetailDialog(
            image = selectedImage!!,
            onDismiss = { selectedImage = null }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Slit Lamp Images", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DoctorGreen)
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, null, Modifier.size(80.dp), tint = Color.Red.copy(alpha = 0.5f))
                        Spacer(Modifier.height(24.dp))
                        Text("Error Loading Images", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Red)
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
            images.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.RemoveRedEye, null, Modifier.size(80.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(Modifier.height(24.dp))
                        Text("No Slit Lamp Images", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DoctorBlue)
                        Spacer(Modifier.height(8.dp))
                        Text("No slit lamp examination images found.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Header with count
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DoctorBlue.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RemoveRedEye, null, tint = DoctorBlue, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Slit Lamp Examinations", fontWeight = FontWeight.Bold, color = DoctorBlue)
                                    Text("${images.size} image(s) available", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // Image cards
                    items(images) { slitLampImage ->
                        SlitLampImageCard(
                            image = slitLampImage,
                            onClick = { selectedImage = slitLampImage }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
@Composable
private fun SlitLampImageCard(
    image: SlitLampImage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Image preview
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.1f))
            ) {
                // Decode and display Base64 image
                val imageBytes = remember(image.image) {
                    image.image?.let { base64String ->
                        try {
                            val cleanBase64 = if (base64String.contains(",")) base64String.substringAfter(",") else base64String
                            Base64.decode(cleanBase64)
                        } catch (e: Exception) { null }
                    }
                }

                if (imageBytes != null) {
                    AsyncImage(
                        model = imageBytes,
                        contentDescription = "Slit lamp image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    }
                }

                // Eye side badge
                image.eyeSide?.let { side ->
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .background(
                                color = when (side.lowercase()) {
                                    "left" -> Color(0xFF2196F3)
                                    "right" -> Color(0xFF4CAF50)
                                    else -> Color(0xFF9C27B0)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ).padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(side, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Patient name
            Text(
                text = image.patientName ?: "Unknown Patient",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Notes preview
            if (!image.notes.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = image.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.DarkGray
                )
            }

            Spacer(Modifier.height(8.dp))

            // Timestamp and doctor
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(formatTimestamp(image.timestamp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                if (!image.doctorName.isNullOrEmpty()) {
                    Text(image.doctorName, style = MaterialTheme.typography.bodySmall, color = DoctorBlue, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
@Composable
private fun SlitLampDetailDialog(
    image: SlitLampImage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RemoveRedEye, null, tint = DoctorBlue, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("${image.eyeSide ?: "Eye"} - Slit Lamp", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Full image
                Box(
                    modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black)
                ) {
                    val imageBytes = remember(image.image) {
                        image.image?.let { base64String ->
                            try {
                                val cleanBase64 = if (base64String.contains(",")) base64String.substringAfter(",") else base64String
                                Base64.decode(cleanBase64)
                            } catch (e: Exception) { null }
                        }
                    }

                    if (imageBytes != null) {
                        AsyncImage(
                            model = imageBytes,
                            contentDescription = "Slit lamp image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Failed to load image", color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Patient name
                Text("Patient", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text(image.patientName ?: "Unknown", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.height(12.dp))

                // Notes
                if (!image.notes.isNullOrEmpty()) {
                    Text("Notes", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(image.notes, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                }

                // Eye side
                Row {
                    Text("Eye Side: ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(
                        image.eyeSide ?: "Not specified",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = when (image.eyeSide?.lowercase()) {
                            "left" -> Color(0xFF2196F3)
                            "right" -> Color(0xFF4CAF50)
                            else -> Color(0xFF9C27B0)
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Timestamp
                Row {
                    Text("Captured: ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(formatTimestamp(image.timestamp), style = MaterialTheme.typography.bodyMedium)
                }

                // Doctor name
                if (!image.doctorName.isNullOrEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("By: ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(image.doctorName, style = MaterialTheme.typography.bodyMedium, color = DoctorBlue)
                    }
                }
            }
        }
    )
}

private fun formatTimestamp(timestamp: String?): String {
    if (timestamp.isNullOrEmpty()) return "Unknown date"
    return try {
        val parts = timestamp.split("T")
        if (parts.size >= 2) {
            val datePart = parts[0]
            val timePart = parts[1].substringBefore(".")
            val datePieces = datePart.split("-")
            val timePieces = timePart.split(":")
            if (datePieces.size >= 3 && timePieces.size >= 2) {
                val month = when (datePieces[1]) {
                    "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
                    "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
                    "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"
                    else -> datePieces[1]
                }
                "${datePieces[2]} $month ${datePieces[0]}, ${timePieces[0]}:${timePieces[1]}"
            } else timestamp
        } else timestamp
    } catch (e: Exception) { timestamp }
}
