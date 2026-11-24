package com.haikal.xboxcontroller.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haikal.xboxcontroller.connection.ConnectionStatus
import com.haikal.xboxcontroller.console.ConsoleProfile
import com.haikal.xboxcontroller.ui.theme.CharcoalSurface
import com.haikal.xboxcontroller.ui.theme.ErrorRed
import com.haikal.xboxcontroller.ui.theme.TextPrimary
import com.haikal.xboxcontroller.ui.theme.TextSecondary
import com.haikal.xboxcontroller.ui.theme.WarningAmber
import com.haikal.xboxcontroller.ui.theme.XboxButton
import com.haikal.xboxcontroller.ui.theme.XboxGreen
import com.haikal.xboxcontroller.ui.theme.XboxGreenBright
import com.haikal.xboxcontroller.viewmodel.ControllerViewModel
import com.haikal.xboxcontroller.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenConsoleSelection: () -> Unit,
    onOpenController: () -> Unit,
    onOpenSettings: () -> Unit,
    controllerViewModel: ControllerViewModel? = null,
) {
    val settings by viewModel.settings.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Xbox Virtual Controller",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
        )
        Text(
            text = "Android virtual Xbox controller over a USB-to-Windows bridge (adb reverse).",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )

        StatusCard(settings.localTestingMode, selectedProfile)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            XboxButton(
                text = "Select Console (${selectedProfile?.displayName ?: "none"})",
                onClick = onOpenConsoleSelection,
                modifier = Modifier.fillMaxWidth(),
            )
            XboxButton(
                text = "Start Virtual Controller",
                onClick = onOpenController,
                modifier = Modifier.fillMaxWidth(),
                containerColor = XboxGreenBright,
            )
            XboxButton(
                text = "Settings",
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        LocalTestingRow(
            enabled = settings.localTestingMode,
            onToggle = { viewModel.setLocalTestingMode(it) },
        )
    }
}

@Composable
private fun StatusCard(localTesting: Boolean, profile: ConsoleProfile?) {
    val (statusText, statusColor) = when {
        localTesting -> "Local testing mode active — no bridge connection" to WarningAmber
        profile == null -> "No console selected" to TextSecondary
        profile.compatibilityStatus == ConsoleProfile.CompatibilityStatus.SUPPORTED ->
            "${profile.displayName}: supported via USB bridge" to XboxGreenBright
        profile.compatibilityStatus == ConsoleProfile.CompatibilityStatus.REQUIRES_HARDWARE ->
            "${profile.displayName}: requires hardware dongle for direct console output" to WarningAmber
        else -> "${profile.displayName}: not supported for direct console output" to ErrorRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalSurface, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Column {
            Text(
                text = profile?.notes ?: "Pick a target console to see compatibility.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = statusColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun LocalTestingRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Local testing mode (log input, no connection)",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = XboxGreen,
                checkedThumbColor = Color.White,
            ),
        )
    }
}
