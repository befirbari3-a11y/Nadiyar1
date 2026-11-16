package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.*
import com.example.util.MathMarkup
import com.example.util.MediaUtils
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object ExamPdfExporter {

    private const val A4_W = 595 // points (72 dpi for 210mm)
    private const val A4_H = 842 // points (72 dpi for 297mm)

    fun resolveTypeface(context: Context, fontKey: String): Typeface {
        val resId = when (fontKey) {
            "scheherazade" -> R.font.scheherazade_new
            "lateef" -> R.font.lateef
            "kufi" -> R.font.noto_kufi_arabic
            "vazirmatn" -> R.font.vazirmatn
            "naskh" -> R.font.noto_naskh_arabic
            "amiri" -> R.font.amiri
            "cairo" -> R.font.cairo
            else -> null
        }
        return if (resId != null) {
            ResourcesCompat.getFont(context, resId) ?: Typeface.DEFAULT
        } else {
            Typeface.SERIF
        }
    }

    fun printExam(context: Context, doc: ExamDocumentState) {
        val pm = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("Nadiyar-Exam.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(doc.pages.size)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                runCatching {
                    val pdf = renderExamPdfDocument(context, doc)
                    destination?.let { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { out -> pdf.writeTo(out) }
                    }
                    pdf.close()
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }.onFailure {
                    callback?.onWriteFailed(it.message)
                }
            }
        }
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .build()
        pm.print("Nadiyar Exam", adapter, attrs)
    }

    fun shareExamPdf(context: Context, doc: ExamDocumentState) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "Nadiyar-Exam.pdf")
        val pdf = renderExamPdfDocument(context, doc)
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        sharePdfFile(context, file)
    }

    fun printCertificate(context: Context, cert: CertificateState) {
        val pm = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("Nadiyar-Certificate.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                runCatching {
                    val pdf = renderCertificatePdfDocument(context, cert)
                    destination?.let { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { out -> pdf.writeTo(out) }
                    }
                    pdf.close()
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }.onFailure {
                    callback?.onWriteFailed(it.message)
                }
            }
        }
        val media = if (cert.isLandscape) PrintAttributes.MediaSize.ISO_A4.asLandscape() else PrintAttributes.MediaSize.ISO_A4
        pm.print("Nadiyar Certificate", adapter, PrintAttributes.Builder().setMediaSize(media).build())
    }

    fun shareCertificatePdf(context: Context, cert: CertificateState) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "Nadiyar-Certificate.pdf")
        val pdf = renderCertificatePdfDocument(context, cert)
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        sharePdfFile(context, file)
    }

    private fun sharePdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, file.name))
    }

    fun renderExamPdfDocument(context: Context, doc: ExamDocumentState): PdfDocument {
        val pdf = PdfDocument()
        val typeface = resolveTypeface(context, doc.style.fontKey)
        val st = doc.style
        val d = LocalizationPresets.strings(doc.language)
        val tot = WordDocExporter.calculateTotalMarks(doc)
        val bodySize = (st.fontSizeSp * 0.78f).coerceIn(8.5f, 20f)
        val hdrScale = (st.headerScalePct / 100f).coerceIn(0.65f, 1.25f)

        doc.pages.forEachIndexed { pi, pageData ->
            val pageInfo = PdfDocument.PageInfo.Builder(A4_W, A4_H, pi + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            // Paper background
            val bgPaint = Paint().apply { color = MediaUtils.parseColorInt(st.paperBgColorHex, Color.WHITE) }
            canvas.drawRect(0f, 0f, A4_W.toFloat(), A4_H.toFloat(), bgPaint)

            // Border
            drawExamBorder(canvas, st.borderStyle, MediaUtils.parseColorInt(st.borderColorHex, Color.DKGRAY))

            val marginX = 36f
            var curY = 30f
            val contentW = (A4_W - marginX * 2).toInt()

            val hdrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                color = MediaUtils.parseColorInt(st.headerColorHex, Color.BLACK)
                textSize = bodySize * 0.92f * hdrScale
            }
            val qTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = Typeface.create(typeface, Typeface.BOLD)
                color = MediaUtils.parseColorInt(st.questionTitleColorHex, Color.BLUE)
                textSize = bodySize * 1.04f
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                color = MediaUtils.parseColorInt(st.bodyTextColorHex, Color.BLACK)
                textSize = bodySize
            }

            if (pageData.showHeader) {
                curY = drawExamHeader(canvas, doc, d, tot, hdrPaint, marginX, curY, contentW.toFloat())
            }

            val pageQs = doc.questions.withIndex().filter { it.value.pageIndex.coerceIn(0, doc.pages.lastIndex) == pi }
            val align = if (doc.language.isRtl) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_NORMAL
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                strokeWidth = 0.8f
            }
            val sepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 0.8f
            }

            pageQs.forEach { (qi, q) ->
                val noHdr = q.type.isNoHeader
                var displayNum = 0
                for (idx in 0..qi) if (!doc.questions[idx].type.isNoHeader) displayNum++
                val qNumStr = LocalizationPresets.formatNumber(displayNum, doc.numeralMode, doc.language)

                val qStartY = curY
                val pad = if (q.bgColorHex.isNotBlank()) 6f else 0f
                if (q.bgColorHex.isNotBlank()) {
                    curY += pad
                }

                if (!noHdr) {
                    val titleStr = "${d.qPrefix}$qNumStr/ ${MathMarkup.toPlainReadable(q.title)}"
                    val markStr = "(${q.marks} ${d.markWord})"
                    val markW = qTitlePaint.measureText(markStr) + 12f
                    val titleLayout = StaticLayout.Builder.obtain(
                        titleStr, 0, titleStr.length, qTitlePaint, (contentW - markW - pad * 2).toInt().coerceAtLeast(100)
                    ).setAlignment(align).build()
                    canvas.save()
                    val tx = if (doc.language.isRtl) marginX + markW + pad else marginX + pad
                    canvas.translate(tx, curY)
                    titleLayout.draw(canvas)
                    canvas.restore()

                    val mx = if (doc.language.isRtl) marginX + pad else (A4_W - marginX - markW + 4f)
                    canvas.drawText(markStr, mx, curY + qTitlePaint.textSize, qTitlePaint)
                    curY += titleLayout.height + 5f
                }

                when (q.type) {
                    QuestionType.MATCH -> {
                        q.items.forEachIndexed { ii, it ->
                            val idxStr = LocalizationPresets.formatNumber(ii + 1, doc.numeralMode, doc.language)
                            val rightLbl = d.matchRightLabels.getOrElse(ii) { "${ii + 1}-" }
                            val leftText = "$idxStr- ${MathMarkup.toPlainReadable(it.text)}"
                            val rightText = "$rightLbl ${MathMarkup.toPlainReadable(it.matchRight)}"
                            val halfW = (contentW / 2 - 12).coerceAtLeast(80)
                            val lLay = StaticLayout.Builder.obtain(leftText, 0, leftText.length, bodyPaint, halfW).build()
                            val rLay = StaticLayout.Builder.obtain(rightText, 0, rightText.length, bodyPaint, halfW).build()
                            val colLeftX = if (doc.language.isRtl) marginX + halfW + 16f else marginX + 4f
                            val colRightX = if (doc.language.isRtl) marginX + 4f else marginX + halfW + 16f
                            canvas.save(); canvas.translate(colLeftX, curY); lLay.draw(canvas); canvas.restore()
                            canvas.save(); canvas.translate(colRightX, curY); rLay.draw(canvas); canvas.restore()
                            curY += maxOf(lLay.height, rLay.height) + 4f
                        }
                    }
                    QuestionType.CONCEPT -> {
                        val cText = "[ ${MathMarkup.toPlainReadable(q.conceptCenter)} ]"
                        val cw = bodyPaint.measureText(cText) + 24f
                        val cx = (A4_W - cw) / 2f
                        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.1f; color = Color.DKGRAY }
                        canvas.drawRoundRect(cx, curY, cx + cw, curY + 22f, 6f, 6f, boxPaint)
                        canvas.drawText(cText, cx + 12f, curY + 15f, bodyPaint)
                        curY += 30f
                        val branchesText = q.conceptBranches.joinToString("   |   ") { "[ ${MathMarkup.toPlainReadable(it)} ]" }
                        val bLay = StaticLayout.Builder.obtain(branchesText, 0, branchesText.length, bodyPaint, contentW).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
                        canvas.save(); canvas.translate(marginX, curY); bLay.draw(canvas); canvas.restore()
                        curY += bLay.height + 6f
                    }
                    QuestionType.COUNT -> {
                        val colW = (contentW / 4).coerceAtLeast(60)
                        q.items.chunked(4).forEachIndexed { rIdx, chunk ->
                            var rowMaxH = 0
                            chunk.forEachIndexed { cIdx, it ->
                                val idxStr = LocalizationPresets.formatNumber(rIdx * 4 + cIdx + 1, doc.numeralMode, doc.language)
                                val txt = "$idxStr- ${MathMarkup.toPlainReadable(it.text)}"
                                val lay = StaticLayout.Builder.obtain(txt, 0, txt.length, bodyPaint, colW - 6).build()
                                val x = if (doc.language.isRtl) A4_W - marginX - (cIdx + 1) * colW else marginX + cIdx * colW
                                canvas.save(); canvas.translate(x, curY); lay.draw(canvas); canvas.restore()
                                rowMaxH = maxOf(rowMaxH, lay.height)
                            }
                            curY += rowMaxH + 4f
                        }
                    }
                    else -> {
                        if (q.type == QuestionType.IMG && q.imageBase64.isNotBlank()) {
                            MediaUtils.decodeBase64Bitmap(q.imageBase64)?.let { bmp ->
                                val w = (q.imageWidthDp * 0.75f).coerceIn(60f, contentW.toFloat())
                                val h = w * bmp.height / bmp.width.coerceAtLeast(1)
                                val dst = RectF(marginX, curY, marginX + w, curY + h)
                                canvas.drawBitmap(bmp, null, dst, null)
                                curY += h + 6f
                            }
                        }
                        q.items.forEachIndexed { ii, it ->
                            val idxStr = LocalizationPresets.formatNumber(ii + 1, doc.numeralMode, doc.language)
                            val lineStr = "${if (noHdr) "" else "$idxStr- "}${MathMarkup.toPlainReadable(it.text)}"
                            val lay = StaticLayout.Builder.obtain(lineStr, 0, lineStr.length, bodyPaint, contentW - 8).build()
                            canvas.save(); canvas.translate(marginX + 4f, curY); lay.draw(canvas); canvas.restore()
                            curY += lay.height + 3f

                            if (q.type == QuestionType.MCQ) {
                                val opW = (contentW - 16) / 4
                                var maxOpH = 0
                                it.options.forEachIndexed { oi, op ->
                                    val opStr = "${d.mcqLabels.getOrElse(oi) { "" }} ${MathMarkup.toPlainReadable(op.text)}"
                                    val opLay = StaticLayout.Builder.obtain(opStr, 0, opStr.length, bodyPaint, opW - 4).build()
                                    val ox = if (doc.language.isRtl) A4_W - marginX - 8 - (oi + 1) * opW else marginX + 8 + oi * opW
                                    canvas.save(); canvas.translate(ox, curY); opLay.draw(canvas); canvas.restore()
                                    maxOpH = maxOf(maxOpH, opLay.height)
                                }
                                curY += maxOpH + 3f
                            }
                            if (q.type == QuestionType.CALLIG) {
                                repeat(2) {
                                    curY += q.lineSize.heightDp * 0.72f
                                    canvas.drawLine(marginX, curY, A4_W - marginX, curY, linePaint)
                                }
                                curY += 4f
                            }
                        }
                        if (q.spacerHeightDp > 0) curY += q.spacerHeightDp * 0.72f
                    }
                }

                repeat(q.answerLines) {
                    curY += q.lineSize.heightDp * 0.72f
                    canvas.drawLine(marginX, curY, A4_W - marginX, curY, linePaint)
                }

                if (q.bgColorHex.isNotBlank()) {
                    // Multiply blend preserves crisp black ink while tinting the paper behind the question
                    val tintPaint = Paint().apply {
                        color = MediaUtils.parseColorInt(q.bgColorHex, Color.LTGRAY)
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                    }
                    canvas.drawRoundRect(marginX - 4f, qStartY, A4_W - marginX + 4f, curY + 4f, 6f, 6f, tintPaint)
                    curY += 6f
                }

                if (!noHdr && q.type != QuestionType.FREE && !q.noSeparatorLine) {
                    curY += 4f
                    canvas.drawLine(marginX, curY, A4_W - marginX, curY, sepPaint)
                }
                curY += 7f
            }

            if (pageData.freeWriteText.isNotBlank()) {
                val fwStr = MathMarkup.toPlainReadable(pageData.freeWriteText)
                val fwLay = StaticLayout.Builder.obtain(fwStr, 0, fwStr.length, bodyPaint, contentW).build()
                canvas.save(); canvas.translate(marginX, curY); fwLay.draw(canvas); canvas.restore()
            }

            // Floating Elements
            drawFloatingElements(canvas, pageData.floatingElements, A4_W.toFloat(), A4_H.toFloat(), bodyPaint)

            // Pen Strokes
            drawPenStrokes(canvas, pageData.penStrokes, A4_W.toFloat(), A4_H.toFloat())

            // Footer
            if (pageData.showHeader && doc.header.visible[HeaderFieldKey.FT] != false) {
                val ftText = doc.header.values[HeaderFieldKey.FT].orEmpty()
                val ftPaint = TextPaint(hdrPaint).apply {
                    this.typeface = Typeface.create(typeface, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(ftText, A4_W / 2f, A4_H - 22f, ftPaint)
            }

            pdf.finishPage(page)
        }
        return pdf
    }

    private fun drawExamHeader(
        canvas: Canvas,
        doc: ExamDocumentState,
        d: LangStrings,
        tot: String,
        hdrPaint: TextPaint,
        marginX: Float,
        startY: Float,
        contentW: Float
    ): Float {
        var y = startY
        val hdr = doc.header
        val boldPaint = TextPaint(hdrPaint).apply { typeface = Typeface.create(hdrPaint.typeface, Typeface.BOLD) }

        if (hdr.visible[HeaderFieldKey.BIS] != false) {
            val bis = hdr.values[HeaderFieldKey.BIS].orEmpty()
            val cPaint = TextPaint(boldPaint).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText(bis, A4_W / 2f, y + hdrPaint.textSize, cPaint)
            y += hdrPaint.textSize + 6f
        }

        val colW = (contentW * 0.36f).toInt()
        val cenW = (contentW * 0.24f).toInt()
        val leftX = marginX
        val cenX = marginX + contentW * 0.38f
        val rightX = marginX + contentW * 0.64f

        val colStartX = if (doc.language.isRtl) rightX else leftX
        val colEndX = if (doc.language.isRtl) leftX else rightX

        // Logo if present
        val logoBmp = MediaUtils.decodeBase64Bitmap(hdr.logoBase64)
        val logoPts = (hdr.logoSizeDp * 0.72f).coerceIn(22f, 70f)

        // Start column (Ministry, School, Subject+Grade, Teacher)
        var sy = y
        if (hdr.logoPos == LogoPosition.RIGHT && logoBmp != null) {
            val lw = logoPts * logoBmp.width / logoBmp.height.coerceAtLeast(1)
            canvas.drawBitmap(logoBmp, null, RectF(colStartX, sy, colStartX + lw, sy + logoPts), null)
            sy += logoPts + 3f
        }
        val startLines = buildList {
            if (hdr.visible[HeaderFieldKey.MIN] != false) add(hdr.values[HeaderFieldKey.MIN].orEmpty())
            if (hdr.visible[HeaderFieldKey.SCH] != false) add(hdr.values[HeaderFieldKey.SCH].orEmpty())
            val sg = listOfNotNull(
                hdr.values[HeaderFieldKey.SUB]?.takeIf { hdr.visible[HeaderFieldKey.SUB] != false },
                hdr.values[HeaderFieldKey.GRD]?.takeIf { hdr.visible[HeaderFieldKey.GRD] != false }
            ).joinToString("  ")
            if (sg.isNotBlank()) add(sg)
            if (hdr.visible[HeaderFieldKey.TCH] != false) add(hdr.values[HeaderFieldKey.TCH].orEmpty())
        }
        startLines.forEach { ln ->
            val lay = StaticLayout.Builder.obtain(ln, 0, ln.length, hdrPaint, colW).build()
            canvas.save(); canvas.translate(colStartX, sy); lay.draw(canvas); canvas.restore()
            sy += lay.height + 2f
        }

        // Center column (Logo + Exam Type / Year Box)
        var cy = y
        if (hdr.logoPos == LogoPosition.CENTER && logoBmp != null) {
            val lw = logoPts * logoBmp.width / logoBmp.height.coerceAtLeast(1)
            val lx = cenX + (cenW - lw) / 2f
            canvas.drawBitmap(logoBmp, null, RectF(lx, cy, lx + lw, cy + logoPts), null)
            cy += logoPts + 3f
        }
        if (hdr.visible[HeaderFieldKey.TYP] != false || hdr.visible[HeaderFieldKey.YR] != false) {
            val typTxt = if (hdr.visible[HeaderFieldKey.TYP] != false) hdr.values[HeaderFieldKey.TYP].orEmpty() else ""
            val yrTxt = if (hdr.visible[HeaderFieldKey.YR] != false) hdr.values[HeaderFieldKey.YR].orEmpty() else ""
            val boxTxt = listOf(typTxt, yrTxt).filter { it.isNotBlank() }.joinToString("\n")
            val bLay = StaticLayout.Builder.obtain(boxTxt, 0, boxTxt.length, boldPaint, cenW - 8)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
            val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
                color = hdrPaint.color
            }
            canvas.drawRoundRect(cenX, cy, cenX + cenW, cy + bLay.height + 10f, 10f, 10f, boxStroke)
            canvas.save(); canvas.translate(cenX + 4f, cy + 5f); bLay.draw(canvas); canvas.restore()
            cy += bLay.height + 14f
        }

        // End column (Student Name, Date/Time, Score)
        var ey = y
        if (hdr.logoPos == LogoPosition.LEFT && logoBmp != null) {
            val lw = logoPts * logoBmp.width / logoBmp.height.coerceAtLeast(1)
            canvas.drawBitmap(logoBmp, null, RectF(colEndX, ey, colEndX + lw, ey + logoPts), null)
            ey += logoPts + 3f
        }
        if (hdr.visible[HeaderFieldKey.STU] != false) {
            val stu = "${hdr.values[HeaderFieldKey.STU].orEmpty()} ...................."
            val lay = StaticLayout.Builder.obtain(stu, 0, stu.length, boldPaint, colW).build()
            canvas.save(); canvas.translate(colEndX, ey); lay.draw(canvas); canvas.restore()
            ey += lay.height + 3f
        }
        if (hdr.visible[HeaderFieldKey.DAT] != false || hdr.visible[HeaderFieldKey.TIM] != false) {
            val dt = listOfNotNull(
                hdr.values[HeaderFieldKey.DAT]?.takeIf { hdr.visible[HeaderFieldKey.DAT] != false },
                hdr.values[HeaderFieldKey.TIM]?.takeIf { hdr.visible[HeaderFieldKey.TIM] != false }
            ).joinToString("  |  ")
            val lay = StaticLayout.Builder.obtain(dt, 0, dt.length, hdrPaint, colW).build()
            canvas.save(); canvas.translate(colEndX, ey); lay.draw(canvas); canvas.restore()
            ey += lay.height + 3f
        }
        if (hdr.visible[HeaderFieldKey.SCR] != false) {
            val scr = "${d.scoreLabel} [ $tot ]"
            val lay = StaticLayout.Builder.obtain(scr, 0, scr.length, boldPaint, colW).build()
            canvas.save(); canvas.translate(colEndX, ey); lay.draw(canvas); canvas.restore()
            ey += lay.height + 3f
        }

        val finalY = maxOf(sy, cy, ey) + 6f
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = hdrPaint.color; strokeWidth = 1.5f }
        canvas.drawLine(marginX, finalY, A4_W - marginX, finalY, divPaint)
        return finalY + 10f
    }

    private fun drawExamBorder(canvas: Canvas, style: BorderStyle, color: Int) {
        if (style == BorderStyle.NONE) return
        val m = 16f
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            this.color = color
            strokeWidth = if (style == BorderStyle.DOUBLE) 2f else 1.8f
        }
        when (style) {
            BorderStyle.SOLID -> canvas.drawRect(m, m, A4_W - m, A4_H - m, p)
            BorderStyle.ROUND -> canvas.drawRoundRect(m, m, A4_W - m, A4_H - m, 14f, 14f, p)
            BorderStyle.DASHED -> {
                p.pathEffect = DashPathEffect(floatArrayOf(8f, 5f), 0f)
                canvas.drawRect(m, m, A4_W - m, A4_H - m, p)
            }
            BorderStyle.DOUBLE, BorderStyle.ORNATE -> {
                canvas.drawRect(m, m, A4_W - m, A4_H - m, p)
                p.strokeWidth = 1f
                canvas.drawRect(m + 5f, m + 5f, A4_W - m - 5f, A4_H - m - 5f, p)
            }
            BorderStyle.NONE -> {}
        }
    }

    fun renderCertificatePdfDocument(context: Context, cert: CertificateState): PdfDocument {
        val pdf = PdfDocument()
        val w = if (cert.isLandscape) A4_H else A4_W
        val h = if (cert.isLandscape) A4_W else A4_H
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(w, h, 1).create())
        val canvas = page.canvas

        canvas.drawColor(MediaUtils.parseColorInt(cert.bgColorHex, Color.WHITE))
        drawCertificateOrnamentPdf(canvas, cert.borderStyle, w.toFloat(), h.toFloat())

        val typeface = resolveTypeface(context, cert.fontKey)
        val txtColor = MediaUtils.parseColorInt(cert.textColorHex, Color.BLACK)
        val fs = cert.fontScale.coerceIn(0.6f, 1.8f)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = txtColor
            textSize = 14f * fs
        }
        val boldPaint = TextPaint(paint).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val contentW = (w - 130).coerceAtLeast(200)
        var curY = 66f

        fun drawCentered(txt: String, tp: TextPaint, gap: Float = 8f) {
            if (txt.isBlank()) return
            val lay = StaticLayout.Builder.obtain(txt, 0, txt.length, tp, contentW)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
            canvas.save(); canvas.translate((w - contentW) / 2f, curY); lay.draw(canvas); canvas.restore()
            curY += lay.height + gap
        }

        drawCentered(cert.topLine1, TextPaint(boldPaint).apply { textSize = 13f * fs }, 4f)
        drawCentered(cert.topLine2, TextPaint(paint).apply { textSize = 12f * fs; color = Color.parseColor("#475569") }, 14f)

        if (cert.badgeTitle.isNotBlank()) {
            val badgePaint = TextPaint(boldPaint).apply {
                textSize = 21f * fs
                color = Color.WHITE
            }
            val badgeMaxW = (contentW - 48).coerceAtLeast(120)
            val bLay = StaticLayout.Builder.obtain(cert.badgeTitle, 0, cert.badgeTitle.length, badgePaint, badgeMaxW)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
            var maxLineW = 0f
            for (i in 0 until bLay.lineCount) {
                maxLineW = maxOf(maxLineW, bLay.getLineWidth(i))
            }
            val pillW = (maxLineW + 44f).coerceIn(140f, contentW.toFloat())
            val pillH = bLay.height + 16f
            val pillL = (w - pillW) / 2f
            val pillRect = RectF(pillL, curY, pillL + pillW, curY + pillH)
            val pillFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    pillRect.left, pillRect.top, pillRect.right, pillRect.bottom,
                    txtColor, Color.parseColor("#0F766E"), Shader.TileMode.CLAMP
                )
            }
            val pillStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.8f
                color = Color.parseColor("#EAB308")
            }
            canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillFill)
            canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillStroke)
            canvas.save()
            canvas.translate((w - badgeMaxW) / 2f, curY + 8f)
            bLay.draw(canvas)
            canvas.restore()
            curY += pillH + 16f
        }

        drawCentered(cert.bodyTop, TextPaint(paint).apply { textSize = 14.5f * fs; color = Color.parseColor("#1E293B") }, 8f)
        drawCentered(cert.recipientName, TextPaint(boldPaint).apply { textSize = 19f * fs; color = Color.parseColor("#B45309") }, 8f)
        drawCentered(cert.bodyBottom, TextPaint(paint).apply { textSize = 14.5f * fs; color = Color.parseColor("#1E293B") }, 24f)

        val colW = contentW / 3
        val footerY = h - 105f
        listOf(cert.footerLeft, cert.footerCenter, cert.footerRight).forEachIndexed { idx, txt ->
            if (txt.isNotBlank()) {
                val lay = StaticLayout.Builder.obtain(
                    txt, 0, txt.length,
                    TextPaint(boldPaint).apply { textSize = 12f * fs; color = txtColor },
                    colW
                ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
                canvas.save(); canvas.translate(65f + idx * colW, footerY); lay.draw(canvas); canvas.restore()
            }
        }

        drawFloatingElements(canvas, cert.floatingElements, w.toFloat(), h.toFloat(), paint)
        pdf.finishPage(page)
        return pdf
    }

    private fun drawCertificateOrnamentPdf(canvas: Canvas, style: CertBorderStyle, w: Float, h: Float) {
        val (c1, c2, c3) = when (style) {
            CertBorderStyle.TURQ_GOLD -> Triple(Color.parseColor("#0D9488"), Color.parseColor("#EAB308"), Color.parseColor("#14B8A6"))
            CertBorderStyle.GOLD -> Triple(Color.parseColor("#B45309"), Color.parseColor("#F59E0B"), Color.parseColor("#D97706"))
            CertBorderStyle.ROYAL -> Triple(Color.parseColor("#1E3A8A"), Color.parseColor("#EAB308"), Color.parseColor("#3B82F6"))
            CertBorderStyle.EMERALD -> Triple(Color.parseColor("#047857"), Color.parseColor("#EAB308"), Color.parseColor("#10B981"))
            CertBorderStyle.PURPLE -> Triple(Color.parseColor("#6D28D9"), Color.parseColor("#EAB308"), Color.parseColor("#8B5CF6"))
            CertBorderStyle.CLASSIC_GEO -> Triple(Color.parseColor("#1E3A8A"), Color.parseColor("#B45309"), Color.parseColor("#0284C7"))
            CertBorderStyle.DOUBLE_ORNATE -> Triple(Color.parseColor("#0F172A"), Color.parseColor("#0D9488"), Color.parseColor("#475569"))
        }
        val p1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.STROKE; strokeWidth = 4f; color = c1 }
        val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.STROKE; strokeWidth = 1.6f; color = c2 }
        canvas.drawRoundRect(16f, 16f, w - 16f, h - 16f, 10f, 10f, p1)
        canvas.drawRoundRect(24f, 24f, w - 24f, h - 24f, 6f, 6f, p2)

        val floral = style == CertBorderStyle.TURQ_GOLD ||
            style == CertBorderStyle.EMERALD ||
            style == CertBorderStyle.PURPLE
        val isGeo = style == CertBorderStyle.CLASSIC_GEO

        fun withAlpha(rgb: Int, alphaFrac: Float): Int =
            Color.argb((alphaFrac * 255).toInt().coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))

        val corners = listOf(
            Triple(0f, 0f, 0f),
            Triple(w, 0f, 90f),
            Triple(w, h, 180f),
            Triple(0f, h, 270f)
        )
        val u = 1.35f
        corners.forEach { (ox, oy, deg) ->
            canvas.save()
            canvas.rotate(deg, ox, oy)
            if (floral) {
                val petal1 = Path().apply {
                    moveTo(ox + 14 * u, oy + 14 * u)
                    cubicTo(ox + 34 * u, oy + 14 * u, ox + 46 * u, oy + 22 * u, ox + 52 * u, oy + 36 * u)
                    cubicTo(ox + 38 * u, oy + 34 * u, ox + 26 * u, oy + 26 * u, ox + 14 * u, oy + 14 * u)
                    close()
                }
                val petal2 = Path().apply {
                    moveTo(ox + 14 * u, oy + 14 * u)
                    cubicTo(ox + 14 * u, oy + 34 * u, ox + 22 * u, oy + 46 * u, ox + 36 * u, oy + 52 * u)
                    cubicTo(ox + 34 * u, oy + 38 * u, ox + 26 * u, oy + 26 * u, ox + 14 * u, oy + 14 * u)
                    close()
                }
                canvas.drawPath(petal1, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(c3, 0.35f) })
                canvas.drawPath(petal2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(c1, 0.30f) })
                canvas.drawCircle(ox + 21 * u, oy + 21 * u, 7.5f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(c1, 0.85f) })
                canvas.drawCircle(ox + 32 * u, oy + 17 * u, 5f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(c3, 0.80f) })
                canvas.drawCircle(ox + 17 * u, oy + 32 * u, 5f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(c3, 0.80f) })
                canvas.drawCircle(ox + 21 * u, oy + 21 * u, 3f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c2 })
                val arc = Path().apply {
                    moveTo(ox + 12 * u, oy + 56 * u)
                    cubicTo(ox + 12 * u, oy + 24 * u, ox + 24 * u, oy + 12 * u, ox + 56 * u, oy + 12 * u)
                }
                canvas.drawPath(arc, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE; strokeWidth = 1.8f * u; color = c2
                })
            } else if (isGeo) {
                canvas.drawRect(
                    ox + 12 * u, oy + 12 * u, ox + 40 * u, oy + 40 * u,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.STROKE; strokeWidth = 1.8f * u; color = c2 }
                )
                canvas.drawLine(
                    ox + 12 * u, oy + 52 * u, ox + 52 * u, oy + 12 * u,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.4f * u; color = c2 }
                )
                canvas.drawCircle(ox + 26 * u, oy + 26 * u, 10 * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(c1, 0.25f) })
            } else {
                val cornerPath = Path().apply {
                    moveTo(ox + 12 * u, oy + 46 * u)
                    lineTo(ox + 12 * u, oy + 12 * u)
                    lineTo(ox + 46 * u, oy + 12 * u)
                }
                canvas.drawPath(cornerPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE; strokeWidth = 2.4f * u; color = c1
                })
                canvas.drawCircle(ox + 19 * u, oy + 19 * u, 4f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c2 })
            }
            canvas.restore()
        }
    }

    private fun drawFloatingElements(
        canvas: Canvas,
        elements: List<FloatingElement>,
        w: Float,
        h: Float,
        textPaint: TextPaint
    ) {
        elements.forEach { fl ->
            val x = fl.xFrac * w
            val y = fl.yFrac * h
            val fw = fl.wFrac * w
            val fh = fl.hFrac * h
            val strokeInt = MediaUtils.parseColorInt(fl.strokeColorHex, Color.BLACK)
            val bgInt = MediaUtils.parseColorInt(fl.bgColorHex, Color.TRANSPARENT)

            canvas.save()
            canvas.rotate(fl.rotationDeg, x + fw / 2f, y + fh / 2f)
            when (fl.type) {
                FloatingElementType.IMAGE -> {
                    MediaUtils.decodeBase64Bitmap(fl.imageBase64)?.let { bmp ->
                        canvas.drawBitmap(bmp, null, RectF(x, y, x + fw, y + fh), null)
                    }
                }
                FloatingElementType.MARK_CIRCLE -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.8f; color = strokeInt }
                    canvas.drawOval(x, y, x + fw, y + fh, p)
                    canvas.drawLine(x, y + fh / 2f, x + fw, y + fh / 2f, p)
                    val tp = TextPaint(textPaint).apply {
                        color = Color.BLACK; textAlign = Paint.Align.CENTER; textSize = (fh * 0.32f).coerceIn(8f, 18f)
                    }
                    canvas.drawText(fl.text, x + fw / 2f, y + fh * 0.36f, tp)
                    canvas.drawText(fl.secondaryText, x + fw / 2f, y + fh * 0.82f, tp)
                }
                FloatingElementType.MED_CIRCLE -> {
                    val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = bgInt }
                    val stP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f; color = strokeInt }
                    canvas.drawOval(x, y, x + fw, y + fh, bgP)
                    canvas.drawOval(x, y, x + fw, y + fh, stP)
                    val lay = StaticLayout.Builder.obtain(fl.text, 0, fl.text.length, textPaint, fw.toInt().coerceAtLeast(20))
                        .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
                    canvas.save(); canvas.translate(x, y + (fh - lay.height) / 2f); lay.draw(canvas); canvas.restore()
                }
                FloatingElementType.TEXT_BOX -> {
                    if (fl.bgColorHex != "#FFFFFF" && fl.bgColorHex.isNotBlank()) {
                        canvas.drawRect(x, y, x + fw, y + fh, Paint().apply { color = bgInt })
                    }
                    if (fl.showBorder) {
                        val stP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.4f; color = strokeInt }
                        canvas.drawRect(x, y, x + fw, y + fh, stP)
                    }
                    val lay = StaticLayout.Builder.obtain(fl.text, 0, fl.text.length, textPaint, fw.toInt().coerceAtLeast(20)).build()
                    canvas.save(); canvas.translate(x + 4f, y + 4f); lay.draw(canvas); canvas.restore()
                }
                FloatingElementType.TABLE -> {
                    val rows = fl.tableRows
                    if (rows.isNotEmpty() && rows[0].isNotEmpty()) {
                        val rH = fh / rows.size
                        val cW = fw / rows[0].size
                        val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = strokeInt }
                        rows.forEachIndexed { rIdx, row ->
                            row.forEachIndexed { cIdx, cell ->
                                val cx = x + cIdx * cW
                                val cy = y + rIdx * rH
                                canvas.drawRect(cx, cy, cx + cW, cy + rH, borderP)
                                val lay = StaticLayout.Builder.obtain(cell, 0, cell.length, textPaint, cW.toInt().coerceAtLeast(16))
                                    .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
                                canvas.save(); canvas.translate(cx, cy + (rH - lay.height) / 2f); lay.draw(canvas); canvas.restore()
                            }
                        }
                    }
                }
                FloatingElementType.SHAPE -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.8f; color = strokeInt }
                    when (fl.shapeKind) {
                        ShapeKind.CIRCLE -> canvas.drawOval(x + 2f, y + 2f, x + fw - 2f, y + fh - 2f, p)
                        ShapeKind.SQUARE, ShapeKind.RECT -> canvas.drawRect(x + 2f, y + 2f, x + fw - 2f, y + fh - 2f, p)
                        ShapeKind.TRIANGLE -> {
                            val path = Path().apply {
                                moveTo(x + fw / 2f, y + 2f); lineTo(x + fw - 2f, y + fh - 2f); lineTo(x + 2f, y + fh - 2f); close()
                            }
                            canvas.drawPath(path, p)
                        }
                        ShapeKind.RHOMBUS -> {
                            val path = Path().apply {
                                moveTo(x + fw / 2f, y + 2f); lineTo(x + fw - 2f, y + fh / 2f); lineTo(x + fw / 2f, y + fh - 2f); lineTo(x + 2f, y + fh / 2f); close()
                            }
                            canvas.drawPath(path, p)
                        }
                        ShapeKind.LINE -> canvas.drawLine(x, y + fh / 2f, x + fw, y + fh / 2f, p)
                        ShapeKind.VLINE -> canvas.drawLine(x + fw / 2f, y, x + fw / 2f, y + fh, p)
                        ShapeKind.DLINE -> canvas.drawLine(x + 4f, y + fh - 4f, x + fw - 4f, y + 4f, p)
                        ShapeKind.ARROW -> drawArrowOnCanvas(canvas, x + 4f, y + fh / 2f, x + fw - 4f, y + fh / 2f, p)
                        ShapeKind.LARROW -> drawArrowOnCanvas(canvas, x + fw - 4f, y + fh / 2f, x + 4f, y + fh / 2f, p)
                        ShapeKind.SLINE -> {
                            drawArrowOnCanvas(canvas, x + 4f, y + fh / 2f, x + fw - 4f, y + fh / 2f, p)
                            drawArrowOnCanvas(canvas, x + fw - 4f, y + fh / 2f, x + 4f, y + fh / 2f, p)
                        }
                        ShapeKind.VARROW -> {
                            drawArrowOnCanvas(canvas, x + fw / 2f, y + 4f, x + fw / 2f, y + fh - 4f, p)
                            drawArrowOnCanvas(canvas, x + fw / 2f, y + fh - 4f, x + fw / 2f, y + 4f, p)
                        }
                        ShapeKind.RAY -> {
                            canvas.drawCircle(x + 6f, y + fh / 2f, 4f, Paint(p).apply { style = Paint.Style.FILL })
                            drawArrowOnCanvas(canvas, x + 6f, y + fh / 2f, x + fw - 4f, y + fh / 2f, p)
                        }
                    }
                    if (fl.text.isNotBlank() && fl.text != "...") {
                        val lay = StaticLayout.Builder.obtain(fl.text, 0, fl.text.length, textPaint, fw.toInt().coerceAtLeast(20))
                            .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
                        canvas.save(); canvas.translate(x, y + (fh - lay.height) / 2f); lay.draw(canvas); canvas.restore()
                    }
                }
                FloatingElementType.NUMBER_LINE -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f; color = strokeInt }
                    val midY = y + fh * 0.42f
                    drawArrowOnCanvas(canvas, x + 6f, midY, x + fw - 6f, midY, p)
                    drawArrowOnCanvas(canvas, x + fw - 6f, midY, x + 6f, midY, p)
                    for (i in 1..7) {
                        val tx = x + fw * (i / 8f)
                        canvas.drawLine(tx, midY - 4f, tx, midY + 4f, p)
                    }
                    val tp = TextPaint(textPaint).apply { textAlign = Paint.Align.CENTER; textSize = 10f }
                    canvas.drawText(fl.text.ifBlank { "-3   -2   -1   0   1   2   3" }, x + fw / 2f, y + fh * 0.85f, tp)
                }
                FloatingElementType.COORD_GRID -> {
                    val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.6f; color = Color.LTGRAY }
                    val axisP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f; color = strokeInt }
                    for (i in 1..7) {
                        val gx = x + fw * (i / 8f)
                        val gy = y + fh * (i / 8f)
                        canvas.drawLine(gx, y + 6f, gx, y + fh - 6f, gridP)
                        canvas.drawLine(x + 6f, gy, x + fw - 6f, gy, gridP)
                    }
                    drawArrowOnCanvas(canvas, x + fw / 2f, y + fh - 4f, x + fw / 2f, y + 4f, axisP)
                    drawArrowOnCanvas(canvas, x + 4f, y + fh / 2f, x + fw - 4f, y + fh / 2f, axisP)
                }
            }
            canvas.restore()
        }
    }

    private fun drawPenStrokes(canvas: Canvas, strokes: List<PenStroke>, w: Float, h: Float) {
        strokes.forEach { st ->
            if (st.points.size < 2) return@forEach
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = MediaUtils.parseColorInt(st.colorHex, Color.BLACK)
                strokeWidth = st.widthDp
                if (st.dashed) pathEffect = DashPathEffect(floatArrayOf(7f, 5f), 0f)
            }
            val path = Path()
            path.moveTo(st.points.first().x * w, st.points.first().y * h)
            for (i in 1 until st.points.size) {
                path.lineTo(st.points[i].x * w, st.points[i].y * h)
            }
            canvas.drawPath(path, p)
            if (st.isArrow) {
                val p1 = st.points[st.points.size - 2]
                val p2 = st.points.last()
                drawArrowOnCanvas(canvas, p1.x * w, p1.y * h, p2.x * w, p2.y * h, p)
            }
        }
    }

    private fun drawArrowOnCanvas(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, paint: Paint) {
        canvas.drawLine(x1, y1, x2, y2, paint)
        val ang = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        val sz = 8.0
        val head = Paint(paint).apply { pathEffect = null }
        canvas.drawLine(x2, y2, (x2 - sz * cos(ang - 0.5)).toFloat(), (y2 - sz * sin(ang - 0.5)).toFloat(), head)
        canvas.drawLine(x2, y2, (x2 - sz * cos(ang + 0.5)).toFloat(), (y2 - sz * sin(ang + 0.5)).toFloat(), head)
    }
}
