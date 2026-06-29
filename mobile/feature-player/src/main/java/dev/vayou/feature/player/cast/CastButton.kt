package dev.vayou.feature.player.cast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastContext
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.components.VayouDialog
import dev.vayou.core.ui.designsystem.components.VayouTextButton
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.feature.player.buttons.PlayerButton
import kotlinx.coroutines.delay

@Composable
fun CastButton(modifier: Modifier = Modifier, inPlayerOverlay: Boolean = false) {
    var showChooser by remember { mutableStateOf(false) }
    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = if (CastSessionManager.isConnected || CastSessionManager.isConnecting) VayouIcons.CastConnected else VayouIcons.Cast,
            contentDescription = stringResource(R.string.cast),
            modifier = Modifier.size(VayouTheme.iconSize.md),
        )
    }

    if (inPlayerOverlay) {
        PlayerButton(modifier = modifier, onClick = { showChooser = true }, content = icon)
    } else {
        IconButton(modifier = modifier, onClick = { showChooser = true }, content = icon)
    }

    if (showChooser) {
        CastChooserDialog(onDismiss = { showChooser = false })
    }
}

@Composable
private fun CastChooserDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    when {
        CastSessionManager.isConnecting -> VayouDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.cast_connecting)) },
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    VayouCircularProgress(
                        modifier = Modifier.size(VayouTheme.iconSize.md),
                        color = VayouTheme.colors.accent,
                        strokeWidth = 2.dp,
                        size = 24.dp,
                    )
                    Text(
                        text = CastSessionManager.deviceName ?: stringResource(R.string.cast_connecting),
                        style = VayouTheme.typography.bodyLarge,
                        color = VayouTheme.colors.onSurface,
                    )
                }
            },
            dismissButton = {
                VayouTextButton(onClick = {
                    onDismiss()
                    runCatching { CastContext.getSharedInstance(context).sessionManager.endCurrentSession(true) }
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {},
        )

        CastSessionManager.isConnected -> VayouDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.cast_active)) },
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = VayouIcons.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(VayouTheme.iconSize.md),
                        tint = VayouTheme.colors.accent,
                    )
                    Text(
                        text = CastSessionManager.deviceName ?: stringResource(R.string.cast_active),
                        style = VayouTheme.typography.bodyLarge,
                        color = VayouTheme.colors.onSurface,
                    )
                }
            },
            dismissButton = {
                VayouTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                VayouTextButton(onClick = {
                    onDismiss()
                    runCatching { CastContext.getSharedInstance(context).sessionManager.endCurrentSession(true) }
                }) {
                    Text(stringResource(R.string.disconnect))
                }
            },
        )

        else -> CastDiscoveryDialog(
            onDismiss = onDismiss,
            onDeviceSelected = { route ->
                onDismiss()
                runCatching { MediaRouter.getInstance(context).selectRoute(route) }
            },
        )
    }
}

@Composable
private fun CastDiscoveryDialog(
    onDismiss: () -> Unit,
    onDeviceSelected: (MediaRouter.RouteInfo) -> Unit,
) {
    val context = LocalContext.current
    val routes = remember { mutableStateListOf<MediaRouter.RouteInfo>() }
    var isScanning by remember { mutableStateOf(true) }

    val selector = remember {
        MediaRouteSelector.Builder()
            .addControlCategory(
                CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID),
            )
            .build()
    }
    val router = remember { MediaRouter.getInstance(context) }

    DisposableEffect(Unit) {
        val callback = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                if (route.matchesSelector(selector) && !routes.contains(route)) {
                    routes.add(route)
                    isScanning = false
                }
            }

            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                routes.remove(route)
            }

            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                val idx = routes.indexOfFirst { it.id == route.id }
                if (idx >= 0) routes[idx] = route
            }
        }
        router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
        router.routes.filter { it.matchesSelector(selector) }.forEach { route ->
            if (!routes.contains(route)) {
                routes.add(route)
                isScanning = false
            }
        }
        onDispose { router.removeCallback(callback) }
    }

    LaunchedEffect(Unit) {
        delay(5_000)
        isScanning = false
    }

    VayouDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cast_to)) },
        content = {
            if (routes.isEmpty() && !isScanning) {
                Text(
                    text = stringResource(R.string.no_cast_devices_found),
                    style = VayouTheme.typography.bodyMedium,
                    color = VayouTheme.colors.onSurfaceVariant,
                )
            } else {
                val uniqueRoutes = remember(routes) { routes.distinctBy { it.name } }
                LazyColumn {
                    items(uniqueRoutes, key = { it.name }) { route ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(route) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(
                                imageVector = VayouIcons.Tv,
                                contentDescription = null,
                                modifier = Modifier.size(VayouTheme.iconSize.md),
                                tint = VayouTheme.colors.onSurfaceVariant,
                            )
                            Text(
                                text = route.name,
                                style = VayouTheme.typography.bodyLarge,
                                color = VayouTheme.colors.onSurface,
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            VayouTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            if (isScanning) VayouCircularProgress(
                modifier = Modifier.size(VayouTheme.iconSize.sm),
                color = VayouTheme.colors.accent,
                strokeWidth = 2.dp,
                size = 20.dp,
            )
        },
    )
}
