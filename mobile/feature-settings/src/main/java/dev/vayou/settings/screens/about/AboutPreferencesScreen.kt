package dev.vayou.settings.screens.about

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.platform.UriHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.ClickablePreferenceItem
import dev.vayou.core.ui.components.ListSectionTitle
import dev.vayou.core.ui.components.PreferenceGroup
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouScaffold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutPreferencesScreen(
    onLibrariesClick: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val appVersion = remember { context.appVersion() }

    VayouScaffold(
        topBar = {
            VayouTopAppBar(
                title = stringResource(id = R.string.about_name),
                navigationIcon = {
                    VayouIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = VayouIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            ListSectionTitle(text = stringResource(R.string.app_name))
            PreferenceGroup {
                ClickablePreferenceItem(
                    title = stringResource(R.string.version),
                    description = appVersion,
                    icon = VayouIcons.Info,
                    enabled = false,
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.libraries),
                    icon = VayouIcons.BugReport,
                    onClick = onLibrariesClick,
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.send_feedback),
                    icon = VayouIcons.Share,
                    onClick = {
                        uriHandler.openUriOrShowToast(
                            "mailto:ohgawa@proton.me?subject=Vayou%20Feedback",
                            context,
                        )
                    },
                )
            }
        }
    }
}

internal fun UriHandler.openUriOrShowToast(uri: String, context: Context) {
    try {
        openUri(uri = uri)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.error_opening_link), Toast.LENGTH_SHORT).show()
    }
}

private fun Context.appVersion(): String {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    @Suppress("DEPRECATION")
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode
    }
    return "${packageInfo.versionName} ($versionCode)"
}
