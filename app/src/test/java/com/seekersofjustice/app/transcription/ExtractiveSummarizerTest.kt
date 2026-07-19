package com.seekersofjustice.app.transcription

import com.seekersofjustice.app.domain.Segment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractiveSummarizerTest {

    private val summarizer = ExtractiveSummarizer(RegexSentenceSplitter())

    private val englishText =
        "My name is Ayesha Khan and I am an eyewitness to this incident. " +
            "On Tuesday evening at around seven I was returning from the market. " +
            "I saw two men arrive on a motorcycle and stop outside the shop. " +
            "One of them snatched cash from the shopkeeper while the other kept watch. " +
            "When the shopkeeper resisted, they pushed him and he fell to the ground. " +
            "After that, both men fled the scene at speed. " +
            "The motorcycle was red and its number plate was smeared with mud. " +
            "I informed the police immediately and stayed at the scene."

    private val urduText =
        "میرا نام عائشہ خان ہے اور میں اس واقعے کی عینی شاہد ہوں۔ " +
            "منگل کی شام تقریباً سات بجے میں بازار سے واپس آ رہی تھی۔ " +
            "میں نے دیکھا کہ دو افراد موٹر سائیکل پر آئے اور دکان کے باہر رکے۔ " +
            "ان میں سے ایک شخص نے دکاندار سے رقم چھینی اور دوسرا پہرہ دیتا رہا۔ " +
            "دکاندار نے مزاحمت کی تو انہوں نے اسے دھکا دیا اور وہ زمین پر گر گیا۔ " +
            "اس کے بعد دونوں افراد تیزی سے فرار ہو گئے۔ " +
            "موٹر سائیکل کا رنگ سرخ تھا اور نمبر پلیٹ پر مٹی لگی تھی۔ " +
            "میں نے فوراً پولیس کو اطلاع دی۔"

    @Test
    fun `english text yields 5 key points in document order`() {
        val points = summarizer.summarize(englishText, languageCode = "en")
        assertEquals(5, points.size)
        // Document order: each point's position in the original text must increase.
        val positions = points.map { englishText.indexOf(it.take(30)) }
        assertTrue(positions.all { it >= 0 })
        assertEquals(positions, positions.sorted())
    }

    @Test
    fun `urdu text with urdu full stops yields 5 key points`() {
        val points = summarizer.summarize(urduText, languageCode = "ur")
        assertEquals(5, points.size)
        assertTrue(points.all { it.isNotBlank() })
    }

    @Test
    fun `short text returns all sentences`() {
        val points = summarizer.summarize("One sentence. Two sentences here. Third one.", languageCode = "en")
        assertEquals(3, points.size)
    }

    @Test
    fun `punctuation-sparse text falls back to segments`() {
        val sparse = "میرا نام عائشہ ہے اور میں گواہ ہوں اور واقعہ منگل کو ہوا"
        val segments = listOf(
            Segment(0, 2000, "میرا نام عائشہ ہے"),
            Segment(2000, 4000, "میں گواہ ہوں"),
            Segment(4000, 6000, "واقعہ منگل کو ہوا"),
            Segment(6000, 8000, "دو افراد موٹر سائیکل پر آئے"),
        )
        val points = summarizer.summarize(sparse, segments = segments, languageCode = "ur")
        assertEquals(4, points.size)
        assertEquals("میرا نام عائشہ ہے", points[0])
    }

    @Test
    fun `empty text yields no points`() {
        assertTrue(summarizer.summarize("", languageCode = "en").isEmpty())
    }
}
