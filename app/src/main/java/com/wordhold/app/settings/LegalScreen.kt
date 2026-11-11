package com.wordhold.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wordhold.app.R
import com.wordhold.app.core.ui.components.Heading
import com.wordhold.app.core.ui.components.OutlinedCard
import com.wordhold.app.core.ui.components.TopBar
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.WordholdTheme

/** The legal texts bundled in `assets/legal`. The same files can be hosted for the Play listing. */
enum class LegalDocument(val route: String, val title: String, val asset: String) {
    PrivacyPolicy("privacy", "Privacy Policy", "legal/privacy-policy.md"),
    Terms("terms", "Terms and Conditions", "legal/terms.md");

    companion object {
        fun fromRoute(route: String?): LegalDocument? = entries.firstOrNull { it.route == route }
    }
}

internal sealed interface LegalBlock {
    data class Heading(val text: String) : LegalBlock
    data class Paragraph(val text: String) : LegalBlock
    data class Bullet(val text: String) : LegalBlock
}

/**
 * Parses the small Markdown subset the legal files use: `## ` headings, `- ` bullets
 * and plain paragraphs. The `# ` title line is dropped because the navigation bar
 * already shows it.
 */
internal fun parseLegal(markdown: String, contactEmail: String): List<LegalBlock> =
    markdown.replace("{{CONTACT_EMAIL}}", contactEmail)
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("# ") }
        .map { line ->
            when {
                line.startsWith("## ") -> LegalBlock.Heading(line.removePrefix("## "))
                line.startsWith("- ") -> LegalBlock.Bullet(line.removePrefix("- "))
                else -> LegalBlock.Paragraph(line)
            }
        }

@Composable
fun LegalScreen(
    document: LegalDocument,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    val contactEmail = stringResource(R.string.contact_email)
    val blocks = remember(document, contactEmail) {
        val text = context.assets.open(document.asset).bufferedReader().use { it.readText() }
        parseLegal(text, contactEmail)
    }

    Column(modifier.fillMaxSize().background(colors.sage)) {
        TopBar(title = document.title, onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Spacing.screen, vertical = Spacing.section)
        ) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = WordholdTheme.shapes.xl,
                padding = PaddingValues(Spacing.screen + Spacing.xs / 2),
            ) {
                blocks.forEachIndexed { index, block ->
                    when (block) {
                        is LegalBlock.Heading -> Heading(
                            text = block.text,
                            style = type.sectionTitle,
                            modifier = Modifier.padding(top = Spacing.grid, bottom = Spacing.sm),
                        )
                        is LegalBlock.Paragraph -> BasicText(
                            text = block.text,
                            // The first line is the effective date.
                            style = (if (index == 0) type.caption else type.body).copy(color = colors.ink),
                            modifier = Modifier.padding(bottom = Spacing.grid),
                        )
                        is LegalBlock.Bullet -> Row(Modifier.padding(bottom = Spacing.sm)) {
                            BasicText("\u2022", style = type.body.copy(color = colors.ink))
                            BasicText(
                                text = block.text,
                                style = type.body.copy(color = colors.ink),
                                modifier = Modifier.padding(start = Spacing.sm),
                            )
                        }
                    }
                }
            }
        }
    }
}
