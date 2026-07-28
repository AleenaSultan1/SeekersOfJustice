package com.seekersofjustice.app.ui.record

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seekersofjustice.app.R
import com.seekersofjustice.app.domain.model.MemoLanguage
import com.seekersofjustice.app.domain.model.RelationToCrime
import com.seekersofjustice.app.ui.common.EnumDropdown
import com.seekersofjustice.app.ui.common.formatDuration
import com.seekersofjustice.app.ui.common.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordMemoScreen(
    onBack: () -> Unit,
    viewModel: RecordMemoViewModel = viewModel(factory = RecordMemoViewModel.Factory),
) {
    val context = LocalContext.current
    val step by viewModel.step.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val speakerName by viewModel.speakerName.collectAsStateWithLifecycle()
    val speakerRelation by viewModel.speakerRelation.collectAsStateWithLifecycle()
    val importError by viewModel.importError.collectAsStateWithLifecycle()
    val recordingState by viewModel.recorder.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startRecording(context)
    }

    // Notifications keep the long transcription job alive past 10 min; ask once at save.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.save(onBack) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(viewModel::importFile)
    }

    val unsupportedFileMessage = stringResource(R.string.error_unsupported_file)
    LaunchedEffect(importError) {
        if (importError) {
            snackbarHostState.showSnackbar(unsupportedFileMessage)
            viewModel.clearImportError()
        }
    }

    fun saveWithNotificationPermission() {
        val needsAsk = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsAsk) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.save(onBack)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_memo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            OutlinedTextField(
                value = speakerName,
                onValueChange = viewModel::setSpeakerName,
                label = { Text(stringResource(R.string.speaker_name)) },
                singleLine = true,
                enabled = step is RecordMemoViewModel.Step.Idle,
                modifier = Modifier.fillMaxWidth(),
            )
            EnumDropdown(
                label = stringResource(R.string.speaker_relation),
                options = RelationToCrime.entries,
                selected = speakerRelation,
                optionLabel = { stringResource(it.labelRes()) },
                onSelected = viewModel::setSpeakerRelation,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.memo_language), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                MemoLanguage.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = language == option,
                        onClick = { viewModel.setLanguage(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, MemoLanguage.entries.size),
                        enabled = step is RecordMemoViewModel.Step.Idle,
                    ) {
                        Text(stringResource(option.labelRes()))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when (val s = step) {
                RecordMemoViewModel.Step.Idle -> {
                    FilledIconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                viewModel.startRecording(context)
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.size(96.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = stringResource(R.string.start_recording),
                            modifier = Modifier.size(44.dp),
                        )
                    }
                    Text(stringResource(R.string.tap_to_record), style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        filePickerLauncher.launch(
                            arrayOf("audio/x-wav", "audio/wav", "audio/flac", "audio/*")
                        )
                    }) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.choose_file))
                    }
                }

                RecordMemoViewModel.Step.Recording -> {
                    FilledIconButton(
                        onClick = viewModel::stopRecording,
                        modifier = Modifier.size(96.dp),
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = stringResource(R.string.stop_recording),
                            modifier = Modifier.size(44.dp),
                        )
                    }
                    Text(
                        formatDuration(recordingState.elapsedMs),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    LinearProgressIndicator(
                        progress = { recordingState.amplitude },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is RecordMemoViewModel.Step.Ready -> {
                    Text(
                        stringResource(R.string.recording_ready, formatDuration(s.durationMs)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = viewModel::discard) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.discard))
                        }
                        Button(onClick = ::saveWithNotificationPermission) {
                            Text(stringResource(R.string.save_memo))
                        }
                    }
                }
            }
        }
    }
}
