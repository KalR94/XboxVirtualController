package com.haikal.xboxcontroller.connection

import org.json.JSONObject

/**
 * Full virtual controller input state. All stick values are -1.0..1.0
 * (y is already inverted so up = +1.0), triggers 0.0..1.0.
 */
data class ControllerInputState(
    val buttons: Set<Button> = emptySet(),
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,
) {
    val isNeutral: Boolean
        get() = buttons.isEmpty() &&
            leftStickX == 0f && leftStickY == 0f &&
            rightStickX == 0f && rightStickY == 0f &&
            leftTrigger == 0f && rightTrigger == 0f
}

enum class Button(val wireName: String) {
    A("A"),
    B("B"),
    X("X"),
    Y("Y"),
    LB("LB"),
    RB("RB"),
    VIEW("View"),
    MENU("Menu"),
    GUIDE("Guide"),
    L3("L3"),
    R3("R3"),
    // D-Pad mapped onto the same buttons object on the wire.
    DPAD_UP("Up"),
    DPAD_DOWN("Down"),
    DPAD_LEFT("Left"),
    DPAD_RIGHT("Right");

    companion object {
        fun fromWireName(name: String): Button? = entries.firstOrNull { it.wireName == name }
    }
}

object InputSerializer {

    /**
     * Serializes the current state into the exact wire format expected by
     * the bridge, e.g.:
     * {"type":"controller_input","console":"XBOX_ONE",
     *  "buttons":{"A":true,"LB":true,...},
     *  "left_stick":{"x":-1.0,"y":0.5},
     *  "right_stick":{"x":0.0,"y":0.0},
     *  "triggers":{"LT":0.0,"RT":1.0}}
     */
    fun serialize(state: ControllerInputState, console: String): String {
        val buttons = JSONObject()
        for (button in Button.entries) {
            buttons.put(button.wireName, state.buttons.contains(button))
        }

        val leftStick = JSONObject()
        leftStick.put("x", clampStick(state.leftStickX))
        leftStick.put("y", clampStick(state.leftStickY))

        val rightStick = JSONObject()
        rightStick.put("x", clampStick(state.rightStickX))
        rightStick.put("y", clampStick(state.rightStickY))

        val triggers = JSONObject()
        triggers.put("LT", clampTrigger(state.leftTrigger))
        triggers.put("RT", clampTrigger(state.rightTrigger))

        val root = JSONObject()
        root.put("type", "controller_input")
        root.put("console", console)
        root.put("buttons", buttons)
        root.put("left_stick", leftStick)
        root.put("right_stick", rightStick)
        root.put("triggers", triggers)
        return root.toString()
    }

    private fun clampStick(v: Float): Double =
        v.coerceIn(-1f, 1f).toDouble()

    private fun clampTrigger(v: Float): Double =
        v.coerceIn(0f, 1f).toDouble()
}
