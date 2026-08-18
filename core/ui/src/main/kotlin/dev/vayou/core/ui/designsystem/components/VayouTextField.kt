package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A field that is a label, a value and a rule underneath -- no box, no floating label.
 *
 * The rule carries focus: thin and faint at rest, thicker and full contrast while typing. That is
 * enough to say which field is live without a container drawing a second rectangle around text that
 * is already legible, and it keeps a column of fields reading as one list.
 */
@Composable
fun VayouTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    focusRequester: FocusRequester? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    /** A button on the field's own line -- the one thing to do with what has been typed. */
    trailing: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val ruleColour by animateColorAsState(
        targetValue = if (isFocused) VayouTheme.colors.onSurface else VayouTheme.colors.outlineVariant,
        animationSpec = tween(FocusMs),
        label = "fieldRuleColour",
    )
    val ruleThickness by animateDpAsState(
        targetValue = if (isFocused) FocusedRule else RestingRule,
        animationSpec = tween(FocusMs),
        label = "fieldRuleThickness",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = VayouTheme.typography.labelMedium, color = VayouTheme.colors.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = VayouTheme.spacing.sm)
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                textStyle = VayouTheme.typography.bodyLarge.copy(color = VayouTheme.colors.onSurface),
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                cursorBrush = SolidColor(VayouTheme.colors.onSurface),
                interactionSource = interactionSource,
                visualTransformation = visualTransformation,
            )
            trailing?.invoke()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ruleThickness)
                .background(ruleColour),
        )
    }
}

/**
 * Search typed straight into the app bar, where the title was.
 *
 * No box and no rule -- unlike [VayouTextField] this is not a field in a form, it *is* the title of
 * the screen. Borrowing the title's own size keeps the bar from changing height the moment search
 * opens, and the placeholder already says what is being typed into.
 */
@Composable
fun VayouSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onSearch: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            singleLine = true,
            // The bar's size but not the bar's weight: a title is a label and carries semibold,
            // while this is content being typed, and typed text at title weight reads as a heading
            // you cannot edit.
            textStyle = VayouTheme.typography.titleLarge.copy(
                color = VayouTheme.colors.onSurface,
                fontWeight = FontWeight.Normal,
            ),
            cursorBrush = SolidColor(VayouTheme.colors.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
            decorationBox = { field ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = VayouTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                        // A step below the secondary role, not at it: onSurfaceVariant is a shade
                        // off the body colour, and a hint drawn in it reads as text already typed --
                        // the one thing a placeholder must never look like.
                        color = VayouTheme.colors.outline,
                    )
                }
                field()
            },
        )
        trailing?.invoke()
    }
}

private const val FocusMs = 160

private val RestingRule = 1.dp

private val FocusedRule = 2.dp
