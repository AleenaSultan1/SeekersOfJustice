package com.seekersofjustice.app.audio

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder

/**
 * Decodes WAV or FLAC files into the 16 kHz mono float32 PCM that
 * [com.seekersofjustice.app.domain.Transcriber] expects.
 */
class AudioDecoder {

    companion object {
        const val TARGET_SAMPLE_RATE = 16_000
    }

    fun decode(file: File): FloatArray {
        val header = file.inputStream().use { ByteArray(4).also(it::read) }
        return when {
            header.decodeToString() == "RIFF" -> decodeWav(file)
            header.decodeToString() == "fLaC" -> decodeWithCodec(file)
            else -> throw IllegalArgumentException("Unsupported audio file: ${file.name}")
        }
    }

    // --- WAV: direct RIFF parse (PCM16 and IEEE float32), no codec involved ---

    private fun decodeWav(file: File): FloatArray {
        val bytes = file.readBytes()
        val bb = java.nio.ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(bytes.size > 44) { "WAV file too short" }
        bb.position(12) // skip RIFF header

        var audioFormat = 0
        var channels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var dataOffset = -1
        var dataSize = 0

        while (bb.remaining() >= 8) {
            val id = ByteArray(4).also { bb.get(it) }.decodeToString()
            val size = bb.int
            when (id) {
                "fmt " -> {
                    val start = bb.position()
                    audioFormat = bb.short.toInt() and 0xFFFF
                    channels = bb.short.toInt()
                    sampleRate = bb.int
                    bb.int // byte rate
                    bb.short // block align
                    bitsPerSample = bb.short.toInt()
                    bb.position(start + size + (size and 1))
                }
                "data" -> {
                    dataOffset = bb.position()
                    dataSize = size
                    bb.position(bb.position() + size + (size and 1))
                }
                else -> bb.position(bb.position() + size + (size and 1))
            }
        }
        require(dataOffset >= 0 && channels > 0 && sampleRate > 0) { "Malformed WAV file" }
        // WAVE_FORMAT_EXTENSIBLE (0xFFFE) with PCM sub-format still has plain PCM data.
        require(audioFormat == 1 || audioFormat == 3 || audioFormat == 0xFFFE) {
            "Unsupported WAV encoding: $audioFormat"
        }
        if (dataSize <= 0 || dataOffset + dataSize > bytes.size) {
            dataSize = bytes.size - dataOffset
        }

        val data = java.nio.ByteBuffer.wrap(bytes, dataOffset, dataSize).order(ByteOrder.LITTLE_ENDIAN)
        val interleaved: FloatArray = when {
            audioFormat == 3 && bitsPerSample == 32 -> {
                FloatArray(dataSize / 4) { data.float }
            }
            bitsPerSample == 16 -> {
                FloatArray(dataSize / 2) { data.short / 32768f }
            }
            else -> throw IllegalArgumentException("Unsupported WAV: format=$audioFormat bits=$bitsPerSample")
        }
        return downmixAndResample(interleaved, channels, sampleRate)
    }

    // --- FLAC: MediaExtractor + MediaCodec ---

    private fun decodeWithCodec(file: File): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i
                format = f
                break
            }
        }
        require(trackIndex >= 0 && format != null) { "No audio track in ${file.name}" }
        extractor.selectTrack(trackIndex)

        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        val samples = ArrayList<FloatArray>()
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val inIndex = codec.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val buf = codec.getInputBuffer(inIndex)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, 10_000)
            when {
                outIndex >= 0 -> {
                    val buf = codec.getOutputBuffer(outIndex)!!
                    buf.position(info.offset)
                    buf.limit(info.offset + info.size)
                    if (info.size > 0) {
                        val chunk = if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                            val fb = buf.order(ByteOrder.nativeOrder()).asFloatBuffer()
                            FloatArray(fb.remaining()) { fb.get() }
                        } else {
                            val sb = buf.order(ByteOrder.nativeOrder()).asShortBuffer()
                            FloatArray(sb.remaining()) { sb.get() / 32768f }
                        }
                        samples.add(chunk)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val out = codec.outputFormat
                    channels = out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    sampleRate = out.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    if (out.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        pcmEncoding = out.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    }
                }
            }
        }
        codec.stop()
        codec.release()
        extractor.release()

        val total = samples.sumOf { it.size }
        val interleaved = FloatArray(total)
        var pos = 0
        for (chunk in samples) {
            chunk.copyInto(interleaved, pos)
            pos += chunk.size
        }
        return downmixAndResample(interleaved, channels, sampleRate)
    }

    // --- shared post-processing ---

    private fun downmixAndResample(interleaved: FloatArray, channels: Int, sampleRate: Int): FloatArray {
        val mono = if (channels <= 1) interleaved else {
            FloatArray(interleaved.size / channels) { frame ->
                var sum = 0f
                for (c in 0 until channels) sum += interleaved[frame * channels + c]
                sum / channels
            }
        }
        if (sampleRate == TARGET_SAMPLE_RATE) return mono

        // Linear interpolation — ample fidelity for speech recognition.
        val outLen = (mono.size.toLong() * TARGET_SAMPLE_RATE / sampleRate).toInt()
        val ratio = sampleRate.toDouble() / TARGET_SAMPLE_RATE
        return FloatArray(outLen) { i ->
            val src = i * ratio
            val i0 = src.toInt().coerceAtMost(mono.size - 1)
            val i1 = (i0 + 1).coerceAtMost(mono.size - 1)
            val frac = (src - i0).toFloat()
            mono[i0] * (1 - frac) + mono[i1] * frac
        }
    }
}
