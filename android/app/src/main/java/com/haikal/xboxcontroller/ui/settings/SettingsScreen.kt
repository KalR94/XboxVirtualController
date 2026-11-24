package com.haikal.xboxcontroller.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.haikal.xboxcontroller.ui.theme.TextPrimary
import com.haikal.xboxcontroller.ui.theme.TextSecondary
import com.haikal.xboxcontroller.ui.theme.XboxButton
import com.haikal.xboxcontroller.ui.theme.XboxGreen
import com.haikal.xboxcontroller.ui.theme.XboxGreenBright
import com.haikal.xboxcontroller.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()

    var host by remember { mutableStateOf(settings.bridgeHost) }
    var portText by remember { mutableStateOf(settings.bridgePort.toString()) }
    var leftSens by remember { mutableStateOf(settings.leftStickSensitivity) }
    var rightSens by remember { mutableStateOf(settings.rightStickSensitivity) }
    var triggerSens by remember { mutableStateOf(settings.triggerSensitivity) }
    var haptics by remember { mutableStateOf(settings.hapticsEnabled) }

    // Load once from persisted settings.
    LaunchedEffect(Unit) {
        host = settings.bridgeHost
        portText = settings.bridgePort.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )

        // Bridge address
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bridge address (USB-to-Windows via adb reverse)", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                singleLine = true,
            )
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter { c -> c.isDigit() } },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                singleLine = true,
            )
            XboxButton(
                text = "Save Bridge Address",
                onClick = {
                    val port = portText.toIntOrNull() ?: HomeViewModel.DEFAULT_PORT
                    viewModel.saveBridgeAddress(host, port)
                },
            )
        }

        // Sensitivities
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Analog sensitivity", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            SensitivitySlider("Left stick", leftSens) {
                leftSens = it
                viewModel.setStickSensitivities(leftSens, rightSens)
            }
            SensitivitySlider("Right stick", rightSens) {
                rightSens = it
                viewModel.setStickSensitivities(leftSens, rightSens)
            }
            SensitivitySlider("Triggers", triggerSens) {
                triggerSens = it
                viewModel.setTriggerSensitivity(triggerSens)
            }
        }

        // Haptics
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Haptic feedback on press", color = TextPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = haptics,
                onCheckedChange = {
                    haptics = it
                    viewModel.setHaptics(it)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = XboxGreen),
            )
        }

        // Local testing mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Local testing mode (debug log, no bridge)",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = settings.localTestingMode,
                onCheckedChange = { viewModel.setLocalTestingMode(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = XboxGreen),
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Back",
            color = XboxGreenBright,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(4.dp),
        )
        XboxButton(text = "Go Back", onClick = onBack)
    }
}

@Composable
private fun SensitivitySlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Text("$label: %.2fx".format(value), color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.5f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = XboxGreenBright,
                activeTrackColor = XboxGreen,
            ),
        )
    }
}
