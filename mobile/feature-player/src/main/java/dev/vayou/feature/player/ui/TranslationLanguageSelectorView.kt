package dev.vayou.feature.player.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.vayou.core.ui.R

@Composable
fun BoxScope.TranslationLanguageSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.translate_subtitle_title),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = OverlayContentPadding)
                .selectableGroup(),
        ) {
            TRANSLATION_LANGUAGES.forEach { (code, label) ->
                RadioButtonRow(
                    selected = code == selectedLanguage,
                    text = label,
                    onClick = {
                        onLanguageSelected(code)
                        onBack()
                    },
                )
            }
        }
    }
}
