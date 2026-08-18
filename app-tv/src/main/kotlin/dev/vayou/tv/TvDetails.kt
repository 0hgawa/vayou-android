package dev.vayou.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * What a file is, in the few lines worth reading from a sofa.
 *
 * Nothing here can be pressed, which is the whole design: it answers a question and the way out is
 * the back key, the one key on a remote whose meaning nobody has to be taught. That also keeps it
 * short -- with nothing to focus there is nothing to scroll with, so a caller that hands over more
 * lines than fit is a caller asking for the wrong screen.
 */
@Composable
fun TvDetails(title: String, lines: List<Pair<String, String>>, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)

    TvDialog(title = title, onDismiss = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = TvRowInset),
            verticalArrangement = Arrangement.spacedBy(TvRowInset),
        ) {
            lines.forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(TvRowGap / 2)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
