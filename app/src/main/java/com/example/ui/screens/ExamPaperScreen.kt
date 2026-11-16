package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ExamStudioUiState
import com.example.util.MediaUtils

@Composable
fun ExamPaperScreen(
    uiState: ExamStudioUiState,
    onSelectPage: (Int) -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: (Int) -> Unit,
    onTogglePageHeader: (Int) -> Unit,
    onToggleHeaderLock: () -> Unit,
    onUpdateStyle: ((ExamStyle) -> ExamStyle) -> Unit,
    onSetZoom: (Float) -> Unit,
    onAddFreeBox: () -> Unit,
    onAddMarkCircle: () -> Unit,
    onAddMedCircle: () -> Unit,
    onAddTable: (Int, Int) -> Unit,
    onAddImage: (String) -> Unit,
    onAddShape: (ShapeKind) -> Unit,
    onAddNumberLine: () -> Unit,
    onAddCoordGrid: () -> Unit,
    onTogglePen: () -> Unit,
    onSetPenMode: (PenMode) -> Unit,
    onSetPenColor: (String) -> Unit,
    onSetPenWidth: (Float) -> Unit,
    onSetPenDashed: (Boolean) -> Unit,
    onAddPenStroke: (Int, PenStroke) -> Unit,
    onUndoPenStroke: (Int) -> Unit,
    onClearPenStrokes: (Int) -> Unit,
    onSelectFloating: (String?) -> Unit,
    onUpdateFloating: (Int, String, (FloatingElement) -> FloatingElement) -> Unit,
    onDeleteFloating: (Int, String) -> Unit,
    onMoveQuestion: (String, Int) -> Unit,
    onMoveQuestionPage: (String, Int) -> Unit,
    onSplitQuestion: (String, Int) -> Unit,
    onUpdateQuestion: (String, (Question) -> Question) -> Unit,
    onDeleteQuestion: (String) -> Unit,
    onAddQuestionItem: (String) -> Unit,
    onDeleteQuestionItem: (String, Int) -> Unit,
    onUpdateFreeWrite: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val doc = uiState.document
    val st = doc.style

    var fontMenuOpen by remember { mutableStateOf(false) }
    var borderMenuOpen by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // key to title
    var editingQuestionId by remember { mutableStateOf<String?>(null) }
    var bgColorQuestionId by remember { mutableStateOf<String?>(null) }
    var mathTargetItemIdx by remember { mutableStateOf<Int?>(null) }
    var mathDialogKind by remember { mutableStateOf<MathInsertKind?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { MediaUtils.uriToBase64(context, it, maxDim = 640)?.let(onAddImage) }
    }

    val qImgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val qid = editingQuestionId ?: return@rememberLauncherForActivityResult
        uri?.let {
            MediaUtils.uriToBase64(context, it, maxDim = 640)?.let { b64 ->
                onUpdateQuestion(qid) { q -> q.copy(imageBase64 = b64) }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        // Style Toolbar
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(10.dp)
        ) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                // Font selector
                Box {
                    OutlinedButton(onClick = { fontMenuOpen = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("فۆنت: ${ExamFontOption.fromKey(st.fontKey).label}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = fontMenuOpen, onDismissRequest = { fontMenuOpen = false }) {
                        ExamFontOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label, fontFamily = opt.fontFamily) },
                                onClick = {
                                    onUpdateStyle { it.copy(fontKey = opt.key) }
                                    fontMenuOpen = false
                                }
                            )
                        }
                    }
                }

                // Font size
                StepperPill("قەبارە: ${st.fontSizeSp}", onMinus = {
                    onUpdateStyle { it.copy(fontSizeSp = (it.fontSizeSp - 1).coerceIn(9, 26)) }
                }, onPlus = {
                    onUpdateStyle { it.copy(fontSizeSp = (it.fontSizeSp + 1).coerceIn(9, 26)) }
                })

                // Header scale
                StepperPill("کلێشە: ${st.headerScalePct}%", onMinus = {
                    onUpdateStyle { it.copy(headerScalePct = (it.headerScalePct - 5).coerceIn(65, 125)) }
                }, onPlus = {
                    onUpdateStyle { it.copy(headerScalePct = (it.headerScalePct + 5).coerceIn(65, 125)) }
                })

                // Zoom
                StepperPill("زوم: ${(uiState.paperZoom * 100).toInt()}%", onMinus = {
                    onSetZoom(uiState.paperZoom - 0.15f)
                }, onPlus = {
                    onSetZoom(uiState.paperZoom + 0.15f)
                })

                // Colors
                ColorPill("کلێشە", st.headerColorHex) { colorDialog = "hdr" to "ڕەنگێ کلێشەی" }
                ColorPill("پرسیار", st.questionTitleColorHex) { colorDialog = "qh" to "ڕەنگێ پرسیاران" }
                ColorPill("نڤێسین", st.bodyTextColorHex) { colorDialog = "txt" to "ڕەنگێ نڤێسینێ" }
                ColorPill("پەڕ", st.paperBgColorHex) { colorDialog = "bg" to "ڕەنگێ وەرقا A4" }

                // Border style
                Box {
                    OutlinedButton(onClick = { borderMenuOpen = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("چوارچێوە: ${st.borderStyle.label}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = borderMenuOpen, onDismissRequest = { borderMenuOpen = false }) {
                        BorderStyle.entries.forEach { bs ->
                            DropdownMenuItem(
                                text = { Text(bs.label) },
                                onClick = {
                                    onUpdateStyle { it.copy(borderStyle = bs) }
                                    borderMenuOpen = false
                                }
                            )
                        }
                    }
                }
                ColorPill("چوارچێوە", st.borderColorHex) { colorDialog = "frm" to "ڕەنگێ چوارچێوەی" }

                FilledTonalButton(
                    onClick = { onTogglePageHeader(uiState.activePageIndex) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("± کلێشە", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }

                FilledTonalButton(
                    onClick = onToggleHeaderLock,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(if (doc.header.locked) "🔒 سابتە" else "🔓 کلێشە", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // On-Paper Tools Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    "📐 ئامرازێن سەر پەڕێ A4 (بەرسڤ، خشتە، وێنە، دەق، پێنوس):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(onClick = onAddFreeBox, modifier = Modifier.testTag("add_free_box_btn")) {
                        Text("📝 نڤێسین ل هەر جهەکی", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(onClick = onAddMarkCircle, modifier = Modifier.testTag("add_mark_circle_btn")) {
                        Text("⭕ بازنێ نمرا", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onTogglePen,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.penEnabled) CrimsonDanger else AmberWarm
                        ),
                        modifier = Modifier.testTag("toggle_smart_pen_btn")
                    ) {
                        Text(if (uiState.penEnabled) "✓ بڕینا پێنوسی" else "✏️ پێنوسێ زیرەک", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(onClick = { onAddTable(3, 2) }, modifier = Modifier.testTag("add_table_btn")) {
                        Text("+ خشتە", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.testTag("add_paper_image_btn")
                    ) {
                        Text("🖼️ + وێنە / لۆگۆ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    listOf(ShapeKind.CIRCLE, ShapeKind.TRIANGLE, ShapeKind.SQUARE, ShapeKind.RECT, ShapeKind.RHOMBUS).forEach { sk ->
                        OutlinedButton(onClick = { onAddShape(sk) }) {
                            Text(sk.label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                        }
                    }
                }

                MathAndShapeToolbox(
                    showPaperItems = true,
                    onInsertTextToFocused = { token ->
                        onUpdateFreeWrite(uiState.activePageIndex, doc.pages[uiState.activePageIndex].freeWriteText + token)
                    },
                    onOpenMathDialog = { kind -> mathDialogKind = kind },
                    onAddShape = onAddShape,
                    onAddNumberLine = onAddNumberLine,
                    onAddCoordGrid = onAddCoordGrid,
                    onAddMedCircle = onAddMedCircle,
                    onAddMiniTable = onAddTable
                )
            }
        }

        // Smart Pen Toolbar
        AnimatedVisibility(visible = uiState.penEnabled) {
            Surface(
                color = Color(0xFFFEFCE8),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFDE047)),
                modifier = Modifier.fillMaxWidth()
            ) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("✏️ پێنوسێ زیرەک:", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                    PenMode.entries.forEach { pm ->
                        FilterChip(
                            selected = uiState.penMode == pm,
                            onClick = { onSetPenMode(pm) },
                            label = { Text(pm.label, fontSize = 11.5.sp) }
                        )
                    }
                    FilterChip(
                        selected = uiState.penDashed,
                        onClick = { onSetPenDashed(!uiState.penDashed) },
                        label = { Text("پچڕپچڕ", fontSize = 11.5.sp) }
                    )
                    ColorPill("ڕەنگ", uiState.penColorHex) { colorDialog = "pen" to "ڕەنگێ پێنوسی" }
                    StepperPill("ئەستووری: ${uiState.penWidthDp.toInt()}", onMinus = {
                        onSetPenWidth(uiState.penWidthDp - 1f)
                    }, onPlus = {
                        onSetPenWidth(uiState.penWidthDp + 1f)
                    })
                    OutlinedButton(onClick = { onUndoPenStroke(uiState.activePageIndex) }) { Text("↶") }
                    OutlinedButton(
                        onClick = { onClearPenStrokes(uiState.activePageIndex) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDanger)
                    ) { Text("✕ هەمی") }
                    Button(onClick = onTogglePen) { Text("✓ بڕین") }
                }
            }
        }

        // A4 Paper Pages Area
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val baseW = (maxWidth.value - 8f).coerceIn(280f, 595f)
            val scaledW = baseW * uiState.paperZoom
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                doc.pages.forEachIndexed { pi, _ ->
                    A4PageBlock(
                        pageIndex = pi,
                        totalPages = doc.pages.size,
                        isActivePage = uiState.activePageIndex == pi,
                        doc = doc,
                        penEnabled = uiState.penEnabled,
                        penMode = uiState.penMode,
                        penColorHex = uiState.penColorHex,
                        penWidthDp = uiState.penWidthDp,
                        penDashed = uiState.penDashed,
                        selectedFloatingId = if (uiState.activePageIndex == pi) uiState.selectedFloatingId else null,
                        paperWidthDp = scaledW,
                        onSelectPage = { onSelectPage(pi) },
                        onToggleHeader = { onTogglePageHeader(pi) },
                        onDeletePage = { onDeletePage(pi) },
                        onMoveQuestion = onMoveQuestion,
                        onMoveQuestionPage = onMoveQuestionPage,
                        onSplitQuestion = onSplitQuestion,
                        onEditQuestionRequest = { q -> editingQuestionId = q.id },
                        onDeleteQuestion = onDeleteQuestion,
                        onAdjustQuestionLines = { qid, delta ->
                            onUpdateQuestion(qid) { q -> q.copy(answerLines = (q.answerLines + delta).coerceAtLeast(0)) }
                        },
                        onToggleQuestionSeparator = { qid ->
                            onUpdateQuestion(qid) { q -> q.copy(noSeparatorLine = !q.noSeparatorLine) }
                        },
                        onPickQuestionBg = { qid ->
                            bgColorQuestionId = qid
                        },
                        onUpdateFreeWrite = { txt -> onUpdateFreeWrite(pi, txt) },
                        onAddPenStroke = { stroke -> onAddPenStroke(pi, stroke) },
                        onSelectFloating = { fid ->
                            onSelectPage(pi)
                            onSelectFloating(fid)
                        },
                        onUpdateFloating = { fid, tr -> onUpdateFloating(pi, fid, tr) },
                        onDeleteFloating = { fid -> onDeleteFloating(pi, fid) }
                    )
                }
            }
        }

        // Add Page Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Button(
                onClick = onAddPage,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                modifier = Modifier.testTag("add_a4_page_button")
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("+ زێدەکرنا پەڕەکێ دی یێ A4", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Color Picker Dialog
    colorDialog?.let { (key, title) ->
        val cur = when (key) {
            "hdr" -> st.headerColorHex
            "qh" -> st.questionTitleColorHex
            "txt" -> st.bodyTextColorHex
            "bg" -> st.paperBgColorHex
            "frm" -> st.borderColorHex
            else -> uiState.penColorHex
        }
        ColorPickerDialog(
            title = title,
            currentHex = cur,
            onDismiss = { colorDialog = null },
            onColorSelected = { hex ->
                when (key) {
                    "hdr" -> onUpdateStyle { it.copy(headerColorHex = hex) }
                    "qh" -> onUpdateStyle { it.copy(questionTitleColorHex = hex) }
                    "txt" -> onUpdateStyle { it.copy(bodyTextColorHex = hex) }
                    "bg" -> onUpdateStyle { it.copy(paperBgColorHex = hex) }
                    "frm" -> onUpdateStyle { it.copy(borderColorHex = hex) }
                    "pen" -> onSetPenColor(hex)
                }
            }
        )
    }

    // Math Insert Dialog for FreeWrite or Quick-Edited Question
    mathDialogKind?.let { kind ->
        MathInsertDialog(
            initialKind = kind,
            onDismiss = {
                mathDialogKind = null
                mathTargetItemIdx = null
            },
            onInsertToken = { token ->
                val targetQid = editingQuestionId
                val targetIdx = mathTargetItemIdx
                if (targetQid != null && targetIdx != null) {
                    onUpdateQuestion(targetQid) { q ->
                        if (targetIdx == -1) {
                            q.copy(title = q.title + " " + token, isDefaultTitle = false)
                        } else {
                            q.copy(items = q.items.mapIndexed { i, it ->
                                if (i == targetIdx) it.copy(text = it.text + " " + token, isDefault = false) else it
                            })
                        }
                    }
                } else {
                    val curTxt = doc.pages[uiState.activePageIndex].freeWriteText
                    onUpdateFreeWrite(uiState.activePageIndex, if (curTxt.isBlank()) token else "$curTxt $token")
                }
            }
        )
    }

    // Question Background Color Dialog
    bgColorQuestionId?.let { qid ->
        val curHex = doc.questions.firstOrNull { it.id == qid }?.bgColorHex.orEmpty().ifBlank { "#EFF6FF" }
        ColorPickerDialog(
            title = "بەکڕاوەندێ پرسیارێ",
            currentHex = curHex,
            onDismiss = { bgColorQuestionId = null },
            onColorSelected = { hex ->
                onUpdateQuestion(qid) { it.copy(bgColorHex = hex) }
            }
        )
    }

    // Quick Edit Question Modal when tapping a question on A4
    editingQuestionId?.let { qid ->
        val q = doc.questions.firstOrNull { it.id == qid }
        if (q != null) {
            val d = LocalizationPresets.strings(doc.language)
            val dispNum = doc.questions.takeWhile { it.id != q.id }.count { !it.type.isNoHeader } + if (q.type.isNoHeader) 0 else 1
            AlertDialog(
                onDismissRequest = { editingQuestionId = null },
                title = { Text("دەستکاریکرنا پرسیارێ", fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        QuestionEditorCard(
                            q = q,
                            displayNumber = dispNum,
                            pageCount = doc.pages.size,
                            langStrings = d,
                            numeralMode = doc.numeralMode,
                            lang = doc.language,
                            onFocusTarget = {},
                            onUpdate = { tr -> onUpdateQuestion(qid, tr) },
                            onDelete = { onDeleteQuestion(qid); editingQuestionId = null },
                            onMoveUp = { onMoveQuestion(qid, -1) },
                            onMoveDown = { onMoveQuestion(qid, 1) },
                            onAddItem = { onAddQuestionItem(qid) },
                            onDeleteItem = { idx -> onDeleteQuestionItem(qid, idx) },
                            onChangePage = { pg -> onMoveQuestionPage(qid, pg) },
                            onSplitNextPage = { onSplitQuestion(qid, q.pageIndex + 1) },
                            onOpenBgPicker = { bgColorQuestionId = qid },
                            onPickQuestionImage = {
                                qImgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onOpenMathInsert = { itemIdx ->
                                mathTargetItemIdx = itemIdx
                                mathDialogKind = MathInsertKind.FRAC
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { editingQuestionId = null }) { Text("تەمام") }
                }
            )
        }
    }
}

@Composable
private fun StepperPill(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onMinus, modifier = Modifier.size(30.dp)) {
                Text("－", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onPlus, modifier = Modifier.size(30.dp)) {
                Text("＋", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ColorPill(label: String, hex: String, onClick: () -> Unit) {
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(MediaUtils.parseColor(hex), RoundedCornerShape(4.dp))
                    .border(1.dp, SubtleBorder, RoundedCornerShape(4.dp))
            )
        }
    }
}
