package ir.hrka.hooshmand.ai_chat.impl.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState

/**
 * Renders markdown for AI chat model replies using mikepenz's Material 3 markdown renderer.
 *
 * Uses [rememberMarkdownState] with `retainState = true` so streaming token updates do not
 * flash a loading placeholder between parses. Typography text direction follows Persian/Arabic
 * (RTL) or Latin (LTR) based on [markdown] content.
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
    val isRtl = resolvesToRtl(markdown)
    val textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
    val textAlign = if (isRtl) TextAlign.Right else TextAlign.Left

    if (markdown.isBlank()) {
        Text(
            text = " ",
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    textDirection = textDirection,
                ),
            color = color,
            textAlign = textAlign,
            modifier = modifier,
        )
        return
    }

    val markdownState =
        rememberMarkdownState(
            content = markdown,
            retainState = true,
        )

    fun TextStyle.withChatDirection(): TextStyle =
        copy(
            textDirection = textDirection,
            textAlign = textAlign,
        )

    Markdown(
        markdownState = markdownState,
        colors = markdownColor(text = color),
        typography =
            markdownTypography(
                h1 = MaterialTheme.typography.titleLarge.withChatDirection(),
                h2 = MaterialTheme.typography.titleMedium.withChatDirection(),
                h3 = MaterialTheme.typography.titleSmall.withChatDirection(),
                h4 = MaterialTheme.typography.titleSmall.withChatDirection(),
                h5 = MaterialTheme.typography.bodyLarge.withChatDirection(),
                h6 = MaterialTheme.typography.bodyMedium.withChatDirection(),
                text = MaterialTheme.typography.bodyMedium.withChatDirection(),
                code = MaterialTheme.typography.bodySmall.withChatDirection(),
                quote = MaterialTheme.typography.bodyMedium.withChatDirection(),
                paragraph = MaterialTheme.typography.bodyMedium.withChatDirection(),
                ordered = MaterialTheme.typography.bodyMedium.withChatDirection(),
                bullet = MaterialTheme.typography.bodyMedium.withChatDirection(),
                list = MaterialTheme.typography.bodyMedium.withChatDirection(),
            ),
        modifier = modifier,
    )
}

