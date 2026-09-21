package io.github.aikiovade.nightdimmer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aikiovade.nightdimmer.ui.DimmerScreen
import io.github.aikiovade.nightdimmer.ui.DimmerViewModel
import io.github.aikiovade.nightdimmer.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = LocalContext.current.applicationContext as NightDimmerApp
            val viewModel: DimmerViewModel = viewModel(factory = DimmerViewModel.factory(app))
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            val overlayPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { viewModel.onScreenResumed() }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) viewModel.onScreenResumed()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // The persistent notification can only be posted once the user
            // allowed it, so it is requested the first time the dimmer runs.
            LaunchedEffect(settings.isEnabled) {
                if (settings.isEnabled) requestNotificationPermission(notificationPermissionLauncher::launch)
            }

            MyApplicationTheme(deepBlackThemeEnabled = settings.deepBlackThemeEnabled) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DimmerScreen(
                        viewModel = viewModel,
                        onRequestOverlayPermission = {
                            overlayPermissionLauncher.launch(overlayPermissionIntent())
                        },
                        onOpenExactAlarmSettings = { startActivity(exactAlarmSettingsIntent()) },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission(launch: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun overlayPermissionIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:$packageName".toUri(),
    )

    private fun exactAlarmSettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri())
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
        }
}
