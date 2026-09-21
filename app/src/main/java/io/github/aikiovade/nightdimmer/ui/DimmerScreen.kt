package io.github.aikiovade.nightdimmer.ui

import android.animation.ValueAnimator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aikiovade.nightdimmer.R
import io.github.aikiovade.nightdimmer.domain.model.DimmerSettings
import io.github.aikiovade.nightdimmer.domain.model.ScheduleWindow
import java.util.Locale

private val CardShape = RoundedCornerShape(24.dp)

/** Numbers that change under the user's finger must not make the text jump. */
private const val TABULAR_FIGURES = "tnum"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DimmerScreen(
    viewModel: DimmerViewModel,
    onRequestOverlayPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val effectiveDimLevel by viewModel.effectiveDimLevel.collectAsStateWithLifecycle()
    val overlayPermissionGranted by viewModel.overlayPermissionGranted.collectAsStateWithLifecycle()
    val exactAlarmsAllowed by viewModel.exactAlarmsAllowed.collectAsStateWithLifecycle()
    val startBlocked by viewModel.startBlocked.collectAsStateWithLifecycle()
    val lightSensorAvailable by viewModel.lightSensorAvailable.collectAsStateWithLifecycle()

    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showBlueLightDialog by rememberSaveable { mutableStateOf(false) }
    var showScheduleDialog by rememberSaveable { mutableStateOf(false) }

    val haptics = LocalHapticFeedback.current
    val motionEnabled = rememberMotionEnabled()
    val sliderDescription = stringResource(R.string.dim_level_slider)

    val dimLevel = if (settings.autoDimEnabled && settings.isEnabled) {
        effectiveDimLevel
    } else {
        settings.dimLevel
    }

    // Direct manipulation: while the finger is down the number follows the
    // slider exactly. Only programmatic changes (auto-dimming, restored
    // settings) get to animate.
    var isDragging by remember { mutableStateOf(false) }
    val displayedLevel by animateFloatAsState(
        targetValue = dimLevel,
        animationSpec = if (!motionEnabled || isDragging) {
            snap()
        } else {
            tween(durationMillis = 220, easing = FastOutSlowInEasing)
        },
        label = "dimLevel",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header(
            isEnabled = settings.isEnabled,
            onOpenSettings = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showSettingsDialog = true
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        DimLevelDisplay(
            dimLevel = displayedLevel,
            autoDimEnabled = settings.autoDimEnabled,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = dimLevel,
            onValueChange = {
                isDragging = true
                viewModel.setDimLevel(it, persist = false)
            },
            onValueChangeFinished = {
                isDragging = false
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.persistPendingChanges()
            },
            enabled = !settings.autoDimEnabled,
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = sliderDescription
                    stateDescription = formatPercent(dimLevel)
                },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SliderCaption(text = stringResource(R.string.dim_level_min))
            SliderCaption(text = stringResource(R.string.dim_level_max))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FeatureCard(
                title = stringResource(R.string.blue_light),
                icon = Icons.Default.Nightlight,
                isActive = settings.blueLightEnabled,
                status = stringResource(
                    if (settings.blueLightEnabled) R.string.enabled else R.string.disabled,
                ),
                motionEnabled = motionEnabled,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showBlueLightDialog = true
                },
                modifier = Modifier.weight(1f),
            )
            FeatureCard(
                title = stringResource(R.string.schedule),
                icon = Icons.Default.DateRange,
                isActive = settings.scheduleEnabled,
                status = scheduleStatus(settings),
                motionEnabled = motionEnabled,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showScheduleDialog = true
                },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ToggleCard(
            items = listOf(
                ToggleItem(
                    title = stringResource(R.string.enable_dimmer),
                    summary = stringResource(R.string.enable_dimmer_summary),
                    checked = settings.isEnabled,
                    onCheckedChange = { enable ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setDimmerEnabled(enable)
                        if (enable && !overlayPermissionGranted) onRequestOverlayPermission()
                    },
                ),
                ToggleItem(
                    title = stringResource(R.string.auto_dimming),
                    summary = stringResource(R.string.auto_dimming_summary),
                    checked = settings.autoDimEnabled,
                    onCheckedChange = { enabled ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setAutoDimEnabled(enabled)
                    },
                ),
            ),
        )

        WarningList(
            overlayPermissionGranted = overlayPermissionGranted,
            startBlocked = startBlocked,
            scheduleEnabled = settings.scheduleEnabled,
            exactAlarmsAllowed = exactAlarmsAllowed,
            autoDimEnabled = settings.autoDimEnabled,
            lightSensorAvailable = lightSensorAvailable,
            onRequestOverlayPermission = onRequestOverlayPermission,
            onOpenExactAlarmSettings = onOpenExactAlarmSettings,
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            deepBlackThemeEnabled = settings.deepBlackThemeEnabled,
            onDeepBlackThemeChange = viewModel::setDeepBlackThemeEnabled,
            onDismiss = { showSettingsDialog = false },
        )
    }

    if (showBlueLightDialog) {
        BlueLightDialog(
            settings = settings,
            onEnabledChange = viewModel::setBlueLightEnabled,
            onIntensityChange = { viewModel.setBlueLightIntensity(it, persist = false) },
            onIntensityChangeFinished = viewModel::persistPendingChanges,
            onDismiss = { showBlueLightDialog = false },
        )
    }

    if (showScheduleDialog) {
        ScheduleDialog(
            settings = settings,
            onScheduleEnabledChange = viewModel::setScheduleEnabled,
            onSave = viewModel::setSchedule,
            onDismiss = { showScheduleDialog = false },
        )
    }
}

/**
 * Animations are decoration, and decoration must yield to the system setting
 * for reduced motion (`Settings.Global.ANIMATOR_DURATION_SCALE`).
 */
@Composable
private fun rememberMotionEnabled(): Boolean = remember {
    runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(true)
}

private fun animationMillis(motionEnabled: Boolean, millis: Int): Int =
    if (motionEnabled) millis else 0

@Composable
private fun Header(isEnabled: Boolean, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(
                    if (isEnabled) R.string.screen_dimmer_active else R.string.screen_dimmer_inactive,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                .clickable(onClickLabel = stringResource(R.string.settings)) { onOpenSettings() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DimLevelDisplay(dimLevel: Float, autoDimEnabled: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        // Oversized ghost of the number for depth: purely decorative, so it is
        // hidden from accessibility services to avoid reading the value twice.
        Text(
            text = formatPercent(dimLevel),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-4).sp,
                fontFeatureSettings = TABULAR_FIGURES,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            ),
            maxLines = 1,
            modifier = Modifier
                .offset(y = (-16).dp)
                .clearAndSetSemantics { },
        )
        Text(
            text = formatPercent(dimLevel),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
                fontFeatureSettings = TABULAR_FIGURES,
                color = MaterialTheme.colorScheme.onBackground,
            ),
            maxLines = 1,
        )
    }
    Text(
        text = stringResource(
            if (autoDimEnabled) R.string.auto_dimming_active else R.string.current_dim_level,
        ),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SliderCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun FeatureCard(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    status: String,
    motionEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = tween(animationMillis(motionEnabled, PRESS_MILLIS), easing = FastOutSlowInEasing),
        label = "featureCardScale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        animationSpec = tween(animationMillis(motionEnabled, STATE_MILLIS)),
        label = "featureCardContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        animationSpec = tween(animationMillis(motionEnabled, STATE_MILLIS)),
        label = "featureCardContent",
    )
    val secondaryContentColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(animationMillis(motionEnabled, STATE_MILLIS)),
        label = "featureCardSecondaryContent",
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isActive) null else subtleBorder(),
        shape = CardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(32.dp),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFeatureSettings = TABULAR_FIGURES,
                    ),
                    color = secondaryContentColor,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ToggleCard(items: List<ToggleItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = subtleBorder(),
        shape = CardShape,
    ) {
        Column {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                ToggleRow(item = item)
            }
        }
    }
}

@Composable
private fun ToggleRow(item: ToggleItem) {
    // The whole row is the switch: a bigger target than the thumb alone, and
    // accessibility services announce one control instead of two.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = item.checked,
                role = Role.Switch,
                onValueChange = item.onCheckedChange,
            )
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Switch(
            checked = item.checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

/**
 * Surfaces sit on a near-black background, so a hairline border does the
 * separation work that shadows cannot do in a dark theme.
 */
@Composable
private fun subtleBorder(): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

@Composable
private fun WarningList(
    overlayPermissionGranted: Boolean,
    startBlocked: Boolean,
    scheduleEnabled: Boolean,
    exactAlarmsAllowed: Boolean,
    autoDimEnabled: Boolean,
    lightSensorAvailable: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    val warnings = buildList {
        if (!overlayPermissionGranted) {
            add(
                Warning(
                    text = stringResource(R.string.warning_overlay_permission),
                    actionLabel = stringResource(R.string.action_grant),
                    onAction = onRequestOverlayPermission,
                ),
            )
        }
        if (startBlocked) {
            add(Warning(text = stringResource(R.string.warning_start_blocked)))
        }
        if (scheduleEnabled && !exactAlarmsAllowed) {
            add(
                Warning(
                    text = stringResource(R.string.warning_exact_alarms),
                    actionLabel = stringResource(R.string.action_open_settings),
                    onAction = onOpenExactAlarmSettings,
                ),
            )
        }
        if (autoDimEnabled && !lightSensorAvailable) {
            add(Warning(text = stringResource(R.string.warning_no_light_sensor)))
        }
    }

    if (warnings.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        warnings.forEach { warning ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = warning.text,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                warning.actionLabel?.let { label ->
                    TextButton(onClick = warning.onAction ?: {}) { Text(label) }
                }
            }
        }
    }
}

private data class ToggleItem(
    val title: String,
    val summary: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)

private data class Warning(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

@Composable
private fun SettingsDialog(
    deepBlackThemeEnabled: Boolean,
    onDeepBlackThemeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = deepBlackThemeEnabled,
                        role = Role.Switch,
                        onValueChange = onDeepBlackThemeChange,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.deep_black_theme), modifier = Modifier.weight(1f))
                Switch(checked = deepBlackThemeEnabled, onCheckedChange = null)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun BlueLightDialog(
    settings: DimmerSettings,
    onEnabledChange: (Boolean) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onIntensityChangeFinished: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.blue_light_filter)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .toggleable(
                            value = settings.blueLightEnabled,
                            role = Role.Switch,
                            onValueChange = onEnabledChange,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.enable_filter), modifier = Modifier.weight(1f))
                    Switch(checked = settings.blueLightEnabled, onCheckedChange = null)
                }
                Text(
                    text = stringResource(
                        R.string.intensity,
                        (settings.blueLightIntensity * 100).toInt(),
                    ),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFeatureSettings = TABULAR_FIGURES,
                    ),
                )
                Slider(
                    value = settings.blueLightIntensity,
                    onValueChange = onIntensityChange,
                    onValueChangeFinished = onIntensityChangeFinished,
                    enabled = settings.blueLightEnabled,
                    valueRange = 0f..1f,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDialog(
    settings: DimmerSettings,
    onScheduleEnabledChange: (Boolean) -> Unit,
    onSave: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val window = remember(settings.scheduleStartMinutes, settings.scheduleEndMinutes) {
        ScheduleWindow(settings.scheduleStartMinutes, settings.scheduleEndMinutes)
    }
    val startState = rememberTimePickerState(
        initialHour = settings.scheduleStartMinutes / 60,
        initialMinute = settings.scheduleStartMinutes % 60,
        is24Hour = true,
    )
    val endState = rememberTimePickerState(
        initialHour = settings.scheduleEndMinutes / 60,
        initialMinute = settings.scheduleEndMinutes % 60,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedule)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = settings.scheduleEnabled,
                            role = Role.Switch,
                            onValueChange = onScheduleEnabledChange,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.enable_schedule), modifier = Modifier.weight(1f))
                    Switch(checked = settings.scheduleEnabled, onCheckedChange = null)
                }
                Text(
                    text = stringResource(R.string.start_time),
                    style = MaterialTheme.typography.labelLarge,
                )
                TimeInput(state = startState)
                Text(
                    text = stringResource(R.string.end_time),
                    style = MaterialTheme.typography.labelLarge,
                )
                TimeInput(state = endState)
                Text(
                    text = stringResource(R.string.schedule_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (window.startMinutes == window.endMinutes) {
                    Text(
                        text = stringResource(R.string.schedule_same_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        startState.hour * 60 + startState.minute,
                        endState.hour * 60 + endState.minute,
                    )
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun scheduleStatus(settings: DimmerSettings): String =
    if (settings.scheduleEnabled) {
        "${formatTime(settings.scheduleStartMinutes)} – ${formatTime(settings.scheduleEndMinutes)}"
    } else {
        stringResource(R.string.disabled)
    }

private fun formatPercent(level: Float): String = "${(level * 100).toInt()}%"

private fun formatTime(minutes: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60)

private const val PRESSED_SCALE = 0.97f
private const val PRESS_MILLIS = 140
private const val STATE_MILLIS = 180
