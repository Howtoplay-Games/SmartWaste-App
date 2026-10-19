package sg.smartwaste.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import sg.smartwaste.app.data.auth.SessionState
import sg.smartwaste.app.ui.auth.AuthScreen
import sg.smartwaste.app.ui.chat.ChatSheet
import sg.smartwaste.app.ui.components.AiSupportFab
import sg.smartwaste.app.ui.components.BarItem
import sg.smartwaste.app.ui.components.PillBottomBar
import sg.smartwaste.app.ui.components.appContainer
import sg.smartwaste.app.ui.deposit.DepositScreen
import sg.smartwaste.app.ui.home.HomeScreen
import sg.smartwaste.app.ui.map.MapScreen
import sg.smartwaste.app.ui.navigation.Routes
import sg.smartwaste.app.ui.profile.ProfileScreen
import sg.smartwaste.app.ui.rewards.RewardsScreen
import sg.smartwaste.app.ui.scan.ResultScreen
import sg.smartwaste.app.ui.scan.ScanScreen
import sg.smartwaste.app.ui.theme.SW

private val barItems = listOf(
    BarItem(Routes.HOME, "Home", Icons.Rounded.Home),
    BarItem(Routes.MAP, "Map", Icons.Rounded.Map),
    BarItem(Routes.SCAN, "Scan", Icons.Rounded.QrCodeScanner, accent = true),
    BarItem(Routes.REWARDS, "Rewards", Icons.Rounded.CardGiftcard),
    BarItem(Routes.PROFILE, "Profile", Icons.Rounded.Person),
)

private val barRoutes = barItems.map { it.route }.toSet()

@Composable
fun SmartWasteRoot(onReady: () -> Unit) {
    val auth = appContainer().auth
    val session by auth.session.collectAsStateWithLifecycle(initialValue = SessionState.Loading)

    if (session == SessionState.Loading) {
        Box(Modifier.fillMaxSize().background(SW.Bg))
        return
    }
    LaunchedEffect(Unit) { onReady() }

    Crossfade(targetState = session is SessionState.SignedIn, label = "session") { signedIn ->
        if (signedIn) MainApp() else AuthScreen()
    }
}

@Composable
private fun MainApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route?.substringBefore("?")
    var chatOpen by rememberSaveable { mutableStateOf(false) }
    val showBar = route in barRoutes

    Box(Modifier.fillMaxSize().background(SW.Bg)) {
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onScan = { nav.switchTab(Routes.SCAN) },
                    onOpenMap = { mode, category, place -> nav.navigate(Routes.map(mode = mode, category = category, place = place)) },
                    onRewards = { nav.switchTab(Routes.REWARDS) },
                    onProfile = { nav.switchTab(Routes.PROFILE) },
                    onDeposit = { nav.navigate(Routes.deposit()) },
                )
            }
            composable(
                Routes.MAP_PATTERN,
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("auto") { type = NavType.BoolType; defaultValue = false },
                    navArgument("place") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("q") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { backStack ->
                val args = backStack.arguments
                MapScreen(
                    mode = args?.getString("mode")?.ifBlank { null },
                    category = args?.getString("category")?.ifBlank { null },
                    auto = args?.getBoolean("auto") ?: false,
                    placeId = args?.getString("place")?.ifBlank { null },
                    query = args?.getString("q")?.ifBlank { null },
                    onVerifyDeposit = { id -> nav.navigate(Routes.deposit(place = id)) },
                )
            }
            composable(Routes.SCAN) {
                ScanScreen(
                    onResult = { nav.navigate(Routes.RESULT) },
                    onBinQr = { qr -> nav.navigate(Routes.deposit(qr = qr)) },
                )
            }
            composable(Routes.RESULT) {
                ResultScreen(
                    onBack = { nav.popBackStack() },
                    onFindBin = { cat ->
                        val bins = cat?.binFriendly != false
                        nav.navigate(Routes.map(mode = if (bins) "bins" else "buyers", category = cat?.name, auto = true))
                    },
                    onSell = { cat -> nav.navigate(Routes.map(mode = "buyers", category = cat?.name, auto = true)) },
                    onDeposit = { nav.navigate(Routes.deposit()) },
                    onScanAgain = { nav.popBackStack(Routes.SCAN, inclusive = false) },
                )
            }
            composable(
                Routes.DEPOSIT_PATTERN,
                arguments = listOf(
                    navArgument("place") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("qr") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { backStack ->
                DepositScreen(
                    placeId = backStack.arguments?.getString("place")?.ifBlank { null },
                    qr = backStack.arguments?.getString("qr")?.ifBlank { null },
                    onBack = { nav.popBackStack() },
                    onHome = { nav.switchTab(Routes.HOME) },
                    onRewards = { nav.switchTab(Routes.REWARDS) },
                )
            }
            composable(Routes.REWARDS) {
                RewardsScreen(onEarn = { nav.navigate(Routes.deposit()) }, onScan = { nav.switchTab(Routes.SCAN) })
            }
            composable(Routes.PROFILE) { ProfileScreen() }
        }

        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            PillBottomBar(barItems, route, onSelect = { nav.switchTab(it.route) })
        }

        AnimatedVisibility(
            visible = route != null && !chatOpen,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = fabBottomPadding(route, showBar)),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            AiSupportFab(onClick = { chatOpen = true })
        }

        if (chatOpen) ChatSheet(onDismiss = { chatOpen = false })
    }
}

/** Keeps the AI button in the bottom-right corner, clear of the nav bar and bottom panels. */
private fun fabBottomPadding(route: String?, showBar: Boolean) = when {
    route == Routes.MAP -> 400.dp
    route == Routes.RESULT -> 150.dp
    showBar -> 104.dp
    else -> 20.dp
}

/** Bottom-bar navigation: single top, state restored per tab, Home as the root. */
private fun NavHostController.switchTab(route: String) {
    val target = if (route == Routes.MAP) Routes.map() else route
    navigate(target) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
