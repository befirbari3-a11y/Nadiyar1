package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.export.WordDocExporter
import com.example.model.*
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.MathAndShapeToolbox
import com.example.ui.components.MathInsertDialog
import com.example.ui.components.MathInsertKind
import com.example.ui.components.RichMathText
import com.example.ui.theme.*
import com.example.util.MediaUtils

@Composable
fun QuestionsScreen(
    doc: ExamDocumentState,
    onToggleHeaderLock: () -> Unit,
    onApplySubjectPreset: (SubjectPreset) -> Unit,
    onAddQuestion: (QuestionType) -> Unit,
    onUpdateQuestion: (String, (Question) -> Question) -> Unit,
    onDeleteQuestion: (String) -> Unit,
    onMoveQuestion: (String, Int) -> Unit,
    onAddQuestionItem: (String) -> Unit,
    onDeleteQuestionItem: (String, Int) -> Unit,
    onMoveQuestionToPage: (String, Int) -> Unit,
    onSplitQuestionToNextPage: (String, Int) -> Unit
) {
    val context = LocalContext.current
    val totalMarks = remember(doc) { WordDocExporter.calculateTotalMarks(doc) }
    val d = LocalizationPresets.strings(doc.language)
    val subjectPresets = remember { LocalizationPresets.subjectPresets() }

    var mathDialogKind by remember { mutableStateOf<MathInsertKind?>(null) }
    var focusedTarget by remember { mutableStateOf<Pair<String, Int>?>(null) } // questionId to itemIndex (-1 = title)
    var colorTargetQuestionId by remember { mutableStateOf<String?>(null) }
    var imageTargetQuestionId by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val qid = imageTargetQuestionId ?: return@rememberLauncherForActivityResult
        uri?.let {
            MediaUtils.uriToBase64(context, it, maxDim = 640)?.let { b64 ->
                onUpdateQuestion(qid) { q -> q.copy(imageBase64 = b64) }
            }
        }
    }

    var displayNum = 0
    val displayNumMap = remember(doc.questions) {
        doc.questions.associate { q ->
            if (q.type.isNoHeader) q.id to 0 else q.id to ++displayNum
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 900.dp)
    ) {
        // Total Marks & Quick Lock Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "کۆیێ نمرێ پرسیاران: $totalMarks",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = RoyalBlue
                    )
                    FilledTonalButton(
                        onClick = onToggleHeaderLock,
                        modifier = Modifier.testTag("quick_header_lock_button")
                    ) {
                        Text(
                            if (doc.header.locked) "🔓 کلێشە یا سابتە" else "🔒 سابتکرنا کلێشەی",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 6 Subject Presets
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        "📚 قاڵبێن ئامادە یێن پرسیارێن بابەتان (ب ئێک کلیک دروست بکە):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subjectPresets.forEach { preset ->
                            Button(
                                onClick = { onApplySubjectPreset(preset) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(preset.colorHex)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("preset_${preset.id}")
                            ) {
                                Text(preset.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Collapsible Math & Symbol Toolbox
        item {
            MathAndShapeToolbox(
                showPaperItems = false,
                onInsertTextToFocused = { token ->
                    val target = focusedTarget
                    if (target != null) {
                        val (qid, idx) = target
                        onUpdateQuestion(qid) { q ->
                            if (idx == -1) {
                                q.copy(title = q.title + token, isDefaultTitle = false)
                            } else {
                                q.copy(items = q.items.mapIndexed { i, it ->
                                    if (i == idx) it.copy(text = it.text + token, isDefault = false) else it
                                })
                            }
                        }
                    } else if (doc.questions.isNotEmpty()) {
                        val q = doc.questions.last()
                        onUpdateQuestion(q.id) { cur ->
                            if (cur.items.isNotEmpty()) {
                                cur.copy(items = cur.items.mapIndexed { i, it ->
                                    if (i == 0) it.copy(text = it.text + token, isDefault = false) else it
                                })
                            } else cur.copy(title = cur.title + token, isDefaultTitle = false)
                        }
                    }
                },
                onOpenMathDialog = { kind -> mathDialogKind = kind }
            )
        }

        // Question Cards
        if (doc.questions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null, tint = MutedText, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "هێشتا چ پرسیارەک نینە. ژ خوارێ جۆرەکێ هەڵبژێرە یان قاڵبەکێ ئامادە کلیک بکە.",
                            color = MutedText,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            itemsIndexed(doc.questions, key = { _, q -> q.id }) { _, q ->
                val dispNum = displayNumMap[q.id] ?: 0
                QuestionEditorCard(
                    q = q,
                    displayNumber = dispNum,
                    pageCount = doc.pages.size,
                    langStrings = d,
                    numeralMode = doc.numeralMode,
                    lang = doc.language,
                    onFocusTarget = { itemIdx -> focusedTarget = q.id to itemIdx },
                    onUpdate = { transform -> onUpdateQuestion(q.id, transform) },
                    onDelete = { onDeleteQuestion(q.id) },
                    onMoveUp = { onMoveQuestion(q.id, -1) },
                    onMoveDown = { onMoveQuestion(q.id, 1) },
                    onAddItem = { onAddQuestionItem(q.id) },
                    onDeleteItem = { idx -> onDeleteQuestionItem(q.id, idx) },
                    onChangePage = { pg -> onMoveQuestionToPage(q.id, pg) },
                    onSplitNextPage = { onSplitQuestionToNextPage(q.id, q.pageIndex + 1) },
                    onOpenBgPicker = { colorTargetQuestionId = q.id },
                    onPickQuestionImage = {
                        imageTargetQuestionId = q.id
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onOpenMathInsert = { itemIdx ->
                        focusedTarget = q.id to itemIdx
                        mathDialogKind = MathInsertKind.FRAC
                    }
                )
            }
        }

        // 17 Question Types Add Grid
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text("➕ پرسیارەکا نوی زێدە بکە:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuestionType.entries.forEach { tp ->
                            Button(
                                onClick = { onAddQuestion(tp) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(tp.colorHex)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_q_${tp.key}")
                            ) {
                                Text(tp.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    mathDialogKind?.let { kind ->
        MathInsertDialog(
            initialKind = kind,
            onDismiss = { mathDialogKind = null },
            onInsertToken = { token ->
                val target = focusedTarget
                if (target != null) {
                    val (qid, idx) = target
                    onUpdateQuestion(qid) { q ->
                        if (idx == -1) {
                            q.copy(title = q.title + " " + token, isDefaultTitle = false)
                        } else {
                            q.copy(items = q.items.mapIndexed { i, it ->
                                if (i == idx) it.copy(text = it.text + " " + token, isDefault = false) else it
                            })
                        }
                    }
                } else if (doc.questions.isNotEmpty()) {
                    val lastQ = doc.questions.last()
                    onUpdateQuestion(lastQ.id) { cur ->
                        if (cur.items.isNotEmpty()) {
                            cur.copy(items = cur.items.mapIndexed { i, it ->
                                if (i == 0) it.copy(text = it.text + " " + token, isDefault = false) else it
                            })
                        } else cur.copy(title = cur.title + " " + token, isDefaultTitle = false)
                    }
                }
            }
        )
    }

    colorTargetQuestionId?.let { qid ->
        val curHex = doc.questions.firstOrNull { it.id == qid }?.bgColorHex.orEmpty().ifBlank { "#EFF6FF" }
        ColorPickerDialog(
            title = "بەکڕاوەندێ پرسیارێ",
            currentHex = curHex,
            onDismiss = { colorTargetQuestionId = null },
            onColorSelected = { hex ->
                onUpdateQuestion(qid) { it.copy(bgColorHex = hex) }
            }
        )
    }
}

@Composable
fun QuestionEditorCard(
    q: Question,
    displayNumber: Int,
    pageCount: Int,
    langStrings: LangStrings,
    numeralMode: NumeralMode,
    lang: ExamLanguage,
    onFocusTarget: (Int) -> Unit,
    onUpdate: ((Question) -> Question) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddItem: () -> Unit,
    onDeleteItem: (Int) -> Unit,
    onChangePage: (Int) -> Unit,
    onSplitNextPage: () -> Unit,
    onOpenBgPicker: () -> Unit,
    onPickQuestionImage: () -> Unit,
    onOpenMathInsert: (Int) -> Unit
) {
    val noHdr = q.type.isNoHeader
    val cardBg = if (q.bgColorHex.isNotBlank()) MediaUtils.parseColor(q.bgColorHex, CardSurface) else CardSurface

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.2.dp, SubtleBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            // Card Header Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeTitle = if (noHdr) {
                    q.type.label
                } else {
                    "${langStrings.qPrefix}${LocalizationPresets.formatNumber(displayNumber, numeralMode, lang)} (${q.type.label})"
                }
                Text(
                    badgeTitle,
                    fontWeight = FontWeight.ExtraBold,
                    color = RoyalBlue,
                    fontSize = 13.5.sp,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onOpenBgPicker, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Palette, contentDescription = "Background color", modifier = Modifier.size(18.dp))
                    }
                    if (q.bgColorHex.isNotBlank()) {
                        IconButton(onClick = { onUpdate { it.copy(bgColorHex = "") } }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.FormatColorReset, contentDescription = "Clear background", modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onSplitNextPage, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Split to next page", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete question", tint = CrimsonDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (pageCount > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("پەڕ:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    (0 until pageCount).forEach { pg ->
                        FilterChip(
                            selected = q.pageIndex == pg,
                            onClick = { onChangePage(pg) },
                            label = { Text("${pg + 1}", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Title & Marks
            if (!noHdr) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = q.title,
                        onValueChange = { nv -> onFocusTarget(-1); onUpdate { it.copy(title = nv, isDefaultTitle = false) } },
                        label = { Text("دەقێ پرسیارێ") },
                        trailingIcon = {
                            IconButton(onClick = { onOpenMathInsert(-1) }) {
                                Icon(Icons.Default.Functions, contentDescription = "Insert math", tint = VioletMath)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = q.marks,
                        onValueChange = { nv -> onUpdate { it.copy(marks = nv, isDefaultMarks = false) } },
                        label = { Text(langStrings.markWord) },
                        singleLine = true,
                        modifier = Modifier.width(82.dp)
                    )
                }
            }

            // Question Body by Type
            when (q.type) {
                QuestionType.MATCH -> {
                    q.items.forEachIndexed { ii, it ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${LocalizationPresets.formatNumber(ii + 1, numeralMode, lang)}-", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = it.text,
                                onValueChange = { nv ->
                                    onUpdate { cur ->
                                        cur.copy(items = cur.items.mapIndexed { idx, x -> if (idx == ii) x.copy(text = nv, isDefault = false) else x })
                                    }
                                },
                                label = { Text("ستوونا أ") },
                                modifier = Modifier.weight(1f)
                            )
                            Text(langStrings.matchRightLabels.getOrElse(ii) { "${ii + 1}-" }, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = it.matchRight,
                                onValueChange = { nv ->
                                    onUpdate { cur ->
                                        cur.copy(items = cur.items.mapIndexed { idx, x -> if (idx == ii) x.copy(matchRight = nv, isDefault = false) else x })
                                    }
                                },
                                label = { Text("ستوونا ب") },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDeleteItem(ii) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove item", tint = CrimsonDanger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                QuestionType.CONCEPT -> {
                    OutlinedTextField(
                        value = q.conceptCenter,
                        onValueChange = { nv -> onUpdate { it.copy(conceptCenter = nv, isDefaultConcept = false) } },
                        label = { Text("چەمکێ سەرەکی (ناڤەڕاست)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    q.conceptBranches.forEachIndexed { bi, br ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = br,
                                onValueChange = { nv ->
                                    onUpdate { cur ->
                                        cur.copy(
                                            conceptBranches = cur.conceptBranches.mapIndexed { idx, s -> if (idx == bi) nv else s },
                                            isDefaultConcept = false
                                        )
                                    }
                                },
                                label = { Text("بەش ${bi + 1}") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDeleteItem(bi) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove branch", tint = CrimsonDanger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                else -> {
                    if (q.type == QuestionType.IMG) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onPickQuestionImage,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                            ) {
                                Text("🖼 هەڵبژارتنا وێنە", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (q.imageBase64.isNotBlank()) {
                                OutlinedButton(onClick = { onUpdate { it.copy(imageWidthDp = (it.imageWidthDp + 30).coerceAtMost(500)) } }) {
                                    Text("＋ گەورە")
                                }
                                OutlinedButton(onClick = { onUpdate { it.copy(imageWidthDp = (it.imageWidthDp - 30).coerceAtLeast(80)) } }) {
                                    Text("－ بچوک")
                                }
                            }
                        }
                        if (q.imageBase64.isNotBlank()) {
                            val bmp = remember(q.imageBase64) { MediaUtils.decodeBase64ImageBitmap(q.imageBase64) }
                            bmp?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = "Question image",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .width(q.imageWidthDp.coerceAtMost(280).dp)
                                        .heightIn(max = 180.dp)
                                )
                            }
                        }
                    }

                    q.items.forEachIndexed { ii, it ->
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!noHdr) {
                                        Text(
                                            "${LocalizationPresets.formatNumber(ii + 1, numeralMode, lang)}-",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    OutlinedTextField(
                                        value = it.text,
                                        onValueChange = { nv ->
                                            onFocusTarget(ii)
                                            onUpdate { cur ->
                                                cur.copy(items = cur.items.mapIndexed { idx, x -> if (idx == ii) x.copy(text = nv, isDefault = false) else x })
                                            }
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { onOpenMathInsert(ii) }) {
                                                Icon(Icons.Default.Functions, contentDescription = "Insert math", tint = VioletMath)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onDeleteItem(ii) }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete item", tint = CrimsonDanger, modifier = Modifier.size(16.dp))
                                    }
                                }
                                // Preview rendered math if item contains math tokens
                                if (it.text.contains("[")) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(VioletContainer.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        RichMathText(
                                            text = it.text,
                                            color = Color(0xFF4C1D95),
                                            fontSize = 14.sp,
                                            fontFamily = ScheherazadeNewFont
                                        )
                                    }
                                }

                                if (q.type == QuestionType.MCQ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        it.options.chunked(2).forEachIndexed { rIdx, pair ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                pair.forEachIndexed { cIdx, op ->
                                                    val oi = rIdx * 2 + cIdx
                                                    OutlinedTextField(
                                                        value = op.text,
                                                        onValueChange = { nv ->
                                                            onUpdate { cur ->
                                                                cur.copy(items = cur.items.mapIndexed { idx, x ->
                                                                    if (idx == ii) {
                                                                        x.copy(options = x.options.mapIndexed { oIdx, oldOp ->
                                                                            if (oIdx == oi) oldOp.copy(text = nv, isDefault = false) else oldOp
                                                                        })
                                                                    } else x
                                                                })
                                                            }
                                                        },
                                                        label = { Text(langStrings.mcqLabels.getOrElse(oi) { "" }) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Item & Answer Lines Toolbar
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(
                    onClick = onAddItem,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(if (q.type == QuestionType.CONCEPT) "+ بەش" else "+ خاڵ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onUpdate { it.copy(answerLines = it.answerLines + 1) } },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("+ هێڵ (${q.answerLines})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onUpdate { it.copy(answerLines = (it.answerLines - 1).coerceAtLeast(0)) } },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("- هێڵ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LineSize.entries.forEach { sz ->
                    FilterChip(
                        selected = q.lineSize == sz,
                        onClick = { onUpdate { it.copy(lineSize = sz, answerLines = if (it.answerLines == 0) 2 else it.answerLines) } },
                        label = { Text(sz.label, fontSize = 11.sp) }
                    )
                }
                if (q.type == QuestionType.SPACER) {
                    OutlinedButton(onClick = { onUpdate { it.copy(spacerHeightDp = it.spacerHeightDp + 25) } }) {
                        Text("+ بۆشایی سپی", fontSize = 11.5.sp)
                    }
                    OutlinedButton(onClick = { onUpdate { it.copy(spacerHeightDp = (it.spacerHeightDp - 25).coerceAtLeast(0)) } }) {
                        Text("- بۆشایی سپی", fontSize = 11.5.sp)
                    }
                }
            }
        }
    }
}
