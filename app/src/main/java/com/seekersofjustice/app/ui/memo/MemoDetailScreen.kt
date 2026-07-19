package com.seekersofjustice.app.ui.memo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seekersofjustice.app.R
import com.seekersofjustice.app.domain.model.Memo
import com.seekersofjustice.app.domain.model.MemoStatus
import com.seekersofjustice.app.ui.common.formatDate
import com.seekersofjustice.app.ui.common.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoDetailScreen(
    onBack: () -> Unit,
    viewModel: MemoDetailViewModel = viewModel(factory = MemoDetailViewModel.Factory),
) {
    val memo by viewModel.memo.collectAsStateWithLifecycle()
    val playback by viewModel.player.state.collectAsStateWithLifecycle()

    LaunchedEffect(memo?.audioPath) {
        memo?.audioPath?.let(viewModel::ensurePlayerLoaded)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.memo_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val m = memo ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                formatDate(m.createdAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PlayerCard(
                isPlaying = playback.isPlaying,
                positionMs = playback.positionMs,
                durationMs = if (playback.durationMs > 0) playback.durationMs else m.durationMs,
                onPlayPause = viewModel.player::playPause,
                onSeek = viewModel.player::seekTo,
            )

            when (m.status) {
                MemoStatus.PENDING, MemoStatus.PROCESSING -> ProcessingCard()
                MemoStatus.FAILED -> FailedCard(m, onRetry = viewModel::retry)
                MemoStatus.DONE -> ResultsSection(m)
            }
        }
    }
}

@Composable
private fun PlayerCard(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FilledIconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                )
            }
            Column(Modifier.weight(1f)) {
                Slider(
                    value = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                    onValueChange = { onSeek((it * durationMs).toLong()) },
                )
                Text(
                    // LTR isolate: keeps "position / duration" ordered correctly in RTL locales.
                    "⁦${formatDuration(positionMs)} / ${formatDuration(durationMs)}⁩",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProcessingCard() {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(Modifier.size(28.dp))
            Text(stringResource(R.string.processing_message), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun FailedCard(memo: Memo, onRetry: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(stringResource(R.string.transcription_failed), style = MaterialTheme.typography.titleMedium)
            }
            memo.errorMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun ResultsSection(memo: Memo) {
    memo.keyPoints.takeIf { it.isNotEmpty() }?.let { points ->
        SectionCard(title = stringResource(R.string.key_points)) {
            points.forEachIndexed { index, point ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(point, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    memo.transcript?.let {
        SectionCard(title = stringResource(R.string.transcript)) {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
    memo.translationEn?.let {
        SectionCard(title = stringResource(R.string.english_translation)) {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
