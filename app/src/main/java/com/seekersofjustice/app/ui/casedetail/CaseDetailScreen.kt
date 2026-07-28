package com.seekersofjustice.app.ui.casedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seekersofjustice.app.R
import com.seekersofjustice.app.domain.model.Memo
import com.seekersofjustice.app.domain.model.MemoStatus
import com.seekersofjustice.app.ui.common.ConfirmDeleteDialog
import com.seekersofjustice.app.ui.common.formatDate
import com.seekersofjustice.app.ui.common.formatDuration
import com.seekersofjustice.app.ui.common.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    onBack: () -> Unit,
    onAddMemo: (Long) -> Unit,
    onMemoClick: (Long) -> Unit,
    viewModel: CaseDetailViewModel = viewModel(factory = CaseDetailViewModel.Factory),
) {
    val details by viewModel.details.collectAsStateWithLifecycle()
    var caseMenuOpen by remember { mutableStateOf(false) }
    var deletingCase by remember { mutableStateOf(false) }
    var deletingMemo by remember { mutableStateOf<Memo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(details?.case?.firNumber?.let { stringResource(R.string.fir_prefix, it) } ?: "")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { caseMenuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.delete_case))
                        }
                        DropdownMenu(expanded = caseMenuOpen, onDismissRequest = { caseMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_case)) },
                                onClick = { caseMenuOpen = false; deletingCase = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddMemo(viewModel.caseId) },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) {
                Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.add_memo))
            }
        },
    ) { padding ->
        val d = details ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(d.case.fullName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(
                                R.string.person_summary,
                                d.case.age,
                                stringResource(d.case.sex.labelRes()),
                                stringResource(d.case.relation.labelRes()),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.memos_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (d.memos.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_memos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(d.memos, key = { it.id }) { memo ->
                    MemoRow(
                        memo = memo,
                        casePersonName = d.case.fullName,
                        onClick = { onMemoClick(memo.id) },
                        onRetry = { viewModel.retryMemo(memo.id) },
                        onDelete = { deletingMemo = memo },
                    )
                }
            }
        }
    }

    if (deletingCase) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_case_title),
            message = stringResource(R.string.delete_case_message),
            onConfirm = { viewModel.deleteCase(onDeleted = onBack) },
            onDismiss = { deletingCase = false },
        )
    }
    deletingMemo?.let { memo ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_statement_title),
            message = stringResource(R.string.delete_statement_message),
            onConfirm = { viewModel.deleteMemo(memo.id) },
            onDismiss = { deletingMemo = null },
        )
    }
}

@Composable
private fun MemoRow(
    memo: Memo,
    casePersonName: String,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val speaker = memo.speakerName?.takeIf { it.isNotBlank() }
    val speakerLine = when {
        speaker != null && memo.speakerRelation != null ->
            stringResource(R.string.speaker_summary, speaker, stringResource(memo.speakerRelation.labelRes()))
        speaker != null -> speaker
        else -> casePersonName
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(speakerLine, style = MaterialTheme.typography.bodyLarge)
                Text(
                    formatDate(memo.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(
                        R.string.memo_summary,
                        formatDuration(memo.durationMs),
                        stringResource(memo.language.labelRes()),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(status = memo.status, onRetry = onRetry)
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.delete_statement))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_statement)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: MemoStatus, onRetry: () -> Unit) {
    when (status) {
        MemoStatus.PENDING, MemoStatus.PROCESSING -> AssistChip(
            onClick = {},
            label = { Text(stringResource(status.labelRes())) },
            leadingIcon = {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            },
        )
        MemoStatus.DONE -> AssistChip(
            onClick = {},
            label = { Text(stringResource(status.labelRes())) },
        )
        MemoStatus.FAILED -> AssistChip(
            onClick = onRetry,
            label = { Text(stringResource(R.string.retry)) },
            leadingIcon = {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = stringResource(R.string.status_failed),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }
}
