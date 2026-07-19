package com.seekersofjustice.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Records microphone audio straight to a 16 kHz mono PCM16 WAV file — the
 * exact format Whisper consumes, so recordings never need resampling.
 */
class WavRecorder {

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val HEADER_SIZE = 44L
    }

    data class RecordingState(
        val isRecording: Boolean = false,
        val elapsedMs: Long = 0,
        /** Peak amplitude of the last chunk, normalized to [0, 1]. */
        val amplitude: Float = 0f,
    )

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state

    private var audioRecord: AudioRecord? = null
    private var job: Job? = null
    private var outputFile: File? = null

    // Loop-control flag separate from _state: the read loop republishes
    // RecordingState every chunk and would overwrite a stop written into it.
    @Volatile
    private var running = false

    /** Caller must hold RECORD_AUDIO permission. */
    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, file: File) {
        if (_state.value.isRecording) return
        file.parentFile?.mkdirs()
        outputFile = file

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 4,
        )
        check(record.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord failed to initialize" }
        audioRecord = record
        record.startRecording()
        running = true
        _state.value = RecordingState(isRecording = true)

        job = scope.launch(Dispatchers.IO) {
            val chunk = ShortArray(minBuffer)
            val bytes = ByteArray(minBuffer * 2)
            FileOutputStream(file).use { out ->
                out.write(ByteArray(HEADER_SIZE.toInt())) // placeholder header
                var totalBytes = 0L
                while (isActive && running) {
                    val read = record.read(chunk, 0, chunk.size)
                    if (read <= 0) continue
                    var peak = 0
                    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until read) {
                        bb.putShort(chunk[i])
                        val a = abs(chunk[i].toInt())
                        if (a > peak) peak = a
                    }
                    out.write(bytes, 0, read * 2)
                    totalBytes += read * 2
                    _state.value = RecordingState(
                        isRecording = true,
                        elapsedMs = totalBytes * 1000 / (SAMPLE_RATE * 2),
                        amplitude = peak / 32768f,
                    )
                }
            }
        }
    }

    /** Stops recording, patches the WAV header, and returns (file, durationMs). */
    suspend fun stop(): Pair<File, Long>? {
        val file = outputFile ?: return null
        running = false
        job?.join()
        _state.value = _state.value.copy(isRecording = false)
        job = null
        audioRecord?.run {
            try {
                stop()
            } catch (_: IllegalStateException) {
            }
            release()
        }
        audioRecord = null

        writeWavHeader(file)
        val durationMs = (file.length() - HEADER_SIZE) * 1000 / (SAMPLE_RATE * 2)
        _state.value = RecordingState()
        outputFile = null
        return file to durationMs
    }

    fun discard() {
        outputFile?.delete()
    }

    /**
     * Synchronous teardown for lifecycle callbacks (no coroutine available):
     * releases the hardware and deletes the partial file.
     */
    fun abort() {
        running = false
        _state.value = RecordingState()
        job?.cancel()
        job = null
        audioRecord?.run {
            try {
                stop()
            } catch (_: IllegalStateException) {
            }
            release()
        }
        audioRecord = null
        outputFile?.delete()
        outputFile = null
    }

    private fun writeWavHeader(file: File) {
        val totalLen = file.length()
        val dataLen = totalLen - HEADER_SIZE
        val header = ByteBuffer.allocate(HEADER_SIZE.toInt()).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt((totalLen - 8).toInt())
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)                       // fmt chunk size
            putShort(1)                      // PCM
            putShort(1)                      // mono
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2)          // byte rate
            putShort(2)                      // block align
            putShort(16)                     // bits per sample
            put("data".toByteArray())
            putInt(dataLen.toInt())
        }
        RandomAccessFile(file, "rw").use {
            it.seek(0)
            it.write(header.array())
        }
    }
}
