package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ExamJsonCodec
import com.example.export.WordDocExporter
import com.example.model.*
import com.example.util.MathMarkup
import com.example.util.RichSegment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Nadiyar Kurdi", appName)
  }

  @Test
  fun `math markup parses math and rich inline styles`() {
    val input = "Solve [frac:3|4] + [root:3|8] and [b:bold] [hi:#FEF08A|mark]"
    val segments = MathMarkup.parse(input)
    assertTrue(segments.any { it == RichSegment.Fraction("3", "4") })
    assertTrue(segments.any { it == RichSegment.Root("3", "8") })
    assertTrue(segments.any { it == RichSegment.Styled(text = "bold", bold = true) })
    assertTrue(segments.any { it == RichSegment.Styled(text = "mark", highlightHex = "#FEF08A") })

    val html = MathMarkup.toWordHtml(input)
    assertTrue(html.contains("<b>bold</b>"))
    assertTrue(html.contains("background-color:#FEF08A"))
    assertTrue(html.contains("<span class=\"fr\">"))

    val plain = MathMarkup.toPlainReadable(input)
    assertTrue(plain.contains("(3/4)"))
    assertTrue(plain.contains("bold"))
  }

  @Test
  fun `calculateTotalMarks sums visible question marks and respects manual override`() {
    val q1 = LocalizationPresets.createQuestion(QuestionType.ESSAY, ExamLanguage.BADINI, NumeralMode.EN, 0).copy(marks = "15")
    val q2 = LocalizationPresets.createQuestion(QuestionType.MCQ, ExamLanguage.BADINI, NumeralMode.EN, 0).copy(marks = "٢٥")
    val qWrite = LocalizationPresets.createQuestion(QuestionType.WRITE, ExamLanguage.BADINI, NumeralMode.EN, 0).copy(marks = "99")
    val doc = ExamDocumentState(
      language = ExamLanguage.ENGLISH,
      numeralMode = NumeralMode.EN,
      header = HeaderState(values = mapOf(HeaderFieldKey.SCR to "")),
      questions = listOf(q1, q2, qWrite),
      pages = listOf(ExamPage())
    )
    assertEquals("40", WordDocExporter.calculateTotalMarks(doc))

    val manualDoc = doc.copy(header = doc.header.copy(values = mapOf(HeaderFieldKey.SCR to "100")))
    assertEquals("100", WordDocExporter.calculateTotalMarks(manualDoc))
  }

  @Test
  fun `json codec round trips document and certificate fontScale`() {
    val q = LocalizationPresets.createQuestion(QuestionType.MATCH, ExamLanguage.SORANI, NumeralMode.KU, 0)
    val doc = ExamDocumentState(
      language = ExamLanguage.SORANI,
      numeralMode = NumeralMode.KU,
      header = HeaderState(values = LocalizationPresets.strings(ExamLanguage.SORANI).headerDefaults, locked = true),
      questions = listOf(q),
      pages = listOf(ExamPage())
    )
    val decodedDoc = ExamJsonCodec.decodeDocument(ExamJsonCodec.encodeDocument(doc))
    assertEquals(ExamLanguage.SORANI, decodedDoc.language)
    assertEquals(NumeralMode.KU, decodedDoc.numeralMode)
    assertTrue(decodedDoc.header.locked)
    assertEquals(1, decodedDoc.questions.size)
    assertEquals(QuestionType.MATCH, decodedDoc.questions[0].type)

    val cert = CertificateState(
      recipientName = "ئەحمەد",
      fontScale = 1.4f,
      borderStyle = CertBorderStyle.CLASSIC_GEO
    )
    val decodedCert = ExamJsonCodec.decodeCertificate(ExamJsonCodec.encodeCertificate(cert))
    assertEquals("ئەحمەد", decodedCert.recipientName)
    assertEquals(1.4f, decodedCert.fontScale, 0.01f)
    assertEquals(CertBorderStyle.CLASSIC_GEO, decodedCert.borderStyle)
  }
}
