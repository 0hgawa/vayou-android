package dev.vayou.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class EqPreset(val gains: Map<Int, Int>) {
    FLAT(mapOf(60 to 0, 230 to 0, 910 to 0, 3600 to 0, 14000 to 0)),

    /**
     * Speech, which no genre curve serves: an audiobook, a sermon, a podcast, an interview.
     *
     * A voice lives between roughly 300Hz and 4kHz, so the two ends are cut and the middle lifted.
     * The 60Hz cut is the biggest single gain — nothing a voice says happens down there, and what
     * does is room rumble and the handling of the microphone, sitting on top of the words. The lift
     * at 3.6k is the consonants, which is what "I can't make out the words" usually means.
     */
    VOCAL(mapOf(60 to -300, 230 to -100, 910 to 200, 3600 to 400, 14000 to -200)),
    CLASSICAL(mapOf(60 to 0, 230 to 0, 910 to 0, 3600 to -400, 14000 to -600)),
    DANCE(mapOf(60 to 600, 230 to 0, 910 to 200, 3600 to 400, 14000 to 100)),
    FOLK(mapOf(60 to 0, 230 to 0, 910 to 0, 3600 to 200, 14000 to -100)),
    HEAVY_METAL(mapOf(60 to 400, 230 to 100, 910 to 900, 3600 to 300, 14000 to 0)),
    HIP_HOP(mapOf(60 to 500, 230 to 300, 910 to 0, 3600 to 100, 14000 to 300)),
    JAZZ(mapOf(60 to 0, 230 to 0, 910 to 0, 3600 to 200, 14000 to -400)),
    POP(mapOf(60 to -100, 230 to 200, 910 to 500, 3600 to 100, 14000 to -200)),
    ROCK(mapOf(60 to 500, 230 to 300, 910 to -100, 3600 to 300, 14000 to 400)),
    CUSTOM(emptyMap()),
}
