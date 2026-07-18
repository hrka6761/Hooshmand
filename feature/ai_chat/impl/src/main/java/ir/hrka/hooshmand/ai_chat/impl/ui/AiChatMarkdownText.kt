package ir.hrka.hooshmand.ai_chat.impl.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState

/**
 * Renders markdown for AI chat model replies using mikepenz's Material 3 markdown renderer.
 *
 * Uses [rememberMarkdownState] with `retainState = true` so streaming token updates do not
 * flash a loading placeholder between parses.
 *
 * @param markdown Markdown source text to display.
 * @param color Primary text color for paragraphs and most inline content.
 * @param modifier Optional [Modifier] for the markdown root.
 */
@Composable
internal fun AiChatMarkdownText(
    markdown: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (markdown.isBlank()) {
        Text(
            text = " ",
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = modifier,
        )
        return
    }

    val markdownState =
        rememberMarkdownState(
            content = markdown,
            retainState = true,
        )

    Markdown(
        markdownState = markdownState,
        colors = markdownColor(text = color),
        typography =
            markdownTypography(
                h1 = MaterialTheme.typography.titleLarge,
                h2 = MaterialTheme.typography.titleMedium,
                h3 = MaterialTheme.typography.titleSmall,
                h4 = MaterialTheme.typography.titleSmall,
                h5 = MaterialTheme.typography.bodyLarge,
                h6 = MaterialTheme.typography.bodyMedium,
                text = MaterialTheme.typography.bodyMedium,
                code = MaterialTheme.typography.bodySmall,
                quote = MaterialTheme.typography.bodyMedium,
                paragraph = MaterialTheme.typography.bodyMedium,
                ordered = MaterialTheme.typography.bodyMedium,
                bullet = MaterialTheme.typography.bodyMedium,
                list = MaterialTheme.typography.bodyMedium,
            ),
        modifier = modifier,
    )
}
