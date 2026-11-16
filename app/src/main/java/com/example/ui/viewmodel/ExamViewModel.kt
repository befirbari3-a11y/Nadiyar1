package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ExamJsonCodec
import com.example.data.ExamRepository
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExamStudioUiState(
    val activeTab: AppTab = AppTab.QUESTIONS,
    val activePageIndex: Int = 0,
    val autoSaveEnabled: Boolean = true,
    val saveStatusText: String = "🔒 ل دەف تە",
    val penEnabled: Boolean = false,
    val penMode: PenMode = PenMode.SMART,
    val penColorHex: String = "#000000",
    val penWidthDp: Float = 2f,
    val penDashed: Boolean = false,
    val selectedFloatingId: String? = null,
    val paperZoom: Float = 1.0f,
    val document: ExamDocumentState = LocalizationPresets.defaultDocumentState(),
    val certificate: CertificateState = LocalizationPresets.certificateTemplates().first().state,
    val snackbarMessage: String? = null
)

class ExamViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExamRepository(AppDatabase.getInstance(application).examDao())
    private val _uiState = MutableStateFlow(ExamStudioUiState())
    val uiState: StateFlow<ExamStudioUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<ExamDocumentState>()
    private var autoSaveJob: Job? = null
    private var lastDebouncedUndoMs: Long = 0L

    init {
        viewModelScope.launch {
            val loadedDoc = repository.loadExamDocument()
            val loadedCert = repository.loadCertificate()
            _uiState.update {
                it.copy(
                    document = loadedDoc,
                    certificate = loadedCert,
                    activePageIndex = 0
                )
            }
        }
    }

    private fun pushUndo() {
        val current = _uiState.value.document
        if (undoStack.lastOrNull() != current) {
            undoStack.addLast(current)
            if (undoStack.size > 40) undoStack.removeFirst()
        }
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull()
        if (prev == null) {
            showMessage("چ گوهۆڕینەکا دی بۆ زڤڕاندنێ نینە")
            return
        }
        _uiState.update {
            it.copy(
                document = prev,
                activePageIndex = it.activePageIndex.coerceIn(0, prev.pages.lastIndex.coerceAtLeast(0))
            )
        }
        scheduleAutoSave()
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(activeTab = tab, selectedFloatingId = null) }
    }

    fun setAutoSave(enabled: Boolean) {
        _uiState.update { it.copy(autoSaveEnabled = enabled) }
        if (enabled) scheduleAutoSave()
    }

    fun saveNow(explicit: Boolean = false) {
        viewModelScope.launch {
            repository.saveExamDocument(_uiState.value.document)
            repository.saveCertificate(_uiState.value.certificate)
            _uiState.update {
                it.copy(
                    saveStatusText = if (explicit) "✓ هاتە خەزنکرن!" else "🔒 ل دەف تە",
                    snackbarMessage = if (explicit) "✓ کارێ تە هاتە خەزنکرن!" else it.snackbarMessage
                )
            }
        }
    }

    private fun scheduleAutoSave() {
        if (!_uiState.value.autoSaveEnabled) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(550)
            repository.saveExamDocument(_uiState.value.document)
            _uiState.update { it.copy(saveStatusText = "🔒 ل دەف تە") }
        }
    }

    private fun updateDocument(recordUndo: Boolean = true, transform: (ExamDocumentState) -> ExamDocumentState) {
        if (recordUndo) {
            pushUndo()
        } else {
            val now = System.currentTimeMillis()
            if (now - lastDebouncedUndoMs > 1800L) {
                pushUndo()
                lastDebouncedUndoMs = now
            }
        }
        _uiState.update { state ->
            val updated = transform(state.document)
            state.copy(
                document = updated,
                activePageIndex = state.activePageIndex.coerceIn(0, updated.pages.lastIndex.coerceAtLeast(0))
            )
        }
        scheduleAutoSave()
    }

    /* Language & Numerals */
    fun setLanguage(lang: ExamLanguage) {
        updateDocument { doc ->
            val d = LocalizationPresets.strings(lang)
            val newHeader = if (doc.header.locked) {
                doc.header
            } else {
                val updatedVals = doc.header.values.toMutableMap()
                HeaderFieldKey.entries.forEach { k ->
                    if (k != HeaderFieldKey.SCR) {
                        val def = d.headerDefaults[k]
                        if (def != null) {
                            updatedVals[k] = LocalizationPresets.formatNumber(def, doc.numeralMode, lang)
                        }
                    }
                }
                doc.header.copy(values = updatedVals)
            }
            val newQuestions = doc.questions.map { q ->
                if (q.type.isNoHeader) {
                    q.copy(
                        items = q.items.map { itObj ->
                            if (itObj.isDefault && q.type == QuestionType.WRITE) itObj.copy(text = d.defaultWrite)
                            else itObj
                        }
                    )
                } else {
                    q.copy(
                        title = if (q.isDefaultTitle) d.defaultTitles[q.type] ?: q.title else q.title,
                        marks = if (q.isDefaultMarks) LocalizationPresets.formatNumber("10", doc.numeralMode, lang) else q.marks,
                        conceptCenter = if (q.type == QuestionType.CONCEPT && q.isDefaultConcept) d.conceptCenter else q.conceptCenter,
                        conceptBranches = if (q.type == QuestionType.CONCEPT && q.isDefaultConcept) d.conceptBranches else q.conceptBranches,
                        items = q.items.map { itObj ->
                            val newText = if (itObj.isDefault) LocalizationPresets.defaultItemText(q.type, lang) else itObj.text
                            val newOpts = itObj.options.mapIndexed { idx, op ->
                                if (op.isDefault) op.copy(text = "${d.optionWord} ${LocalizationPresets.formatNumber(idx + 1, doc.numeralMode, lang)}")
                                else op
                            }
                            itObj.copy(text = newText, options = newOpts)
                        }
                    )
                }
            }
            val newStyle = if (lang == ExamLanguage.ENGLISH && doc.style.fontKey == "scheherazade") {
                doc.style.copy(fontKey = "times")
            } else if (lang != ExamLanguage.ENGLISH && doc.style.fontKey == "times") {
                doc.style.copy(fontKey = "scheherazade")
            } else doc.style

            doc.copy(
                language = lang,
                header = newHeader,
                questions = newQuestions,
                style = newStyle
            )
        }
    }

    fun setNumeralMode(mode: NumeralMode) {
        updateDocument { doc ->
            val fmtVals = doc.header.values.toMutableMap()
            listOf(HeaderFieldKey.YR, HeaderFieldKey.DAT, HeaderFieldKey.TIM, HeaderFieldKey.SCR).forEach { k ->
                fmtVals[k] = LocalizationPresets.formatNumber(fmtVals[k].orEmpty(), mode, doc.language)
            }
            val fmtQs = doc.questions.map { q ->
                q.copy(
                    marks = LocalizationPresets.formatNumber(q.marks, mode, doc.language),
                    items = q.items.map { itObj ->
                        itObj.copy(
                            options = itObj.options.mapIndexed { idx, op ->
                                if (op.isDefault) {
                                    val s = LocalizationPresets.strings(doc.language)
                                    op.copy(text = "${s.optionWord} ${LocalizationPresets.formatNumber(idx + 1, mode, doc.language)}")
                                } else op
                            }
                        )
                    }
                )
            }
            doc.copy(numeralMode = mode, header = doc.header.copy(values = fmtVals), questions = fmtQs)
        }
    }

    /* Header & Logo */
    fun toggleHeaderLock() {
        updateDocument { doc ->
            doc.copy(header = doc.header.copy(locked = !doc.header.locked))
        }
    }

    fun updateHeaderField(key: HeaderFieldKey, value: String) {
        if (_uiState.value.document.header.locked) return
        updateDocument(recordUndo = false) { doc ->
            val m = doc.header.values.toMutableMap()
            m[key] = value
            doc.copy(header = doc.header.copy(values = m))
        }
    }

    fun toggleHeaderFieldVisibility(key: HeaderFieldKey, visible: Boolean) {
        if (_uiState.value.document.header.locked) return
        updateDocument { doc ->
            val v = doc.header.visible.toMutableMap()
            v[key] = visible
            doc.copy(header = doc.header.copy(visible = v))
        }
    }

    fun resetHeader() {
        updateDocument { doc ->
            val d = LocalizationPresets.strings(doc.language)
            doc.copy(
                header = HeaderState(
                    values = d.headerDefaults,
                    visible = HeaderFieldKey.entries.associateWith { true },
                    locked = false,
                    logoBase64 = doc.header.logoBase64,
                    logoPos = doc.header.logoPos,
                    logoSizeDp = doc.header.logoSizeDp
                ),
                pages = doc.pages.mapIndexed { idx, p -> if (idx == 0) p.copy(showHeader = true) else p }
            )
        }
    }

    fun setHeaderLogo(base64: String) {
        if (_uiState.value.document.header.locked) {
            showMessage("کلێشە قفلە، بەری گوهۆڕینێ ڤەکە.")
            return
        }
        updateDocument { doc -> doc.copy(header = doc.header.copy(logoBase64 = base64)) }
    }

    fun setHeaderLogoPos(pos: LogoPosition) {
        if (_uiState.value.document.header.locked) return
        updateDocument { doc -> doc.copy(header = doc.header.copy(logoPos = pos)) }
    }

    fun changeHeaderLogoSize(delta: Int) {
        if (_uiState.value.document.header.locked) return
        updateDocument { doc ->
            val newSize = (doc.header.logoSizeDp + delta).coerceIn(28, 120)
            doc.copy(header = doc.header.copy(logoSizeDp = newSize))
        }
    }

    fun saveHeaderPreset() {
        viewModelScope.launch {
            repository.saveHeaderPreset(_uiState.value.document.header)
            showMessage("✓ کلێشە و لۆگۆ بۆ هەردەم هاتنە خەزنکرن!")
        }
    }

    fun loadHeaderPreset() {
        viewModelScope.launch {
            val preset = repository.loadHeaderPreset(_uiState.value.document.language)
            if (preset == null) {
                showMessage("هێشتا چ کلێشەکا خەزنکری نینە.")
            } else {
                updateDocument { doc -> doc.copy(header = preset.copy(locked = false)) }
                showMessage("✓ کلێشێ خەزنکری هاتە گەڕاندنەڤە")
            }
        }
    }

    /* Questions */
    fun addQuestion(type: QuestionType) {
        if (type == QuestionType.TABLE) {
            selectTab(AppTab.A4_PAPER)
            addFloatingTable(3, 2)
            return
        }
        val pg = _uiState.value.activePageIndex.coerceIn(0, _uiState.value.document.pages.lastIndex)
        updateDocument { doc ->
            val q = LocalizationPresets.createQuestion(type, doc.language, doc.numeralMode, pg)
            doc.copy(questions = doc.questions + q)
        }
    }

    fun applySubjectPreset(preset: SubjectPreset) {
        val pg = _uiState.value.activePageIndex.coerceIn(0, _uiState.value.document.pages.lastIndex)
        updateDocument { doc ->
            val added = preset.builder(doc.language, doc.numeralMode, pg)
            doc.copy(questions = doc.questions + added)
        }
        showMessage("✓ پرسیارێن ${preset.title} هاتنە زێدەکرن")
    }

    fun updateQuestion(questionId: String, recordUndo: Boolean = false, transform: (Question) -> Question) {
        updateDocument(recordUndo = recordUndo) { doc ->
            doc.copy(questions = doc.questions.map { if (it.id == questionId) transform(it) else it })
        }
    }

    fun deleteQuestion(questionId: String) {
        updateDocument { doc ->
            doc.copy(questions = doc.questions.filterNot { it.id == questionId })
        }
    }

    fun moveQuestion(questionId: String, delta: Int) {
        updateDocument { doc ->
            val idx = doc.questions.indexOfFirst { it.id == questionId }
            val target = idx + delta
            if (idx < 0 || target !in doc.questions.indices) return@updateDocument doc
            val list = doc.questions.toMutableList()
            val item = list.removeAt(idx)
            list.add(target, item)
            doc.copy(questions = list)
        }
    }

    fun addQuestionItem(questionId: String) {
        updateDocument { doc ->
            val d = LocalizationPresets.strings(doc.language)
            doc.copy(questions = doc.questions.map { q ->
                if (q.id != questionId) q
                else if (q.type == QuestionType.CONCEPT) {
                    q.copy(conceptBranches = q.conceptBranches + "...", isDefaultConcept = false)
                } else {
                    val newItem = QuestionItem(
                        text = LocalizationPresets.defaultItemText(q.type, doc.language),
                        matchRight = "............",
                        isDefault = true,
                        options = if (q.type == QuestionType.MCQ) {
                            List(4) { i ->
                                McqOption(
                                    text = "${d.optionWord} ${LocalizationPresets.formatNumber(i + 1, doc.numeralMode, doc.language)}",
                                    isDefault = true
                                )
                            }
                        } else emptyList()
                    )
                    q.copy(items = q.items + newItem)
                }
            })
        }
    }

    fun deleteQuestionItem(questionId: String, itemIndex: Int) {
        updateDocument { doc ->
            doc.copy(questions = doc.questions.map { q ->
                if (q.id != questionId) q
                else if (q.type == QuestionType.CONCEPT) {
                    q.copy(conceptBranches = q.conceptBranches.filterIndexed { i, _ -> i != itemIndex })
                } else {
                    q.copy(items = q.items.filterIndexed { i, _ -> i != itemIndex })
                }
            })
        }
    }

    fun moveQuestionToPage(questionId: String, targetPage: Int) {
        updateDocument { doc ->
            val pages = doc.pages.toMutableList()
            while (targetPage >= pages.size) {
                pages.add(ExamPage(showHeader = false))
            }
            doc.copy(
                pages = pages,
                questions = doc.questions.map { if (it.id == questionId) it.copy(pageIndex = targetPage) else it }
            )
        }
        _uiState.update { it.copy(activePageIndex = targetPage) }
    }

    fun splitQuestionToNextPage(questionId: String, nextPage: Int) {
        val q = _uiState.value.document.questions.firstOrNull { it.id == questionId } ?: return
        val count = if (q.type == QuestionType.CONCEPT) q.conceptBranches.size else q.items.size
        if (count < 2) {
            moveQuestionToPage(questionId, nextPage)
            return
        }
        updateDocument { doc ->
            val pages = doc.pages.toMutableList()
            while (nextPage >= pages.size) {
                pages.add(ExamPage(showHeader = false))
            }
            val half = (count + 1) / 2
            val totalM = LocalizationPresets.toEnglishDigits(q.marks).toDoubleOrNull() ?: 0.0
            val firstMark = LocalizationPresets.formatNumber((totalM / 2).toInt(), doc.numeralMode, doc.language)
            val secondMark = LocalizationPresets.formatNumber(((totalM + 1) / 2).toInt(), doc.numeralMode, doc.language)

            val list = mutableListOf<Question>()
            doc.questions.forEach { cur ->
                if (cur.id != questionId) {
                    list.add(cur)
                } else {
                    if (cur.type == QuestionType.CONCEPT) {
                        val first = cur.copy(
                            conceptBranches = cur.conceptBranches.take(half),
                            marks = if (cur.type.isNoHeader) cur.marks else firstMark
                        )
                        val second = cur.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            pageIndex = nextPage,
                            title = if (cur.type.isNoHeader) "" else "(بەردەوامیا) ${cur.title}",
                            isDefaultTitle = false,
                            marks = if (cur.type.isNoHeader) cur.marks else secondMark,
                            isDefaultMarks = false,
                            conceptBranches = cur.conceptBranches.drop(half)
                        )
                        list.add(first); list.add(second)
                    } else {
                        val first = cur.copy(
                            items = cur.items.take(half),
                            marks = if (cur.type.isNoHeader) cur.marks else firstMark
                        )
                        val second = cur.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            pageIndex = nextPage,
                            title = if (cur.type.isNoHeader) "" else "(بەردەوامیا) ${cur.title}",
                            isDefaultTitle = false,
                            marks = if (cur.type.isNoHeader) cur.marks else secondMark,
                            isDefaultMarks = false,
                            items = cur.items.drop(half)
                        )
                        list.add(first); list.add(second)
                    }
                }
            }
            doc.copy(pages = pages, questions = list)
        }
        _uiState.update { it.copy(activePageIndex = nextPage) }
    }

    /* A4 Pages & Styling */
    fun selectPage(index: Int) {
        _uiState.update {
            it.copy(activePageIndex = index.coerceIn(0, it.document.pages.lastIndex.coerceAtLeast(0)))
        }
    }

    fun addPage() {
        updateDocument { doc ->
            doc.copy(pages = doc.pages + ExamPage(showHeader = false))
        }
        _uiState.update { it.copy(activePageIndex = it.document.pages.lastIndex) }
    }

    fun deletePage(index: Int) {
        if (_uiState.value.document.pages.size <= 1) return
        updateDocument { doc ->
            val remaining = doc.pages.filterIndexed { i, _ -> i != index }
            val reindexedQs = doc.questions.map { q ->
                val newPg = when {
                    q.pageIndex > index -> q.pageIndex - 1
                    q.pageIndex == index -> (index - 1).coerceAtLeast(0)
                    else -> q.pageIndex
                }.coerceIn(0, remaining.lastIndex)
                q.copy(pageIndex = newPg)
            }
            doc.copy(pages = remaining, questions = reindexedQs)
        }
    }

    fun togglePageHeader(index: Int) {
        updateDocument { doc ->
            doc.copy(pages = doc.pages.mapIndexed { i, p -> if (i == index) p.copy(showHeader = !p.showHeader) else p })
        }
    }

    fun updatePageFreeWrite(index: Int, text: String) {
        updateDocument(recordUndo = false) { doc ->
            doc.copy(pages = doc.pages.mapIndexed { i, p -> if (i == index) p.copy(freeWriteText = text) else p })
        }
    }

    fun updateStyle(transform: (ExamStyle) -> ExamStyle) {
        updateDocument(recordUndo = false) { doc -> doc.copy(style = transform(doc.style)) }
    }

    fun setZoom(zoom: Float) {
        _uiState.update { it.copy(paperZoom = zoom.coerceIn(0.6f, 2.0f)) }
    }

    /* Smart Pen */
    fun togglePen() {
        _uiState.update { it.copy(penEnabled = !it.penEnabled, selectedFloatingId = null) }
    }

    fun setPenMode(mode: PenMode) {
        _uiState.update { it.copy(penMode = mode) }
    }

    fun setPenColor(hex: String) {
        _uiState.update { it.copy(penColorHex = hex) }
    }

    fun setPenWidth(w: Float) {
        _uiState.update { it.copy(penWidthDp = w.coerceIn(1f, 8f)) }
    }

    fun setPenDashed(dashed: Boolean) {
        _uiState.update { it.copy(penDashed = dashed) }
    }

    fun addPenStroke(pageIndex: Int, stroke: PenStroke) {
        updateDocument { doc ->
            doc.copy(pages = doc.pages.mapIndexed { i, p ->
                if (i == pageIndex) p.copy(penStrokes = p.penStrokes + stroke) else p
            })
        }
    }

    fun undoPenStroke(pageIndex: Int) {
        updateDocument { doc ->
            doc.copy(pages = doc.pages.mapIndexed { i, p ->
                if (i == pageIndex && p.penStrokes.isNotEmpty()) p.copy(penStrokes = p.penStrokes.dropLast(1)) else p
            })
        }
    }

    fun clearPenStrokes(pageIndex: Int) {
        updateDocument { doc ->
            doc.copy(pages = doc.pages.mapIndexed { i, p ->
                if (i == pageIndex) p.copy(penStrokes = emptyList()) else p
            })
        }
    }

    /* Floating Elements on Exam A4 */
    fun selectFloating(id: String?) {
        _uiState.update { it.copy(selectedFloatingId = id) }
    }

    private fun addFloatingToActivePage(el: FloatingElement) {
        val pgIdx = _uiState.value.activePageIndex
        updateDocument { doc ->
            doc.copy(pages = doc.pages.mapIndexed { i, p ->
                if (i == pgIdx) p.copy(floatingElements = p.floatingElements + el) else p
            })
        }
        _uiState.update { it.copy(selectedFloatingId = el.id) }
    }

    fun addFloatingTextBox() {
        addFloatingToActivePage(
            FloatingElement(
                type = FloatingElementType.TEXT_BOX,
                xFrac = 0.1f, yFrac = 0.25f, wFrac = 0.42f, hFrac = 0.08f,
                text = "پرسیار یان نڤێسینا خۆ ل ڤێرە بنڤێسە..."
            )
        )
    }

    fun addFloatingMarkCircle() {
        val doc = _uiState.value.document
        val ten = LocalizationPresets.formatNumber("10", doc.numeralMode, doc.language)
        addFloatingToActivePage(
            FloatingElement(
                type = FloatingElementType.MARK_CIRCLE,
                xFrac = 0.12f, yFrac = 0.24f, wFrac = 0.12f, hFrac = 0.085f,
                text = ten, secondaryText = ten, fontSizeSp = 15f
            )
        )
    }

    fun addFloatingMedCircle() {
        addFloatingToActivePage(
            FloatingElement(
                type = FloatingElementType.MED_CIRCLE,
                xFrac = 0.35f, yFrac = 0.25f, wFrac = 0.22f, hFrac = 0.15f,
                text = "نڤێسین ل ڤێرە...", strokeColorHex = "#0D9488", bgColorHex = "#F0FDFA"
            )
        )
    }

    fun addFloatingTable(cols: Int, rows: Int) {
        val doc = _uiState.value.document
        val table = List(rows) { r ->
            List(cols) { c ->
                if (r == 0) LocalizationPresets.formatNumber(c + 1, doc.numeralMode, doc.language) else ""
            }
        }
        addFloatingToActivePage(
            FloatingElement(
                type = FloatingElementType.TABLE,
                xFrac = 0.1f, yFrac = 0.26f,
                wFrac = (cols * 0.14f).coerceIn(0.28f, 0.8f),
                hFrac = (rows * 0.045f).coerceIn(0.08f, 0.4f),
                tableRows = table
            )
        )
    }

    fun addFloatingImage(base64: String) {
        addFloatingToActivePage(
            FloatingElement(
                type = FloatingElementType.IMAGE,
                xFrac = 0.15f, yFrac = 0.26f, wFrac = 0.28f, hFrac = 0.18f,
                imageBase64 = base64
            )
        )
    }

    fun addFloatingShape(kind: ShapeKind) {
        val isV = kind == ShapeKind.VLINE || kind == ShapeKind.VARROW
        val isH = kind == ShapeKind.LINE || kind == ShapeKind.SLINE || kind == ShapeKind.ARROW ||
            kind == ShapeKind.LARROW || kind == ShapeKind.RAY
        val isD = kind == ShapeKind.DLINE
        val w = when {
            isV -> 0.07f
            isH || kind == ShapeKind.RECT -> 0.32f
            isD -> 0.2f
            else -> 0.2f
        }
        val h = when {
            isV -> 0.2f
            isH -> 0.05f
            kind == ShapeKind.RECT -> 0.12f
            isD -> 0.14f
            else -> 0.14f
        }
        addFloatingToActivePage(
            FloatingElement(
                type = FloatingElementType.SHAPE,
                xFrac = 0.2f, yFrac = 0.28f, wFrac = w, hFrac = h,
                shapeKind = kind,
                text = if (isV || isH || isD) "" else "..."
            )
        )
    }

    fun addFloatingNumberLine() {
        addFloatingToActivePage(
            FloatingElement(
                type = FloatingElementType.NUMBER_LINE,
                xFrac = 0.15f, yFrac = 0.3f, wFrac = 0.48f, hFrac = 0.075f,
                text = "-3   -2   -1   0   1   2   3"
            )
        )
    }

    fun addFloatingCoordGrid() {
        addFloatingToActivePage(
            FloatingElement(
                type = FloatingElementType.COORD_GRID,
                xFrac = 0.2f, yFrac = 0.28f, wFrac = 0.3f, hFrac = 0.21f
            )
        )
    }

    fun updateFloatingElement(pageIndex: Int, elementId: String, recordUndo: Boolean = false, transform: (FloatingElement) -> FloatingElement) {
        updateDocument(recordUndo = recordUndo) { doc ->
            doc.copy(pages = doc.pages.mapIndexed { i, p ->
                if (i == pageIndex) p.copy(floatingElements = p.floatingElements.map { if (it.id == elementId) transform(it) else it })
                else p
            })
        }
    }

    fun deleteFloatingElement(pageIndex: Int, elementId: String) {
        updateDocument { doc ->
            doc.copy(pages = doc.pages.mapIndexed { i, p ->
                if (i == pageIndex) p.copy(floatingElements = p.floatingElements.filterNot { it.id == elementId }) else p
            })
        }
        _uiState.update { if (it.selectedFloatingId == elementId) it.copy(selectedFloatingId = null) else it }
    }

    /* JSON Import / Export & Reset */
    fun exportJsonString(): String = ExamJsonCodec.encodeDocument(_uiState.value.document)

    fun importJsonString(json: String) {
        runCatching {
            val decoded = ExamJsonCodec.decodeDocument(json)
            updateDocument { decoded }
            showMessage("✓ فایل ب سەرکەفتنێ هاتە ڤەکرن")
        }.onFailure {
            showMessage("فایل دروست نینە")
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            repository.clearExamDocument()
            val fresh = LocalizationPresets.defaultDocumentState()
            val firstQ = LocalizationPresets.createQuestion(QuestionType.MCQ, fresh.language, fresh.numeralMode, 0)
            val doc = fresh.copy(questions = listOf(firstQ))
            undoStack.clear()
            _uiState.update { it.copy(document = doc, activePageIndex = 0, selectedFloatingId = null) }
            repository.saveExamDocument(doc)
            showMessage("✓ ئەسیلەکا نوی هاتە دروستکرن")
        }
    }

    /* Certificate Studio (Independent) */
    fun applyCertificateTemplate(template: CertificateTemplate) {
        _uiState.update {
            it.copy(certificate = template.state.copy(floatingElements = it.certificate.floatingElements))
        }
        saveCertificateDebounced()
    }

    fun updateCertificate(transform: (CertificateState) -> CertificateState) {
        _uiState.update { it.copy(certificate = transform(it.certificate)) }
        saveCertificateDebounced()
    }

    fun addCertImage(base64: String) {
        val el = FloatingElement(
            type = FloatingElementType.IMAGE,
            xFrac = 0.08f, yFrac = 0.1f, wFrac = 0.16f, hFrac = 0.18f,
            imageBase64 = base64
        )
        updateCertificate { it.copy(floatingElements = it.floatingElements + el) }
    }

    fun addCertSnippet(isStamp: Boolean) {
        val el = if (isStamp) {
            FloatingElement(
                type = FloatingElementType.MED_CIRCLE,
                xFrac = 0.4f, yFrac = 0.65f, wFrac = 0.18f, hFrac = 0.18f,
                text = "مۆرا فەرمی یا قوتابخانێ",
                strokeColorHex = "#B45309", bgColorHex = "#FFFBEB"
            )
        } else {
            FloatingElement(
                type = FloatingElementType.TEXT_BOX,
                xFrac = 0.12f, yFrac = 0.68f, wFrac = 0.24f, hFrac = 0.12f,
                text = "ئیمزایا ڕێڤەبەری\n...................."
            )
        }
        updateCertificate { it.copy(floatingElements = it.floatingElements + el) }
    }

    fun updateCertFloating(id: String, transform: (FloatingElement) -> FloatingElement) {
        updateCertificate { cert ->
            cert.copy(floatingElements = cert.floatingElements.map { if (it.id == id) transform(it) else it })
        }
    }

    fun deleteCertFloating(id: String) {
        updateCertificate { cert ->
            cert.copy(floatingElements = cert.floatingElements.filterNot { it.id == id })
        }
    }

    private var certSaveJob: Job? = null
    private fun saveCertificateDebounced() {
        certSaveJob?.cancel()
        certSaveJob = viewModelScope.launch {
            delay(450)
            repository.saveCertificate(_uiState.value.certificate)
        }
    }
}
