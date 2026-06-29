package dev.vayou.core.ui.composables

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import dev.vayou.core.ui.designsystem.components.VayouButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.R
import dev.vayou.core.ui.preview.DayNightPreview
import dev.vayou.core.ui.theme.VayouPlayerTheme

@Composable
fun PermissionDetailView(
    text: String,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.permission_not_granted),
            style = VayouTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 5.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = text,
            style = VayouTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 5.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        VayouButton(
            onClick = {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:" + context.packageName)
                    context.startActivity(this)
                }
            },
        ) {
            Text(text = stringResource(R.string.open_settings))
        }
    }
}

@DayNightPreview
@Composable
fun PermissionDetailViewPreview() {
    VayouPlayerTheme {
        Surface {
            PermissionDetailView(
                text = stringResource(
                    id = R.string.permission_settings,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ),
            )
        }
    }
}
