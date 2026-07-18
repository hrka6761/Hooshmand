package ir.hrka.hooshmand.ai_chat.impl.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ir.hrka.hooshmand.ai_chat.impl.AiChatModelSettings
import ir.hrka.hooshmand.ai_chat.impl.R
import ir.hrka.llm.runtime.api.LlmAccelerator

/**
 * Dialog for editing [AiChatModelSettings], matching the Gallery model-config dialog layout.
 *
 * Tab 0 edits accelerator and sampling knobs. Tab 1 edits the system instruction.
 *
 * @param settings Current settings shown when the dialog opens.
 * @param onDismissed Called when the user cancels or dismisses the dialog.
 * @param onConfirm Called with the edited settings when the user taps OK.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiChatSettingsDialog(
    settings: AiChatModelSettings,
    onDismissed: () -> Unit,
    onConfirm: (AiChatModelSettings) -> Unit,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var accelerator by remember { mutableStateOf(settings.accelerator) }
    var maxTokens by remember { mutableFloatStateOf(settings.maxTokens.toFloat()) }
    var topK by remember { mutableFloatStateOf(settings.topK.toFloat()) }
    var topP by remember { mutableFloatStateOf(settings.topP) }
    var temperature by remember { mutableFloatStateOf(settings.temperature) }
    var systemInstruction by remember {
        mutableStateOf(settings.systemInstruction.orEmpty())
    }
    val interactionSource = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismissed) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) {
                        focusManager.clearFocus()
                    }
                    .imePadding(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.ai_chat_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                )

                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(text = stringResource(R.string.ai_chat_settings_tab_model))
                        },
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(text = stringResource(R.string.ai_chat_settings_tab_system_prompt))
                        },
                    )
                }

                if (selectedTabIndex == 0) {
                    Column(
                        modifier =
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AcceleratorSelectorRow(
                            selected = accelerator,
                            onSelected = { accelerator = it },
                        )
                        NumberSliderRow(
                            label = stringResource(R.string.ai_chat_settings_max_tokens),
                            value = maxTokens,
                            valueRange = MAX_TOKENS_MIN..MAX_TOKENS_MAX,
                            isInteger = true,
                            onValueChange = { maxTokens = it },
                        )
                        NumberSliderRow(
                            label = stringResource(R.string.ai_chat_settings_topk),
                            value = topK,
                            valueRange = TOP_K_MIN..TOP_K_MAX,
                            isInteger = true,
                            onValueChange = { topK = it },
                        )
                        NumberSliderRow(
                            label = stringResource(R.string.ai_chat_settings_topp),
                            value = topP,
                            valueRange = TOP_P_MIN..TOP_P_MAX,
                            isInteger = false,
                            onValueChange = { topP = it },
                        )
                        NumberSliderRow(
                            label = stringResource(R.string.ai_chat_settings_temperature),
                            value = temperature,
                            valueRange = TEMPERATURE_MIN..TEMPERATURE_MAX,
                            isInteger = false,
                            onValueChange = { temperature = it },
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = systemInstruction,
                        onValueChange = { systemInstruction = it },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.ai_chat_settings_system_prompt_placeholder),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        OutlinedButton(
                            onClick = { systemInstruction = "" },
                        ) {
                            Text(text = stringResource(R.string.ai_chat_settings_restore_default))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismissed) {
                        Text(text = stringResource(R.string.ai_chat_settings_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                AiChatModelSettings(
                                    accelerator = accelerator,
                                    maxTokens = maxTokens.toInt().coerceAtLeast(1),
                                    topK = topK.toInt().coerceAtLeast(1),
                                    topP = topP.coerceIn(TOP_P_MIN, TOP_P_MAX),
                                    temperature = temperature.coerceAtLeast(TEMPERATURE_MIN),
                                    systemInstruction = systemInstruction.ifBlank { null },
                                ),
                            )
                        },
                    ) {
                        Text(text = stringResource(R.string.ai_chat_settings_ok))
                    }
                }
            }
        }
    }
}

/**
 * Segmented accelerator selector matching Gallery's accelerator control.
 *
 * @param selected Currently selected accelerator.
 * @param onSelected Called when the user picks a different accelerator.
 */
@Composable
private fun AcceleratorSelectorRow(
    selected: LlmAccelerator,
    onSelected: (LlmAccelerator) -> Unit,
) {
    val options = LlmAccelerator.entries
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.ai_chat_settings_accelerator),
            style = MaterialTheme.typography.titleSmall,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = { onSelected(option) },
                    selected = selected == option,
                    label = {
                        Text(
                            text =
                                when (option) {
                                    LlmAccelerator.CPU ->
                                        stringResource(R.string.ai_chat_settings_accelerator_cpu)
                                    LlmAccelerator.GPU ->
                                        stringResource(R.string.ai_chat_settings_accelerator_gpu)
                                    LlmAccelerator.NPU ->
                                        stringResource(R.string.ai_chat_settings_accelerator_npu)
                                    LlmAccelerator.TPU ->
                                        stringResource(R.string.ai_chat_settings_accelerator_tpu)
                                },
                            maxLines = 1,
                        )
                    },
                )
            }
        }
    }
}

/**
 * Slider plus compact numeric field for one model setting, matching Gallery's number rows.
 *
 * @param label Setting name shown above the controls.
 * @param value Current numeric value.
 * @param valueRange Allowed slider range.
 * @param isInteger When `true`, displays and commits integer values.
 * @param onValueChange Called when the slider or text field changes the value.
 */
@Composable
private fun NumberSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    isInteger: Boolean,
    onValueChange: (Float) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var textFieldDisplayValue by remember(value, isInteger) {
        mutableStateOf(formatSliderValue(value, isInteger))
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text =
                stringResource(
                    R.string.ai_chat_settings_range_format,
                    label,
                    formatSliderValue(valueRange.start, isInteger),
                    formatSliderValue(valueRange.endInclusive, isInteger),
                ),
            style = MaterialTheme.typography.titleSmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = value.coerceIn(valueRange.start, valueRange.endInclusive),
                onValueChange = {
                    val next = if (isInteger) it.toInt().toFloat() else it
                    onValueChange(next)
                    textFieldDisplayValue = formatSliderValue(next, isInteger)
                },
                valueRange = valueRange,
                modifier =
                    Modifier
                        .height(24.dp)
                        .weight(1f)
                        .padding(end = 8.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = textFieldDisplayValue,
                onValueChange = { text ->
                    textFieldDisplayValue = text
                    text.toFloatOrNull()?.let { parsed ->
                        onValueChange(parsed.coerceIn(valueRange.start, valueRange.endInclusive))
                    }
                },
                modifier =
                    Modifier
                        .width(80.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (!focusState.isFocused) {
                                textFieldDisplayValue = formatSliderValue(value, isInteger)
                            }
                        },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            ) { innerTextField ->
                Box(
                    modifier =
                        Modifier.border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color =
                                if (isFocused) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            shape = RoundedCornerShape(4.dp),
                        ),
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        innerTextField()
                    }
                }
            }
        }
    }
}

/**
 * Formats a slider value for display in the compact numeric field.
 *
 * @param value Raw float value.
 * @param isInteger When `true`, formats without decimals.
 * @return Display string for the text field.
 */
private fun formatSliderValue(value: Float, isInteger: Boolean): String =
    if (isInteger) {
        value.toInt().toString()
    } else {
        "%.2f".format(value)
    }

/** Minimum allowed maximum-context-length setting. */
private const val MAX_TOKENS_MIN = 256f

/** Maximum allowed maximum-context-length setting. */
private const val MAX_TOKENS_MAX = 8192f

/** Minimum allowed TopK setting. */
private const val TOP_K_MIN = 1f

/** Maximum allowed TopK setting. */
private const val TOP_K_MAX = 128f

/** Minimum allowed TopP setting. */
private const val TOP_P_MIN = 0f

/** Maximum allowed TopP setting. */
private const val TOP_P_MAX = 1f

/** Minimum allowed temperature setting. */
private const val TEMPERATURE_MIN = 0f

/** Maximum allowed temperature setting. */
private const val TEMPERATURE_MAX = 2f
