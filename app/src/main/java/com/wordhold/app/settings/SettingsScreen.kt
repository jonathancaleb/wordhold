package com.wordhold.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wordhold.app.core.ui.components.Alert
import com.wordhold.app.core.ui.components.Heading
import com.wordhold.app.core.ui.components.ListRow
import com.wordhold.app.core.ui.components.TopBar
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.wordholdApp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenLegal: (LegalDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.wordholdApp
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    val scope = rememberCoroutineScope()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Column(modifier.fillMaxSize().background(colors.sage)) {
        TopBar(title = "Settings", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Spacing.screen, vertical = Spacing.section),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Heading("Data", style = type.sectionTitle)
            ListRow("Reset progress", onClick = { confirmReset = true })
            BasicText(
                text = "Deletes all scores, levels, streaks and unfinished games from this phone.",
                style = type.caption.copy(color = colors.ink),
                modifier = Modifier.padding(horizontal = Spacing.xs),
            )

            Heading("About", style = type.sectionTitle, modifier = Modifier.padding(top = Spacing.section))
            ListRow("Privacy Policy", onClick = { onOpenLegal(LegalDocument.PrivacyPolicy) })
            ListRow("Terms and Conditions", onClick = { onOpenLegal(LegalDocument.Terms) })
            ListRow("Version", value = version)
        }
    }

    if (confirmReset) {
        Alert(
            title = "Reset progress?",
            message = "All scores, levels, streaks and unfinished games will be deleted. This cannot be undone.",
            confirmLabel = "Reset",
            onDismiss = { confirmReset = false },
            onConfirm = {
                confirmReset = false
                app.savedGames.clearAll()
                scope.launch { app.sessions.deleteAll() }
            },
        )
    }
}
