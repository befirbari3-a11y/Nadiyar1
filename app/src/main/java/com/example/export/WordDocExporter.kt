package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.*
import com.example.util.MathMarkup
import com.example.util.MathMarkup.escapeHtml
import java.io.File

object WordDocExporter {

    fun calculateTotalMarks(doc: ExamDocumentState): String {
        val manual = doc.header.values[HeaderFieldKey.SCR].orEmpty().trim()
        if (manual.isNotEmpty()) {
            return LocalizationPresets.formatNumber(manual, doc.numeralMode, doc.language)
        }
        var sum = 0.0
        doc.questions.forEach { q ->
            if (!q.type.isNoHeader) {
                val n = LocalizationPresets.toEnglishDigits(q.marks).toDoubleOrNull()
                if (n != null) sum += n
            }
        }
        val clean = if (sum % 1.0 == 0.0) sum.toInt().toString() else sum.toString()
        return LocalizationPresets.formatNumber(clean, doc.numeralMode, doc.language)
    }

    fun buildExamWordHtml(doc: ExamDocumentState): String {
        val d = LocalizationPresets.strings(doc.language)
        val dir = if (doc.language.isRtl) "rtl" else "ltr"
        val tot = calculateTotalMarks(doc)
        val hdr = doc.header
        val st = doc.style
        val logoHtml = if (hdr.logoBase64.isNotBlank()) {
            """<img src="${hdr.logoBase64}" width="${hdr.logoSizeDp}">"""
        } else ""

        val pagesHtml = doc.pages.mapIndexed { pi, pg ->
            val sb = StringBuilder()
            if (pg.showHeader) {
                fun row(k: HeaderFieldKey, text: String): String =
                    if (hdr.visible[k] != false) "<div>$text</div>" else ""

                val min = escapeHtml(hdr.values[HeaderFieldKey.MIN].orEmpty())
                val sch = escapeHtml(hdr.values[HeaderFieldKey.SCH].orEmpty())
                val sub = escapeHtml(hdr.values[HeaderFieldKey.SUB].orEmpty())
                val grd = escapeHtml(hdr.values[HeaderFieldKey.GRD].orEmpty())
                val tch = escapeHtml(hdr.values[HeaderFieldKey.TCH].orEmpty())
                val bis = escapeHtml(hdr.values[HeaderFieldKey.BIS].orEmpty())
                val typ = escapeHtml(hdr.values[HeaderFieldKey.TYP].orEmpty())
                val yr = escapeHtml(hdr.values[HeaderFieldKey.YR].orEmpty())
                val stu = escapeHtml(hdr.values[HeaderFieldKey.STU].orEmpty())
                val dat = escapeHtml(hdr.values[HeaderFieldKey.DAT].orEmpty())
                val tim = escapeHtml(hdr.values[HeaderFieldKey.TIM].orEmpty())

                sb.append(
                    """<table width="100%" style="border-bottom:2px solid ${st.headerColorHex};margin-bottom:8px;color:${st.headerColorHex};font-size:12px"><tr>
                    <td width="38%" valign="top">${if (hdr.logoPos == LogoPosition.RIGHT) logoHtml else ""}${row(HeaderFieldKey.MIN, min)}${row(HeaderFieldKey.SCH, sch)}${if (hdr.visible[HeaderFieldKey.SUB] != false || hdr.visible[HeaderFieldKey.GRD] != false) "<div>${if (hdr.visible[HeaderFieldKey.SUB] != false) sub else ""} &nbsp; ${if (hdr.visible[HeaderFieldKey.GRD] != false) grd else ""}</div>" else ""}${row(HeaderFieldKey.TCH, tch)}</td>
                    <td width="24%" align="center" valign="top">${row(HeaderFieldKey.BIS, "<b>$bis</b>")}${if (hdr.logoPos == LogoPosition.CENTER) logoHtml else ""}${if (hdr.visible[HeaderFieldKey.TYP] != false || hdr.visible[HeaderFieldKey.YR] != false) """<div style="border:1.5px solid ${st.headerColorHex};padding:4px 8px;margin-top:3px"><b>${if (hdr.visible[HeaderFieldKey.TYP] != false) typ else ""}</b><br><small>${if (hdr.visible[HeaderFieldKey.YR] != false) yr else ""}</small></div>""" else ""}</td>
                    <td width="38%" valign="top">${if (hdr.logoPos == LogoPosition.LEFT) """<div align="left">$logoHtml</div>""" else ""}${row(HeaderFieldKey.STU, "<b>$stu</b> ____________")}${if (hdr.visible[HeaderFieldKey.DAT] != false || hdr.visible[HeaderFieldKey.TIM] != false) """<div style="border-top:1px solid #ccc;border-bottom:1px solid #ccc;padding:2px 0;margin:2px 0">${if (hdr.visible[HeaderFieldKey.DAT] != false) dat else ""} &nbsp;|&nbsp; ${if (hdr.visible[HeaderFieldKey.TIM] != false) tim else ""}</div>""" else ""}${row(HeaderFieldKey.SCR, """${d.scoreLabel} <span style="border:1.5px solid ${st.headerColorHex};padding:1px 8px"><b>$tot</b></span>""")}</td>
                    </tr></table>"""
                )
            }

            val pageQs = doc.questions.withIndex().filter { it.value.pageIndex.coerceIn(0, doc.pages.lastIndex) == pi }
            pageQs.forEach { (qi, q) ->
                val noHdr = q.type.isNoHeader
                var displayNum = 0
                for (idx in 0..qi) if (!doc.questions[idx].type.isNoHeader) displayNum++
                val qNumStr = LocalizationPresets.formatNumber(displayNum, doc.numeralMode, doc.language)

                val bodyHtml = StringBuilder()
                when (q.type) {
                    QuestionType.MATCH -> {
                        bodyHtml.append("""<table width="100%" style="margin:4px 0">""")
                        q.items.forEachIndexed { ii, it ->
                            val idxStr = LocalizationPresets.formatNumber(ii + 1, doc.numeralMode, doc.language)
                            val rightLbl = d.matchRightLabels.getOrElse(ii) { "${ii + 1}-" }
                            bodyHtml.append("""<tr><td width="50%" style="padding:3px">$idxStr- ${MathMarkup.toWordHtml(it.text)}</td><td width="50%" style="padding:3px">$rightLbl ${MathMarkup.toWordHtml(it.matchRight)}</td></tr>""")
                        }
                        bodyHtml.append("</table>")
                    }
                    QuestionType.CONCEPT -> {
                        bodyHtml.append("""<p align="center"><b>[ ${MathMarkup.toWordHtml(q.conceptCenter)} ]</b><br>${q.conceptBranches.joinToString(" &nbsp; ") { "[ ${MathMarkup.toWordHtml(it)} ]" }}</p>""")
                    }
                    QuestionType.COUNT -> {
                        bodyHtml.append("""<table width="100%" style="margin:3px 0">""")
                        q.items.chunked(4).forEachIndexed { rowIdx, chunk ->
                            bodyHtml.append("<tr>")
                            chunk.forEachIndexed { colIdx, it ->
                                val idxStr = LocalizationPresets.formatNumber(rowIdx * 4 + colIdx + 1, doc.numeralMode, doc.language)
                                bodyHtml.append("""<td width="25%" valign="top">$idxStr- ${MathMarkup.toWordHtml(it.text)}</td>""")
                            }
                            bodyHtml.append("</tr>")
                        }
                        bodyHtml.append("</table>")
                    }
                    else -> {
                        if (q.type == QuestionType.IMG && q.imageBase64.isNotBlank()) {
                            bodyHtml.append("""<p><img src="${q.imageBase64}" width="${q.imageWidthDp}"></p>""")
                        }
                        q.items.forEachIndexed { ii, it ->
                            val idxStr = LocalizationPresets.formatNumber(ii + 1, doc.numeralMode, doc.language)
                            bodyHtml.append("""<div style="margin-bottom:3px">${if (noHdr) "" else "$idxStr- "}${MathMarkup.toWordHtml(it.text)}""")
                            if (q.type == QuestionType.MCQ) {
                                bodyHtml.append("""<table width="100%" style="margin-top:2px"><tr>""")
                                it.options.forEachIndexed { oi, op ->
                                    bodyHtml.append("""<td width="25%">${d.mcqLabels.getOrElse(oi) { "" }} ${MathMarkup.toWordHtml(op.text)}</td>""")
                                }
                                bodyHtml.append("</tr></table>")
                            }
                            if (q.type == QuestionType.CALLIG) {
                                bodyHtml.append("""<div style="border-bottom:1px solid #888;height:20px"></div><div style="border-bottom:1px solid #888;height:20px"></div>""")
                            }
                            bodyHtml.append("</div>")
                        }
                        if (q.spacerHeightDp > 0) {
                            bodyHtml.append("""<div style="height:${q.spacerHeightDp}px"></div>""")
                        }
                    }
                }
                repeat(q.answerLines) {
                    bodyHtml.append("""<div style="border-bottom:1px solid #888;height:${q.lineSize.heightDp}px"></div>""")
                }
                val qHdr = if (noHdr) "" else """<table width="100%" style="color:${st.questionTitleColorHex};font-weight:bold;margin:6px 0 3px"><tr><td>${d.qPrefix}$qNumStr/ ${MathMarkup.toWordHtml(q.title)}</td><td align="${if (doc.language.isRtl) "left" else "right"}" width="110">(${escapeHtml(q.marks)} ${d.markWord})</td></tr></table>"""
                val bgCss = if (q.bgColorHex.isNotBlank()) "background-color:${q.bgColorHex};padding:6px;" else ""
                val sepCss = if (noHdr || q.type == QuestionType.FREE || q.noSeparatorLine) "" else "border-bottom:1px solid #cbd5e1;padding-bottom:6px;"
                sb.append("""<div style="${bgCss}${sepCss}margin-bottom:8px">$qHdr$bodyHtml</div>""")
            }

            if (pg.freeWriteText.isNotBlank()) {
                sb.append("""<div>${MathMarkup.toWordHtml(pg.freeWriteText)}</div>""")
            }
            pg.floatingElements.forEach { fl ->
                when (fl.type) {
                    FloatingElementType.TABLE -> {
                        sb.append("""<table border="1" style="border-collapse:collapse;width:100%;margin:4px 0">""")
                        fl.tableRows.forEachIndexed { rIdx, row ->
                            sb.append("<tr>")
                            row.forEach { cell ->
                                val tag = if (rIdx == 0) "th" else "td"
                                sb.append("""<$tag style="padding:4px">${escapeHtml(cell)}</$tag>""")
                            }
                            sb.append("</tr>")
                        }
                        sb.append("</table><br>")
                    }
                    FloatingElementType.IMAGE -> if (fl.imageBase64.isNotBlank()) {
                        sb.append("""<p><img src="${fl.imageBase64}" width="180"></p>""")
                    }
                    FloatingElementType.TEXT_BOX, FloatingElementType.MED_CIRCLE -> {
                        sb.append("""<div style="padding:4px 0">${escapeHtml(fl.text).replace("\n", "<br>")}</div>""")
                    }
                    else -> {}
                }
            }
            if (pg.showHeader && hdr.visible[HeaderFieldKey.FT] != false) {
                sb.append("""<p align="center" style="margin-top:16px;font-weight:bold;color:${st.headerColorHex}">${escapeHtml(hdr.values[HeaderFieldKey.FT].orEmpty())}</p>""")
            }

            """<div style="border:1.5px solid ${st.borderColorHex};background-color:${st.paperBgColorHex};color:${st.bodyTextColorHex};padding:14px">$sb</div>""" +
                if (pi < doc.pages.lastIndex) """<br clear="all" style="page-break-before:always">""" else ""
        }.joinToString("")

        val fontCss = com.example.ui.theme.ExamFontOption.fromKey(st.fontKey).cssName
        return """<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" dir="$dir"><head><meta charset="utf-8"><style>body{font-family:$fontCss,Arial,sans-serif;font-size:${st.fontSizeSp}px;color:${st.bodyTextColorHex}}table{border-collapse:collapse}td,th{font-size:${st.fontSizeSp}px}.fr{display:inline-table;vertical-align:middle;text-align:center}.ft{border-bottom:1px solid #000;display:block;padding:0 4px}.fb{display:block;padding:0 4px}</style></head><body>$pagesHtml</body></html>"""
    }

    fun buildCertificateWordHtml(cert: CertificateState): String {
        val fontCss = com.example.ui.theme.ExamFontOption.fromKey(cert.fontKey).cssName
        val flHtml = cert.floatingElements.joinToString("") { fl ->
            when (fl.type) {
                FloatingElementType.IMAGE -> if (fl.imageBase64.isNotBlank()) """<p align="center"><img src="${fl.imageBase64}" width="120"></p>""" else ""
                else -> """<div style="text-align:center;margin:8px 0">${escapeHtml(fl.text).replace("\n", "<br>")}</div>"""
            }
        }
        return """<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" dir="rtl"><head><meta charset="utf-8"><style>body{font-family:$fontCss,Arial,sans-serif;}</style></head><body style="background:${cert.bgColorHex};color:${cert.textColorHex};border:6px double #0d9488;padding:32px;text-align:center">
            $flHtml
            <div style="font-size:${(15 * cert.fontScale).toInt()}px;font-weight:700">${escapeHtml(cert.topLine1)}</div>
            <div style="font-size:${(14 * cert.fontScale).toInt()}px;margin:4px 0 14px">${escapeHtml(cert.topLine2)}</div>
            <div style="font-size:${(28 * cert.fontScale).toInt()}px;font-weight:800;margin-bottom:16px">${escapeHtml(cert.badgeTitle)}</div>
            <div style="font-size:${(18 * cert.fontScale).toInt()}px;line-height:1.9">${escapeHtml(cert.bodyTop).replace("\n", "<br>")}</div>
            <div style="font-size:${(24 * cert.fontScale).toInt()}px;font-weight:800;margin:10px 0">${escapeHtml(cert.recipientName)}</div>
            <div style="font-size:${(18 * cert.fontScale).toInt()}px;line-height:1.9">${escapeHtml(cert.bodyBottom).replace("\n", "<br>")}</div>
            <table width="100%" style="margin-top:34px;text-align:center;font-size:${(15 * cert.fontScale).toInt()}px;font-weight:700"><tr>
                <td>${escapeHtml(cert.footerLeft).replace("\n", "<br>")}</td>
                <td>${escapeHtml(cert.footerCenter).replace("\n", "<br>")}</td>
                <td>${escapeHtml(cert.footerRight).replace("\n", "<br>")}</td>
            </tr></table>
        </body></html>"""
    }

    fun writeToUri(context: Context, uri: Uri, html: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            os.write(html.toByteArray(Charsets.UTF_8))
        }
        true
    }.getOrDefault(false)

    fun shareDocFile(context: Context, fileName: String, html: String, mimeType: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { os ->
            if (mimeType == "application/msword") {
                os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            }
            os.write(html.toByteArray(Charsets.UTF_8))
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, fileName))
    }
}
