package com.haikal.xboxcontroller.console

/**
 * Xbox console generations supported by the app.
 *
 * Compatibility semantics:
 *  - USB-to-Windows bridge (phone as virtual Xbox pad on a Windows PC):
 *    SUPPORTED.
 *  - Direct output to a physical Xbox console over the network/USB:
 *    NOT_SUPPORTED — honest limitation, no fake "works" claims.
 *  - Requires extra hardware dongle: REQUIRES_HARDWARE.
 */
enum class ConsoleProfile(
    val displayName: String,
    val generation: Generation,
    val compatibilityStatus: CompatibilityStatus,
    val wireName: String,
    val notes: String,
) {
    XBOX_360(
        displayName = "Xbox 360",
        generation = Generation.SEVENTH,
        compatibilityStatus = CompatibilityStatus.SUPPORTED,
        wireName = "XBOX_360",
        notes = "USB-to-Windows bridge: supported as a standard XInput device.",
    ),
    XBOX_360_E(
        displayName = "Xbox 360 E",
        generation = Generation.SEVENTH,
        compatibilityStatus = CompatibilityStatus.SUPPORTED,
        wireName = "XBOX_360",
        notes = "USB-to-Windows bridge: supported as a standard XInput device.",
    ),
    XBOX_360_ELITE(
        displayName = "Xbox 360 Elite",
        generation = Generation.SEVENTH,
        compatibilityStatus = CompatibilityStatus.SUPPORTED,
        wireName = "XBOX_360",
        notes = "USB-to-Windows bridge: supported as a standard XInput device.",
    ),
    XBOX_ONE(
        displayName = "Xbox One",
        generation = Generation.EIGHTH,
        compatibilityStatus = CompatibilityStatus.SUPPORTED,
        wireName = "XBOX_ONE",
        notes = "USB-to-Windows bridge: supported. Physical console output is not supported.",
    ),
    XBOX_ONE_S(
        displayName = "Xbox One S",
        generation = Generation.EIGHTH,
        compatibilityStatus = CompatibilityStatus.SUPPORTED,
        wireName = "XBOX_ONE",
        notes = "USB-to-Windows bridge: supported. Physical console output is not supported.",
    ),
    XBOX_ONE_X(
        displayName = "Xbox One X",
        generation = Generation.EIGHTH,
        compatibilityStatus = CompatibilityStatus.SUPPORTED,
        wireName = "XBOX_ONE",
        notes = "USB-to-Windows bridge: supported. Physical console output is not supported.",
    ),
    XBOX_SERIES_S(
        displayName = "Xbox Series S",
        generation = Generation.NINTH,
        compatibilityStatus = CompatibilityStatus.REQUIRES_HARDWARE,
        wireName = "XBOX_SERIES_S",
        notes = "Direct console output requires a USB host bridge dongle; PC bridge is supported.",
    ),
    XBOX_SERIES_X(
        displayName = "Xbox Series X",
        generation = Generation.NINTH,
        compatibilityStatus = CompatibilityStatus.REQUIRES_HARDWARE,
        wireName = "XBOX_SERIES_X",
        notes = "Direct console output requires a USB host bridge dongle; PC bridge is supported.",
    ),
    CUSTOM_UNKNOWN(
        displayName = "Custom / Unknown",
        generation = Generation.UNKNOWN,
        compatibilityStatus = CompatibilityStatus.NOT_SUPPORTED,
        wireName = "CUSTOM_UNKNOWN",
        notes = "Unknown target: only PC bridge and local testing mode are available.",
    ),
    ;

    enum class Generation(val label: String) {
        SEVENTH("7th generation"),
        EIGHTH("8th generation"),
        NINTH("9th generation"),
        UNKNOWN("Unknown generation"),
    }

    enum class CompatibilityStatus(val label: String) {
        SUPPORTED("Supported"),
        NOT_SUPPORTED("Not supported"),
        REQUIRES_HARDWARE("Requires hardware"),
    }
}
