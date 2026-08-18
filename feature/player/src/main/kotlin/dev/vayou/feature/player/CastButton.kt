package dev.vayou.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouCancelButton
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import dev.vayou.core.ui.designsystem.components.VayouDialog
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults
import dev.vayou.core.ui.designsystem.components.VayouTextButton
import dev.vayou.core.ui.theme.VayouTheme
import kotlinx.coroutines.delay

/**
 * The cast key.
 *
 * Filled once a television is playing, so the bar says where the sound is going without being
 * opened. Android's own output switcher offers the same routes from the volume keys; this is the
 * one inside the app, for whoever is looking at the player rather than at the system.
 *
 * It reads the route directly rather than a session object of our own. What is casting is a fact
 * the framework already holds, and a second copy of it is a second thing to keep true.
 */
/**
 * The name of the receiver the sound is going to, or null while it is coming out of the phone.
 *
 * The route stays in here: what another screen needs is the name to print, not a handle on the
 * framework's routing.
 */
@Composable
fun rememberCastRouteName(): String? = rememberSelectedRoute()?.name

@Composable
fun CastButton(modifier: Modifier = Modifier, onVideo: Boolean = false) {
    val route = rememberSelectedRoute()
    var isChooserOpen by remember { mutableStateOf(false) }

    val glyph: @Composable () -> Unit = {
        Icon(
            imageVector = if (route == null) VayouIcons.Cast else VayouIcons.CastConnected,
            contentDescription = stringResource(R.string.cast),
            modifier = Modifier.size(VayouTheme.iconSize.md),
        )
    }

    if (onVideo) {
        PlayerButton(modifier = modifier, onClick = { isChooserOpen = true }, content = glyph)
    } else {
        VayouIconButton(modifier = modifier, onClick = { isChooserOpen = true }, content = glyph)
    }

    if (isChooserOpen) {
        val onDismiss = { isChooserOpen = false }
        if (route == null) CastChooser(onDismiss) else CastConnected(route, onDismiss)
    }
}

/** The television being played to, or null while everything is coming out of the phone. */
@Composable
internal fun rememberSelectedRoute(): MediaRouter.RouteInfo? {
    val router = rememberMediaRouter() ?: return null
    var selected by remember { mutableStateOf(router.selectedRoute.takeIf { it.isReceiver }) }

    DisposableEffect(router) {
        val callback = object : MediaRouter.Callback() {
            override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
                selected = route.takeIf { it.isReceiver }
            }

            override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
                selected = null
            }
        }
        // Passive: this only wants to know what is already selected, and an active scan holds the
        // Wi-Fi awake for as long as the screen is up.
        router.addCallback(CastRoutes, callback)
        onDispose { router.removeCallback(callback) }
    }
    return selected
}

/**
 * The televisions on this network.
 *
 * An active scan, unlike the passive one above: this dialog exists to find something, and a
 * receiver that has not announced itself since the app opened would otherwise never appear. It ends
 * when the dialog closes.
 */
@Composable
private fun CastChooser(onDismiss: () -> Unit) {
    val router = rememberMediaRouter()
    val routes = remember { mutableStateListOf<MediaRouter.RouteInfo>() }
    var isSearching by remember { mutableStateOf(true) }

    DisposableEffect(router) {
        if (router == null) return@DisposableEffect onDispose {}
        val callback = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                if (route.matchesSelector(CastRoutes) && route !in routes) routes += route
            }

            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                routes -= route
            }

            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                val index = routes.indexOfFirst { it.id == route.id }
                if (index >= 0) routes[index] = route
            }
        }
        router.addCallback(CastRoutes, callback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
        routes += router.routes.filter { it.matchesSelector(CastRoutes) && it !in routes }
        onDispose { router.removeCallback(callback) }
    }

    // Stops saying "searching" after a moment, so a network with nothing on it says so rather than
    // spinning for ever. The scan itself carries on while the dialog is up.
    LaunchedEffect(Unit) {
        delay(SearchPatienceMs)
        isSearching = false
    }

    VayouDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.cast_to),
        confirmButton = {},
        dismissButton = { VayouCancelButton(onClick = onDismiss) },
    ) {
        when {
            routes.isNotEmpty() -> Column(modifier = Modifier.heightIn(max = VayouSheetDefaults.ListMaxHeight)) {
                // One row per name: a receiver often advertises itself twice, once over each
                // protocol, and two identical lines is a choice nobody can make.
                routes.distinctBy { it.name }.forEach { route ->
                    DeviceRow(name = route.name) {
                        onDismiss()
                        runCatching { router?.selectRoute(route) }
                    }
                }
            }

            isSearching -> Waiting(stringResource(R.string.cast_searching))

            else -> Text(
                text = stringResource(R.string.no_cast_devices_found),
                modifier = Modifier.fillMaxWidth(),
                style = VayouTheme.typography.bodyMedium,
                color = VayouTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CastConnected(route: MediaRouter.RouteInfo, onDismiss: () -> Unit) {
    val router = rememberMediaRouter()
    VayouDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.cast_active),
        confirmButton = {
            VayouTextButton(onClick = {
                onDismiss()
                // Back to the phone by selecting the default route, which is what Media3 watches
                // for: it moves the queue and the position home on its own.
                runCatching { router?.selectRoute(router.defaultRoute) }
            }) {
                Text(text = stringResource(R.string.disconnect))
            }
        },
        dismissButton = { VayouCancelButton(onClick = onDismiss) },
    ) {
        DeviceRow(name = route.name, tint = VayouTheme.colors.accent, onClick = null)
    }
}

@Composable
private fun DeviceRow(name: String, tint: androidx.compose.ui.graphics.Color? = null, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(vertical = VayouTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.lg),
    ) {
        Icon(
            imageVector = VayouIcons.Tv,
            contentDescription = null,
            modifier = Modifier.size(VayouTheme.iconSize.md),
            tint = tint ?: VayouTheme.colors.onSurfaceVariant,
        )
        Text(text = name, style = VayouTheme.typography.bodyLarge, color = VayouTheme.colors.onSurface)
    }
}

@Composable
private fun Waiting(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VayouTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.lg),
    ) {
        VayouCircularProgress(strokeWidth = ThinStroke, size = VayouTheme.iconSize.md)
        Text(text = text, style = VayouTheme.typography.bodyLarge, color = VayouTheme.colors.onSurface)
    }
}

/**
 * Null on a device with no Play services, which is a device that was never going to cast. Every
 * caller then draws nothing rather than a key that cannot work.
 */
@Composable
private fun rememberMediaRouter(): MediaRouter? {
    val context = LocalContext.current
    return remember(context) { runCatching { MediaRouter.getInstance(context) }.getOrNull() }
}

/**
 * A room to send a film to, as against a thing to listen through.
 *
 * Not simply "the route is not the phone's own speaker". A pair of headphones is a route, so is a
 * car, so is a Bluetooth microphone -- the framework routes sound to all of them, and none of them
 * is a television. Asking whether the route speaks the cast protocol is the only question that
 * separates the two, and it is the same one the chooser asks when it lists what is out there.
 */
internal val MediaRouter.RouteInfo.isReceiver: Boolean
    get() = !isDefault && matchesSelector(CastRoutes)

/** Receivers that speak the default media protocol, which is every Chromecast and Android TV. */
internal val CastRoutes: MediaRouteSelector = MediaRouteSelector.Builder()
    .addControlCategory(
        CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID),
    )
    .build()

private const val SearchPatienceMs = 2_000L

private val ThinStroke = 2.dp
