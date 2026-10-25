package dev.auralis.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.auralis.MainViewModel
import dev.auralis.ui.components.AmbientBackground
import dev.auralis.ui.components.AuralisBottomBar
import dev.auralis.ui.components.MiniPlayer
import dev.auralis.ui.components.topLevelRoutes
import dev.auralis.ui.screens.DiscoverScreen
import dev.auralis.ui.screens.HomeScreen
import dev.auralis.ui.screens.LibraryScreen
import dev.auralis.ui.screens.LikedScreen
import dev.auralis.ui.screens.PlayerScreen
import dev.auralis.ui.screens.PlaylistScreen
import dev.auralis.ui.screens.StudioScreen
import dev.auralis.ui.theme.AuralisTheme
import dev.auralis.ui.theme.Ink

@Composable
fun AuralisRoot(vm: MainViewModel) {
    val mood by vm.mood.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()

    AuralisTheme(mood) {
        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val showChrome = route == null || route in topLevelRoutes

        var askedPermission by rememberSaveable { mutableStateOf(false) }
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { vm.onPermissionResult() }

        LaunchedEffect(Unit) {
            if (!library.permission && !askedPermission) {
                askedPermission = true
                permissionLauncher.launch(vm.requiredPermissions())
            }
        }

        LaunchedEffect(Unit) {
            vm.messages.collect { message ->
                snackbar.showSnackbar(message)
            }
        }

        fun navigateTopLevel(target: String) {
            if (route == target) return
            nav.navigate(target) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Ink),
        ) {
            if (settings.showAmbient) {
                AmbientBackground(mood)
            }

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (showChrome) {
                        Column(Modifier.navigationBarsPadding()) {
                            val nowPlaying = player.track
                            if (nowPlaying != null) {
                                MiniPlayer(
                                    track = nowPlaying,
                                    artwork = player.artwork,
                                    isPlaying = player.isPlaying,
                                    isBuffering = player.isBuffering,
                                    progress = player.progress,
                                    accent = mood.accent,
                                    onOpen = { nav.navigate("player") },
                                    onToggle = { vm.togglePlay() },
                                    onNext = { vm.next() },
                                )
                            }
                            AuralisBottomBar(
                                currentRoute = route,
                                onNavigate = { navigateTopLevel(it) },
                                accent = mood.accent,
                            )
                        }
                    }
                },
            ) { padding ->
                NavHost(
                    navController = nav,
                    startDestination = "home",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    composable("home") {
                        HomeScreen(
                            vm = vm,
                            onOpenPlayer = { nav.navigate("player") },
                            onOpenPlaylist = { id -> nav.navigate("playlist/$id") },
                            onOpenLiked = { nav.navigate("liked") },
                            onOpenDiscover = { navigateTopLevel("discover") },
                            onOpenStudio = { navigateTopLevel("studio") },
                            onRequestPermission = { permissionLauncher.launch(vm.requiredPermissions()) },
                        )
                    }
                    composable("library") {
                        LibraryScreen(
                            vm = vm,
                            onOpenPlaylist = { id -> nav.navigate("playlist/$id") },
                            onRequestPermission = { permissionLauncher.launch(vm.requiredPermissions()) },
                        )
                    }
                    composable("discover") {
                        DiscoverScreen(vm)
                    }
                    composable("studio") {
                        StudioScreen(
                            vm = vm,
                            onRequestPermission = { permissionLauncher.launch(vm.requiredPermissions()) },
                        )
                    }
                    composable("player") {
                        PlayerScreen(vm = vm, onBack = { nav.popBackStack() })
                    }
                    composable("liked") {
                        LikedScreen(vm = vm, onBack = { nav.popBackStack() })
                    }
                    composable(
                        route = "playlist/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        PlaylistScreen(
                            vm = vm,
                            playlistId = entry.arguments?.getLong("id") ?: 0L,
                            onBack = { nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
