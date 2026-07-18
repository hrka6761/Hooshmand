package ir.hrka.hooshmand.ai_chat.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Resolves whether [text] should be laid out right-to-left.
 *
 * Uses the Unicode first-strong heuristic: the first character with strong directionality
 * decides the result. Arabic-script letters (including Persian) resolve to RTL; Latin and
 * other LTR scripts resolve to LTR. Neutral/weak characters are skipped.
 *
 * @param text Message or markdown source to inspect.
 * @return `true` when the text should use [LayoutDirection.Rtl].
 */
internal fun resolvesToRtl(text: String): Boolean {
    if (text.isBlank()) return false

    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        index += Character.charCount(codePoint)

        when (Character.getDirectionality(codePoint)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            -> return true

            Character.DIRECTIONALITY_LEFT_TO_RIGHT,
            -> return false

            else -> Unit
        }
    }
    return false
}

/**
 * Provides [LayoutDirection.Rtl] or [LayoutDirection.Ltr] for [content] based on [text].
 *
 * Applies to chat message bodies so Persian and Arabic render with correct alignment and
 * bidirectional ordering without flipping the whole chat screen.
 *
 * @param text Source text used to resolve direction.
 * @param content Composable subtree that should inherit the resolved layout direction.
 */
@Composable
internal fun AiChatDirectionalContent(
    text: String,
    content: @Composable () -> Unit,
) {
    val direction =
        if (resolvesToRtl(text)) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        content()
    }
}
