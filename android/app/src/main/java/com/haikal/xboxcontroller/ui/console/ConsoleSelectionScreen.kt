package com.haikal.xboxcontroller.ui.console

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haikal.xboxcontroller.console.ConsoleProfile
import com.haikal.xboxcontroller.ui.theme.CharcoalSurface
import com.haikal.xboxcontroller.ui.theme.ErrorRed
import com.haikal.xboxcontroller.ui.theme.TextPrimary
import com.haikal.xboxcontroller.ui.theme.TextSecondary
import com.haikal.xboxcontroller.ui.theme.WarningAmber
import com.haikal.xboxcontroller.ui.theme.XboxGreen
import com.haikal.xboxcontroller.ui.theme.XboxGreenBright
import com.haikal.xboxcontroller.viewmodel.HomeViewModel

@Composable
fun ConsoleSelectionScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
) {
    val selected by viewModel.selectedProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Console Selection",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        Text(
            text = "USB-to-Windows bridge (adb reverse): supported for all profiles below. " +
                "Output directly to a physical Xbox console is not supported by this app.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ConsoleProfile.entries.toList()) { profile ->
                ConsoleCard(
                    profile = profile,
                    selected = selected?.wireName == profile.wireName,
                    onClick = {
                        viewModel.setConsole(profile)
                    },
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Back",
                    color = XboxGreenBright,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun ConsoleCard(
    profile: ConsoleProfile,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        selected -> XboxGreenBright
        profile.compatibilityStatus == ConsoleProfile.CompatibilityStatus.SUPPORTED -> XboxGreen
        profile.compatibilityStatus == ConsoleProfile.CompatibilityStatus.REQUIRES_HARDWARE -> WarningAmber
        else -> ErrorRed
    }
    val statusColor: Color = borderColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalSurface, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .background(borderColor, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = profile.generation.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = profile.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    text = profile.compatibilityStatus.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Text("✓", color = XboxGreenBright, fontSize = MaterialTheme.typography.titleLarge.fontSize)
            }
        }
    }
}
