package com.seekersofjustice.app.transcription

import com.seekersofjustice.app.domain.Segment
import kotlin.math.pow

/** Splits text into sentences. Interface so JVM tests can avoid android.icu. */
interface SentenceSplitter {
    fun split(text: String, languageCode: String): List<String>
}

/** Regex fallback covering English and Urdu terminators (۔ ؟). */
class RegexSentenceSplitter : SentenceSplitter {
    override fun split(text: String, languageCode: String): List<String> =
        text.split(Regex("(?<=[.!?؟۔])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}

/** ICU-based splitter; handles locale-specific sentence breaks. */
class IcuSentenceSplitter : SentenceSplitter {
    override fun split(text: String, languageCode: String): List<String> {
        val iterator = android.icu.text.BreakIterator.getSentenceInstance(
            android.icu.util.ULocale(languageCode)
        )
        iterator.setText(text)
        val sentences = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != android.icu.text.BreakIterator.DONE) {
            val sentence = text.substring(start, end).trim()
            if (sentence.isNotEmpty()) sentences.add(sentence)
            start = end
            end = iterator.next()
        }
        return sentences
    }
}

/**
 * Extractive key-point summarizer: TF-scored sentence ranking, no LLM.
 * Works for English and Urdu; falls back to Whisper segments when the
 * transcript is punctuation-sparse (common for Urdu output).
 */
class ExtractiveSummarizer(
    private val splitter: SentenceSplitter = RegexSentenceSplitter(),
) {

    companion object {
        private val ENGLISH_STOPWORDS = setOf(
            "the", "a", "an", "and", "or", "but", "if", "then", "so", "of", "to", "in",
            "on", "at", "by", "for", "with", "about", "as", "into", "from", "up", "down",
            "is", "am", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "will", "would", "can", "could", "shall", "should",
            "may", "might", "must", "i", "me", "my", "we", "us", "our", "you", "your",
            "he", "him", "his", "she", "her", "it", "its", "they", "them", "their",
            "this", "that", "these", "those", "there", "here", "what", "which", "who",
            "when", "where", "how", "not", "no", "yes", "very", "just", "also", "than",
        )
        private val URDU_STOPWORDS = setOf(
            "کے", "کی", "کا", "کو", "نے", "سے", "میں", "پر", "اور", "یا", "ہے", "ہیں",
            "تھا", "تھی", "تھے", "ہو", "ہوں", "ہوا", "ہوئی", "ہوئے", "گا", "گی", "گے",
            "یہ", "وہ", "اس", "ان", "اپنا", "اپنی", "اپنے", "ہم", "تم", "آپ", "کچھ",
            "سب", "ایک", "دو", "بھی", "تو", "ہی", "نہ", "نہیں", "کہ", "جو", "جب",
            "تب", "پھر", "اب", "یہاں", "وہاں", "کیا", "کیوں", "کیسے", "کون", "لیے",
            "لئے", "ساتھ", "بعد", "پہلے", "تک", "والا", "والی", "والے", "رہا", "رہی",
            "رہے", "دیا", "دی", "دیے", "لیا", "لی", "گیا", "گئی", "گئے", "کر", "کریں",
        )
        private val URDU_DIACRITICS = Regex("[\\u064B-\\u065F\\u0670]")
        private val WORD_REGEX = Regex("[\\p{L}\\p{Mn}\\p{Nd}]+")
    }

    fun summarize(
        text: String,
        segments: List<Segment> = emptyList(),
        languageCode: String = "en",
        count: Int = 5,
    ): List<String> {
        var sentences = splitter.split(text, languageCode)
        if (sentences.size < 3 && segments.isNotEmpty()) {
            // Punctuation-sparse transcript: use Whisper segments as candidates.
            sentences = segments.map { it.text.trim() }.filter { it.isNotEmpty() }
        }
        if (sentences.size <= count) return sentences

        val stopwords = if (languageCode == "ur") URDU_STOPWORDS else ENGLISH_STOPWORDS

        val tokenized = sentences.map { tokenize(it, languageCode) }
        val frequency = HashMap<String, Int>()
        for (tokens in tokenized) {
            for (token in tokens) {
                if (token !in stopwords) frequency.merge(token, 1, Int::plus)
            }
        }
        val maxFreq = frequency.values.maxOrNull() ?: return sentences.take(count)

        val scored = sentences.indices.map { i ->
            val tokens = tokenized[i]
            val raw = tokens.sumOf { token ->
                if (token in stopwords) 0.0 else (frequency[token] ?: 0).toDouble() / maxFreq
            }
            // Soft length normalization: favors informative mid-length sentences.
            val score = if (tokens.isEmpty()) 0.0 else raw / tokens.size.toDouble().pow(0.8)
            i to score
        }

        return scored
            .sortedByDescending { it.second }
            .take(count)
            .map { it.first }
            .sorted() // restore document order
            .map { sentences[it].take(200) }
    }

    private fun tokenize(sentence: String, languageCode: String): List<String> {
        val normalized = if (languageCode == "ur") {
            sentence.replace(URDU_DIACRITICS, "")
        } else {
            sentence.lowercase()
        }
        return WORD_REGEX.findAll(normalized).map { it.value }.toList()
    }
}
