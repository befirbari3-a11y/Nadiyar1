package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.export.WordDocExporter
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.MediaUtils
import kotlin.math.*

@Composable
fun A4PageBlock(
    pageIndex: Int,
    totalPages: Int,
    isActivePage: Boolean,
    doc: ExamDocumentState,
    penEnabled: Boolean,
    penMode: PenMode,
    penColorHex: String,
    penWidthDp: Float,
    penDashed: Boolean,
    selectedFloatingId: String?,
    paperWidthDp: Float,
    onSelectPage: () -> Unit,
    onToggleHeader: () -> Unit,
    onDeletePage: () -> Unit,
    onMoveQuestion: (String, Int) -> Unit,
    onMoveQuestionPage: (String, Int) -> Unit,
    onSplitQuestion: (String, Int) -> Unit,
    onEditQuestionRequest: (Question) -> Unit,
    onDeleteQuestion: (String) -> Unit,
    onAdjustQuestionLines: (String, Int) -> Unit,
    onToggleQuestionSeparator: (String) -> Unit = {},
    onPickQuestionBg: (String) -> Unit = {},
    onUpdateFreeWrite: (String) -> Unit,
    onAddPenStroke: (PenStroke) -> Unit,
    onSelectFloating: (String?) -> Unit,
    onUpdateFloating: (String, (FloatingElement) -> FloatingElement) -> Unit,
    onDeleteFloating: (String) -> Unit
) {
    val page = doc.pages[pageIndex]
    val st = doc.style
    val fontFamily = ExamFontOption.fromKey(st.fontKey).fontFamily
    val scale = (paperWidthDp / 595f).coerceIn(0.45f, 2.2f)

    val pageQs = remember(doc.questions, pageIndex, doc.pages.size) {
        doc.questions.withIndex().filter { it.value.pageIndex.coerceIn(0, doc.pages.lastIndex) == pageIndex }
    }
    val nonHdrCount = remember(pageQs) { pageQs.count { !it.value.type.isNoHeader } }
    val fillFraction = remember(pageQs, page.showHeader, page.freeWriteText, st.fontSizeSp) {
        estimatePageFill(page, pageQs.map { it.value }, st.fontSizeSp)
    }
    val fillPct = (fillFraction * 100).roundToInt()

    var editingFreeWrite by remember { mutableStateOf(false) }
    var editingFloating by remember { mutableStateOf<FloatingElement?>(null) }
    var colorPickerTargetId by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Page Fill Meter Bar
        Surface(
            color = CardSurface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (isActivePage) RoyalBlue else SubtleBorder),
            modifier = Modifier
                .width(paperWidthDp.dp)
                .padding(bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusColor = when {
                    fillPct > 96 -> CrimsonDanger
                    fillPct > 80 -> AmberWarm
                    else -> EmeraldSuccess
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (fillPct > 98) {
                            "⚠️ پەڕێ ${pageIndex + 1} پڕ بوو ($fillPct%) — پرسیار: $nonHdrCount | پرسیارەکێ بڤەگوهێزە پەڕێ ${pageIndex + 2}"
                        } else {
                            "پەڕێ ${pageIndex + 1} | پرسیار: $nonHdrCount | پڕبوون: $fillPct% — جھێ مای: ${(100 - fillPct).coerceAtLeast(0)}%"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { fillFraction.coerceIn(0f, 1f) },
                        color = statusColor,
                        trackColor = Color(0xFFE2E8F0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(99.dp))
                    )
                }
                FilledTonalButton(
                    onClick = onToggleHeader,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 34.dp)
                ) {
                    Text("± کلێشە", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (pageIndex > 0) {
                    IconButton(
                        onClick = onDeletePage,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete page", tint = CrimsonDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Actual A4 Sheet (210 : 297 aspect ratio)
        val paperHeightDp = paperWidthDp * (297f / 210f)
        val paperBg = MediaUtils.parseColor(st.paperBgColorHex, Color.White)
        val borderCol = MediaUtils.parseColor(st.borderColorHex, SlateNavy)
        val hdrCol = MediaUtils.parseColor(st.headerColorHex, Color.Black)
        val qCol = MediaUtils.parseColor(st.questionTitleColorHex, Color(0xFF1E3A8A))
        val bodyCol = MediaUtils.parseColor(st.bodyTextColorHex, Color.Black)

        var paperPxSize by remember { mutableStateOf(IntSize(1, 1)) }
        var livePenPoints by remember { mutableStateOf<List<NormalizedPoint>>(emptyList()) }

        Box(
            modifier = Modifier
                .width(paperWidthDp.dp)
                .height(paperHeightDp.dp)
                .background(paperBg)
                .clipToBounds()
                .onSizeChanged { paperPxSize = it }
                .clickable(
                    enabled = !penEnabled,
                    onClick = {
                        onSelectPage()
                        onSelectFloating(null)
                    }
                )
        ) {
            // Border Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawPaperBorder(st.borderStyle, borderCol, isActivePage && totalPages > 1)
            }

            // Main Paper Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = (24f * scale).dp, vertical = (20f * scale).dp)
            ) {
                if (page.showHeader) {
                    A4HeaderBlock(
                        doc = doc,
                        hdrColor = hdrCol,
                        fontFamily = fontFamily,
                        scale = scale
                    )
                    Spacer(Modifier.height((8f * scale).dp))
                }

                pageQs.forEach { (qi, q) ->
                    val noHdr = q.type.isNoHeader
                    var dispNum = 0
                    for (idx in 0..qi) if (!doc.questions[idx].type.isNoHeader) dispNum++
                    val d = LocalizationPresets.strings(doc.language)
                    val qNumStr = LocalizationPresets.formatNumber(dispNum, doc.numeralMode, doc.language)

                    val qBg = if (q.bgColorHex.isNotBlank()) MediaUtils.parseColor(q.bgColorHex, Color.Transparent) else Color.Transparent
                    val fs = (st.fontSizeSp * scale * 0.92f).coerceIn(7f, 24f).sp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = (6f * scale).dp)
                            .then(
                                if (qBg != Color.Transparent) {
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(qBg)
                                        .padding((5f * scale).dp)
                                } else Modifier
                            )
                            .clickable(enabled = !penEnabled) { onEditQuestionRequest(q) }
                    ) {
                        // Compact Question Quick Bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 2.dp)
                        ) {
                            MiniActionChip("✎ دەستکاری") { onEditQuestionRequest(q) }
                            MiniActionChip("▲") { onMoveQuestion(q.id, -1) }
                            MiniActionChip("▼") { onMoveQuestion(q.id, 1) }
                            MiniActionChip("➔ پەڕێ ${pageIndex + 2}") { onMoveQuestionPage(q.id, pageIndex + 1) }
                            val itemCount = if (q.type == QuestionType.CONCEPT) q.conceptBranches.size else q.items.size
                            if (itemCount > 1) {
                                MiniActionChip("✂️ بەشەک") { onSplitQuestion(q.id, pageIndex + 1) }
                            }
                            if (pageIndex > 0) {
                                MiniActionChip("⬅ پەڕێ $pageIndex") { onMoveQuestionPage(q.id, pageIndex - 1) }
                            }
                            MiniActionChip("+ هێڵ") { onAdjustQuestionLines(q.id, 1) }
                            MiniActionChip("- هێڵ") { onAdjustQuestionLines(q.id, -1) }
                            MiniActionChip("―") { onToggleQuestionSeparator(q.id) }
                            MiniActionChip("🎨") { onPickQuestionBg(q.id) }
                            MiniActionChip("✕", isDanger = true) { onDeleteQuestion(q.id) }
                        }

                        if (!noHdr) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                RichMathText(
                                    text = "${d.qPrefix}$qNumStr/ ${q.title}",
                                    color = qCol,
                                    fontSize = fs,
                                    fontFamily = fontFamily,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "(${q.marks} ${d.markWord})",
                                    color = qCol,
                                    fontSize = fs,
                                    fontFamily = fontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height((3f * scale).dp))
                        }

                        // Question Body
                        when (q.type) {
                            QuestionType.MATCH -> {
                                q.items.forEachIndexed { ii, it ->
                                    val num = LocalizationPresets.formatNumber(ii + 1, doc.numeralMode, doc.language)
                                    val rLbl = d.matchRightLabels.getOrElse(ii) { "${ii + 1}-" }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(0.6.dp, Color(0xFFCBD5E1))
                                            .padding((3f * scale).dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        RichMathText(
                                            text = "$num- ${it.text}",
                                            color = bodyCol,
                                            fontSize = fs,
                                            fontFamily = fontFamily,
                                            modifier = Modifier.weight(1f)
                                        )
                                        RichMathText(
                                            text = "$rLbl ${it.matchRight}",
                                            color = bodyCol,
                                            fontSize = fs,
                                            fontFamily = fontFamily,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            QuestionType.CONCEPT -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .border(1.2.dp, SlateDark, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        RichMathText(q.conceptCenter, bodyCol, fs, fontFamily, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height((5f * scale).dp))
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        q.conceptBranches.forEach { br ->
                                            Box(
                                                modifier = Modifier
                                                    .border(1.dp, SlateDark, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                RichMathText(br, bodyCol, fs, fontFamily, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            QuestionType.COUNT -> {
                                q.items.chunked(4).forEachIndexed { rIdx, row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        row.forEachIndexed { cIdx, it ->
                                            val num = LocalizationPresets.formatNumber(rIdx * 4 + cIdx + 1, doc.numeralMode, doc.language)
                                            RichMathText(
                                                text = "$num- ${it.text}",
                                                color = bodyCol,
                                                fontSize = fs,
                                                fontFamily = fontFamily,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                            else -> {
                                if (q.type == QuestionType.IMG && q.imageBase64.isNotBlank()) {
                                    val bmp = remember(q.imageBase64) { MediaUtils.decodeBase64ImageBitmap(q.imageBase64) }
                                    bmp?.let {
                                        Image(
                                            bitmap = it,
                                            contentDescription = "Question illustration",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .width((q.imageWidthDp * scale * 0.7f).dp)
                                                .padding(vertical = 3.dp)
                                        )
                                    }
                                }
                                q.items.forEachIndexed { ii, it ->
                                    val num = LocalizationPresets.formatNumber(ii + 1, doc.numeralMode, doc.language)
                                    RichMathText(
                                        text = "${if (noHdr) "" else "$num- "}${it.text}",
                                        color = bodyCol,
                                        fontSize = fs,
                                        fontFamily = fontFamily
                                    )
                                    if (q.type == QuestionType.MCQ && it.options.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = (10f * scale).dp, top = 2.dp, bottom = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            it.options.forEachIndexed { oi, op ->
                                                RichMathText(
                                                    text = "${d.mcqLabels.getOrElse(oi) { "" }} ${op.text}",
                                                    color = bodyCol,
                                                    fontSize = fs,
                                                    fontFamily = fontFamily,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                    if (q.type == QuestionType.CALLIG) {
                                        repeat(2) {
                                            Spacer(Modifier.height((q.lineSize.heightDp * scale * 0.65f).dp))
                                            HorizontalDivider(color = Color(0xFF64748B), thickness = 0.8.dp)
                                        }
                                    }
                                }
                                if (q.spacerHeightDp > 0) {
                                    Spacer(Modifier.height((q.spacerHeightDp * scale * 0.7f).dp))
                                }
                            }
                        }

                        repeat(q.answerLines) {
                            Spacer(Modifier.height((q.lineSize.heightDp * scale * 0.65f).dp))
                            HorizontalDivider(color = Color(0xFF64748B), thickness = 0.8.dp)
                        }

                        if (!noHdr && q.type != QuestionType.FREE && !q.noSeparatorLine) {
                            Spacer(Modifier.height((4f * scale).dp))
                            HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 0.8.dp)
                        }
                    }
                }

                // Free-write zone at bottom of page
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(enabled = !penEnabled) { editingFreeWrite = true }
                        .padding(top = 4.dp)
                ) {
                    if (page.freeWriteText.isBlank()) {
                        Text(
                            text = "ل ڤێرە ڕاستەوخۆ ب دەستێ خۆ بنڤێسە (کلیک بکە)...",
                            color = Color(0xFF94A3B8),
                            fontSize = (11f * scale).coerceAtLeast(8f).sp,
                            fontFamily = fontFamily
                        )
                    } else {
                        RichMathText(
                            text = page.freeWriteText,
                            color = bodyCol,
                            fontSize = (st.fontSizeSp * scale * 0.92f).coerceIn(7f, 24f).sp,
                            fontFamily = fontFamily
                        )
                    }
                }

                // Footer
                if (page.showHeader && doc.header.visible[HeaderFieldKey.FT] != false) {
                    Text(
                        text = doc.header.values[HeaderFieldKey.FT].orEmpty(),
                        color = hdrCol,
                        fontSize = (12f * scale).coerceAtLeast(8f).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Floating Elements Layer
            page.floatingElements.forEach { fl ->
                FloatingElementView(
                    element = fl,
                    paperPxSize = paperPxSize,
                    isSelected = selectedFloatingId == fl.id && !penEnabled,
                    fontFamily = fontFamily,
                    scale = scale,
                    onSelect = { if (!penEnabled) onSelectFloating(fl.id) },
                    onMoveBy = { dxPx, dyPx ->
                        if (!fl.locked && paperPxSize.width > 1 && paperPxSize.height > 1) {
                            val nx = (fl.xFrac + dxPx / paperPxSize.width).coerceIn(0f, 0.92f)
                            val ny = (fl.yFrac + dyPx / paperPxSize.height).coerceIn(0f, 0.94f)
                            onUpdateFloating(fl.id) { it.copy(xFrac = nx, yFrac = ny) }
                        }
                    },
                    onTogglePin = { onUpdateFloating(fl.id) { it.copy(locked = !it.locked) } },
                    onResize = { factor ->
                        if (!fl.locked) {
                            onUpdateFloating(fl.id) {
                                it.copy(
                                    wFrac = (it.wFrac * factor).coerceIn(0.06f, 0.92f),
                                    hFrac = (it.hFrac * factor).coerceIn(0.03f, 0.7f),
                                    fontSizeSp = (it.fontSizeSp * (if (factor > 1f) 1.08f else 0.92f)).coerceIn(8f, 28f)
                                )
                            }
                        }
                    },
                    onRotate = { deg ->
                        if (!fl.locked) {
                            onUpdateFloating(fl.id) { it.copy(rotationDeg = (it.rotationDeg + deg + 360f) % 360f) }
                        }
                    },
                    onEdit = { editingFloating = fl },
                    onPickColor = { colorPickerTargetId = fl.id },
                    onDelete = { onDeleteFloating(fl.id) }
                )
            }

            // Smart Pen Vector Overlay
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (penEnabled) {
                            Modifier.pointerInput(penMode, penColorHex, penWidthDp, penDashed, paperPxSize) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val w = paperPxSize.width.coerceAtLeast(1).toFloat()
                                        val h = paperPxSize.height.coerceAtLeast(1).toFloat()
                                        livePenPoints = listOf(NormalizedPoint(offset.x / w, offset.y / h))
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val w = paperPxSize.width.coerceAtLeast(1).toFloat()
                                        val h = paperPxSize.height.coerceAtLeast(1).toFloat()
                                        val pt = NormalizedPoint((change.position.x / w).coerceIn(0f, 1f), (change.position.y / h).coerceIn(0f, 1f))
                                        livePenPoints = if (penMode == PenMode.LINE || penMode == PenMode.ARROW) {
                                            val start = livePenPoints.firstOrNull() ?: pt
                                            listOf(start, snapNormalizedPoint(start, pt))
                                        } else {
                                            livePenPoints + pt
                                        }
                                    },
                                    onDragEnd = {
                                        if (livePenPoints.size >= 2) {
                                            val processed = processSmartStroke(livePenPoints, penMode)
                                            onAddPenStroke(
                                                PenStroke(
                                                    points = processed,
                                                    colorHex = penColorHex,
                                                    widthDp = penWidthDp,
                                                    dashed = penDashed,
                                                    isArrow = penMode == PenMode.ARROW
                                                )
                                            )
                                        }
                                        livePenPoints = emptyList()
                                    },
                                    onDragCancel = { livePenPoints = emptyList() }
                                )
                            }
                        } else Modifier
                    )
            ) {
                page.penStrokes.forEach { st ->
                    drawPenStrokeCompose(st, size.width, size.height)
                }
                if (livePenPoints.size >= 2) {
                    drawPenStrokeCompose(
                        PenStroke(
                            points = livePenPoints,
                            colorHex = penColorHex,
                            widthDp = penWidthDp,
                            dashed = penDashed,
                            isArrow = penMode == PenMode.ARROW
                        ),
                        size.width,
                        size.height
                    )
                }
            }
        }
    }

    if (editingFreeWrite) {
        var draft by remember(page.freeWriteText) { mutableStateOf(page.freeWriteText) }
        AlertDialog(
            onDismissRequest = { editingFreeWrite = false },
            title = { Text("✍️ نڤێسینا ڕاستەوخۆ ل سەر پەڕێ A4", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    minLines = 5,
                    maxLines = 10,
                    label = { Text("نڤێسینا خۆ بنڤێسە") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateFreeWrite(draft)
                    editingFreeWrite = false
                }) { Text("تەمام") }
            },
            dismissButton = {
                TextButton(onClick = { editingFreeWrite = false }) { Text("لابردن") }
            }
        )
    }

    editingFloating?.let { fl ->
        FloatingElementEditDialog(
            element = fl,
            onDismiss = { editingFloating = null },
            onSave = { updated ->
                onUpdateFloating(fl.id) { updated }
                editingFloating = null
            }
        )
    }

    colorPickerTargetId?.let { targetId ->
        val cur = page.floatingElements.firstOrNull { it.id == targetId }?.strokeColorHex ?: "#000000"
        ColorPickerDialog(
            title = "ڕەنگێ توخمێ",
            currentHex = cur,
            onDismiss = { colorPickerTargetId = null },
            onColorSelected = { hex ->
                onUpdateFloating(targetId) { it.copy(strokeColorHex = hex) }
            }
        )
    }
}

@Composable
private fun A4HeaderBlock(
    doc: ExamDocumentState,
    hdrColor: Color,
    fontFamily: FontFamily,
    scale: Float
) {
    val hdr = doc.header
    val d = LocalizationPresets.strings(doc.language)
    val tot = WordDocExporter.calculateTotalMarks(doc)
    val hdrScale = (doc.style.headerScalePct / 100f).coerceIn(0.65f, 1.25f)
    val fs = (10.5f * scale * hdrScale).coerceIn(6.5f, 16f).sp
    val logoBmp = remember(hdr.logoBase64) { MediaUtils.decodeBase64ImageBitmap(hdr.logoBase64) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehindLineBottom(hdrColor, 1.4.dp)
            .padding(bottom = (6f * scale).dp)
    ) {
        if (hdr.visible[HeaderFieldKey.BIS] != false) {
            Text(
                text = hdr.values[HeaderFieldKey.BIS].orEmpty(),
                color = hdrColor,
                fontSize = (fs.value * 1.06f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height((3f * scale).dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((6f * scale).dp)
        ) {
            // Start column
            Column(modifier = Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (hdr.logoPos == LogoPosition.RIGHT && logoBmp != null) {
                    Image(
                        bitmap = logoBmp,
                        contentDescription = "School logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height((hdr.logoSizeDp * scale * 0.65f).dp)
                    )
                }
                if (hdr.visible[HeaderFieldKey.MIN] != false) {
                    Text(hdr.values[HeaderFieldKey.MIN].orEmpty(), color = hdrColor, fontSize = fs, fontFamily = fontFamily, lineHeight = (fs.value * 1.2f).sp)
                }
                if (hdr.visible[HeaderFieldKey.SCH] != false) {
                    Text(hdr.values[HeaderFieldKey.SCH].orEmpty(), color = hdrColor, fontSize = fs, fontFamily = fontFamily, lineHeight = (fs.value * 1.2f).sp)
                }
                val sg = listOfNotNull(
                    hdr.values[HeaderFieldKey.SUB]?.takeIf { hdr.visible[HeaderFieldKey.SUB] != false },
                    hdr.values[HeaderFieldKey.GRD]?.takeIf { hdr.visible[HeaderFieldKey.GRD] != false }
                ).joinToString("  ")
                if (sg.isNotBlank()) {
                    Text(sg, color = hdrColor, fontSize = fs, fontFamily = fontFamily, lineHeight = (fs.value * 1.2f).sp)
                }
                if (hdr.visible[HeaderFieldKey.TCH] != false) {
                    Text(hdr.values[HeaderFieldKey.TCH].orEmpty(), color = hdrColor, fontSize = fs, fontFamily = fontFamily, lineHeight = (fs.value * 1.2f).sp)
                }
            }

            // Center column
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (hdr.logoPos == LogoPosition.CENTER && logoBmp != null) {
                    Image(
                        bitmap = logoBmp,
                        contentDescription = "School logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height((hdr.logoSizeDp * scale * 0.65f).dp)
                    )
                }
                if (hdr.visible[HeaderFieldKey.TYP] != false || hdr.visible[HeaderFieldKey.YR] != false) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .border(1.2.dp, hdrColor, RoundedCornerShape(10.dp))
                            .padding(horizontal = (6f * scale).dp, vertical = (3f * scale).dp)
                    ) {
                        if (hdr.visible[HeaderFieldKey.TYP] != false) {
                            Text(
                                hdr.values[HeaderFieldKey.TYP].orEmpty(),
                                color = hdrColor, fontSize = fs, fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily, textAlign = TextAlign.Center
                            )
                        }
                        if (hdr.visible[HeaderFieldKey.YR] != false) {
                            Text(
                                hdr.values[HeaderFieldKey.YR].orEmpty(),
                                color = hdrColor, fontSize = (fs.value * 0.85f).sp,
                                fontFamily = fontFamily, textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // End column
            Column(modifier = Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (hdr.logoPos == LogoPosition.LEFT && logoBmp != null) {
                    Image(
                        bitmap = logoBmp,
                        contentDescription = "School logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.End)
                            .height((hdr.logoSizeDp * scale * 0.65f).dp)
                    )
                }
                if (hdr.visible[HeaderFieldKey.STU] != false) {
                    Text(
                        "${hdr.values[HeaderFieldKey.STU].orEmpty()} ...............",
                        color = hdrColor, fontSize = fs, fontWeight = FontWeight.Bold, fontFamily = fontFamily
                    )
                }
                if (hdr.visible[HeaderFieldKey.DAT] != false || hdr.visible[HeaderFieldKey.TIM] != false) {
                    val dt = listOfNotNull(
                        hdr.values[HeaderFieldKey.DAT]?.takeIf { hdr.visible[HeaderFieldKey.DAT] != false },
                        hdr.values[HeaderFieldKey.TIM]?.takeIf { hdr.visible[HeaderFieldKey.TIM] != false }
                    ).joinToString(" | ")
                    Text(
                        dt,
                        color = hdrColor,
                        fontSize = (fs.value * 0.9f).sp,
                        fontFamily = fontFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, Color(0xFF94A3B8))
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                    )
                }
                if (hdr.visible[HeaderFieldKey.SCR] != false) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(d.scoreLabel, color = hdrColor, fontSize = fs, fontFamily = fontFamily)
                        Box(
                            modifier = Modifier
                                .border(1.2.dp, hdrColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(tot, color = hdrColor, fontSize = fs, fontWeight = FontWeight.Bold, fontFamily = fontFamily)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingElementView(
    element: FloatingElement,
    paperPxSize: IntSize,
    isSelected: Boolean,
    fontFamily: FontFamily,
    scale: Float,
    onSelect: () -> Unit,
    onMoveBy: (Float, Float) -> Unit,
    onTogglePin: () -> Unit,
    onResize: (Float) -> Unit,
    onRotate: (Float) -> Unit,
    onEdit: () -> Unit,
    onPickColor: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val wPx = (element.wFrac * paperPxSize.width).coerceAtLeast(24f)
    val hPx = (element.hFrac * paperPxSize.height).coerceAtLeast(20f)
    val xPx = (element.xFrac * paperPxSize.width).roundToInt()
    val yPx = (element.yFrac * paperPxSize.height).roundToInt()

    val wDp = with(density) { wPx.toDp() }
    val hDp = with(density) { hPx.toDp() }

    val strokeCol = MediaUtils.parseColor(element.strokeColorHex, Color.Black)
    val bgCol = MediaUtils.parseColor(element.bgColorHex, Color.Transparent)

    Box(
        modifier = Modifier
            .offset { IntOffset(xPx, yPx) }
            .size(wDp, hDp)
            .then(
                if (isSelected && !element.locked) {
                    Modifier.border(1.5.dp, RoyalBlue, RoundedCornerShape(4.dp))
                } else Modifier
            )
            .pointerInput(element.id, element.locked, paperPxSize) {
                if (!element.locked) {
                    detectDragGestures(
                        onDragStart = { onSelect() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onMoveBy(dragAmount.x, dragAmount.y)
                        }
                    )
                }
            }
            .clickable(onClick = onSelect)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(element.rotationDeg)
        ) {
            when (element.type) {
                FloatingElementType.TEXT_BOX -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgCol, RoundedCornerShape(4.dp))
                            .then(
                                if (element.showBorder) Modifier.border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(4.dp)) else Modifier
                            )
                            .padding(4.dp)
                    ) {
                        Text(
                            text = element.text,
                            color = Color.Black,
                            fontSize = (element.fontSizeSp * scale * 0.85f).coerceIn(7f, 24f).sp,
                            fontFamily = fontFamily
                        )
                    }
                }
                FloatingElementType.MARK_CIRCLE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.8.dp, strokeCol, CircleShape),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            element.text,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = (element.fontSizeSp * scale * 0.8f).coerceIn(7f, 20f).sp,
                            fontFamily = fontFamily
                        )
                        HorizontalDivider(color = strokeCol, thickness = 1.4.dp)
                        Text(
                            element.secondaryText,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = (element.fontSizeSp * scale * 0.8f).coerceIn(7f, 20f).sp,
                            fontFamily = fontFamily
                        )
                    }
                }
                FloatingElementType.MED_CIRCLE -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(bgCol)
                            .border(1.6.dp, strokeCol, CircleShape)
                            .padding(6.dp)
                    ) {
                        Text(
                            element.text,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = (element.fontSizeSp * scale * 0.82f).coerceIn(7f, 20f).sp,
                            fontFamily = fontFamily
                        )
                    }
                }
                FloatingElementType.TABLE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgCol)
                    ) {
                        element.tableRows.forEachIndexed { rIdx, row ->
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                row.forEach { cell ->
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(0.8.dp, strokeCol)
                                            .padding(2.dp)
                                    ) {
                                        Text(
                                            cell,
                                            color = Color.Black,
                                            fontWeight = if (rIdx == 0) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = (element.fontSizeSp * scale * 0.78f).coerceIn(6f, 18f).sp,
                                            fontFamily = fontFamily,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                FloatingElementType.IMAGE -> {
                    val bmp = remember(element.imageBase64) { MediaUtils.decodeBase64ImageBitmap(element.imageBase64) }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Floating image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                FloatingElementType.SHAPE -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawFloatingShape(element.shapeKind, strokeCol)
                    }
                    if (element.text.isNotBlank() && element.text != "...") {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(4.dp)) {
                            Text(
                                element.text,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontSize = (element.fontSizeSp * scale * 0.82f).coerceIn(7f, 20f).sp,
                                fontFamily = fontFamily
                            )
                        }
                    }
                }
                FloatingElementType.NUMBER_LINE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                            val midY = size.height / 2f
                            drawArrowCompose(Offset(6f, midY), Offset(size.width - 6f, midY), strokeCol, 2.dp.toPx())
                            drawArrowCompose(Offset(size.width - 6f, midY), Offset(6f, midY), strokeCol, 2.dp.toPx())
                            for (i in 1..7) {
                                val tx = size.width * (i / 8f)
                                drawLine(strokeCol, Offset(tx, midY - 4.dp.toPx()), Offset(tx, midY + 4.dp.toPx()), strokeWidth = 1.6.dp.toPx())
                            }
                        }
                        Text(
                            element.text,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = (9.5f * scale).coerceAtLeast(7f).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                FloatingElementType.COORD_GRID -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (i in 1..7) {
                            val gx = size.width * (i / 8f)
                            val gy = size.height * (i / 8f)
                            drawLine(Color(0xFFCBD5E1), Offset(gx, 4f), Offset(gx, size.height - 4f), 1.dp.toPx())
                            drawLine(Color(0xFFCBD5E1), Offset(4f, gy), Offset(size.width - 4f, gy), 1.dp.toPx())
                        }
                        drawArrowCompose(Offset(size.width / 2f, size.height - 2f), Offset(size.width / 2f, 2f), strokeCol, 1.8.dp.toPx())
                        drawArrowCompose(Offset(2f, size.height / 2f), Offset(size.width - 2f, size.height / 2f), strokeCol, 1.8.dp.toPx())
                    }
                }
            }
        }

        // Action Bar when selected
        if (isSelected) {
            Surface(
                color = SlateNavy,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = (-34).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    FloatingBarBtn(if (element.locked) "🔓" else "📌", onClick = onTogglePin)
                    if (element.type != FloatingElementType.IMAGE && element.type != FloatingElementType.COORD_GRID) {
                        FloatingBarBtn("✎", onClick = onEdit)
                    }
                    if (!element.locked) {
                        FloatingBarBtn("＋", onClick = { onResize(1.15f) })
                        FloatingBarBtn("－", onClick = { onResize(0.85f) })
                        if (element.type == FloatingElementType.SHAPE || element.type == FloatingElementType.NUMBER_LINE) {
                            FloatingBarBtn("↻45°", onClick = { onRotate(45f) })
                        }
                    }
                    FloatingBarBtn("🎨", onClick = onPickColor)
                    FloatingBarBtn("✕", isDanger = true, onClick = onDelete)
                }
            }
        }
    }
}

@Composable
private fun FloatingBarBtn(label: String, isDanger: Boolean = false, onClick: () -> Unit) {
    Surface(
        color = if (isDanger) CrimsonDanger else Color(0xFF334155),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp)
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun FloatingElementEditDialog(
    element: FloatingElement,
    onDismiss: () -> Unit,
    onSave: (FloatingElement) -> Unit
) {
    var text by remember { mutableStateOf(element.text) }
    var sec by remember { mutableStateOf(element.secondaryText) }
    var showBdr by remember { mutableStateOf(element.showBorder) }
    var rows by remember { mutableStateOf(element.tableRows) }
    var bgHex by remember { mutableStateOf(element.bgColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("دەستکاریکرنا توخمێ", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                when (element.type) {
                    FloatingElementType.MARK_CIRCLE -> {
                        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("نمرە (سەری)") }, singleLine = true)
                        OutlinedTextField(value = sec, onValueChange = { sec = it }, label = { Text("ل سەر (بنی)") }, singleLine = true)
                    }
                    FloatingElementType.TABLE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = {
                                val cols = rows.firstOrNull()?.size ?: 3
                                rows = rows + listOf(List(cols) { "" })
                            }) { Text("+ ڕێز") }
                            OutlinedButton(onClick = {
                                if (rows.size > 1) rows = rows.dropLast(1)
                            }) { Text("- ڕێز") }
                            OutlinedButton(onClick = {
                                rows = rows.map { it + "" }
                            }) { Text("+ ستوون") }
                            OutlinedButton(onClick = {
                                if ((rows.firstOrNull()?.size ?: 0) > 1) rows = rows.map { it.dropLast(1) }
                            }) { Text("- ستوون") }
                        }
                        rows.forEachIndexed { rIdx, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEachIndexed { cIdx, cell ->
                                    OutlinedTextField(
                                        value = cell,
                                        onValueChange = { nv ->
                                            rows = rows.mapIndexed { ri, r ->
                                                if (ri == rIdx) r.mapIndexed { ci, c -> if (ci == cIdx) nv else c } else r
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            minLines = 3,
                            label = { Text("نڤێسین") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (element.type == FloatingElementType.TEXT_BOX) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showBdr, onCheckedChange = { showBdr = it })
                                Text("چوارچێوێ پچڕپچڕ")
                            }
                        }
                    }
                }
                if (element.type == FloatingElementType.TEXT_BOX ||
                    element.type == FloatingElementType.MED_CIRCLE ||
                    element.type == FloatingElementType.TABLE
                ) {
                    Text("ڕەنگێ بەکڕاوەندی:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("#FFFFFF", "#F0FDFA", "#FFFBEB", "#EFF6FF", "#FEF08A", "#FBCFE8").forEach { hex ->
                            val selected = bgHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MediaUtils.parseColor(hex))
                                    .border(if (selected) 2.5.dp else 1.dp, if (selected) RoyalBlue else SubtleBorder, CircleShape)
                                    .clickable { bgHex = hex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(element.copy(text = text, secondaryText = sec, showBorder = showBdr, tableRows = rows, bgColorHex = bgHex))
            }) { Text("خەزن") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لابردن") } }
    )
}

@Composable
private fun MiniActionChip(text: String, isDanger: Boolean = false, onClick: () -> Unit) {
    Surface(
        color = if (isDanger) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.6.dp, if (isDanger) Color(0xFFFCA5A5) else SubtleBorder),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = if (isDanger) Color(0xFFB91C1C) else SlateDark,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

private fun Modifier.drawBehindLineBottom(color: Color, strokeWidth: androidx.compose.ui.unit.Dp): Modifier =
    this.drawBehind {
        val y = size.height
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth.toPx())
    }

private fun DrawScope.drawPaperBorder(style: BorderStyle, color: Color, activeHighlight: Boolean) {
    val strokeW = if (style == BorderStyle.DOUBLE) 2.2.dp.toPx() else 1.8.dp.toPx()
    when (style) {
        BorderStyle.SOLID -> drawRect(color, style = Stroke(strokeW))
        BorderStyle.ROUND -> drawRoundRect(color, cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()), style = Stroke(2.2.dp.toPx()))
        BorderStyle.DASHED -> drawRect(
            color,
            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
        )
        BorderStyle.DOUBLE, BorderStyle.ORNATE -> {
            drawRect(color, style = Stroke(2.4.dp.toPx()))
            val inset = 6.dp.toPx()
            drawRect(
                color,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(1.2.dp.toPx())
            )
        }
        BorderStyle.NONE -> {}
    }
    if (activeHighlight) {
        drawRect(RoyalBlue.copy(alpha = 0.55f), style = Stroke(2.5.dp.toPx()))
    }
}

private fun DrawScope.drawFloatingShape(kind: ShapeKind, color: Color) {
    val st = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val pad = 3.dp.toPx()
    when (kind) {
        ShapeKind.CIRCLE -> drawOval(color, topLeft = Offset(pad, pad), size = Size(size.width - pad * 2, size.height - pad * 2), style = st)
        ShapeKind.SQUARE, ShapeKind.RECT -> drawRect(color, topLeft = Offset(pad, pad), size = Size(size.width - pad * 2, size.height - pad * 2), style = st)
        ShapeKind.TRIANGLE -> {
            val p = Path().apply {
                moveTo(size.width / 2f, pad)
                lineTo(size.width - pad, size.height - pad)
                lineTo(pad, size.height - pad)
                close()
            }
            drawPath(p, color, style = st)
        }
        ShapeKind.RHOMBUS -> {
            val p = Path().apply {
                moveTo(size.width / 2f, pad)
                lineTo(size.width - pad, size.height / 2f)
                lineTo(size.width / 2f, size.height - pad)
                lineTo(pad, size.height / 2f)
                close()
            }
            drawPath(p, color, style = st)
        }
        ShapeKind.LINE -> drawLine(color, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 2.4.dp.toPx())
        ShapeKind.VLINE -> drawLine(color, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth = 2.4.dp.toPx())
        ShapeKind.DLINE -> drawLine(color, Offset(pad, size.height - pad), Offset(size.width - pad, pad), strokeWidth = 2.4.dp.toPx())
        ShapeKind.ARROW -> drawArrowCompose(Offset(pad, size.height / 2f), Offset(size.width - pad, size.height / 2f), color, 2.4.dp.toPx())
        ShapeKind.LARROW -> drawArrowCompose(Offset(size.width - pad, size.height / 2f), Offset(pad, size.height / 2f), color, 2.4.dp.toPx())
        ShapeKind.SLINE -> {
            drawArrowCompose(Offset(pad, size.height / 2f), Offset(size.width - pad, size.height / 2f), color, 2.4.dp.toPx())
            drawArrowCompose(Offset(size.width - pad, size.height / 2f), Offset(pad, size.height / 2f), color, 2.4.dp.toPx())
        }
        ShapeKind.VARROW -> {
            drawArrowCompose(Offset(size.width / 2f, pad), Offset(size.width / 2f, size.height - pad), color, 2.4.dp.toPx())
            drawArrowCompose(Offset(size.width / 2f, size.height - pad), Offset(size.width / 2f, pad), color, 2.4.dp.toPx())
        }
        ShapeKind.RAY -> {
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(pad + 3.dp.toPx(), size.height / 2f))
            drawArrowCompose(Offset(pad + 3.dp.toPx(), size.height / 2f), Offset(size.width - pad, size.height / 2f), color, 2.4.dp.toPx())
        }
    }
}

private fun DrawScope.drawPenStrokeCompose(stroke: PenStroke, w: Float, h: Float) {
    if (stroke.points.size < 2) return
    val color = MediaUtils.parseColor(stroke.colorHex, Color.Black)
    val path = Path()
    path.moveTo(stroke.points.first().x * w, stroke.points.first().y * h)
    for (i in 1 until stroke.points.size) {
        path.lineTo(stroke.points[i].x * w, stroke.points[i].y * h)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = stroke.widthDp.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = if (stroke.dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null
        )
    )
    if (stroke.isArrow) {
        val p1 = stroke.points[stroke.points.size - 2]
        val p2 = stroke.points.last()
        drawArrowCompose(
            Offset(p1.x * w, p1.y * h),
            Offset(p2.x * w, p2.y * h),
            color,
            stroke.widthDp.dp.toPx()
        )
    }
}

private fun DrawScope.drawArrowCompose(start: Offset, end: Offset, color: Color, strokeWidth: Float) {
    drawLine(color, start, end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    val ang = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val sz = 10.dp.toPx()
    drawLine(color, end, Offset((end.x - sz * cos(ang - 0.5)).toFloat(), (end.y - sz * sin(ang - 0.5)).toFloat()), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color, end, Offset((end.x - sz * cos(ang + 0.5)).toFloat(), (end.y - sz * sin(ang + 0.5)).toFloat()), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun snapNormalizedPoint(start: NormalizedPoint, end: NormalizedPoint): NormalizedPoint {
    val dx = abs(end.x - start.x)
    val dy = abs(end.y - start.y)
    return when {
        dy < max(0.015f, dx * 0.12f) -> NormalizedPoint(end.x, start.y)
        dx < max(0.015f, dy * 0.12f) -> NormalizedPoint(start.x, end.y)
        else -> end
    }
}

private fun processSmartStroke(points: List<NormalizedPoint>, mode: PenMode): List<NormalizedPoint> {
    if (mode != PenMode.SMART || points.size < 4) return points
    val first = points.first()
    val last = points.last()
    val len = hypot(last.x - first.x, last.y - first.y)
    if (len < 0.02f) return points
    var maxDist = 0f
    points.forEach { p ->
        val d = abs((last.y - first.y) * p.x - (last.x - first.x) * p.y + last.x * first.y - last.y * first.x) / len
        if (d > maxDist) maxDist = d
    }
    return if (maxDist < max(0.018f, len * 0.11f)) {
        listOf(first, snapNormalizedPoint(first, last))
    } else {
        points
    }
}

private fun estimatePageFill(page: ExamPage, questions: List<Question>, fontSizeSp: Int): Float {
    var units = if (page.showHeader) 0.19f else 0.03f
    val fsFactor = (fontSizeSp / 15f).coerceIn(0.75f, 1.5f)
    questions.forEach { q ->
        units += if (q.type.isNoHeader) 0.01f else 0.055f * fsFactor
        when (q.type) {
            QuestionType.COUNT -> units += ((q.items.size + 3) / 4) * 0.035f * fsFactor
            QuestionType.MATCH -> units += q.items.size * 0.038f * fsFactor
            QuestionType.CONCEPT -> units += 0.11f * fsFactor
            QuestionType.MCQ -> units += q.items.size * 0.065f * fsFactor
            QuestionType.CALLIG -> units += q.items.size * 0.08f * fsFactor
            else -> units += q.items.size * 0.036f * fsFactor
        }
        units += q.answerLines * (q.lineSize.heightDp / 850f)
        units += q.spacerHeightDp / 850f
    }
    if (page.freeWriteText.isNotBlank()) {
        units += (page.freeWriteText.lines().size * 0.032f).coerceAtMost(0.25f)
    }
    return units.coerceIn(0.05f, 1.2f)
}
