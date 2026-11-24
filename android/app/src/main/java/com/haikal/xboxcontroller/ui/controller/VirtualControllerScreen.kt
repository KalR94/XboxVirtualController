package com.haikal.xboxcontroller.ui.controller

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haikal.xboxcontroller.connection.Button
import com.haikal.xboxcontroller.connection.ConnectionStatus
import com.haikal.xboxcontroller.connection.ControllerInputState
import com.haikal.xboxcontroller.ui.theme.CharcoalDark
import com.haikal.xboxcontroller.ui.theme.CharcoalElevated
import com.haikal.xboxcontroller.ui.theme.CharcoalSurface
import com.haikal.xboxcontroller.ui.theme.ErrorRed
import com.haikal.xboxcontroller.ui.theme.TextPrimary
import com.haikal.xboxcontroller.ui.theme.TextSecondary
import com.haikal.xboxcontroller.ui.theme.WarningAmber
import com.haikal.xboxcontroller.ui.theme.XboxGreen
import com.haikal.xboxcontroller.ui.theme.XboxGreenBright
import com.haikal.xboxcontroller.ui.theme.XboxGreenPressed
import com.haikal.xboxcontroller.viewmodel.ControllerViewModel
import kotlin.math.roundToInt

/**
 * Full multi-touch virtual Xbox controller.
 *
 * Every control owns its own pointerInput modifier, so multiple fingers
 * can drive sticks, D-Pad, triggers and face buttons simultaneously.
 * Values: sticks -1..1 (y up = +1), triggers 0..1.
 */
@Composable
fun VirtualControllerScreen(
    viewModel: ControllerViewModel,
    onBack: () -> Unit,
) {
    val inputState by viewModel.inputState.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val localTesting by viewModel.localTestingMode.collectAsState()
    val debugLog by viewModel.debugLog.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val haptics = LocalHapticFeedback.current

    val hapticTick: () -> Unit = {
        if (settings.hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusHeader(connectionStatus, localTesting, onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left cluster: D-Pad top, LeftStick bottom-left
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DPad8Way(
                    pressed = inputState.buttons,
                    onButtonChange = { pressed ->
                        val current = inputState.buttons
                        val next = if (pressed) current + Button.DPAD_UP else current - Button.DPAD_UP
                        viewModel.updateInput(inputState.copy(buttons = next))
                        if (pressed) hapticTick()
                    },
                    onAny = { up, down, left, right ->
                        val dpad = buildSet {
                            if (up) add(Button.DPAD_UP)
                            if (down) add(Button.DPAD_DOWN)
                            if (left) add(Button.DPAD_LEFT)
                            if (right) add(Button.DPAD_RIGHT)
                        }
                        val others = inputState.buttons - Button.DPAD_UP - Button.DPAD_DOWN -
                            Button.DPAD_LEFT - Button.DPAD_RIGHT
                        viewModel.updateInput(inputState.copy(buttons = others + dpad))
                    },
                    modifier = Modifier.size(140.dp),
                )
                Stick(
                    label = "L",
                    x = inputState.leftStickX,
                    y = inputState.leftStickY,
                    sensitivity = settings.leftStickSensitivity,
                    onMove = { x, y ->
                        viewModel.updateInput(
                            inputState.copy(leftStickX = x, leftStickY = y),
                        )
                    },
                    modifier = Modifier.size(130.dp),
                )
            }

            // Center cluster: Guide, View, Menu, L3/R3
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SmallButton(
                    label = "Guide",
                    pressed = inputState.buttons.contains(Button.GUIDE),
                    color = XboxGreen,
                    onChange = { p ->
                        viewModel.updateInput(toggle(inputState, Button.GUIDE, p))
                        if (p) hapticTick()
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton(
                        label = "View",
                        pressed = inputState.buttons.contains(Button.VIEW),
                        color = CharcoalElevated,
                        onChange = { p ->
                            viewModel.updateInput(toggle(inputState, Button.VIEW, p))
                            if (p) hapticTick()
                        },
                    )
                    SmallButton(
                        label = "Menu",
                        pressed = inputState.buttons.contains(Button.MENU),
                        color = CharcoalElevated,
                        onChange = { p ->
                            viewModel.updateInput(toggle(inputState, Button.MENU, p))
                            if (p) hapticTick()
                        },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton(
                        label = "L3",
                        pressed = inputState.buttons.contains(Button.L3),
                        color = CharcoalElevated,
                        onChange = { p ->
                            viewModel.updateInput(toggle(inputState, Button.L3, p))
                            if (p) hapticTick()
                        },
                    )
                    SmallButton(
                        label = "R3",
                        pressed = inputState.buttons.contains(Button.R3),
                        color = CharcoalElevated,
                        onChange = { p ->
                            viewModel.updateInput(toggle(inputState, Button.R3, p))
                            if (p) hapticTick()
                        },
                    )
                }
            }

            // Right cluster: face buttons, bumpers, triggers, right stick
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Trigger("LT", inputState.leftTrigger, settings.triggerSensitivity) { v ->
                        viewModel.updateInput(inputState.copy(leftTrigger = v))
                    }
                    Trigger("RT", inputState.rightTrigger, settings.triggerSensitivity) { v ->
                        viewModel.updateInput(inputState.copy(rightTrigger = v))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FaceButton("LB", inputState.buttons.contains(Button.LB), XboxGreenPressed) { p ->
                        viewModel.updateInput(toggle(inputState, Button.LB, p))
                        if (p) hapticTick()
                    }
                    FaceButton("RB", inputState.buttons.contains(Button.RB), XboxGreenPressed) { p ->
                        viewModel.updateInput(toggle(inputState, Button.RB, p))
                        if (p) hapticTick()
                    }
                }
                Stick(
                    label = "R",
                    x = inputState.rightStickX,
                    y = inputState.rightStickY,
                    sensitivity = settings.rightStickSensitivity,
                    onMove = { x, y ->
                        viewModel.updateInput(
                            inputState.copy(rightStickX = x, rightStickY = y),
                        )
                    },
                    modifier = Modifier.size(130.dp),
                )
            }
        }

        // Face buttons row (A/B/X/Y)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            FaceButton("X", inputState.buttons.contains(Button.X), Color(0xFF3B82C4)) { p ->
                viewModel.updateInput(toggle(inputState, Button.X, p))
                if (p) hapticTick()
            }
            Spacer(Modifier.width(14.dp))
            FaceButton("Y", inputState.buttons.contains(Button.Y), Color(0xFFF2C14E)) { p ->
                viewModel.updateInput(toggle(inputState, Button.Y, p))
                if (p) hapticTick()
            }
            Spacer(Modifier.width(28.dp))
            FaceButton("A", inputState.buttons.contains(Button.A), Color(0xFF57A64A)) { p ->
                viewModel.updateInput(toggle(inputState, Button.A, p))
                if (p) hapticTick()
            }
            Spacer(Modifier.width(14.dp))
            FaceButton("B", inputState.buttons.contains(Button.B), Color(0xFFD9534F)) { p ->
                viewModel.updateInput(toggle(inputState, Button.B, p))
                if (p) hapticTick()
            }
            Spacer(Modifier.weight(1f))
        }

        if (localTesting) {
            DebugLogPanel(debugLog, onClear = { viewModel.clearDebugLog() })
        }
    }
}

private fun toggle(
    state: ControllerInputState,
    button: Button,
    pressed: Boolean,
): ControllerInputState {
    val next = if (pressed) state.buttons + button else state.buttons - button
    return state.copy(buttons = next)
}

@Composable
private fun StatusHeader(
    status: ConnectionStatus,
    localTesting: Boolean,
    onBack: () -> Unit,
) {
    val (text, color) = when (status) {
        is ConnectionStatus.Connected -> "Connected ${status.host}:${status.port}" to XboxGreenBright
        ConnectionStatus.Connecting -> "Connecting…" to WarningAmber
        is ConnectionStatus.Retrying -> "Retrying (#${status.attempt}) in ${status.delayMs}ms" to WarningAmber
        is ConnectionStatus.Failed -> status.message to ErrorRed
        ConnectionStatus.Disconnected ->
            if (localTesting) "Local testing mode — input logged to debug panel" to WarningAmber
            else "Disconnected" to TextSecondary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("←", color = XboxGreenBright, fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
        Column(Modifier.weight(1f)) {
            Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "Back",
            color = XboxGreenBright,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(4.dp)
                .pointerInput(Unit) { detectTapGestures { onBack() } },
        )
    }
}

/**
 * Analog stick: drag inside the circle, clamped to -1..1 with sensitivity.
 * Own pointerInput so it works simultaneously with other controls.
 */
@Composable
private fun Stick(
    label: String,
    x: Float,
    y: Float,
    sensitivity: Float,
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(CharcoalSurface)
            .border(2.dp, XboxGreen, CircleShape)
            .onSizeChanged { size = it }
            .pointerInput(sensitivity) {
                detectDragGestures(
                    onDragStart = { center ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = size.width / 2f
                        val dx = ((center.x - cx) / radius) * sensitivity
                        val dy = ((center.y - cy) / radius) * sensitivity
                        onMove(dx.coerceIn(-1f, 1f), (-dy).coerceIn(-1f, 1f))
                    },
                    onDrag = { change, _ ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = size.width / 2f
                        val dx = ((change.position.x - cx) / radius) * sensitivity
                        val dy = ((change.position.y - cy) / radius) * sensitivity
                        change.consume()
                        onMove(dx.coerceIn(-1f, 1f), (-dy).coerceIn(-1f, 1f))
                    },
                    onDragEnd = {
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        onMove(0f, 0f)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = this.size.minDimension / 3f
            drawCircle(
                color = XboxGreenBright,
                radius = radius * 0.55f,
                center = Offset(cx + x * radius, cy - y * radius),
            )
        }
        Text(label, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopCenter).padding(6.dp))
    }
}

/**
 * D-Pad with full 8-direction support: a diagonal drag presses two
 * directions at once (single pointer, multi-direction).
 */
@Composable
private fun DPad8Way(
    pressed: Set<Button>,
    onButtonChange: (Boolean) -> Unit,
    onAny: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun computeDirections(offset: androidx.compose.ui.geometry.Offset): Array<Boolean> {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val dx = offset.x - cx
        val dy = offset.y - cy
        val dead = size.width / 6f
        val active = (dx * dx + dy * dy) > dead * dead
        val angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble()))
        val right = active && angle > -45 && angle < 45
        val down = active && angle >= 45 && angle <= 135
        val left = active && (angle > 135 || angle < -135)
        val up = active && angle >= -135 && angle <= -45
        return arrayOf(up, down, left, right)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(CharcoalSurface)
            .border(2.dp, XboxGreenPressed, CircleShape)
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val (u, d, l, r) = computeDirections(offset)
                        onAny(u, d, l, r)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val (u, d, l, r) = computeDirections(change.position)
                        onAny(u, d, l, r)
                    },
                    onDragEnd = { onAny(false, false, false, false) },
                    onDragCancel = { onAny(false, false, false, false) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("▲", color = if (pressed.contains(Button.DPAD_UP)) XboxGreenBright else TextSecondary, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◀", color = if (pressed.contains(Button.DPAD_LEFT)) XboxGreenBright else TextSecondary, fontSize = 18.sp)
                Spacer(Modifier.width(24.dp))
                Text("▶", color = if (pressed.contains(Button.DPAD_RIGHT)) XboxGreenBright else TextSecondary, fontSize = 18.sp)
            }
            Text("▼", color = if (pressed.contains(Button.DPAD_DOWN)) XboxGreenBright else TextSecondary, fontSize = 18.sp)
        }
    }
}

/** Trigger: vertical drag slider 0..1. */
@Composable
private fun Trigger(
    label: String,
    value: Float,
    sensitivity: Float,
    onChange: (Float) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 110.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CharcoalSurface)
            .border(2.dp, XboxGreenPressed, RoundedCornerShape(10.dp))
            .onSizeChanged { size = it }
            .pointerInput(sensitivity) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val depth = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        onChange((depth * sensitivity).coerceIn(0f, 1f))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val depth = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                        onChange((depth * sensitivity).coerceIn(0f, 1f))
                    },
                    onDragEnd = { onChange(0f) },
                    onDragCancel = { onChange(0f) },
                )
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((value * 110).dp)
                .background(XboxGreen),
        )
        Text(
            "$label ${(value * 100).roundToInt()}%",
            color = TextPrimary,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(4.dp),
        )
    }
}

/** Round face/bumper button with pressed visual state. */
@Composable
private fun FaceButton(
    label: String,
    pressed: Boolean,
    color: Color,
    onChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (pressed) XboxGreenBright else color)
            .border(2.dp, if (pressed) XboxGreenBright else CharcoalElevated, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { onPressed ->
                        onChange(true)
                        try {
                            awaitRelease()
                        } finally {
                            onChange(false)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (pressed) CharcoalDark else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** Rectangular small button (Guide/View/Menu/L3/R3) with pressed state. */
@Composable
private fun SmallButton(
    label: String,
    pressed: Boolean,
    color: Color,
    onChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 84.dp, height = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (pressed) XboxGreenBright else color)
            .border(1.dp, if (pressed) XboxGreenBright else XboxGreenPressed, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { onPressed ->
                        onChange(true)
                        try {
                            awaitRelease()
                        } finally {
                            onChange(false)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (pressed) CharcoalDark else TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

/** Debug panel shown in local testing mode. */
@Composable
private fun DebugLogPanel(log: List<String>, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(CharcoalSurface, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Debug log (local testing)", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(
                "Clear",
                color = XboxGreenBright,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.pointerInput(Unit) { detectTapGestures { onClear() } },
            )
        }
        Spacer(Modifier.height(6.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(log.asReversed()) { line ->
                Text(line, color = TextPrimary, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}
