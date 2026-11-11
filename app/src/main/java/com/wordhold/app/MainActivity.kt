package com.wordhold.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wordhold.app.catalogue.CatalogueScreen
import com.wordhold.app.core.game.GameHostScreen
import com.wordhold.app.core.game.GameIntroScreen
import com.wordhold.app.core.game.GameRegistry
import com.wordhold.app.core.ui.components.BottomNav
import com.wordhold.app.core.ui.components.NavItem
import com.wordhold.app.core.ui.components.WordholdIcons
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.progress.ProgressScreen
import com.wordhold.app.settings.LegalDocument
import com.wordhold.app.settings.LegalScreen
import com.wordhold.app.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always light, so the system bars always get dark icons, whatever
        // the system's own dark-mode setting.
        val bars = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        setContent {
            WordholdTheme {
                Box(Modifier.fillMaxSize().background(WordholdTheme.colors.sage)) {
                    WordholdNavHost()
                }
            }
        }
    }
}

private const val HOME = "home"
private const val INTRO = "intro/{gameId}"
private const val GAME = "game/{gameId}"
private const val SETTINGS = "settings"
private const val LEGAL = "legal/{document}"
private const val PUSH_MILLIS = 350

@Composable
private fun WordholdNavHost() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.popBackStack() }
    val play: (String) -> Unit = { gameId -> navController.navigate("game/$gameId") { launchSingleTop = true } }
    NavHost(
        navController = navController,
        startDestination = HOME,
        // Push and pop: the new screen slides over from the right while the old one
        // drifts a third of the way left.
        enterTransition = { slideInHorizontally(tween(PUSH_MILLIS)) { it } },
        exitTransition = { slideOutHorizontally(tween(PUSH_MILLIS)) { -it / 3 } },
        popEnterTransition = { slideInHorizontally(tween(PUSH_MILLIS)) { -it / 3 } },
        popExitTransition = { slideOutHorizontally(tween(PUSH_MILLIS)) { it } },
    ) {
        composable(HOME) {
            HomeScreen(
                onOpen = { gameId -> navController.navigate("intro/$gameId") { launchSingleTop = true } },
                onPlay = play,
                onOpenSettings = { navController.navigate(SETTINGS) { launchSingleTop = true } },
            )
        }
        composable(INTRO) { backStackEntry ->
            val entry = backStackEntry.arguments?.getString("gameId")?.let(GameRegistry::find)
            if (entry == null) {
                LaunchedEffect(Unit) { back() }
            } else {
                GameIntroScreen(entry, onBack = back, onPlay = { play(entry.id) })
            }
        }
        composable(GAME) { backStackEntry ->
            val entry = backStackEntry.arguments?.getString("gameId")?.let(GameRegistry::find)
            if (entry == null) {
                LaunchedEffect(Unit) { back() }
            } else {
                GameHostScreen(entry, onBack = back, onHome = { navController.popBackStack(HOME, inclusive = false) })
            }
        }
        composable(SETTINGS) {
            SettingsScreen(
                onBack = back,
                onOpenLegal = { navController.navigate("legal/${it.route}") { launchSingleTop = true } },
            )
        }
        composable(LEGAL) { backStackEntry ->
            val document = LegalDocument.fromRoute(backStackEntry.arguments?.getString("document"))
            if (document == null) LaunchedEffect(Unit) { back() } else LegalScreen(document, onBack = back)
        }
    }
}

private const val TAB_GAMES = 0
private const val TAB_DAILY = 1
private const val TAB_PROGRESS = 2

private val NAV_ITEMS = listOf(
    NavItem("Games", WordholdIcons.Games),
    NavItem("Daily", WordholdIcons.Daily),
    NavItem("Progress", WordholdIcons.Progress),
)

/**
 * Root screen: the catalogue or progress, above the bottom bar. Daily is not a screen
 * of its own: it goes straight into today's puzzle.
 */
@Composable
private fun HomeScreen(
    onOpen: (gameId: String) -> Unit,
    onPlay: (gameId: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(TAB_GAMES) }
    Column(Modifier.fillMaxSize().background(WordholdTheme.colors.sage)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                TAB_GAMES -> CatalogueScreen(onOpen = onOpen, onPlay = onPlay)
                else -> ProgressScreen(onOpenSettings = onOpenSettings)
            }
        }
        BottomNav(
            items = NAV_ITEMS,
            selectedIndex = tab,
            onSelect = { index ->
                if (index == TAB_DAILY) onPlay(GameRegistry.daily().id) else tab = index
            },
        )
    }
}
