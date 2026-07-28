package com.seekersofjustice.app.ui.cases

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seekersofjustice.app.R
import com.seekersofjustice.app.domain.model.Case
import com.seekersofjustice.app.ui.common.ConfirmDeleteDialog
import com.seekersofjustice.app.ui.common.formatDate
import com.seekersofjustice.app.ui.common.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseListScreen(
    onCaseClick: (Long) -> Unit,
    onSignOut: () -> Unit,
    viewModel: CaseListViewModel = viewModel(factory = CaseListViewModel.Factory),
) {
    val cases by viewModel.cases.collectAsStateWithLifecycle()
    val duplicateFirError by viewModel.duplicateFirError.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Case?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cases_title)) },
                navigationIcon = {
                    Image(
                        painter = painterResource(R.drawable.pp_crest),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp).size(30.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = { toggleLanguage() }) {
                        Icon(Icons.Default.Language, contentDescription = stringResource(R.string.switch_language))
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = stringResource(R.string.auth_sign_out))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_case))
            }
        },
    ) { padding ->
        if (cases.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.no_cases),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cases, key = { it.id }) { case ->
                    CaseRow(
                        case = case,
                        onClick = { onCaseClick(case.id) },
                        onDelete = { pendingDelete = case },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCaseDialog(
            duplicateFirError = duplicateFirError,
            onClearError = viewModel::clearDuplicateFirError,
            onDismiss = {
                showAddDialog = false
                viewModel.clearDuplicateFirError()
            },
            onSave = { fir, name, age, sex, relation ->
                viewModel.addCase(fir, name, age, sex, relation) { showAddDialog = false }
            },
        )
    }

    pendingDelete?.let { case ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_case_title),
            message = stringResource(R.string.delete_case_message),
            onConfirm = { viewModel.deleteCase(case.id) },
            onDismiss = { pendingDelete = null },
        )
    }
}

private fun toggleLanguage() {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val next = if (current.startsWith("ur")) "en" else "ur"
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(next))
}

@Composable
private fun CaseRow(case: Case, onClick: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.fir_prefix, case.firNumber),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(case.relation.labelRes())) },
                    )
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.delete_case))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_case)) },
                                onClick = { menuOpen = false; onDelete() },
                            )
                        }
                    }
                }
            }
            Text(case.fullName, style = MaterialTheme.typography.bodyLarge)
            Text(
                formatDate(case.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
