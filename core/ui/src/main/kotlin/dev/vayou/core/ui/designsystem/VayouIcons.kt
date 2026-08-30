package dev.vayou.core.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The app's icon set, drawn from Phosphor (MIT), regular weight.
 *
 * The vectors are built in code rather than pulled from a library so the app carries only the icons
 * it actually uses; androidx's material-icons-extended ships thousands of generated classes and
 * costs build time for no benefit here. Built lazily, and icons sharing a glyph are aliases rather
 * than duplicates.
 */
object VayouIcons {

    val Add: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Add", "M224,128a8,8,0,0,1-8,8H136v80a8,8,0,0,1-16,0V136H40a8,8,0,0,1,0-16h80V40a8,8,0,0,1,16,0v80h80A8,8,0,0,1,224,128Z")
    }

    /** [Add]'s crossbar on its own, for the pair of steppers either side of a number. */
    val Remove: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Remove", "M224,128a8,8,0,0,1-8,8H40a8,8,0,0,1,0-16H216A8,8,0,0,1,224,128Z")
    }

    val Cast: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Cast", "M224,192a8,8,0,0,1-16,0c0-79.4-64.6-144-144-144a8,8,0,0,1,0-16C152.22,32,224,103.78,224,192ZM64,104a8,8,0,0,0,0,16,72.08,72.08,0,0,1,72,72,8,8,0,0,0,16,0A88.1,88.1,0,0,0,64,104Zm4,72a12,12,0,1,0,12,12A12,12,0,0,0,68,176Z")
    }

    /**
     * [Cast] turned inside out: the same waves, cut out of a filled plate.
     *
     * Not a fatter version of the outline. Every other pair in this set says "on" by filling the
     * glyph, but a cast wave has nothing to fill -- it is three arcs and a dot, and thickening them
     * gives a smear that reads as a worse drawing rather than as a state. Inverting is the move the
     * shape allows: the plate says on, and the waves stay exactly the waves.
     *
     * One path under the even-odd rule, which is what makes the waves holes rather than more ink.
     * They are scaled to seven tenths about the middle so the plate keeps a margin around them.
     */
    val CastConnected: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        vector(
            "M72,16H184A56,56,0,0,1,240,72V184A56,56,0,0,1,184,240H72A56,56,0,0,1,16,184V72" +
                "A56,56,0,0,1,72,16Z" +
                "M195.2,172.8a5.6,5.6,0,0,1-11.2,0c0-55.58-45.22-100.8-100.8-100.8a5.6,5.6,0,0,1,0-11.2" +
                "C144.95,60.8,195.2,111.05,195.2,172.8Z" +
                "M83.2,111.2a5.6,5.6,0,0,0,0,11.2,50.46,50.46,0,0,1,50.4,50.4,5.6,5.6,0,0,0,11.2,0" +
                "A61.67,61.67,0,0,0,83.2,111.2Z" +
                "M86,161.6a8.4,8.4,0,1,0,8.4,8.4A8.4,8.4,0,0,0,86,161.6Z",
            name = "CastConnected",
            viewport = PhosphorGrid,
            isEvenOdd = true,
        )
    }

    /**
     * The filled transport trio, for the player's own controls. The outlined [Pause] above is for
     * rows and menus, where a hollow glyph sits at the same weight as the text beside it. Under the
     * thumb it reads as an unfilled shape rather than a button, and Play only ships filled -- an
     * outlined pause toggling to a solid play would change weight every time it was pressed.
     */
    val PauseFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("PauseFilled", "M216,48V208a16,16,0,0,1-16,16H160a16,16,0,0,1-16-16V48a16,16,0,0,1,16-16h40A16,16,0,0,1,216,48ZM96,32H56A16,16,0,0,0,40,48V208a16,16,0,0,0,16,16H96a16,16,0,0,0,16-16V48A16,16,0,0,0,96,32Z")
    }

    /**
     * The skip pair, on the standard grid and matching [Play]: the same rounded triangle with a bar
     * beside it. Phosphor's own fill variant sets the bar against the triangle's point, so at 24dp
     * the two fuse into one shape.
     */
    val SkipPreviousFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        vector(
            "M8.09 14.647c-1.787-1.154-1.787-4.14 0-5.294l10.79-6.968c1.736-1.121 3.87.339 " +
                "3.87 2.648v13.934c0 2.31-2.134 3.769-3.87 2.648zM2 5a.75.75 0 0 1 1.5 0v14A.75.75 0 0 1 2 19z",
            name = "SkipPreviousFilled",
        )
    }

    val SkipNextFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        vector(
            "M16.66 14.647c1.787-1.154 1.787-4.14 0-5.294L5.87 2.385C4.135 1.264 2 2.724 2 " +
                "5.033v13.934c0 2.31 2.134 3.769 3.87 2.648zM22.75 5a.75.75 0 0 0-1.5 0v14a.75.75 0 0 0 1.5 0z",
            name = "SkipNextFilled",
        )
    }

    val PlayNext: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("PlayNext", "M32,64a8,8,0,0,1,8-8H216a8,8,0,0,1,0,16H40A8,8,0,0,1,32,64Zm8,72H160a8,8,0,0,0,0-16H40a8,8,0,0,0,0,16Zm72,48H40a8,8,0,0,0,0,16h72a8,8,0,0,0,0-16Zm135.66-57.7a8,8,0,0,1-10,5.36L208,122.75V192a32.05,32.05,0,1,1-16-27.69V112a8,8,0,0,1,10.3-7.66l40,12A8,8,0,0,1,247.66,126.3ZM192,192a16,16,0,1,0-16,16A16,16,0,0,0,192,192Z", autoMirror = true)
    }

    /**
     * A television, for a channel list. Solar's outline, whose aerial reads at 24dp where a plain
     * rectangle with a stand does not -- and a channel is the one thing here that is not a file.
     */
    val Tv: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "Tv",
            StrokedPath(
                "M22 16c0 2.828 0 4.243-.879 5.121C20.243 22 18.828 22 16 22H8c-2.828 0-4.243 0-5.121-.879C2 " +
                    "20.243 2 18.828 2 16v-4c0-2.828 0-4.243.879-5.121C3.757 6 5.172 6 8 6h8c2.828 0 4.243 0 " +
                    "5.121.879C22 7.757 22 9.172 22 12z",
            ),
            StrokedPath("m9 2l3 3.5L15 2m1 4v16", hasRoundCaps = true),
            StrokedPath("M20 16a1 1 0 1 0-2 0a1 1 0 0 0 2 0m0-4a1 1 0 1 0-2 0a1 1 0 0 0 2 0", isFilled = true),
        )
    }

    val Appearance: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Appearance", "M200.77,53.89A103.27,103.27,0,0,0,128,24h-1.07A104,104,0,0,0,24,128c0,43,26.58,79.06,69.36,94.17A32,32,0,0,0,136,192a16,16,0,0,1,16-16h46.21a31.81,31.81,0,0,0,31.2-24.88,104.43,104.43,0,0,0,2.59-24A103.28,103.28,0,0,0,200.77,53.89Zm13,93.71A15.89,15.89,0,0,1,198.21,160H152a32,32,0,0,0-32,32,16,16,0,0,1-21.31,15.07C62.49,194.3,40,164,40,128a88,88,0,0,1,87.09-88h.9a88.35,88.35,0,0,1,88,87.25A88.86,88.86,0,0,1,213.81,147.6ZM140,76a12,12,0,1,1-12-12A12,12,0,0,1,140,76ZM96,100A12,12,0,1,1,84,88,12,12,0,0,1,96,100Zm0,56a12,12,0,1,1-12-12A12,12,0,0,1,96,156Zm88-56a12,12,0,1,1-12-12A12,12,0,0,1,184,100Z")
    }

    val ArrowBack: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("ArrowBack", "M224,128a8,8,0,0,1-8,8H59.31l58.35,58.34a8,8,0,0,1-11.32,11.32l-72-72a8,8,0,0,1,0-11.32l72-72a8,8,0,0,1,11.32,11.32L59.31,120H216A8,8,0,0,1,224,128Z", autoMirror = true)
    }

    val Filter: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor(
            "Filter",
            "M232,96H24a8,8,0,0,1,0-16H232a8,8,0,0,1,0,16Z" +
                "M192,144H64a8,8,0,0,1,0-16H192a8,8,0,0,1,0,16Z" +
                "M152,192H104a8,8,0,0,1,0-16h48a8,8,0,0,1,0,16Z",
        )
    }

    val ArrowDownward: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("ArrowDownward", "M205.66,149.66l-72,72a8,8,0,0,1-11.32,0l-72-72a8,8,0,0,1,11.32-11.32L120,196.69V40a8,8,0,0,1,16,0V196.69l58.34-58.35a8,8,0,0,1,11.32,11.32Z")
    }

    val ArrowUpward: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("ArrowUpward", "M205.66,117.66a8,8,0,0,1-11.32,0L136,59.31V216a8,8,0,0,1-16,0V59.31L61.66,117.66a8,8,0,0,1-11.32-11.32l72-72a8,8,0,0,1,11.32,0l72,72A8,8,0,0,1,205.66,117.66Z")
    }

    val Artist: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Artist", "M230.92,212c-15.23-26.33-38.7-45.21-66.09-54.16a72,72,0,1,0-73.66,0C63.78,166.78,40.31,185.66,25.08,212a8,8,0,1,0,13.85,8c18.84-32.56,52.14-52,89.07-52s70.23,19.44,89.07,52a8,8,0,1,0,13.85-8ZM72,96a56,56,0,1,1,56,56A56.06,56.06,0,0,1,72,96Z")
    }

    /** Solar's beamed pair of notes. Its flag is the only shape in the set that is not axis-aligned,
     *  which is what makes a music tab read as music rather than as a shape. */
    /** Solar's single note. One circle rather than a beamed pair: at 24dp two of them with a 1.5
     *  stroke leave holes that close up, and every other glyph in the bar is one object. */
    val Audio: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "Audio",
            StrokedPath("M12 18a4 4 0 1 1-8 0a4 4 0 0 1 8 0Zm0 0V6"),
            StrokedPath(
                "m16.117 10.059l-2.634-1.317c-.365-.182-.547-.274-.698-.389a2 2 0 0 1-.75-1.213C12 6.954 12 6.75 12 " +
                    "6.342c0-.971 0-1.457.12-1.787a2 2 0 0 1 2.112-1.305c.348.04.783.258 1.651.692l2.634 1.317c.365" +
                    ".182.547.273.698.389a2 2 0 0 1 .75 1.212c.035.187.035.39.035.799c0 .97 0 1.456-.12 1.786a2 2 0 " +
                    "0 1-2.112 1.306c-.348-.04-.783-.258-1.651-.692Z",
                hasRoundCaps = true,
            ),
        )
    }

    /**
     * Two notes under one beam: a collection of tracks, where [Audio] is a single one.
     *
     * The pair is hard to read at 24dp beside a word, which is why the bar and the rows take the
     * single note -- but on a 56dp tile standing in for an album cover it has the room, and it is
     * the one glyph in the set that says "several of these".
     */
    val AudioNotes: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "AudioNotes",
            StrokedPath("M9 19a3 3 0 1 1-6 0a3 3 0 0 1 6 0Zm12-2a3 3 0 1 1-6 0a3 3 0 0 1 6 0ZM9 19V8m12 9V6"),
            StrokedPath(
                "m15.735 3.755l-4 1.333c-1.32.44-1.98.66-2.357 1.184S9 7.492 9 8.882V12l12-4v-.45c0-2.533 0-3.8-" +
                    ".83-4.398c-.831-.599-2.032-.198-4.435.603Z",
                hasRoundCaps = true,
            ),
        )
    }

    val AudioNotesFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        vector(
            "m10.09 11.963l9.274-3.332v5.54a3.8 3.8 0 0 0-1.91-.501c-1.958 0-3.545 1.426-3.545 3.185s1.587 " +
                "3.185 3.545 3.185c1.959 0 3.546-1.426 3.546-3.185V7.492c0-1.12 0-2.059-.088-2.807a7 7 0 0 " +
                "0-.043-.31c-.084-.51-.234-.988-.522-1.386a2.2 2.2 0 0 0-.676-.617l-.009-.005c-.771-.461-1.639-" +
                ".428-2.532-.224c-.864.198-1.936.6-3.25 1.095l-2.284.859c-.615.231-1.137.427-1.547.63c-.435.216-" +
                ".81.471-1.092.851c-.281.38-.398.79-.452 1.234c-.05.418-.05.926-.05 1.525v7.794a3.8 3.8 0 0 " +
                "0-1.91-.501C4.587 15.63 3 17.056 3 18.815S4.587 22 6.545 22c1.959 0 3.546-1.426 3.546-3.185z",
            name = "AudioNotesFilled",
        )
    }

    val AudioFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        vector(
            "M14.319 2.505A2.75 2.75 0 0 0 11.414 4.3c-.098.27-.132.563-.148.869A17 17 0 0 0 11.25 6v8.536A4.75 " +
                "4.75 0 1 0 12.75 18V9.21q.156.083.343.175L15.8 10.74c.418.21.759.38 1.038.5c.281.123.558.223.843" +
                ".257A2.75 2.75 0 0 0 20.586 9.7c.098-.27.132-.563.148-.87c.016-.303.016-.683.016-1.151v-.083c0-" +
                ".348 0-.62-.049-.878a2.75 2.75 0 0 0-1.03-1.667c-.21-.16-.453-.281-.764-.436L16.2 3.262a22 22 0 " +
                "0 0-1.038-.501c-.28-.123-.558-.223-.843-.256",
            name = "AudioFilled",
        )
    }

    /** Stacked sheets, for the plate a subtitle is laid on. */
    val Background: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "Background",
            StrokedPath(
                "M4.979 9.685C2.993 8.891 2 8.494 2 8s.993-.89 2.979-1.685l2.808-1.123C9.773 4.397 10.767 4 12 " +
                    "4s2.227.397 4.213 1.192l2.808 1.123C21.007 7.109 22 7.506 22 8s-.993.89-2.979 1.685l-2.808 " +
                    "1.124C14.227 11.603 13.233 12 12 12s-2.227-.397-4.213-1.191z",
            ),
            StrokedPath(
                "M22 12s-.993.89-2.979 1.685l-2.808 1.124C14.227 15.603 13.233 16 12 16s-2.227-.397-4.213-1.191L4" +
                    ".98 13.685C2.993 12.891 2 12 2 12m20 4s-.993.89-2.979 1.685l-2.808 1.124C14.227 19.603 " +
                    "13.233 20 12 20s-2.227-.397-4.213-1.192L4.98 17.685C2.993 16.891 2 16 2 16",
                hasRoundCaps = true,
            ),
        )
    }

    val Bold: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Bold", "M178.48,115.7A44,44,0,0,0,148,40H80a8,8,0,0,0-8,8V200a8,8,0,0,0,8,8h80a48,48,0,0,0,18.48-92.3ZM88,56h60a28,28,0,0,1,0,56H88Zm72,136H88V128h72a32,32,0,0,1,0,64Z")
    }

    val Brightness: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Brightness", "M120,40V16a8,8,0,0,1,16,0V40a8,8,0,0,1-16,0Zm72,88a64,64,0,1,1-64-64A64.07,64.07,0,0,1,192,128Zm-16,0a48,48,0,1,0-48,48A48.05,48.05,0,0,0,176,128ZM58.34,69.66A8,8,0,0,0,69.66,58.34l-16-16A8,8,0,0,0,42.34,53.66Zm0,116.68-16,16a8,8,0,0,0,11.32,11.32l16-16a8,8,0,0,0-11.32-11.32ZM192,72a8,8,0,0,0,5.66-2.34l16-16a8,8,0,0,0-11.32-11.32l-16,16A8,8,0,0,0,192,72Zm5.66,114.34a8,8,0,0,0-11.32,11.32l16,16a8,8,0,0,0,11.32-11.32ZM48,128a8,8,0,0,0-8-8H16a8,8,0,0,0,0,16H40A8,8,0,0,0,48,128Zm80,80a8,8,0,0,0-8,8v24a8,8,0,0,0,16,0V216A8,8,0,0,0,128,208Zm112-88H216a8,8,0,0,0,0,16h24a8,8,0,0,0,0-16Z")
    }

    val Calendar: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Calendar", "M208,32H184V24a8,8,0,0,0-16,0v8H88V24a8,8,0,0,0-16,0v8H48A16,16,0,0,0,32,48V208a16,16,0,0,0,16,16H208a16,16,0,0,0,16-16V48A16,16,0,0,0,208,32ZM72,48v8a8,8,0,0,0,16,0V48h80v8a8,8,0,0,0,16,0V48h24V80H48V48ZM208,208H48V96H208V208Zm-96-88v64a8,8,0,0,1-16,0V132.94l-4.42,2.22a8,8,0,0,1-7.16-14.32l16-8A8,8,0,0,1,112,120Zm59.16,30.45L152,176h16a8,8,0,0,1,0,16H136a8,8,0,0,1-6.4-12.8l28.78-38.37A8,8,0,1,0,145.07,132a8,8,0,1,1-13.85-8A24,24,0,0,1,176,136,23.76,23.76,0,0,1,171.16,150.45Z")
    }

    /** Solar's, drawn as a stroke. Its plate is squarer and its lines of text shorter than
     *  Phosphor's, which at 24dp is the difference between four bars and a smudge. */
    val Caption: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "Caption",
            StrokedPath(
                "M2 12c0-3.771 0-5.657 1.172-6.828S6.229 4 10 4h4c3.771 0 5.657 0 6.828 1.172S22 8.229 22 12s0 " +
                    "5.657-1.172 6.828S17.771 20 14 20h-4c-3.771 0-5.657 0-6.828-1.172S2 15.771 2 12Z",
            ),
            StrokedPath("M10 16H6m8-3h4m-4 3h-1.5m-3-3h2m6.5 3h-1.5M6 13h1", hasRoundCaps = true),
        )
    }

    val Check: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Check", "M229.66,77.66l-128,128a8,8,0,0,1-11.32,0l-56-56a8,8,0,0,1,11.32-11.32L96,188.69,218.34,66.34a8,8,0,0,1,11.32,11.32Z")
    }

    /**
     * Points at where the thing goes, not at where you came from. A screen that rose from a bar is
     * dismissed downward, and a back arrow would describe a previous screen that does not exist.
     */
    val ChevronDown: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor(
            "ChevronDown",
            "M213.66,101.66l-80,80a8,8,0,0,1,-11.32,0l-80,-80A8,8,0,0,1,53.66,90.34L128,164.69l74.34," +
                "-74.35a8,8,0,0,1,11.32,11.32Z",
        )
    }

    val ChevronRight: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("ChevronRight", "M181.66,133.66l-80,80a8,8,0,0,1-11.32-11.32L164.69,128,90.34,53.66a8,8,0,0,1,11.32-11.32l80,80A8,8,0,0,1,181.66,133.66Z", autoMirror = true)
    }

    val Contrast: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Contrast", "M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm8,16.37a86.4,86.4,0,0,1,16,3V212.67a86.4,86.4,0,0,1-16,3Zm32,9.26a87.81,87.81,0,0,1,16,10.54V195.83a87.81,87.81,0,0,1-16,10.54ZM40,128a88.11,88.11,0,0,1,80-87.63V215.63A88.11,88.11,0,0,1,40,128Zm160,50.54V77.46a87.82,87.82,0,0,1,0,101.08Z")
    }

    val DarkMode: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("DarkMode", "M233.54,142.23a8,8,0,0,0-8-2,88.08,88.08,0,0,1-109.8-109.8,8,8,0,0,0-10-10,104.84,104.84,0,0,0-52.91,37A104,104,0,0,0,136,224a103.09,103.09,0,0,0,62.52-20.88,104.84,104.84,0,0,0,37-52.91A8,8,0,0,0,233.54,142.23ZM188.9,190.34A88,88,0,0,1,65.66,67.11a89,89,0,0,1,31.4-26A106,106,0,0,0,96,56,104.11,104.11,0,0,0,200,160a106,106,0,0,0,14.92-1.06A89,89,0,0,1,188.9,190.34Z")
    }

    val DashBoard: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("DashBoard", "M104,40H56A16,16,0,0,0,40,56v48a16,16,0,0,0,16,16h48a16,16,0,0,0,16-16V56A16,16,0,0,0,104,40Zm0,64H56V56h48v48Zm96-64H152a16,16,0,0,0-16,16v48a16,16,0,0,0,16,16h48a16,16,0,0,0,16-16V56A16,16,0,0,0,200,40Zm0,64H152V56h48v48Zm-96,32H56a16,16,0,0,0-16,16v48a16,16,0,0,0,16,16h48a16,16,0,0,0,16-16V152A16,16,0,0,0,104,136Zm0,64H56V152h48v48Zm96-64H152a16,16,0,0,0-16,16v48a16,16,0,0,0,16,16h48a16,16,0,0,0,16-16V152A16,16,0,0,0,200,136Zm0,64H152V152h48v48Z")
    }

    val Decoder: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Decoder", "M152,96H104a8,8,0,0,0-8,8v48a8,8,0,0,0,8,8h48a8,8,0,0,0,8-8V104A8,8,0,0,0,152,96Zm-8,48H112V112h32Zm88,0H216V112h16a8,8,0,0,0,0-16H216V56a16,16,0,0,0-16-16H160V24a8,8,0,0,0-16,0V40H112V24a8,8,0,0,0-16,0V40H56A16,16,0,0,0,40,56V96H24a8,8,0,0,0,0,16H40v32H24a8,8,0,0,0,0,16H40v40a16,16,0,0,0,16,16H96v16a8,8,0,0,0,16,0V216h32v16a8,8,0,0,0,16,0V216h40a16,16,0,0,0,16-16V160h16a8,8,0,0,0,0-16Zm-32,56H56V56H200v95.87s0,.09,0,.13,0,.09,0,.13V200Z")
    }

    val Delete: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "Delete",
            StrokedPath(
                "M20.5 6h-17m15.333 2.5l-.46 6.9c-.177 2.654-.265 3.981-1.13 4.79s-2.196.81-4.856.81h-.774c-2.66 " +
                    "0-3.991 0-4.856-.81c-.865-.809-.954-2.136-1.13-4.79l-.46-6.9M9.5 11l.5 5m4.5-5l-.5 5",
                hasRoundCaps = true,
            ),
            StrokedPath(
                "M6.5 6h.11a2 2 0 0 0 1.83-1.32l.034-.103l.097-.291c.083-.249.125-.373.18-.479a1.5 1.5 0 0 1 " +
                    "1.094-.788C9.962 3 10.093 3 10.355 3h3.29c.262 0 .393 0 .51.019a1.5 1.5 0 0 1 1.094.788c" +
                    ".055.106.097.23.18.479l.097.291A2 2 0 0 0 17.5 6",
            ),
        )
    }

    val DoubleTap: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("DoubleTap", "M141.66,133.66l-80,80a8,8,0,0,1-11.32-11.32L124.69,128,50.34,53.66A8,8,0,0,1,61.66,42.34l80,80A8,8,0,0,1,141.66,133.66Zm80-11.32-80-80a8,8,0,0,0-11.32,11.32L204.69,128l-74.35,74.34a8,8,0,0,0,11.32,11.32l80-80A8,8,0,0,0,221.66,122.34Z")
    }

    val Edit: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Edit", "M227.31,73.37,182.63,28.68a16,16,0,0,0-22.63,0L36.69,152A15.86,15.86,0,0,0,32,163.31V208a16,16,0,0,0,16,16H92.69A15.86,15.86,0,0,0,104,219.31L227.31,96a16,16,0,0,0,0-22.63ZM92.69,208H48V163.31l88-88L180.69,120ZM192,108.68,147.31,64l24-24L216,84.68Z")
    }

    /**
     * Phosphor's faders: vertical tracks with a knob sitting at a different height on each. The
     * previous glyph was rows of bars, which reads as a level meter — something the app shows you.
     * An equaliser is something you set, and the knob is what says a value can be moved.
     */
    /** Solar's tuning knobs: two sliders on their tracks. Filled geometry, like the rest of that
     *  family's outline style, so the weight is baked in rather than set by [StandardStroke]. */
    val Equalizer: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        vector(
            "M16.959 9.75a.75.75 0 0 1-.75-.75V2a.75.75 0 0 1 1.5 0v7a.75.75 0 0 1-.75.75",
            "M7 5.75a3.75 3.75 0 1 1 0 7.5a3.75 3.75 0 0 1 0-7.5M9.25 9.5a2.25 2.25 0 1 0-4.5 0a2.25 2.25 0 0 " +
                "0 4.5 0M17 18.25a3.75 3.75 0 1 0 0-7.5a3.75 3.75 0 0 0 0 7.5m2.25-3.75a2.25 2.25 0 1 1-4.5 " +
                "0a2.25 2.25 0 0 1 4.5 0",
            "M6.209 15a.75.75 0 0 1 1.5 0v7a.75.75 0 0 1-1.5 0zm10.75 7.75a.75.75 0 0 1-.75-.75v-2a.75.75 0 0 " +
                "1 1.5 0v2a.75.75 0 0 1-.75.75M6.209 2a.75.75 0 0 1 1.5 0v2a.75.75 0 0 1-1.5 0z",
            name = "Equalizer",
            isEvenOdd = true,
        )
    }

    val Fast: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Fast", "M248.67,114.66,160.48,58.5A15.91,15.91,0,0,0,136,71.84v37.3L56.48,58.5A15.91,15.91,0,0,0,32,71.84V184.16A15.92,15.92,0,0,0,56.48,197.5L136,146.86v37.3a15.92,15.92,0,0,0,24.48,13.34l88.19-56.16a15.8,15.8,0,0,0,0-26.68ZM48,183.94V72.07L135.82,128Zm104,0V72.07L239.82,128Z")
    }

    val FileOpen: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("FileOpen", "M213.66,82.34l-56-56A8,8,0,0,0,152,24H56A16,16,0,0,0,40,40V216a16,16,0,0,0,16,16H200a16,16,0,0,0,16-16V88A8,8,0,0,0,213.66,82.34ZM160,51.31,188.69,80H160ZM200,216H56V40h88V88a8,8,0,0,0,8,8h48V216Zm-42.34-77.66a8,8,0,0,1-11.32,11.32L136,139.31V184a8,8,0,0,1-16,0V139.31l-10.34,10.35a8,8,0,0,1-11.32-11.32l24-24a8,8,0,0,1,11.32,0Z")
    }

    val Focus: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Focus", "M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm8,191.63V184a8,8,0,0,0-16,0v31.63A88.13,88.13,0,0,1,40.37,136H72a8,8,0,0,0,0-16H40.37A88.13,88.13,0,0,1,120,40.37V72a8,8,0,0,0,16,0V40.37A88.13,88.13,0,0,1,215.63,120H184a8,8,0,0,0,0,16h31.63A88.13,88.13,0,0,1,136,215.63Z")
    }

    /**
     * Solar's, which is the body [FolderOff] already drew its minus on.
     *
     * It was Phosphor's tabbed folder, so the settings row and the empty state a tap away were two
     * different folders -- one tabbed and square, one rounded and stroked. Whichever was right, they
     * could not both be: a folder is one drawing wherever the app puts it.
     */
    val Folder: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector("Folder", StrokedPath(FolderBody))
    }

    /** [Folder] with a minus in it: the row that takes folders *out* of the library. */
    val FolderOff: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "FolderOff",
            StrokedPath("M14 14h-4", hasRoundCaps = true),
            StrokedPath(FolderBody),
        )
    }

    val FontSize: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("FontSize", "M208,56V88a8,8,0,0,1-16,0V64H136V192h24a8,8,0,0,1,0,16H96a8,8,0,0,1,0-16h24V64H64V88a8,8,0,0,1-16,0V56a8,8,0,0,1,8-8H200A8,8,0,0,1,208,56Z")
    }

    val Globe: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Globe", "M128,24h0A104,104,0,1,0,232,128,104.12,104.12,0,0,0,128,24Zm88,104a87.61,87.61,0,0,1-3.33,24H174.16a157.44,157.44,0,0,0,0-48h38.51A87.61,87.61,0,0,1,216,128ZM102,168H154a115.11,115.11,0,0,1-26,45A115.27,115.27,0,0,1,102,168Zm-3.9-16a140.84,140.84,0,0,1,0-48h59.88a140.84,140.84,0,0,1,0,48ZM40,128a87.61,87.61,0,0,1,3.33-24H81.84a157.44,157.44,0,0,0,0,48H43.33A87.61,87.61,0,0,1,40,128ZM154,88H102a115.11,115.11,0,0,1,26-45A115.27,115.27,0,0,1,154,88Zm52.33,0H170.71a135.28,135.28,0,0,0-22.3-45.6A88.29,88.29,0,0,1,206.37,88ZM107.59,42.4A135.28,135.28,0,0,0,85.29,88H49.63A88.29,88.29,0,0,1,107.59,42.4ZM49.63,168H85.29a135.28,135.28,0,0,0,22.3,45.6A88.29,88.29,0,0,1,49.63,168Zm98.78,45.6a135.28,135.28,0,0,0,22.3-45.6h35.66A88.29,88.29,0,0,1,148.41,213.6Z")
    }

    val Headset: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Headset", "M201.89,54.66A103.43,103.43,0,0,0,128.79,24H128A104,104,0,0,0,24,128v56a24,24,0,0,0,24,24H64a24,24,0,0,0,24-24V144a24,24,0,0,0-24-24H40.36A88,88,0,0,1,128,40h.67a87.71,87.71,0,0,1,87,80H192a24,24,0,0,0-24,24v40a24,24,0,0,0,24,24h16a24,24,0,0,0,24-24V128A103.41,103.41,0,0,0,201.89,54.66ZM64,136a8,8,0,0,1,8,8v40a8,8,0,0,1-8,8H48a8,8,0,0,1-8-8V136Zm152,48a8,8,0,0,1-8,8H192a8,8,0,0,1-8-8V144a8,8,0,0,1,8-8h24Z")
    }

    val Info: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Info", "M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm0,192a88,88,0,1,1,88-88A88.1,88.1,0,0,1,128,216Zm16-40a8,8,0,0,1-8,8,16,16,0,0,1-16-16V128a8,8,0,0,1,0-16,16,16,0,0,1,16,16v40A8,8,0,0,1,144,176ZM112,84a12,12,0,1,1,12,12A12,12,0,0,1,112,84Z")
    }

    val Language: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Language", "M247.15,212.42l-56-112a8,8,0,0,0-14.31,0l-21.71,43.43A88,88,0,0,1,108,126.93,103.65,103.65,0,0,0,135.69,64H160a8,8,0,0,0,0-16H104V32a8,8,0,0,0-16,0V48H32a8,8,0,0,0,0,16h87.63A87.76,87.76,0,0,1,96,116.35a87.74,87.74,0,0,1-19-31,8,8,0,1,0-15.08,5.34A103.63,103.63,0,0,0,84,127a87.55,87.55,0,0,1-52,17,8,8,0,0,0,0,16,103.46,103.46,0,0,0,64-22.08,104.18,104.18,0,0,0,51.44,21.31l-26.6,53.19a8,8,0,0,0,14.31,7.16L148.94,192h70.11l13.79,27.58A8,8,0,0,0,240,224a8,8,0,0,0,7.15-11.58ZM156.94,176,184,121.89,211.05,176Z")
    }

    val Length: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Length", "M235.32,73.37,182.63,20.69a16,16,0,0,0-22.63,0L20.68,160a16,16,0,0,0,0,22.63l52.69,52.68a16,16,0,0,0,22.63,0L235.32,96A16,16,0,0,0,235.32,73.37ZM84.68,224,32,171.31l32-32,26.34,26.35a8,8,0,0,0,11.32-11.32L75.31,128,96,107.31l26.34,26.35a8,8,0,0,0,11.32-11.32L107.31,96,128,75.31l26.34,26.35a8,8,0,0,0,11.32-11.32L139.31,64l32-32L224,84.69Z")
    }

    val Location: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Location", "M128,64a40,40,0,1,0,40,40A40,40,0,0,0,128,64Zm0,64a24,24,0,1,1,24-24A24,24,0,0,1,128,128Zm0-112a88.1,88.1,0,0,0-88,88c0,31.4,14.51,64.68,42,96.25a254.19,254.19,0,0,0,41.45,38.3,8,8,0,0,0,9.18,0A254.19,254.19,0,0,0,174,200.25c27.45-31.57,42-64.85,42-96.25A88.1,88.1,0,0,0,128,16Zm0,206c-16.53-13-72-60.75-72-118a72,72,0,0,1,144,0C200,161.23,144.53,209,128,222Z")
    }

    /**
     * A slate, not a screen with a play triangle on it. The triangle is this app's word for "start
     * playing"; borrowing it here would make the tab, the folder and the missing-thumbnail
     * placeholder all look like buttons. The slate names the thing, the way the note does for audio.
     */
    /** One film: a clapperboard. [VideoLibrary] is the shelf of them. */
    val Video: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "Video",
            StrokedPath(
                "M2 12c0-4.714 0-7.071 1.464-8.536C4.93 2 7.286 2 12 2s7.071 0 8.535 1.464C22 4.93 22 7.286 22 " +
                    "12s0 7.071-1.465 8.535C19.072 22 16.714 22 12 22s-7.071 0-8.536-1.465C2 19.072 2 16.714 2 12Z",
            ),
            StrokedPath("M21.5 8h-19m8-5.5L7 8m10-5.5L13.5 8", hasRoundCaps = true),
        )
    }

    /**
     * Solar's, where the screen stops at the corner the small window sits in rather than closing
     * behind it. Phosphor's drew the screen whole and notched the window out of it, so the two read
     * as one shape broken in two; leaving the corner open is what says one picture is on top of
     * another. Even-odd, which is what keeps the window a window and not a second plate.
     */
    val Pip: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        vector(
            "M9.94358 2.25H14.0564C15.8942 2.24998 17.3498 2.24997 18.489 2.40314C19.6614 2.56076 20.6104 2.89288 21.3588 3.64124C22.1071 4.38961 22.4392 5.33856 22.5969 6.51098C22.75 7.65019 22.75 9.10583 22.75 10.9436V11C22.75 11.4142 22.4142 11.75 22 11.75C21.5858 11.75 21.25 11.4142 21.25 11C21.25 9.09318 21.2484 7.73851 21.1102 6.71085C20.975 5.70476 20.7213 5.12511 20.2981 4.7019C19.8749 4.27869 19.2952 4.02502 18.2892 3.88976C17.2615 3.75159 15.9068 3.75 14 3.75H10C8.09318 3.75 6.73851 3.75159 5.71085 3.88976C4.70476 4.02502 4.12511 4.27869 3.7019 4.7019C3.27869 5.12511 3.02502 5.70476 2.88976 6.71085C2.75159 7.73851 2.75 9.09318 2.75 11V13C2.75 14.9068 2.75159 16.2615 2.88976 17.2892C3.02502 18.2952 3.27869 18.8749 3.7019 19.2981C4.12511 19.7213 4.70476 19.975 5.71085 20.1102C6.73851 20.2484 8.09318 20.25 10 20.25H11C11.4142 20.25 11.75 20.5858 11.75 21C11.75 21.4142 11.4142 21.75 11 21.75H9.94359C8.10583 21.75 6.65019 21.75 5.51098 21.5969C4.33856 21.4392 3.38961 21.1071 2.64124 20.3588C1.89288 19.6104 1.56076 18.6614 1.40314 17.489C1.24997 16.3498 1.24998 14.8942 1.25 13.0564V10.9436C1.24998 9.10582 1.24997 7.65019 1.40314 6.51098C1.56076 5.33856 1.89288 4.38961 2.64124 3.64124C3.38961 2.89288 4.33856 2.56076 5.51098 2.40314C6.65019 2.24997 8.10582 2.24998 9.94358 2.25Z" +
                "M16.948 12.25H18.052C18.9505 12.25 19.6997 12.2499 20.2945 12.3299C20.9223 12.4143 21.4891 12.6 21.9445 13.0555C22.4 13.5109 22.5857 14.0777 22.6701 14.7055C22.7501 15.3003 22.75 16.0495 22.75 16.948V17.052C22.75 17.9505 22.7501 18.6997 22.6701 19.2945C22.5857 19.9223 22.4 20.4891 21.9445 20.9445C21.4891 21.4 20.9223 21.5857 20.2945 21.6701C19.6997 21.7501 18.9505 21.75 18.052 21.75H16.948C16.0495 21.75 15.3003 21.7501 14.7055 21.6701C14.0777 21.5857 13.5109 21.4 13.0555 20.9445C12.6 20.4891 12.4143 19.9223 12.3299 19.2945C12.2499 18.6997 12.25 17.9505 12.25 17.052V16.948C12.25 16.0495 12.2499 15.3003 12.3299 14.7055C12.4143 14.0777 12.6 13.5109 13.0555 13.0555C13.5109 12.6 14.0777 12.4143 14.7055 12.3299C15.3003 12.2499 16.0495 12.25 16.948 12.25Z" +
                "M14.9054 13.8165C14.4439 13.8786 14.2464 13.9858 14.1161 14.1161C13.9858 14.2464 13.8786 14.4439 13.8165 14.9054C13.7516 15.3884 13.75 16.036 13.75 17C13.75 17.964 13.7516 18.6116 13.8165 19.0946C13.8786 19.5561 13.9858 19.7536 14.1161 19.8839C14.2464 20.0142 14.4439 20.1214 14.9054 20.1835C15.3884 20.2484 16.036 20.25 17 20.25H18C18.964 20.25 19.6116 20.2484 20.0946 20.1835C20.5561 20.1214 20.7536 20.0142 20.8839 19.8839C21.0142 19.7536 21.1214 19.5561 21.1835 19.0946C21.2484 18.6116 21.25 17.964 21.25 17C21.25 16.036 21.2484 15.3884 21.1835 14.9054C21.1214 14.4439 21.0142 14.2464 20.8839 14.1161C20.7536 13.9858 20.5561 13.8786 20.0946 13.8165C19.6116 13.7516 18.964 13.75 18 13.75H17C16.036 13.75 15.3884 13.7516 14.9054 13.8165Z",
            name = "Pip",
            isEvenOdd = true,
        )
    }

    /**
     * Kept where it can be reached, which is not the same as liked.
     *
     * The star says "this is one of mine" and gathers what it marks into a list. A pin says "put
     * this here" -- a folder six levels down on a machine in another room, held at the top of the
     * screen it is reached from. Two acts, two marks; they were one star doing both, and the folder
     * menus ended up saying "Pin" beside a star.
     *
     * Solid rather than outlined: a pin is small and read at a glance, and its silhouette is what
     * makes it a pin. An outline at this size is a smudge with a hole in it.
     */
    val Pin: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        vector("M16,12V4h1V2H7v2h1v8l-2,2v2h5.2v6h1.6v-6H18v-2L16,12z", name = "Pin")
    }

    val Pinch: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Pinch", "M196,88a27.86,27.86,0,0,0-13.35,3.39A28,28,0,0,0,144,74.7V44a28,28,0,0,0-56,0v80l-3.82-6.13A28,28,0,0,0,35.73,146l4.67,8.23C74.81,214.89,89.05,240,136,240a88.1,88.1,0,0,0,88-88V116A28,28,0,0,0,196,88Zm12,64a72.08,72.08,0,0,1-72,72c-37.63,0-47.84-18-81.68-77.68l-4.69-8.27,0-.05A12,12,0,0,1,54,121.61a11.88,11.88,0,0,1,6-1.6,12,12,0,0,1,10.41,6,1.76,1.76,0,0,0,.14.23l18.67,30A8,8,0,0,0,104,152V44a12,12,0,0,1,24,0v68a8,8,0,0,0,16,0V100a12,12,0,0,1,24,0v20a8,8,0,0,0,16,0v-4a12,12,0,0,1,24,0Z")
    }

    /**
     * Phosphor's playlist: three stacked lines with a note lifting off the last one. The app was
     * still drawing this one from a leftover drawable while every other glyph came from the family,
     * so it carried a different stroke and a different optical weight at the same size.
     */
    /**
     * A running order: lines with a play mark beside them.
     *
     * [Playlist] has a note on it and belongs to music. Plain lines alone were not enough -- three
     * strokes with nothing beside them is a menu, and that is what it read as.
     */
    val Queue: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "Queue",
            StrokedPath("M21 6H3m18 4H3m8 4H3m8 4H3", hasRoundCaps = true),
            StrokedPath(
                "M18.875 14.118c1.654.955 2.48 1.433 2.602 2.121a1.5 1.5 0 0 1 0 .521c-.121.69-.948 1.167-2.602 2.121c-" +
                    "1.654.955-2.48 1.433-3.138 1.194a1.5 1.5 0 0 1-.451-.261c-.536-.45-.536-1.404-.536-3.314s0-2.865" +
                    ".536-3.314a1.5 1.5 0 0 1 .451-.26c.657-.24 1.484.238 3.138 1.192Z",
            ),
        )
    }

    /**
     * A list of things to play. Lines and nothing else, which is what a list of films is.
     *
     * [MusicPlaylist] is the same list with a note on the end of it, and the two are not
     * interchangeable: the note was on both, so a folder of films was filed under a music symbol.
     */
    val Playlist: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor(
            "Playlist",
            "M32,64a8,8,0,0,1,8-8H216a8,8,0,0,1,0,16H40A8,8,0,0,1,32,64Zm8,72H216a8,8,0,0,0,0-16H40a" +
                "8,8,0,0,0,0,16Zm104,48H40a8,8,0,0,0,0,16H144a8,8,0,0,0,0-16Z",
        )
    }

    /** The same list with a note at the end: for tracks, where a film's list would be bare. */
    val MusicPlaylist: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor(
            "MusicPlaylist",
            "M32,64a8,8,0,0,1,8,-8H216a8,8,0,0,1,0,16H40A8,8,0,0,1,32,64Zm8,72H160a8,8,0,0,0,0,-16H40a" +
                "8,8,0,0,0,0,16Zm72,48H40a8,8,0,0,0,0,16h72a8,8,0,0,0,0,-16Zm135.66,-57.7a8,8,0,0,1," +
                "-10,5.36L208,122.75V192a32.05,32.05,0,1,1,-16,-27.69V112a8,8,0,0,1,10.3,-7.66l40,12A" +
                "8,8,0,0,1,247.66,126.3ZM192,192a16,16,0,1,0,-16,16A16,16,0,0,0,192,192Z",
        )
    }

    /**
     * Add to the end of the list.
     *
     * For the menu entry that queues something, as against [Queue], which opens the queue. One adds
     * and one shows, and they were the same drawing.
     */
    val ListPlus: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor(
            "ListPlus",
            "M32,64a8,8,0,0,1,8-8H216a8,8,0,0,1,0,16H40A8,8,0,0,1,32,64Zm8,72H216a8,8,0,0,0,0-16H40a" +
                "8,8,0,0,0,0,16Zm104,48H40a8,8,0,0,0,0,16H144a8,8,0,0,0,0-16Zm88,0H216V168a8,8,0,0,0," +
                "-16,0v16H184a8,8,0,0,0,0,16h16v16a8,8,0,0,0,16,0V200h16a8,8,0,0,0,0-16Z",
        )
    }

    /** Not Phosphor's: this one is drawn on the standard grid, with the corners rounded. */
    val Play: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        vector(
            "M21.409 9.353a2.998 2.998 0 0 1 0 5.294L8.597 21.614C6.534 22.737 4 21.277 4 " +
                "18.968V5.033c0-2.31 2.534-3.769 4.597-2.648z",
            name = "Play",
        )
    }

    val Player: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Player", "M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm0,192a88,88,0,1,1,88-88A88.1,88.1,0,0,1,128,216Zm48.24-94.78-64-40A8,8,0,0,0,100,88v80a8,8,0,0,0,12.24,6.78l64-40a8,8,0,0,0,0-13.56ZM116,153.57V102.43L156.91,128Z")
    }

    val Priority: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Priority", "M236.8,188.09,149.35,36.22h0a24.76,24.76,0,0,0-42.7,0L19.2,188.09a23.51,23.51,0,0,0,0,23.72A24.35,24.35,0,0,0,40.55,224h174.9a24.35,24.35,0,0,0,21.33-12.19A23.51,23.51,0,0,0,236.8,188.09ZM222.93,203.8a8.5,8.5,0,0,1-7.48,4.2H40.55a8.5,8.5,0,0,1-7.48-4.2,7.59,7.59,0,0,1,0-7.72L120.52,44.21a8.75,8.75,0,0,1,15,0l87.45,151.87A7.59,7.59,0,0,1,222.93,203.8ZM120,144V104a8,8,0,0,1,16,0v40a8,8,0,0,1-16,0Zm20,36a12,12,0,1,1-12-12A12,12,0,0,1,140,180Z")
    }

    val Replay: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Replay", "M224,128a96,96,0,0,1-94.71,96H128A95.38,95.38,0,0,1,62.1,197.8a8,8,0,0,1,11-11.63A80,80,0,1,0,71.43,71.39a3.07,3.07,0,0,1-.26.25L44.59,96H72a8,8,0,0,1,0,16H24a8,8,0,0,1-8-8V56a8,8,0,0,1,16,0V85.8L60.25,60A96,96,0,0,1,224,128Z")
    }

    val Resume: ImageVector get() = Tv

    val Rotation: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Rotation", "M240,56v48a8,8,0,0,1-8,8H184a8,8,0,0,1,0-16H211.4L184.81,71.64l-.25-.24a80,80,0,1,0-1.67,114.78,8,8,0,0,1,11,11.63A95.44,95.44,0,0,1,128,224h-1.32A96,96,0,1,1,195.75,60L224,85.8V56a8,8,0,1,1,16,0Z")
    }

    /**
     * A shelf of films, for the tab that opens the library. Solar's, where the rest of the bar is
     * Phosphor: the two sit at the same 1.5 weight, and this is the one glyph in the set that says
     * "all of them" rather than "one of them".
     */
    val VideoLibrary: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "VideoLibrary",
            StrokedPath(
                "M19.562 7a2.132 2.132 0 0 0-2.1-2.5H6.538a2.132 2.132 0 0 0-2.1 2.5M17.5 4.5c.028-.26.043-.389" +
                    ".043-.496a2 2 0 0 0-1.787-1.993C15.65 2 15.52 2 15.26 2H8.74c-.26 0-.391 0-.497.011a2 2 0 0 " +
                    "0-1.787 1.993c0 .107.014.237.043.496m8.082 9.116c.559.346.559 1.242 0 1.588l-3.371 2.09c-.543" +
                    ".337-1.21-.1-1.21-.794v-4.18c0-.693.667-1.13 1.21-.794z",
                hasRoundCaps = true,
            ),
            StrokedPath(
                "M2.384 13.793c-.447-3.164-.67-4.745.278-5.77C3.61 7 5.298 7 8.672 7h6.656c3.374 0 5.062 0 6.01 " +
                    "1.024s.724 2.605.278 5.769l-.422 3c-.35 2.48-.525 3.721-1.422 4.464s-2.22.743-4.867.743h-5.81" +
                    "c-2.646 0-3.97 0-4.867-.743s-1.072-1.983-1.422-4.464z",
            ),
        )
    }

    val VideoLibraryFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        vector(
            "M8.51 2h6.98c.232 0 .41 0 .566.015c1.108.109 2.015.775 2.4 1.672H5.544c.385-.897 1.292-1.563 2.4-" +
                "1.672C8.098 2 8.276 2 8.51 2m-2.2 2.723c-1.39 0-2.53.84-2.91 1.954l-.024.07c.398-.12.813-.2 " +
                "1.232-.253c1.08-.139 2.446-.139 4.032-.139h6.892c1.586 0 2.951 0 4.032.139c.42.054.834.132 " +
                "1.232.253l-.023-.07c-.38-1.114-1.52-1.954-2.911-1.954z",
            "M15.328 7.542H8.672c-3.374 0-5.062 0-6.01.987s-.725 2.511-.278 5.56l.422 2.892c.35 2.391.525 3.587 " +
                "1.422 4.303c.898.716 2.22.716 4.867.716h5.81c2.646 0 3.97 0 4.867-.716s1.072-1.912 1.422-4.303l" +
                ".422-2.891c.447-3.05.67-4.574-.278-5.561s-2.636-.987-6.01-.987m-.747 8.252c.559-.346.559-1.242 " +
                "0-1.588l-3.371-2.09c-.543-.337-1.21.101-1.21.794v4.18c0 .693.667 1.13 1.21.794z",
            name = "VideoLibraryFilled",
            isEvenOdd = true,
        )
    }

    val Search: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Search", "M229.66,218.34l-50.07-50.06a88.11,88.11,0,1,0-11.31,11.31l50.06,50.07a8,8,0,0,0,11.32-11.32ZM40,112a72,72,0,1,1,72,72A72.08,72.08,0,0,1,40,112Z")
    }

    val Settings: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Settings", "M128,80a48,48,0,1,0,48,48A48.05,48.05,0,0,0,128,80Zm0,80a32,32,0,1,1,32-32A32,32,0,0,1,128,160Zm109.94-52.79a8,8,0,0,0-3.89-5.4l-29.83-17-.12-33.62a8,8,0,0,0-2.83-6.08,111.91,111.91,0,0,0-36.72-20.67,8,8,0,0,0-6.46.59L128,41.85,97.88,25a8,8,0,0,0-6.47-.6A112.1,112.1,0,0,0,54.73,45.15a8,8,0,0,0-2.83,6.07l-.15,33.65-29.83,17a8,8,0,0,0-3.89,5.4,106.47,106.47,0,0,0,0,41.56,8,8,0,0,0,3.89,5.4l29.83,17,.12,33.62a8,8,0,0,0,2.83,6.08,111.91,111.91,0,0,0,36.72,20.67,8,8,0,0,0,6.46-.59L128,214.15,158.12,231a7.91,7.91,0,0,0,3.9,1,8.09,8.09,0,0,0,2.57-.42,112.1,112.1,0,0,0,36.68-20.73,8,8,0,0,0,2.83-6.07l.15-33.65,29.83-17a8,8,0,0,0,3.89-5.4A106.47,106.47,0,0,0,237.94,107.21Zm-15,34.91-28.57,16.25a8,8,0,0,0-3,3c-.58,1-1.19,2.06-1.81,3.06a7.94,7.94,0,0,0-1.22,4.21l-.15,32.25a95.89,95.89,0,0,1-25.37,14.3L134,199.13a8,8,0,0,0-3.91-1h-.19c-1.21,0-2.43,0-3.64,0a8.08,8.08,0,0,0-4.1,1l-28.84,16.1A96,96,0,0,1,67.88,201l-.11-32.2a8,8,0,0,0-1.22-4.22c-.62-1-1.23-2-1.8-3.06a8.09,8.09,0,0,0-3-3.06l-28.6-16.29a90.49,90.49,0,0,1,0-28.26L61.67,97.63a8,8,0,0,0,3-3c.58-1,1.19-2.06,1.81-3.06a7.94,7.94,0,0,0,1.22-4.21l.15-32.25a95.89,95.89,0,0,1,25.37-14.3L122,56.87a8,8,0,0,0,4.1,1c1.21,0,2.43,0,3.64,0a8.08,8.08,0,0,0,4.1-1l28.84-16.1A96,96,0,0,1,188.12,55l.11,32.2a8,8,0,0,0,1.22,4.22c.62,1,1.23,2,1.8,3.06a8.09,8.09,0,0,0,3,3.06l28.6,16.29A90.49,90.49,0,0,1,222.9,142.12Z")
    }

    val Share: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Share", "M176,160a39.89,39.89,0,0,0-28.62,12.09l-46.1-29.63a39.8,39.8,0,0,0,0-28.92l46.1-29.63a40,40,0,1,0-8.66-13.45l-46.1,29.63a40,40,0,1,0,0,55.82l46.1,29.63A40,40,0,1,0,176,160Zm0-128a24,24,0,1,1-24,24A24,24,0,0,1,176,32ZM64,152a24,24,0,1,1,24-24A24,24,0,0,1,64,152Zm112,72a24,24,0,1,1,24-24A24,24,0,0,1,176,224Z")
    }

    val Style: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Style", "M232,32a8,8,0,0,0-8-8c-44.08,0-89.31,49.71-114.43,82.63A60,60,0,0,0,32,164c0,30.88-19.54,44.73-20.47,45.37A8,8,0,0,0,16,224H92a60,60,0,0,0,57.37-77.57C182.3,121.31,232,76.08,232,32ZM92,208H34.63C41.38,198.41,48,183.92,48,164a44,44,0,1,1,44,44Zm32.42-94.45q5.14-6.66,10.09-12.55A76.23,76.23,0,0,1,155,121.49q-5.9,4.94-12.55,10.09A60.54,60.54,0,0,0,124.42,113.55Zm42.7-2.68a92.57,92.57,0,0,0-22-22c31.78-34.53,55.75-45,69.9-47.91C212.17,55.12,201.65,79.09,167.12,110.87Z")
    }

    val Subtitle: ImageVector get() = Caption

    val Size: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Size", "M213.66,181.66l-32,32a8,8,0,0,1-11.32-11.32L188.69,184H48a8,8,0,0,1,0-16H188.69l-18.35-18.34a8,8,0,0,1,11.32-11.32l32,32A8,8,0,0,1,213.66,181.66Zm-139.32-64a8,8,0,0,0,11.32-11.32L67.31,88H208a8,8,0,0,0,0-16H67.31L85.66,53.66A8,8,0,0,0,74.34,42.34l-32,32a8,8,0,0,0,0,11.32Z", autoMirror = true)
    }

    val Sensitivity: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Sensitivity", "M64,105V40a8,8,0,0,0-16,0v65a32,32,0,0,0,0,62v49a8,8,0,0,0,16,0V167a32,32,0,0,0,0-62Zm-8,47a16,16,0,1,1,16-16A16,16,0,0,1,56,152Zm80-95V40a8,8,0,0,0-16,0V57a32,32,0,0,0,0,62v97a8,8,0,0,0,16,0V119a32,32,0,0,0,0-62Zm-8,47a16,16,0,1,1,16-16A16,16,0,0,1,128,104Zm104,64a32.06,32.06,0,0,0-24-31V40a8,8,0,0,0-16,0v97a32,32,0,0,0,0,62v17a8,8,0,0,0,16,0V199A32.06,32.06,0,0,0,232,168Zm-32,16a16,16,0,1,1,16-16A16,16,0,0,1,200,184Z")
    }

    /**
     * Rows of unequal length with an arrow beside them: the glyph for putting a list in an order,
     * rather than the bare arrow that was standing in for it. Phosphor's own, so it sits at the
     * same weight as everything around it.
     *
     * Two of them and not one, because the arrow is the half that carries the meaning: which way
     * the list runs is the thing a viewer is looking for, and a single static mark would say only
     * that an order exists.
     */
    val SortAscending: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("SortAscending", "M128,128a8,8,0,0,1-8,8H48a8,8,0,0,1,0-16h72A8,8,0,0,1,128,128ZM48,72H184a8,8,0,0,0,0-16H48a8,8,0,0,0,0,16Zm56,112H48a8,8,0,0,0,0,16h56a8,8,0,0,0,0-16Zm125.66-21.66a8,8,0,0,0-11.32,0L192,188.69V112a8,8,0,0,0-16,0v76.69l-26.34-26.35a8,8,0,0,0-11.32,11.32l40,40a8,8,0,0,0,11.32,0l40-40A8,8,0,0,0,229.66,162.34Z")
    }

    val SortDescending: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("SortDescending", "M40,128a8,8,0,0,1,8-8h72a8,8,0,0,1,0,16H48A8,8,0,0,1,40,128Zm8-56h56a8,8,0,0,0,0-16H48a8,8,0,0,0,0,16ZM184,184H48a8,8,0,0,0,0,16H184a8,8,0,0,0,0-16ZM229.66,82.34l-40-40a8,8,0,0,0-11.32,0l-40,40a8,8,0,0,0,11.32,11.32L176,67.31V144a8,8,0,0,0,16,0V67.31l26.34,26.35a8,8,0,0,0,11.32-11.32Z")
    }

    val Speed: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Speed", "M207.06,72.67A111.24,111.24,0,0,0,128,40h-.4C66.07,40.21,16,91,16,153.13V176a16,16,0,0,0,16,16H224a16,16,0,0,0,16-16V152A111.25,111.25,0,0,0,207.06,72.67ZM224,176H119.71l54.76-75.3a8,8,0,0,0-12.94-9.42L99.92,176H32V153.13c0-3.08.15-6.12.43-9.13H56a8,8,0,0,0,0-16H35.27c10.32-38.86,44-68.24,84.73-71.66V80a8,8,0,0,0,16,0V56.33A96.14,96.14,0,0,1,221,128H200a8,8,0,0,0,0,16h23.67c.21,2.65.33,5.31.33,8Z")
    }

    val SwipeHorizontal: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("SwipeHorizontal", "M216,140v36c0,25.59-8.49,42.85-8.85,43.58A8,8,0,0,1,200,224a7.9,7.9,0,0,1-3.57-.85,8,8,0,0,1-3.58-10.73c.06-.12,7.16-14.81,7.16-36.42V140a12,12,0,0,0-24,0v4a8,8,0,0,1-16,0V124a12,12,0,0,0-24,0v12a8,8,0,0,1-16,0V68a12,12,0,0,0-24,0V176a8,8,0,0,1-14.79,4.23l-18.68-30-.14-.23A12,12,0,1,0,41.6,162L70.89,212A8,8,0,1,1,57.08,220l-29.32-50a28,28,0,0,1,48.41-28.17L80,148V68a28,28,0,0,1,56,0V98.7a28,28,0,0,1,38.65,16.69A28,28,0,0,1,216,140Zm37.66-89.66-32-32a8,8,0,0,0-11.31,11.32L228.68,48H176a8,8,0,0,0,0,16h52.69L210.34,82.34a8,8,0,0,0,11.31,11.32l32-32A8,8,0,0,0,253.66,50.34Z")
    }

    val SwipeVertical: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("SwipeVertical", "M165.66,194.34a8,8,0,0,1,0,11.32l-32,32a8,8,0,0,1-11.32,0l-32-32a8,8,0,0,1,11.32-11.32L120,212.69V43.31L101.66,61.66A8,8,0,0,1,90.34,50.34l32-32a8,8,0,0,1,11.32,0l32,32a8,8,0,0,1-11.32,11.32L136,43.31V212.69l18.34-18.35A8,8,0,0,1,165.66,194.34Z")
    }

    val Timer: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Timer", "M128,40a96,96,0,1,0,96,96A96.11,96.11,0,0,0,128,40Zm0,176a80,80,0,1,1,80-80A80.09,80.09,0,0,1,128,216ZM173.66,90.34a8,8,0,0,1,0,11.32l-40,40a8,8,0,0,1-11.32-11.32l40-40A8,8,0,0,1,173.66,90.34ZM96,16a8,8,0,0,1,8-8h48a8,8,0,0,1,0,16H104A8,8,0,0,1,96,16Z")
    }

    val Title: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Title", "M208,56V200a8,8,0,0,1-16,0V136H64v64a8,8,0,0,1-16,0V56a8,8,0,0,1,16,0v64H192V56a8,8,0,0,1,16,0Z")
    }

    val Update: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Update", "M224,48V96a8,8,0,0,1-8,8H168a8,8,0,0,1,0-16h28.69L182.06,73.37a79.56,79.56,0,0,0-56.13-23.43h-.45A79.52,79.52,0,0,0,69.59,72.71,8,8,0,0,1,58.41,61.27a96,96,0,0,1,135,.79L208,76.69V48a8,8,0,0,1,16,0ZM186.41,183.29a80,80,0,0,1-112.47-.66L59.31,168H88a8,8,0,0,0,0-16H40a8,8,0,0,0-8,8v48a8,8,0,0,0,16,0V179.31l14.63,14.63A95.43,95.43,0,0,0,130,222.06h.53a95.36,95.36,0,0,0,67.07-27.33,8,8,0,0,0-11.18-11.44Z")
    }

    val VideoFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("VideoFilled", "M216,104H102.09L210,75.51a8,8,0,0,0,5.68-9.84l-8.16-30a15.93,15.93,0,0,0-19.42-11.13L35.81,64.74a15.75,15.75,0,0,0-9.7,7.4,15.51,15.51,0,0,0-1.55,12L32,111.56c0,.14,0,.29,0,.44v88a16,16,0,0,0,16,16H208a16,16,0,0,0,16-16V112A8,8,0,0,0,216,104ZM192.16,40l6,22.07L164.57,71,136.44,54.72ZM77.55,70.27l28.12,16.24-59.6,15.73-6-22.08Z")
    }

    val VolumeUp: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("VolumeUp", "M155.51,24.81a8,8,0,0,0-8.42.88L77.25,80H32A16,16,0,0,0,16,96v64a16,16,0,0,0,16,16H77.25l69.84,54.31A8,8,0,0,0,160,224V32A8,8,0,0,0,155.51,24.81ZM32,96H72v64H32ZM144,207.64,88,164.09V91.91l56-43.55Zm54-106.08a40,40,0,0,1,0,52.88,8,8,0,0,1-12-10.58,24,24,0,0,0,0-31.72,8,8,0,0,1,12-10.58ZM248,128a79.9,79.9,0,0,1-20.37,53.34,8,8,0,0,1-11.92-10.67,64,64,0,0,0,0-85.33,8,8,0,1,1,11.92-10.67A79.83,79.83,0,0,1,248,128Z", autoMirror = true)
    }

    val Close: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Close", "M205.66,194.34a8,8,0,0,1-11.32,11.32L128,139.31,61.66,205.66a8,8,0,0,1-11.32-11.32L116.69,128,50.34,61.66A8,8,0,0,1,61.66,50.34L128,116.69l66.34-66.35a8,8,0,0,1,11.32,11.32L139.31,128Z")
    }

    val History: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("History", "M136,80v43.47l36.12,21.67a8,8,0,0,1-8.24,13.72l-40-24A8,8,0,0,1,120,128V80a8,8,0,0,1,16,0Zm-8-48A95.44,95.44,0,0,0,60.08,60.15C52.81,67.51,46.35,74.59,40,82V64a8,8,0,0,0-16,0v40a8,8,0,0,0,8,8H72a8,8,0,0,0,0-16H49c7.15-8.42,14.27-16.35,22.39-24.57a80,80,0,1,1,1.66,114.75,8,8,0,1,0-11,11.64A96,96,0,1,0,128,32Z")
    }

    val FolderFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("FolderFilled", "M232,88V200.89A15.13,15.13,0,0,1,216.89,216H40a16,16,0,0,1-16-16V64A16,16,0,0,1,40,48H93.33a16.12,16.12,0,0,1,9.6,3.2L130.67,72H216A16,16,0,0,1,232,88Z")
    }

    val StarFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("StarFilled", "M234.29,114.85l-45,38.83L203,211.75a16.4,16.4,0,0,1-24.5,17.82L128,198.49,77.47,229.57A16.4,16.4,0,0,1,53,211.75l13.76-58.07-45-38.83A16.46,16.46,0,0,1,31.08,86l59-4.76,22.76-55.08a16.36,16.36,0,0,1,30.27,0l22.75,55.08,59,4.76a16.46,16.46,0,0,1,9.37,28.86Z")
    }

    val StarOutlined: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("StarOutlined", "M239.18,97.26A16.38,16.38,0,0,0,224.92,86l-59-4.76L143.14,26.15a16.36,16.36,0,0,0-30.27,0L90.11,81.23,31.08,86a16.46,16.46,0,0,0-9.37,28.86l45,38.83L53,211.75a16.38,16.38,0,0,0,24.5,17.82L128,198.49l50.53,31.08A16.4,16.4,0,0,0,203,211.75l-13.76-58.07,45-38.83A16.43,16.43,0,0,0,239.18,97.26Zm-15.34,5.47-48.7,42a8,8,0,0,0-2.56,7.91l14.88,62.8a.37.37,0,0,1-.17.48c-.18.14-.23.11-.38,0l-54.72-33.65a8,8,0,0,0-8.38,0L69.09,215.94c-.15.09-.19.12-.38,0a.37.37,0,0,1-.17-.48l14.88-62.8a8,8,0,0,0-2.56-7.91l-48.7-42c-.12-.1-.23-.19-.13-.5s.18-.27.33-.29l63.92-5.16A8,8,0,0,0,103,91.86l24.62-59.61c.08-.17.11-.25.35-.25s.27.08.35.25L153,91.86a8,8,0,0,0,6.75,4.92l63.92,5.16c.15,0,.24,0,.33.29S224,102.63,223.84,102.73Z")
    }

    val Network: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Network", "M128,24h0A104,104,0,1,0,232,128,104.12,104.12,0,0,0,128,24Zm87.62,96H175.79C174,83.49,159.94,57.67,148.41,42.4A88.19,88.19,0,0,1,215.63,120ZM96.23,136h63.54c-2.31,41.61-22.23,67.11-31.77,77C118.45,203.1,98.54,177.6,96.23,136Zm0-16C98.54,78.39,118.46,52.89,128,43c9.55,9.93,29.46,35.43,31.77,77Zm11.36-77.6C96.06,57.67,82,83.49,80.21,120H40.37A88.19,88.19,0,0,1,107.59,42.4ZM40.37,136H80.21c1.82,36.51,15.85,62.33,27.38,77.6A88.19,88.19,0,0,1,40.37,136Zm108,77.6c11.53-15.27,25.56-41.09,27.38-77.6h39.84A88.19,88.19,0,0,1,148.41,213.6Z")
    }

    val NetworkFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("NetworkFilled", "M128,24h0A104,104,0,1,0,232,128,104.12,104.12,0,0,0,128,24Zm87.62,96H175.79C174,83.49,159.94,57.67,148.41,42.4A88.19,88.19,0,0,1,215.63,120ZM96.23,136h63.54c-2.31,41.61-22.23,67.11-31.77,77C118.45,203.1,98.54,177.6,96.23,136Zm0-16C98.54,78.39,118.46,52.89,128,43c9.55,9.93,29.46,35.43,31.77,77Zm52.18,93.6c11.53-15.27,25.56-41.09,27.38-77.6h39.84A88.19,88.19,0,0,1,148.41,213.6Z")
    }

    val Wifi: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Wifi", "M140,204a12,12,0,1,1-12-12A12,12,0,0,1,140,204ZM237.08,87A172,172,0,0,0,18.92,87,8,8,0,0,0,29.08,99.37a156,156,0,0,1,197.84,0A8,8,0,0,0,237.08,87ZM205,122.77a124,124,0,0,0-153.94,0A8,8,0,0,0,61,135.31a108,108,0,0,1,134.06,0,8,8,0,0,0,11.24-1.3A8,8,0,0,0,205,122.77Zm-32.26,35.76a76.05,76.05,0,0,0-89.42,0,8,8,0,0,0,9.42,12.94,60,60,0,0,1,70.58,0,8,8,0,1,0,9.42-12.94Z")
    }

    val SettingsFilled: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("SettingsFilled", "M237.94,107.21a8,8,0,0,0-3.89-5.4l-29.83-17-.12-33.62a8,8,0,0,0-2.83-6.08,111.91,111.91,0,0,0-36.72-20.67,8,8,0,0,0-6.46.59L128,41.85,97.88,25a8,8,0,0,0-6.47-.6A111.92,111.92,0,0,0,54.73,45.15a8,8,0,0,0-2.83,6.07l-.15,33.65-29.83,17a8,8,0,0,0-3.89,5.4,106.47,106.47,0,0,0,0,41.56,8,8,0,0,0,3.89,5.4l29.83,17,.12,33.63a8,8,0,0,0,2.83,6.08,111.91,111.91,0,0,0,36.72,20.67,8,8,0,0,0,6.46-.59L128,214.15,158.12,231a7.91,7.91,0,0,0,3.9,1,8.09,8.09,0,0,0,2.57-.42,112.1,112.1,0,0,0,36.68-20.73,8,8,0,0,0,2.83-6.07l.15-33.65,29.83-17a8,8,0,0,0,3.89-5.4A106.47,106.47,0,0,0,237.94,107.21ZM128,168a40,40,0,1,1,40-40A40,40,0,0,1,128,168Z")
    }

    val SelectAll: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("SelectAll", "M104,40a8,8,0,0,1,8-8h32a8,8,0,0,1,0,16H112A8,8,0,0,1,104,40Zm40,168H112a8,8,0,0,0,0,16h32a8,8,0,0,0,0-16ZM208,32H184a8,8,0,0,0,0,16h24V72a8,8,0,0,0,16,0V48A16,16,0,0,0,208,32Zm8,72a8,8,0,0,0-8,8v32a8,8,0,0,0,16,0V112A8,8,0,0,0,216,104Zm0,72a8,8,0,0,0-8,8v24H184a8,8,0,0,0,0,16h24a16,16,0,0,0,16-16V184A8,8,0,0,0,216,176ZM40,152a8,8,0,0,0,8-8V112a8,8,0,0,0-16,0v32A8,8,0,0,0,40,152Zm32,56H48V184a8,8,0,0,0-16,0v24a16,16,0,0,0,16,16H72a8,8,0,0,0,0-16ZM40,80a8,8,0,0,0,8-8V48H72a8,8,0,0,0,0-16H48A16,16,0,0,0,32,48V72A8,8,0,0,0,40,80ZM176,184H80a8,8,0,0,1-8-8V80a8,8,0,0,1,8-8h96a8,8,0,0,1,8,8v96A8,8,0,0,1,176,184Zm-8-96H88v80h80Z")
    }

    val DeselectAll: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("DeselectAll", "M104,40a8,8,0,0,1,8-8h32a8,8,0,0,1,0,16H112A8,8,0,0,1,104,40Zm40,168H112a8,8,0,0,0,0,16h32a8,8,0,0,0,0-16ZM208,32H184a8,8,0,0,0,0,16h24V72a8,8,0,0,0,16,0V48A16,16,0,0,0,208,32Zm8,72a8,8,0,0,0-8,8v32a8,8,0,0,0,16,0V112A8,8,0,0,0,216,104ZM40,152a8,8,0,0,0,8-8V112a8,8,0,0,0-16,0v32A8,8,0,0,0,40,152Zm32,56H48V184a8,8,0,0,0-16,0v24a16,16,0,0,0,16,16H72a8,8,0,0,0,0-16ZM53.92,34.62A8,8,0,1,0,42.08,45.38l160,176a8,8,0,1,0,11.84-10.76Z")
    }

    val FastForward: ImageVector get() = Fast

    val ExtraSettings: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("ExtraSettings", "M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm0,192a88,88,0,1,1,88-88A88.1,88.1,0,0,1,128,216Zm12-88a12,12,0,1,1-12-12A12,12,0,0,1,140,128Zm44,0a12,12,0,1,1-12-12A12,12,0,0,1,184,128Zm-88,0a12,12,0,1,1-12-12A12,12,0,0,1,96,128Z")
    }

    val Image: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "Image",
            StrokedPath(
                "M2 12c0-4.714 0-7.071 1.464-8.536C4.93 2 7.286 2 12 2s7.071 0 8.535 1.464C22 4.93 22 7.286 22 " +
                    "12s0 7.071-1.465 8.535C19.072 22 16.714 22 12 22s-7.071 0-8.536-1.465C2 19.072 2 16.714 2 12Z",
            ),
            StrokedPath("M18 8a2 2 0 1 1-4 0a2 2 0 0 1 4 0Z"),
            StrokedPath(
                "m2 12.5l1.752-1.533a2.3 2.3 0 0 1 3.14.105l4.29 4.29a2 2 0 0 0 2.564.222l.299-.21a3 3 0 0 1 " +
                    "3.731.225L21 18.5",
                hasRoundCaps = true,
            ),
        )
    }

    /** A strip of film with its sprocket holes, for the setting that picks which frame is shown. */
    val Frame: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        strokedVector(
            "Frame",
            StrokedPath(
                "M2 12c0-4.714 0-7.071 1.464-8.536C4.93 2 7.286 2 12 2s7.071 0 8.535 1.464C22 4.93 22 7.286 22 " +
                    "12s0 7.071-1.465 8.535C19.072 22 16.714 22 12 22s-7.071 0-8.536-1.465C2 19.072 2 16.714 2 12Z",
            ),
            StrokedPath("M17 2.5v19M7 2.5v19M2.5 7H7m14.5 0H17M2.5 17H7m14.5 0H17M2 12h20", hasRoundCaps = true),
        )
    }

    /**
     * Solar's, with the keyhole. Drawn as filled geometry rather than as a stroke, which is what
     * that family's outline style is -- so [StandardStroke] does not apply and the weight is baked
     * into the shape.
     */
    val Lock: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        vector(
            "M9.25 16a2.75 2.75 0 1 1 5.5 0a2.75 2.75 0 0 1-5.5 0M12 14.75a1.25 1.25 0 1 0 0 2.5a1.25 " +
                "1.25 0 0 0 0-2.5",
            "M5.25 9.303V8a6.75 6.75 0 0 1 13.5 0v1.303q.34.023.642.064c.9.12 1.658.38 2.26.981c.602.602.86 " +
                "1.36.982 2.26c.116.867.116 1.97.116 3.337v.11c0 1.367 0 2.47-.116 3.337c-.122.9-.38 1.658-.982 " +
                "2.26s-1.36.86-2.26.982c-.867.116-1.97.116-3.337.116h-8.11c-1.367 0-2.47 0-3.337-.116c-.9-.122-" +
                "1.658-.38-2.26-.982s-.86-1.36-.981-2.26c-.117-.867-.117-1.97-.117-3.337v-.11c0-1.367 0-2.47.117-" +
                "3.337c.12-.9.38-1.658.981-2.26c.602-.602 1.36-.86 2.26-.981q.301-.041.642-.064M6.75 8a5.25 5.25 0 " +
                "0 1 10.5 0v1.253q-.56-.004-1.195-.003h-8.11q-.634 0-1.195.003zm-1.942 2.853c-.734.099-1.122.28-" +
                "1.399.556c-.277.277-.457.665-.556 1.4c-.101.755-.103 1.756-.103 3.191s.002 2.436.103 3.192c.099" +
                ".734.28 1.122.556 1.399c.277.277.665.457 1.4.556c.754.101 1.756.103 3.191.103h8c1.435 0 2.436-" +
                ".002 3.192-.103c.734-.099 1.122-.28 1.399-.556c.277-.277.457-.665.556-1.4c.101-.755.103-1.756" +
                ".103-3.191s-.002-2.437-.103-3.192c-.099-.734-.28-1.122-.556-1.399c-.277-.277-.665-.457-1.4-.556c-" +
                ".755-.101-1.756-.103-3.191-.103H8c-1.435 0-2.437.002-3.192.103",
            name = "Lock",
            isEvenOdd = true,
        )
    }

    val MoreHoriz: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("MoreHoriz", "M140,128a12,12,0,1,1-12-12A12,12,0,0,1,140,128Zm56-12a12,12,0,1,0,12,12A12,12,0,0,0,196,116ZM60,116a12,12,0,1,0,12,12A12,12,0,0,0,60,116Z")
    }

    val MoreVert: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("MoreVert", "M112,60a16,16,0,1,1,16,16A16,16,0,0,1,112,60Zm16,52a16,16,0,1,0,16,16A16,16,0,0,0,128,112Zm0,68a16,16,0,1,0,16,16A16,16,0,0,0,128,180Z")
    }

    /** Two rules, for the grip a queue row is dragged by. */
    val DragHandle: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("DragHandle", "M224,160a8,8,0,0,1-8,8H40a8,8,0,0,1,0-16H216A8,8,0,0,1,224,160ZM40,104H216a8,8,0,0,0,0-16H40a8,8,0,0,0,0,16Z")
    }

    /** [Repeat] with a 1 in it: the same loop, around one track. */
    val RepeatOne: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("RepeatOne", "M24,128A72.08,72.08,0,0,1,96,56H204.69L194.34,45.66a8,8,0,0,1,11.32-11.32l24,24a8,8,0,0,1,0,11.32l-24,24a8,8,0,0,1-11.32-11.32L204.69,72H96a56.06,56.06,0,0,0-56,56,8,8,0,0,1-16,0Zm200-8a8,8,0,0,0-8,8,56.06,56.06,0,0,1-56,56H51.31l10.35-10.34a8,8,0,0,0-11.32-11.32l-24,24a8,8,0,0,0,0,11.32l24,24a8,8,0,0,0,11.32-11.32L51.31,200H160a72.08,72.08,0,0,0,72-72A8,8,0,0,0,224,120Zm-88,40a8,8,0,0,0,8-8V104a8,8,0,0,0-11.58-7.16l-16,8a8,8,0,1,0,7.16,14.31l4.42-2.21V152A8,8,0,0,0,136,160Z")
    }

    val Repeat: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Repeat", "M24,128A72.08,72.08,0,0,1,96,56H204.69L194.34,45.66a8,8,0,0,1,11.32-11.32l24,24a8,8,0,0,1,0,11.32l-24,24a8,8,0,0,1-11.32-11.32L204.69,72H96a56.06,56.06,0,0,0-56,56,8,8,0,0,1-16,0Zm200-8a8,8,0,0,0-8,8,56.06,56.06,0,0,1-56,56H51.31l10.35-10.34a8,8,0,0,0-11.32-11.32l-24,24a8,8,0,0,0,0,11.32l24,24a8,8,0,0,0,11.32-11.32L51.31,200H160a72.08,72.08,0,0,0,72-72A8,8,0,0,0,224,120Z")
    }

    val Shuffle: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Shuffle", "M237.66,178.34a8,8,0,0,1,0,11.32l-24,24a8,8,0,0,1-11.32-11.32L212.69,192H200.94a72.12,72.12,0,0,1-58.59-30.15l-41.72-58.4A56.1,56.1,0,0,0,55.06,80H32a8,8,0,0,1,0-16H55.06a72.12,72.12,0,0,1,58.59,30.15l41.72,58.4A56.1,56.1,0,0,0,200.94,176h11.75l-10.35-10.34a8,8,0,0,1,11.32-11.32ZM143,107a8,8,0,0,0,11.16-1.86l1.2-1.67A56.1,56.1,0,0,1,200.94,80h11.75L202.34,90.34a8,8,0,0,0,11.32,11.32l24-24a8,8,0,0,0,0-11.32l-24-24a8,8,0,0,0-11.32,11.32L212.69,64H200.94a72.12,72.12,0,0,0-58.59,30.15l-1.2,1.67A8,8,0,0,0,143,107Zm-30,42a8,8,0,0,0-11.16,1.86l-1.2,1.67A56.1,56.1,0,0,1,55.06,176H32a8,8,0,0,0,0,16H55.06a72.12,72.12,0,0,0,58.59-30.15l1.2-1.67A8,8,0,0,0,113,149Z")
    }

    val GridView: ImageVector get() = DashBoard

    val ListView: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("ListView", "M80,64a8,8,0,0,1,8-8H216a8,8,0,0,1,0,16H88A8,8,0,0,1,80,64Zm136,56H88a8,8,0,0,0,0,16H216a8,8,0,0,0,0-16Zm0,64H88a8,8,0,0,0,0,16H216a8,8,0,0,0,0-16ZM44,52A12,12,0,1,0,56,64,12,12,0,0,0,44,52Zm0,64a12,12,0,1,0,12,12A12,12,0,0,0,44,116Zm0,64a12,12,0,1,0,12,12A12,12,0,0,0,44,180Z", autoMirror = true)
    }

    val Refresh: ImageVector get() = Update

    /** The two marks of an A-B repeat. Nothing else in the app repeats a stretch of anything. */
    val Section: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        phosphor("Section", "M48,48V208H80a8,8,0,0,1,0,16H40a8,8,0,0,1-8-8V40a8,8,0,0,1,8-8H80a8,8,0,0,1,0,16ZM216,32H176a8,8,0,0,0,0,16h32V208H176a8,8,0,0,0,0,16h40a8,8,0,0,0,8-8V40A8,8,0,0,0,216,32Z")
    }
}

/**
 * An icon drawn on Phosphor's grid, which is what almost everything here is.
 *
 * [vector] is the same thing with the grid named, for the handful of glyphs that come from
 * elsewhere: a path is written for one viewport and drawing it on another silently scales it to a
 * tenth of its size or ten times it.
 */
private fun phosphor(name: String, pathData: String, autoMirror: Boolean = false): ImageVector = vector(pathData, name = name, viewport = PhosphorGrid, autoMirror = autoMirror)

/**
 * [isEvenOdd] for a glyph whose holes are drawn as subpaths of the shape around them -- a keyhole, a
 * play triangle cut out of a plate. Filled by the non-zero rule those subpaths fill solid, and the
 * hole disappears.
 */
private fun vector(
    vararg pathData: String,
    name: String,
    viewport: Float = StandardGrid,
    autoMirror: Boolean = false,
    isEvenOdd: Boolean = false,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = viewport,
    viewportHeight = viewport,
    autoMirror = autoMirror,
).apply {
    pathData.forEach {
        addPath(
            pathData = addPathNodes(it),
            fill = SolidColor(Color.Black),
            pathFillType = if (isEvenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
        )
    }
}.build()

/**
 * An icon whose paths are strokes rather than filled outlines.
 *
 * Phosphor ships each glyph as a filled path that *describes* a stroke; Solar and most other sets
 * ship the stroke itself. Handed to [vector], which only fills, a stroked path comes out as a blob
 * or as nothing at all depending on whether it closes.
 *
 * [StandardStroke] is Phosphor Regular's own weight expressed on the standard grid -- 16 of 256 is
 * 1.5 of 24 -- so a stroked icon sits at exactly the weight of everything around it.
 */
private fun strokedVector(
    name: String,
    vararg paths: StrokedPath,
    viewport: Float = StandardGrid,
    strokeWidth: Float = StandardStroke,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = viewport,
    viewportHeight = viewport,
).apply {
    paths.forEach { path ->
        addPath(
            pathData = addPathNodes(path.pathData),
            fill = if (path.isFilled) SolidColor(Color.Black) else null,
            stroke = if (path.isFilled) null else SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            // Round joins throughout, matching the family. Caps are the caller's: a closed shape
            // has no ends to cap, and a run of separate dashes reads as cut pipes without them.
            strokeLineCap = if (path.hasRoundCaps) StrokeCap.Round else StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()

/**
 * One path of a stroked icon.
 *
 * [isFilled] for the odd solid detail inside an outlined glyph -- the two dots on the side of a
 * television, the pip in a dial. Stroked like the rest they come out as rings.
 */
private data class StrokedPath(
    val pathData: String,
    val hasRoundCaps: Boolean = false,
    val isFilled: Boolean = false,
)

/** Drawn on its own by [VayouIcons.Folder], and under a minus by [VayouIcons.FolderOff]. */
private const val FolderBody =
    "M2 6.95c0-.883 0-1.324.07-1.692A4 4 0 0 1 5.257 2.07C5.626 2 6.068 2 6.95 2c.386 0 .58 0 " +
        ".766.017a4 4 0 0 1 2.18.904c.144.119.28.255.554.529L11 4c.816.816 1.224 1.224 1.712 " +
        "1.495a4 4 0 0 0 .848.352C14.098 6 14.675 6 15.828 6h.374c2.632 0 3.949 0 4.804.77q.119" +
        ".105.224.224c.77.855.77 2.172.77 4.804V14c0 3.771 0 5.657-1.172 6.828S17.771 22 14 " +
        "22h-4c-3.771 0-5.657 0-6.828-1.172S2 17.771 2 14z"

/** Phosphor's own grid. */
private const val PhosphorGrid = 256f

/** What everyone else draws on -- Material, Solar, Tabler, and most hand-written paths. */
private const val StandardGrid = 24f

/** Phosphor Regular's 16-of-256, on the standard grid. */
private const val StandardStroke = 1.5f
