package dev.vayou.core.ui

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * How big a file is, said the way this phone says it everywhere else.
 *
 * The system's own, and not a division written here: it names the unit in the reader's language --
 * МБ to somebody reading Russian, where MB was a Latin word left standing in a Cyrillic line --
 * and it counts the way the phone's file manager and its settings count, so the same film is no
 * longer two different sizes depending on which app is asked.
 *
 * Composable because it needs a context, and that is the point: what a size reads like is a
 * question about the reader, which the layer that fetched the bytes has no way to answer. It used
 * to be answered there anyway, and the answer travelled through the model as a second field beside
 * the number it was made from.
 */
@Composable
@ReadOnlyComposable
fun Long.asFileSize(): String = Formatter.formatFileSize(LocalContext.current, this)
