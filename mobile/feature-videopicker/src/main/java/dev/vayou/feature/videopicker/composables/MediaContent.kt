package dev.vayou.feature.videopicker.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.VayouEmptyState
import dev.vayou.core.ui.designsystem.VayouIcons

const val CIRCULAR_PROGRESS_INDICATOR_TEST_TAG = "circularProgressIndicator"

@Composable
fun CenterCircularProgressBar(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VayouCircularProgress(
            modifier = Modifier.testTag(CIRCULAR_PROGRESS_INDICATOR_TEST_TAG),
        )
    }
}

@Composable
fun NoVideosFound(contentPadding: PaddingValues) {
    VayouEmptyState(
        icon = VayouIcons.Video,
        title = stringResource(R.string.no_videos_found),
        modifier = Modifier.padding(contentPadding),
    )
}
