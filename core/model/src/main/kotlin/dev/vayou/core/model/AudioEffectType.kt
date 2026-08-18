package dev.vayou.core.model

/**
 * Audio effects the platform exposes as a single 0..[MAX_STRENGTH] knob. They have no common
 * supertype in the framework, so this enum is what lets one command, one preference accessor and one
 * control serve both instead of each being wired twice.
 */
enum class AudioEffectType {
    BASS_BOOST,
    VIRTUALIZER,
    ;

    companion object {
        /** Strength range shared by every effect here, as defined by the Android audio framework. */
        const val MAX_STRENGTH = 1000
    }
}
