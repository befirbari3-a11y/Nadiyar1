package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.export.ExamPdfExporter
import com.example.export.WordDocExporter
import com.example.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ExamViewModel

@Composable
fun ExamStudioApp(viewModel: ExamViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val doc = uiState.document
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showResetDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var numMenuOpen by remember { mutableStateOf(false) }

    // SAF JSON export
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val ok = runCatching {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(viewModel.exportJsonString().toByteArray(Charsets.UTF_8))
                }
                true
            }.getOrDefault(false)
            viewModel.showMessage(if (ok) "✓ فایلێ JSON هاتە خەزنکرن" else "خەزنکرن سەرنەکەفت")
        }
    }

    // SAF Word export (Exam or Certificate)
    var pendingWordHtml by remember { mutableStateOf("") }
    val wordExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/msword")
    ) { uri: Uri? ->
        uri?.let {
            val ok = WordDocExporter.writeToUri(context, it, pendingWordHtml)
            viewModel.showMessage(if (ok) "✓ فایلێ Word هاتە خەزنکرن" else "خەزنکرن سەرنەکەفت")
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val layoutDir = if (doc.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 720.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = SurfaceBackground,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateNavy)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        // Top Brand & Action Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Nadiyar hussein -kurdi",
                                    color = SkyAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "ئامادە کردن : ماموستا نە دیار حسێن حسن",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.undo() },
                                    modifier = Modifier.testTag("undo_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "زڤڕاندن", tint = Color(0xFFFBBF24))
                                }
                                IconButton(
                                    onClick = { viewModel.saveNow(explicit = true) },
                                    modifier = Modifier.testTag("save_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "خەزن", tint = Color(0xFF4ADE80))
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.saveNow(explicit = false)
                                        if (uiState.activeTab == AppTab.CERTIFICATES) {
                                            ExamPdfExporter.printCertificate(context, uiState.certificate)
                                        } else {
                                            ExamPdfExporter.printExam(context, doc)
                                        }
                                    },
                                    modifier = Modifier.testTag("pdf_print_button")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color(0xFFF87171))
                                }
                                IconButton(
                                    onClick = { showExportDialog = true },
                                    modifier = Modifier.testTag("share_export_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Word / Share", tint = SkyAccent)
                                }
                                IconButton(
                                    onClick = { showResetDialog = true },
                                    modifier = Modifier.testTag("reset_all_button")
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "نوی", tint = Color(0xFFCBD5E1))
                                }
                            }
                        }

                        // Language & Numeral Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDark)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌐", fontSize = 12.sp)
                            ExamLanguage.entries.forEach { l ->
                                val selected = doc.language == l
                                Surface(
                                    color = if (selected) SkyAccent else Color.Transparent,
                                    shape = RoundedCornerShape(99.dp),
                                    border = BorderStroke(1.dp, if (selected) SkyAccent else Color(0xFF475569)),
                                    modifier = Modifier
                                        .clickable { viewModel.setLanguage(l) }
                                        .testTag("lang_${l.key}")
                                ) {
                                    Text(
                                        l.label,
                                        color = if (selected) SlateNavy else Color(0xFFCBD5E1),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(6.dp))

                            Box {
                                Surface(
                                    color = SlateNavy,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF475569)),
                                    modifier = Modifier.clickable { numMenuOpen = true }
                                ) {
                                    Text(
                                        doc.numeralMode.label,
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                DropdownMenu(expanded = numMenuOpen, onDismissRequest = { numMenuOpen = false }) {
                                    NumeralMode.entries.forEach { nm ->
                                        DropdownMenuItem(
                                            text = { Text(nm.label) },
                                            onClick = { viewModel.setNumeralMode(nm); numMenuOpen = false }
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.setAutoSave(!uiState.autoSaveEnabled) }
                            ) {
                                Checkbox(
                                    checked = uiState.autoSaveEnabled,
                                    onCheckedChange = { viewModel.setAutoSave(it) },
                                    colors = CheckboxDefaults.colors(checkedColor = SkyAccent, checkmarkColor = SlateNavy),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text("خۆکار", color = Color(0xFFE2E8F0), fontSize = 11.sp)
                            }

                            Text(
                                uiState.saveStatusText,
                                color = Color(0xFF4ADE80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!isWide) {
                        NavigationBar(
                            containerColor = CardSurface,
                            tonalElevation = 4.dp
                        ) {
                            ExamNavItems(
                                activeTab = uiState.activeTab,
                                questionCount = doc.questions.size,
                                onSelectTab = viewModel::selectTab
                            ).forEach { item ->
                                NavigationBarItem(
                                    selected = item.selected,
                                    onClick = item.onClick,
                                    icon = item.icon,
                                    label = { Text(item.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWide) {
                        NavigationRail(containerColor = CardSurface) {
                            Spacer(Modifier.height(12.dp))
                            ExamNavItems(
                                activeTab = uiState.activeTab,
                                questionCount = doc.questions.size,
                                onSelectTab = viewModel::selectTab
                            ).forEach { item ->
                                NavigationRailItem(
                                    selected = item.selected,
                                    onClick = item.onClick,
                                    icon = item.icon,
                                    label = { Text(item.label, fontSize = 11.sp) },
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (uiState.activeTab) {
                            AppTab.HEADER -> ExamHeaderScreen(
                                doc = doc,
                                onToggleLock = viewModel::toggleHeaderLock,
                                onSavePreset = viewModel::saveHeaderPreset,
                                onLoadPreset = viewModel::loadHeaderPreset,
                                onExportJson = { jsonExportLauncher.launch("Nadiyar-Exam.json") },
                                onImportJson = viewModel::importJsonString,
                                onResetHeader = viewModel::resetHeader,
                                onUpdateField = viewModel::updateHeaderField,
                                onToggleFieldVisibility = viewModel::toggleHeaderFieldVisibility,
                                onSetLogoBase64 = viewModel::setHeaderLogo,
                                onSetLogoPos = viewModel::setHeaderLogoPos,
                                onChangeLogoSize = viewModel::changeHeaderLogoSize
                            )

                            AppTab.QUESTIONS -> QuestionsScreen(
                                doc = doc,
                                onToggleHeaderLock = viewModel::toggleHeaderLock,
                                onApplySubjectPreset = viewModel::applySubjectPreset,
                                onAddQuestion = viewModel::addQuestion,
                                onUpdateQuestion = { qid, tr -> viewModel.updateQuestion(qid, recordUndo = false, transform = tr) },
                                onDeleteQuestion = viewModel::deleteQuestion,
                                onMoveQuestion = viewModel::moveQuestion,
                                onAddQuestionItem = viewModel::addQuestionItem,
                                onDeleteQuestionItem = viewModel::deleteQuestionItem,
                                onMoveQuestionToPage = viewModel::moveQuestionToPage,
                                onSplitQuestionToNextPage = viewModel::splitQuestionToNextPage
                            )

                            AppTab.A4_PAPER -> ExamPaperScreen(
                                uiState = uiState,
                                onSelectPage = viewModel::selectPage,
                                onAddPage = viewModel::addPage,
                                onDeletePage = viewModel::deletePage,
                                onTogglePageHeader = viewModel::togglePageHeader,
                                onToggleHeaderLock = viewModel::toggleHeaderLock,
                                onUpdateStyle = viewModel::updateStyle,
                                onSetZoom = viewModel::setZoom,
                                onAddFreeBox = viewModel::addFloatingTextBox,
                                onAddMarkCircle = viewModel::addFloatingMarkCircle,
                                onAddMedCircle = viewModel::addFloatingMedCircle,
                                onAddTable = viewModel::addFloatingTable,
                                onAddImage = viewModel::addFloatingImage,
                                onAddShape = viewModel::addFloatingShape,
                                onAddNumberLine = viewModel::addFloatingNumberLine,
                                onAddCoordGrid = viewModel::addFloatingCoordGrid,
                                onTogglePen = viewModel::togglePen,
                                onSetPenMode = viewModel::setPenMode,
                                onSetPenColor = viewModel::setPenColor,
                                onSetPenWidth = viewModel::setPenWidth,
                                onSetPenDashed = viewModel::setPenDashed,
                                onAddPenStroke = viewModel::addPenStroke,
                                onUndoPenStroke = viewModel::undoPenStroke,
                                onClearPenStrokes = viewModel::clearPenStrokes,
                                onSelectFloating = viewModel::selectFloating,
                                onUpdateFloating = { pi, fid, tr -> viewModel.updateFloatingElement(pi, fid, false, tr) },
                                onDeleteFloating = viewModel::deleteFloatingElement,
                                onMoveQuestion = viewModel::moveQuestion,
                                onMoveQuestionPage = viewModel::moveQuestionToPage,
                                onSplitQuestion = viewModel::splitQuestionToNextPage,
                                onUpdateQuestion = { qid, tr -> viewModel.updateQuestion(qid, recordUndo = false, transform = tr) },
                                onDeleteQuestion = viewModel::deleteQuestion,
                                onAddQuestionItem = viewModel::addQuestionItem,
                                onDeleteQuestionItem = viewModel::deleteQuestionItem,
                                onUpdateFreeWrite = viewModel::updatePageFreeWrite
                            )

                            AppTab.CERTIFICATES -> CertificateStudioScreen(
                                cert = uiState.certificate,
                                onApplyTemplate = viewModel::applyCertificateTemplate,
                                onUpdateCert = viewModel::updateCertificate,
                                onAddImage = viewModel::addCertImage,
                                onAddSnippet = viewModel::addCertSnippet,
                                onUpdateFloating = viewModel::updateCertFloating,
                                onDeleteFloating = viewModel::deleteCertFloating,
                                onPrintPdf = { ExamPdfExporter.printCertificate(context, uiState.certificate) },
                                onSharePdf = { ExamPdfExporter.shareCertificatePdf(context, uiState.certificate) },
                                onExportWord = {
                                    pendingWordHtml = WordDocExporter.buildCertificateWordHtml(uiState.certificate)
                                    wordExportLauncher.launch("Nadiyar-Certificate.doc")
                                }
                            )
                        }
                    }
                }
            }

            // Export / Share Dialog
            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text("📘 Word / 📄 PDF / 🔗 ناردن", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    showExportDialog = false
                                    if (uiState.activeTab == AppTab.CERTIFICATES) {
                                        ExamPdfExporter.printCertificate(context, uiState.certificate)
                                    } else {
                                        ExamPdfExporter.printExam(context, doc)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("🖨️ چاپ / خەزن وەک PDF") }

                            Button(
                                onClick = {
                                    showExportDialog = false
                                    if (uiState.activeTab == AppTab.CERTIFICATES) {
                                        ExamPdfExporter.shareCertificatePdf(context, uiState.certificate)
                                    } else {
                                        ExamPdfExporter.shareExamPdf(context, doc)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VioletMath),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("📤 ناردنا فایلێ PDF") }

                            Button(
                                onClick = {
                                    showExportDialog = false
                                    pendingWordHtml = if (uiState.activeTab == AppTab.CERTIFICATES) {
                                        WordDocExporter.buildCertificateWordHtml(uiState.certificate)
                                    } else {
                                        WordDocExporter.buildExamWordHtml(doc)
                                    }
                                    val name = if (uiState.activeTab == AppTab.CERTIFICATES) "Nadiyar-Certificate.doc" else "Nadiyar-Exam.doc"
                                    wordExportLauncher.launch(name)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("📘 خەزنکرن وەک Word (.doc)") }

                            FilledTonalButton(
                                onClick = {
                                    showExportDialog = false
                                    val html = if (uiState.activeTab == AppTab.CERTIFICATES) {
                                        WordDocExporter.buildCertificateWordHtml(uiState.certificate)
                                    } else {
                                        WordDocExporter.buildExamWordHtml(doc)
                                    }
                                    val name = if (uiState.activeTab == AppTab.CERTIFICATES) "Nadiyar-Certificate.doc" else "Nadiyar-Exam.doc"
                                    WordDocExporter.shareDocFile(context, name, html, "application/msword")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("🔗 ناردنا فایلێ Word") }

                            FilledTonalButton(
                                onClick = {
                                    showExportDialog = false
                                    val html = if (uiState.activeTab == AppTab.CERTIFICATES) {
                                        WordDocExporter.buildCertificateWordHtml(uiState.certificate)
                                    } else {
                                        WordDocExporter.buildExamWordHtml(doc)
                                    }
                                    val plain = androidx.core.text.HtmlCompat.fromHtml(
                                        html,
                                        androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
                                    ).toString()
                                    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    cm?.setPrimaryClip(android.content.ClipData.newHtmlText("Nadiyar", plain, html))
                                    viewModel.showMessage("✓ کۆپی بوو! نوکە هەرە ناڤ Wordـی و Paste بکە.")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("copy_word_button")
                            ) { Text("📋 کۆپی بۆ Word") }

                            OutlinedButton(
                                onClick = {
                                    showExportDialog = false
                                    jsonExportLauncher.launch("Nadiyar-Exam.json")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("📥 هەڵگرتنا فایلێ JSON") }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showExportDialog = false }) { Text("داخستن") }
                    }
                )
            }

            // Reset Confirmation Dialog
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("🗑 ئەسیلەکا نوی", fontWeight = FontWeight.Bold) },
                    text = { Text("هەمی پرسیار دێ ژێ چن، پشتڕاستی؟") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showResetDialog = false
                                viewModel.resetAll()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
                        ) { Text("بەلێ، ژێ ببە") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) { Text("نەخێر") }
                    }
                )
            }
        }
    }
}

private data class NavItemSpec(
    val label: String,
    val selected: Boolean,
    val tag: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)

@Composable
private fun ExamNavItems(
    activeTab: AppTab,
    questionCount: Int,
    onSelectTab: (AppTab) -> Unit
): List<NavItemSpec> = listOf(
    NavItemSpec(
        label = "١. کلێشە",
        selected = activeTab == AppTab.HEADER,
        tag = "tab_header",
        icon = {
            Icon(
                if (activeTab == AppTab.HEADER) Icons.Filled.Badge else Icons.Outlined.Badge,
                contentDescription = "کلێشە و لۆگۆ"
            )
        },
        onClick = { onSelectTab(AppTab.HEADER) }
    ),
    NavItemSpec(
        label = "٢. پرسیار ($questionCount)",
        selected = activeTab == AppTab.QUESTIONS,
        tag = "tab_questions",
        icon = {
            Icon(
                if (activeTab == AppTab.QUESTIONS) Icons.Filled.EditNote else Icons.Outlined.EditNote,
                contentDescription = "پرسیار"
            )
        },
        onClick = { onSelectTab(AppTab.QUESTIONS) }
    ),
    NavItemSpec(
        label = "٣. پەڕێ A4",
        selected = activeTab == AppTab.A4_PAPER,
        tag = "tab_a4",
        icon = {
            Icon(
                if (activeTab == AppTab.A4_PAPER) Icons.Filled.Description else Icons.Outlined.Description,
                contentDescription = "پەڕێ A4"
            )
        },
        onClick = { onSelectTab(AppTab.A4_PAPER) }
    ),
    NavItemSpec(
        label = "٤. باوەرنامە",
        selected = activeTab == AppTab.CERTIFICATES,
        tag = "tab_certificates",
        icon = {
            Icon(
                if (activeTab == AppTab.CERTIFICATES) Icons.Filled.WorkspacePremium else Icons.Outlined.WorkspacePremium,
                contentDescription = "باوەرنامە و قالب"
            )
        },
        onClick = { onSelectTab(AppTab.CERTIFICATES) }
    )
)
