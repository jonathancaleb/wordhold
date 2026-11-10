package com.wordhold.app.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wordhold.app.core.data.SessionRepository
import com.wordhold.app.core.data.SessionStats
import com.wordhold.app.core.ui.components.Hairline
import com.wordhold.app.core.ui.components.WordholdCard
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.wordholdApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProgressViewModel(sessions: SessionRepository) : ViewModel() {
    val stats: StateFlow<SessionStats> = sessions.overallStats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SessionStats(lastScore = null, bestScore = null, streakDays = 0, completedCount = 0, totalSeconds = 0),
    )
}

/** Combined progress across all games, read from the shared `game_sessions` table. */
@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.wordholdApp
    val viewModel: ProgressViewModel = viewModel(
        factory = viewModelFactory { initializer { ProgressViewModel(app.sessions) } }
    )
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp)
    ) {
        WordholdCard(Modifier.fillMaxWidth()) {
            StatRow("Current streak", if (stats.streakDays == 1) "1 day" else "${stats.streakDays} days")
            Hairline(Modifier.padding(vertical = 12.dp))
            StatRow("Games completed", stats.completedCount.toString())
            Hairline(Modifier.padding(vertical = 12.dp))
            StatRow("Time played", formatDuration(stats.totalSeconds))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val colors = WordholdTheme.colors
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(label, style = WordholdTheme.type.body.copy(color = colors.label))
        BasicText(value, style = WordholdTheme.type.body.copy(color = colors.secondaryLabel))
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
