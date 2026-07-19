package com.seekersofjustice.app.transcription

import com.seekersofjustice.app.domain.Segment
import com.seekersofjustice.app.domain.Transcriber
import com.seekersofjustice.app.domain.TranscriptionResult
import kotlinx.coroutines.delay

/**
 * Canned transcription backend so the whole app is demoable before
 * whisper.cpp is wired up. Delays ~4 s to make the PROCESSING state visible.
 */
class FakeTranscriber : Transcriber {

    companion object {
        private const val URDU_TEXT =
            "میرا نام عائشہ خان ہے اور میں اس واقعے کی عینی شاہد ہوں۔ " +
                "منگل کی شام تقریباً سات بجے میں بازار سے واپس آ رہی تھی۔ " +
                "میں نے دیکھا کہ دو افراد موٹر سائیکل پر آئے اور دکان کے باہر رکے۔ " +
                "ان میں سے ایک شخص نے دکاندار سے رقم چھینی اور دوسرا باہر پہرہ دیتا رہا۔ " +
                "دکاندار نے مزاحمت کی تو انہوں نے اسے دھکا دیا اور وہ زمین پر گر گیا۔ " +
                "اس کے بعد دونوں افراد تیزی سے فرار ہو گئے۔ " +
                "موٹر سائیکل کا رنگ سرخ تھا اور اس کی نمبر پلیٹ پر مٹی لگی ہوئی تھی۔ " +
                "میں نے فوراً پولیس کو اطلاع دی اور جائے وقوعہ پر موجود رہی۔"

        private const val ENGLISH_TEXT =
            "My name is Ayesha Khan and I am an eyewitness to this incident. " +
                "On Tuesday evening at around seven o'clock I was returning from the market. " +
                "I saw two men arrive on a motorcycle and stop outside the shop. " +
                "One of them snatched cash from the shopkeeper while the other kept watch outside. " +
                "When the shopkeeper resisted, they pushed him and he fell to the ground. " +
                "After that, both men fled the scene at speed. " +
                "The motorcycle was red and its number plate was smeared with mud. " +
                "I informed the police immediately and stayed at the scene."

        private const val TRANSLATION_TEXT =
            "My name is Ayesha Khan and I witnessed this incident myself. " +
                "On Tuesday evening around seven o'clock I was coming back from the bazaar. " +
                "I saw two people arrive on a motorcycle and stop outside the shop. " +
                "One of them snatched money from the shopkeeper and the other stood guard outside. " +
                "When the shopkeeper resisted, they pushed him and he fell to the ground. " +
                "Afterwards both of them fled quickly. " +
                "The motorcycle was red in colour and its number plate was covered in mud. " +
                "I informed the police at once and remained at the scene."
    }

    override suspend fun transcribe(
        pcm: FloatArray,
        language: String?,
        translateToEnglish: Boolean,
    ): TranscriptionResult {
        delay(4_000)
        val effectiveLanguage = language ?: "ur" // pretend auto-detect found Urdu
        val text = when {
            translateToEnglish -> TRANSLATION_TEXT
            effectiveLanguage == "ur" -> URDU_TEXT
            else -> ENGLISH_TEXT
        }
        val sentences = text.split(Regex("(?<=[.۔])\\s+"))
        var t = 0L
        val segments = sentences.map { sentence ->
            val duration = 2_000L + sentence.length * 10L
            Segment(startMs = t, endMs = t + duration, text = sentence).also { t += duration }
        }
        return TranscriptionResult(text, effectiveLanguage, segments)
    }
}
