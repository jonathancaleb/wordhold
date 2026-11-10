package com.wordhold.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wordhold.app.catalogue.CatalogueScreen
import com.wordhold.app.core.game.GameHostScreen
import com.wordhold.app.core.game.GameRegistry
import com.wordhold.app.core.ui.components.SegmentedControl
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.progress.ProgressScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status and navigation bar icons follow the system light/dark setting.
        enableEdgeToEdge()
        setContent {
            WordholdTheme {
                Box(Modifier.fillMaxSize().background(WordholdTheme.colors.background)) {
                    WordholdNavHost()
                }
            }
        }
    }
}

private const val HOME = "home"
private const val GAME = "game/{gameId}"
private const val PUSH_MILLIS = 350

@Composable
private fun WordholdNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HOME,
        // iOS push/pop: the new screen slides over from the right while the old one
        // drifts a third of the way left.
        enterTransition = { slideInHorizontally(tween(PUSH_MILLIS)) { it } },
        exitTransition = { slideOutHorizontally(tween(PUSH_MILLIS)) { -it / 3 } },
        popEnterTransition = { slideInHorizontally(tween(PUSH_MILLIS)) { -it / 3 } },
        popExitTransition = { slideOutHorizontally(tween(PUSH_MILLIS)) { it } },
    ) {
        composable(HOME) {
            HomeScreen(onPlay = { gameId -> navController.navigate("game/$gameId") })
        }
        composable(GAME) { backStackEntry ->
            val entry = backStackEntry.arguments?.getString("gameId")?.let(GameRegistry::find)
            if (entry == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                Box(Modifier.fillMaxSize().background(WordholdTheme.colors.background)) {
                    GameHostScreen(entry, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

/** Root screen: large title, then a segmented control switching Games and Progress. */
@Composable
private fun HomeScreen(onPlay: (gameId: String) -> Unit) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .background(WordholdTheme.colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        BasicText(
            text = "Wordhold",
            style = WordholdTheme.type.largeTitle.copy(color = WordholdTheme.colors.label),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
                .semantics { heading() },
        )
        SegmentedControl(
            options = listOf("Games", "Progress"),
            selectedIndex = section,
            onSelect = { section = it },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
        when (section) {
            0 -> CatalogueScreen(onPlay = onPlay)
            else -> ProgressScreen()
        }
    }
}
