package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DimmerController.init(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val deepBlackThemeEnabled by DimmerController.deepBlackThemeEnabled.collectAsStateWithLifecycle()
            MyApplicationTheme(deepBlackThemeEnabled = deepBlackThemeEnabled) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DimmerScreen(
                        modifier = Modifier.padding(innerPadding),
                        onToggle = { enable ->
                            if (enable) {
                                if (Settings.canDrawOverlays(this)) {
                                    DimmerController.toggleEnabled(true, context)
                                } else {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                                    startActivity(intent)
                                }
                            } else {
                                DimmerController.toggleEnabled(false, context)
                            }
                        },
                        checkOverlayPermission = {
                            Settings.canDrawOverlays(this)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DimmerScreen(modifier: Modifier = Modifier, onToggle: (Boolean) -> Unit, checkOverlayPermission: () -> Boolean) {
    val isEnabled by DimmerController.isEnabled.collectAsStateWithLifecycle()
    val dimLevel by DimmerController.dimLevel.collectAsStateWithLifecycle()
    val autoDimEnabled by DimmerController.autoDimEnabled.collectAsStateWithLifecycle()
    val autoDimLevel by DimmerController.autoDimLevel.collectAsStateWithLifecycle()
    val displayedDimLevel = if (autoDimEnabled) autoDimLevel else dimLevel
    val deepBlackThemeEnabled by DimmerController.deepBlackThemeEnabled.collectAsStateWithLifecycle()
    var showSettingsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header-like alignment
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Night Shift+",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isEnabled) "Screen Dimmer Active" else "Screen Dimmer Inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(24.dp))
                    .clickable { showSettingsDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Large display percentage
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "${(displayedDimLevel * 100).toInt()}%",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 112.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
                modifier = Modifier.offset(y = (-24).dp)
            )
            Text(
                text = "${(displayedDimLevel * 100).toInt()}%",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (autoDimEnabled) "AUTO DIMMING ACTIVE" else "CURRENT DIMMING LEVEL",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        Slider(
            value = displayedDimLevel,
            onValueChange = { DimmerController.dimLevel.value = it },
            enabled = !autoDimEnabled,
            valueRange = 0.0f..1.0f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "MIN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
            Text(text = "SUPER DARK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
        }

        Spacer(modifier = Modifier.weight(1f))

        val context = androidx.compose.ui.platform.LocalContext.current
        var showBlueLightDialog by remember { mutableStateOf(false) }
        var showScheduleDialog by remember { mutableStateOf(false) }
        val blueLightEnabled by DimmerController.blueLightEnabled.collectAsStateWithLifecycle()
        val scheduleEnabled by DimmerController.scheduleEnabled.collectAsStateWithLifecycle()
        val startHour by DimmerController.scheduleStartHour.collectAsStateWithLifecycle()
        val startMinute by DimmerController.scheduleStartMinute.collectAsStateWithLifecycle()
        val endHour by DimmerController.scheduleEndHour.collectAsStateWithLifecycle()
        val endMinute by DimmerController.scheduleEndMinute.collectAsStateWithLifecycle()
        val blueLightIntensity by DimmerController.blueLightIntensity.collectAsStateWithLifecycle()

        // Feature Grid
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Blue Light Card
            Card(
                onClick = { showBlueLightDialog = true },
                modifier = Modifier.weight(1f).aspectRatio(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (blueLightEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Nightlight,
                        contentDescription = "Blue Light",
                        tint = if (blueLightEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Blue Light",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (blueLightEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (blueLightEnabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (blueLightEnabled) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Schedule Card
            Card(
                onClick = { showScheduleDialog = true },
                modifier = Modifier.weight(1f).aspectRatio(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (scheduleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Schedule",
                        tint = if (scheduleEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Auto Schedule",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (scheduleEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (scheduleEnabled) "${String.format("%02d:%02d", startHour, startMinute)} - ${String.format("%02d:%02d", endHour, endMinute)}" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (scheduleEnabled) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Control Card (Main Switch)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Enable Dimmer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Dim below minimum brightness",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { onToggle(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto-Dimming",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Adjust based on ambient light",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoDimEnabled,
                        onCheckedChange = { DimmerController.toggleAutoDim(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!checkOverlayPermission()) {
            Text(
                text = "Overlay permission is required to dim the screen. Toggle the switch to grant it.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        // Dialogs
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Settings") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Deep Black Theme")
                            Switch(
                                checked = deepBlackThemeEnabled,
                                onCheckedChange = { DimmerController.toggleDeepBlackTheme(it) }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showBlueLightDialog) {
            AlertDialog(
                onDismissRequest = { showBlueLightDialog = false },
                title = { Text("Blue Light Filter") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Filter")
                            Switch(
                                checked = blueLightEnabled,
                                onCheckedChange = { DimmerController.toggleBlueLight(it) }
                            )
                        }
                        Text("Intensity: ${(blueLightIntensity * 100).toInt()}%")
                        Slider(
                            value = blueLightIntensity,
                            onValueChange = { DimmerController.setBlueLightIntensity(it) },
                            valueRange = 0.1f..0.9f
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBlueLightDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showScheduleDialog) {
            var tempStartHour by remember { mutableStateOf(startHour) }
            var tempStartMinute by remember { mutableStateOf(startMinute) }
            var tempEndHour by remember { mutableStateOf(endHour) }
            var tempEndMinute by remember { mutableStateOf(endMinute) }

            AlertDialog(
                onDismissRequest = { showScheduleDialog = false },
                title = { Text("Auto Schedule") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Schedule")
                            Switch(
                                checked = scheduleEnabled,
                                onCheckedChange = { DimmerController.toggleSchedule(it, context) }
                            )
                        }
                        
                        Text("Start Time", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            // Simple hour/minute selector (can be improved with actual TimePicker, but this works for now)
                            OutlinedButton(onClick = { tempStartHour = (tempStartHour + 1) % 24 }) { Text(String.format("%02d", tempStartHour)) }
                            Text(":", modifier = Modifier.align(Alignment.CenterVertically))
                            OutlinedButton(onClick = { tempStartMinute = (tempStartMinute + 15) % 60 }) { Text(String.format("%02d", tempStartMinute)) }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("End Time", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            OutlinedButton(onClick = { tempEndHour = (tempEndHour + 1) % 24 }) { Text(String.format("%02d", tempEndHour)) }
                            Text(":", modifier = Modifier.align(Alignment.CenterVertically))
                            OutlinedButton(onClick = { tempEndMinute = (tempEndMinute + 15) % 60 }) { Text(String.format("%02d", tempEndMinute)) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        DimmerController.setScheduleStart(tempStartHour, tempStartMinute, context)
                        DimmerController.setScheduleEnd(tempEndHour, tempEndMinute, context)
                        showScheduleDialog = false 
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showScheduleDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
