package dev.vayou.core.model

/** Which decoder gets first refusal on a file. */
enum class DecoderPriority {
    /** The phone's own, falling back to the bundled one when it will not open the file. Coolest. */
    PREFER_DEVICE,

    /** The bundled one first, for formats a phone opens badly rather than not at all. */
    PREFER_APP,

    /** The phone's own or nothing, which is how to find out whether it is the fallback misbehaving. */
    DEVICE_ONLY,
}
