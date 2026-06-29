package dev.vayou

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import dev.vayou.core.common.storagePermission
import dev.vayou.core.media.services.MediaService
import dev.vayou.core.media.sync.MediaSynchronizer
import dev.vayou.core.model.ThemeConfig
import dev.vayou.core.ui.designsystem.components.VayouNavBar
import dev.vayou.core.ui.designsystem.components.VayouNavBarItem
import dev.vayou.core.ui.designsystem.components.VayouScaffold
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.core.ui.theme.VayouPlayerTheme
import dev.vayou.feature.player.PlayerActivity
import dev.vayou.feature.player.cast.CastMiniController
import dev.vayou.feature.player.cast.CastSessionManager
import dev.vayou.navigation.NETWORK_ROUTE
import dev.vayou.navigation.PLAYLIST_GRAPH_ROUTE
import dev.vayou.navigation.SETTINGS_ROUTE
import dev.vayou.navigation.TopLevelDestination
import dev.vayou.navigation.mediaNavGraph
import dev.vayou.navigation.navigateToNetwork
import dev.vayou.navigation.networkScreen
import dev.vayou.navigation.playlistGraph
import dev.vayou.navigation.settingsNavGraph
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var synchronizer: MediaSynchronizer

    @Inject
    lateinit var mediaService: MediaService

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaService.initialize(this@MainActivity)

        var uiState: MainActivityUiState by mutableStateOf(MainActivityUiState.Loading)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> uiState = state }
            }
        }

        installSplashScreen().setKeepOnScreenCondition {
            when (uiState) {
                MainActivityUiState.Loading -> true
                is MainActivityUiState.Success -> {
                    reportFullyDrawn()
                    false
                }
            }
        }

        setContent {
            LaunchedEffect(Unit) { CastSessionManager.initialize(this@MainActivity) }
            val shouldUseDarkTheme = shouldUseDarkTheme(uiState = uiState)

            LaunchedEffect(shouldUseDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { shouldUseDarkTheme },
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { shouldUseDarkTheme },
                    ),
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }

            VayouPlayerTheme(
                darkTheme = shouldUseDarkTheme,
                highContrastDarkTheme = shouldUseHighContrastDarkTheme(uiState = uiState),
                dynamicColor = shouldUseDynamicColors(uiState = uiState),
            ) {
                val storagePermissionState = rememberPermissionState(permission = storagePermission)

                LifecycleEventEffect(event = Lifecycle.Event.ON_START) {
                    storagePermissionState.launchPermissionRequest()
                }

                LaunchedEffect(key1 = storagePermissionState.status.isGranted) {
                    if (storagePermissionState.status.isGranted) {
                        synchronizer.startSync()
                    }
                }

                val mainNavController = rememberNavController()
                val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val selectedNavIndex = when {
                    currentDestination.isInRoute(NETWORK_ROUTE) -> 1
                    currentDestination.isInRoute(PLAYLIST_GRAPH_ROUTE) -> 2
                    currentDestination.isInRoute(SETTINGS_ROUTE) -> 3
                    else -> 0
                }

                val navBarItems = TopLevelDestination.entries.map { destination ->
                    VayouNavBarItem(
                        icon = destination.icon,
                        selectedIcon = destination.selectedIcon,
                        label = stringResource(destination.labelRes),
                    )
                }

                VayouScaffold(
                    containerColor = VayouTheme.colors.surface,
                    bottomBar = {
                        Column {
                            CastMiniController(
                                onExpandClick = {
                                    startActivity(
                                        Intent(this@MainActivity, PlayerActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                        },
                                    )
                                },
                            )
                            VayouNavBar(
                                items = navBarItems,
                                selectedIndex = selectedNavIndex,
                                onItemSelected = { index ->
                                    val opts = navOptions {
                                        popUpTo(mainNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    when (TopLevelDestination.entries[index]) {
                                        TopLevelDestination.HOME -> mainNavController.navigate(
                                            dev.vayou.navigation.MediaRootRoute,
                                            opts,
                                        )
                                        TopLevelDestination.NETWORK -> mainNavController.navigateToNetwork(opts)
                                        TopLevelDestination.PLAYLIST -> mainNavController.navigate(
                                            PLAYLIST_GRAPH_ROUTE,
                                            opts,
                                        )
                                        TopLevelDestination.SETTINGS -> mainNavController.navigate(
                                            SETTINGS_ROUTE,
                                            opts,
                                        )
                                    }
                                },
                            )
                        }
                    },
                ) {
                    NavHost(
                        navController = mainNavController,
                        startDestination = dev.vayou.navigation.MediaRootRoute,
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                            ) + fadeIn(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                                targetOffset = { (it * 0.3f).toInt() },
                            ) + fadeOut(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                                initialOffset = { (it * 0.3f).toInt() },
                            ) + fadeIn(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                            ) + fadeOut(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))
                        },
                    ) {
                        mediaNavGraph(context = this@MainActivity, navController = mainNavController)
                        playlistGraph(mainNavController)
                        networkScreen(
                            context = this@MainActivity,
                            onBackAtRoot = {
                                mainNavController.navigate(
                                    dev.vayou.navigation.MediaRootRoute,
                                    navOptions {
                                        popUpTo(mainNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    },
                                )
                            },
                        )
                        settingsNavGraph(navController = mainNavController)
                    }
                }
            }
        }
    }
}

private fun NavDestination?.isInRoute(route: String): Boolean =
    this?.hierarchy?.any { it.route == route } == true

@Composable
fun shouldUseDarkTheme(uiState: MainActivityUiState): Boolean = when (uiState) {
    MainActivityUiState.Loading -> isSystemInDarkTheme()
    is MainActivityUiState.Success -> when (uiState.preferences.themeConfig) {
        ThemeConfig.SYSTEM -> isSystemInDarkTheme()
        ThemeConfig.OFF -> false
        ThemeConfig.ON -> true
    }
}

@Composable
fun shouldUseHighContrastDarkTheme(uiState: MainActivityUiState): Boolean = when (uiState) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> uiState.preferences.useHighContrastDarkTheme
}

@Composable
fun shouldUseDynamicColors(uiState: MainActivityUiState): Boolean = when (uiState) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> uiState.preferences.useDynamicColors
}
