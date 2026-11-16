package com.example.model

data class LangStrings(
    val isRtl: Boolean,
    val headerDefaults: Map<HeaderFieldKey, String>,
    val scoreLabel: String,
    val qPrefix: String,
    val defaultMark: String,
    val markWord: String,
    val mcqLabels: List<String>,
    val optionWord: String,
    val defaultItem: String,
    val defaultTf: String,
    val defaultBlank: String,
    val defaultDef: String,
    val defaultCount: String,
    val defaultEssay: String,
    val defaultImla: String,
    val defaultCallig: String,
    val defaultWrite: String,
    val matchRightLabels: List<String>,
    val conceptCenter: String,
    val conceptBranches: List<String>,
    val defaultTitles: Map<QuestionType, String>
)

data class SubjectPreset(
    val id: String,
    val title: String,
    val colorHex: Long,
    val builder: (ExamLanguage, NumeralMode, Int) -> List<Question>
)

data class CertificateTemplate(
    val id: String,
    val icon: String,
    val title: String,
    val subtitle: String,
    val accentHex: Long,
    val state: CertificateState
)

object LocalizationPresets {

    private val kurdishDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toKurdishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(kurdishDigits[ch.code - '0'.code])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatNumber(value: Any, mode: NumeralMode, lang: ExamLanguage): String {
        val raw = value.toString()
        val useEnglish = mode == NumeralMode.EN || (mode == NumeralMode.AUTO && lang == ExamLanguage.ENGLISH)
        return if (useEnglish) toEnglishDigits(raw) else toKurdishDigits(raw)
    }

    fun strings(lang: ExamLanguage): LangStrings = when (lang) {
        ExamLanguage.BADINI -> LangStrings(
            isRtl = true,
            headerDefaults = mapOf(
                HeaderFieldKey.BIS to "بسم الله الرحمن الرحیم",
                HeaderFieldKey.MIN to "وەزارەتا پەروەردێ - ڕێڤەبەریا گشتی یا پەروەردێ",
                HeaderFieldKey.SCH to "قوتابخانا: ..................",
                HeaderFieldKey.SUB to "بابەت: ............",
                HeaderFieldKey.GRD to "پۆل: ........",
                HeaderFieldKey.TCH to "مامۆستایێ وانێ: ..................",
                HeaderFieldKey.TYP to "ئەزموونا هەیڤانە",
                HeaderFieldKey.YR to "ساڵێ خواندنێ: ٢٠٢٦ - ٢٠٢٧",
                HeaderFieldKey.STU to "ناڤێ قوتابی:",
                HeaderFieldKey.DAT to "ڕێکەوت: ٢٠٢٦/١١/١٥",
                HeaderFieldKey.TIM to "کات: ٤٥ خولەک",
                HeaderFieldKey.SCR to "",
                HeaderFieldKey.FT to "دگەل داخوازیا سەرکەفتنێ بۆ هەوە"
            ),
            scoreLabel = "نمرە:",
            qPrefix = "پ",
            defaultMark = "١٠",
            markWord = "نمرە",
            mcqLabels = listOf("ا)", "ب)", "ج)", "د)"),
            optionWord = "بژاردە",
            defaultItem = "پرسیارا خۆ ل ڤێرە بنڤێسە...",
            defaultTf = "(    ) ڕستەیا ڕاست یان شاش...",
            defaultBlank = "پەیڤا گونجای ....................",
            defaultDef = "ناڤێ پێناسێ: ............",
            defaultCount = "............",
            defaultEssay = "١- بابەتێ ئێکێ...",
            defaultImla = "ڤێ دەقێ ب ئیملا بنڤێسە...",
            defaultCallig = "ب خەتەکێ جوان و پاک بنڤێسە.",
            defaultWrite = "نڤێسینا خۆ ل ڤێرە بنڤێسە...",
            matchRightLabels = listOf("أ-", "ب-", "ج-", "د-", "هـ-"),
            conceptCenter = "سەرەکی",
            conceptBranches = listOf("بەش ١", "بەش ٢", "بەش ٣"),
            defaultTitles = mapOf(
                QuestionType.MCQ to "هەڵبژاردنا ڕاست دیار بکە:",
                QuestionType.COUNT to "ئەڤان خالێن خوارێ بژمێرە:",
                QuestionType.TF to "نیشانا (✓) بۆ ڕاست و (✗) بۆ شاش دانە:",
                QuestionType.BLANK to "ڤان بۆشاییان پر بکە:",
                QuestionType.MATCH to "ستوونا (أ) ب ستوونا (ب) بگەهینە:",
                QuestionType.DEF to "پێناسا ڤان تشتان بکە:",
                QuestionType.IMG to "ل سەر ڤی وێنەی بەرسڤ بدە:",
                QuestionType.DIFF to "جیاوازیا د ناڤبەرا ڤان دا بنڤێسە:",
                QuestionType.WHY to "هۆکارێ (بۆچی) دیار بکە:",
                QuestionType.CONCEPT to "ڤێ نەخشەیا چەمکی تەمام بکە:",
                QuestionType.FREE to "بەرسڤا ڤان پرسیاران بدە:",
                QuestionType.ESSAY to "داڕشتنەکێ ل سەر ئێک ژ ڤان بابەتان بنڤێسە:",
                QuestionType.IMLA to "ئیملا (دەقێ مامۆستای بنڤێسە):",
                QuestionType.CALLIG to "ب خۆشنووس بنڤێسە:"
            )
        )

        ExamLanguage.SORANI -> LangStrings(
            isRtl = true,
            headerDefaults = mapOf(
                HeaderFieldKey.BIS to "بسم الله الرحمن الرحیم",
                HeaderFieldKey.MIN to "وەزارەتی پەروەردە - بەڕێوەبەرایەتی گشتی پەروەردە",
                HeaderFieldKey.SCH to "قوتابخانە: ..................",
                HeaderFieldKey.SUB to "بابەت: ............",
                HeaderFieldKey.GRD to "پۆل: ........",
                HeaderFieldKey.TCH to "مامۆستای وانە: ..................",
                HeaderFieldKey.TYP to "تاقیکردنەوەی مانگانە",
                HeaderFieldKey.YR to "ساڵی خوێندن: ٢٠٢٦ - ٢٠٢٧",
                HeaderFieldKey.STU to "ناوی قوتابی:",
                HeaderFieldKey.DAT to "ڕێکەوت: ٢٠٢٦/١١/١٥",
                HeaderFieldKey.TIM to "کات: ٤٥ خولەک",
                HeaderFieldKey.SCR to "",
                HeaderFieldKey.FT to "بە هیوای سەرکەوتن بۆ هەمووان"
            ),
            scoreLabel = "نمرە:",
            qPrefix = "پ",
            defaultMark = "١٠",
            markWord = "نمرە",
            mcqLabels = listOf("أ)", "ب)", "ج)", "د)"),
            optionWord = "بژاردە",
            defaultItem = "دەقی پرسیارەکەت لێرە بنووسە...",
            defaultTf = "(    ) ڕستەی ڕاست یان هەڵە...",
            defaultBlank = "وشەی گونجاو ....................",
            defaultDef = "پێناسەی ............",
            defaultCount = "............",
            defaultEssay = "١- بابەتی یەکەم...",
            defaultImla = "دەقی ئیملا لێرە بنووسە...",
            defaultCallig = "بە خۆشنووسی بینووسەوە.",
            defaultWrite = "نووسینەکەت لێرە بنووسە...",
            matchRightLabels = listOf("أ-", "ب-", "ج-", "د-", "هـ-"),
            conceptCenter = "سەرەکی",
            conceptBranches = listOf("بەش ١", "بەش ٢", "بەش ٣"),
            defaultTitles = mapOf(
                QuestionType.MCQ to "وەڵامی ڕاست هەڵبژێرە:",
                QuestionType.COUNT to "ئەمانەی خوارەوە بژمێرە:",
                QuestionType.TF to "نیشانەی (✓) بۆ ڕاست و (✗) بۆ هەڵە دابنێ:",
                QuestionType.BLANK to "ئەم بۆشاییانە پڕبکەرەوە:",
                QuestionType.MATCH to "ستوونی (أ) بە (ب) بگەیەنە:",
                QuestionType.DEF to "پێناسەی ئەمانە بکە:",
                QuestionType.IMG to "سەیری ئەم وێنەیە بکە و وەڵام بدەرەوە:",
                QuestionType.DIFF to "جیاوازی نێوان ئەمانە بنووسە:",
                QuestionType.WHY to "هۆکاری (بۆچی) ڕوونبکەرەوە:",
                QuestionType.CONCEPT to "ئەم نەخشەی چەمکە پڕبکەرەوە:",
                QuestionType.FREE to "وەڵامی ئەم پرسیارانە بدەرەوە:",
                QuestionType.ESSAY to "داڕشتنێک لەسەر یەکێک لەم بابەتانە بنووسە:",
                QuestionType.IMLA to "ئیملا:",
                QuestionType.CALLIG to "بە خۆشنووسی بینووسەوە:"
            )
        )

        ExamLanguage.ARABIC -> LangStrings(
            isRtl = true,
            headerDefaults = mapOf(
                HeaderFieldKey.BIS to "بسم الله الرحمن الرحیم",
                HeaderFieldKey.MIN to "وزارة التربية - المديرية العامة للتربية",
                HeaderFieldKey.SCH to "المدرسة: ..................",
                HeaderFieldKey.SUB to "المادة: ............",
                HeaderFieldKey.GRD to "الصف: ........",
                HeaderFieldKey.TCH to "مدرس المادة: ..................",
                HeaderFieldKey.TYP to "الامتحان الشهري",
                HeaderFieldKey.YR to "العام الدراسي: ٢٠٢٦ - ٢٠٢٧",
                HeaderFieldKey.STU to "اسم الطالب:",
                HeaderFieldKey.DAT to "التاريخ: ٢٠٢٦/١١/١٥",
                HeaderFieldKey.TIM to "الوقت: ٤٥ دقيقة",
                HeaderFieldKey.SCR to "",
                HeaderFieldKey.FT to "مع تمنياتنا لكم بالنجاح والتوفيق"
            ),
            scoreLabel = "الدرجة:",
            qPrefix = "س",
            defaultMark = "١٠",
            markWord = "درجات",
            mcqLabels = listOf("أ)", "ب)", "ج)", "د)"),
            optionWord = "الاختيار",
            defaultItem = "اكتب نص السؤال هنا...",
            defaultTf = "(    ) اكتب العبارة هنا...",
            defaultBlank = "املأ الفراغ بما يناسبه ....................",
            defaultDef = "عرّف ............",
            defaultCount = "............",
            defaultEssay = "١- الموضوع الأول...",
            defaultImla = "اكتب نص الإملاء هنا...",
            defaultCallig = "اكتب العبارة الآتية بخط جميل وواضح.",
            defaultWrite = "اكتب النص هنا...",
            matchRightLabels = listOf("أ-", "ب-", "ج-", "د-", "هـ-"),
            conceptCenter = "المفهوم الرئيس",
            conceptBranches = listOf("فرع ١", "فرع ٢", "فرع ٣"),
            defaultTitles = mapOf(
                QuestionType.MCQ to "اختر الإجابة الصحيحة مما يأتي:",
                QuestionType.COUNT to "عدّد ما يأتي:",
                QuestionType.TF to "ضع علامة (✓) أمام العبارة الصحيحة وعلامة (✗) أمام العبارة الخاطئة:",
                QuestionType.BLANK to "املأ الفراغات الآتية بما يناسبها:",
                QuestionType.MATCH to "صل ما في القائمة (أ) بما يناسبه في القائمة (ب):",
                QuestionType.DEF to "عرّف كلاً مما يأتي:",
                QuestionType.IMG to "تأمل الصورة الآتية ثم أجب عن الأسئلة:",
                QuestionType.DIFF to "ما الفرق بين كل مما يأتي:",
                QuestionType.WHY to "علل ما يأتي (اذكر السبب):",
                QuestionType.CONCEPT to "أكمل خريطة المفاهيم الآتية:",
                QuestionType.FREE to "أجب عن الأسئلة الآتية:",
                QuestionType.ESSAY to "اكتب موضوعاً إنشائياً في أحد المواضيع الآتية:",
                QuestionType.IMLA to "الإملاء (اكتب ما يُملى عليك):",
                QuestionType.CALLIG to "اكتب العبارة الآتية بخط جميل وواضح:"
            )
        )

        ExamLanguage.ENGLISH -> LangStrings(
            isRtl = false,
            headerDefaults = mapOf(
                HeaderFieldKey.BIS to "In the Name of Allah",
                HeaderFieldKey.MIN to "Ministry of Education - General Directorate",
                HeaderFieldKey.SCH to "School: ..................",
                HeaderFieldKey.SUB to "Subject: ............",
                HeaderFieldKey.GRD to "Grade: ........",
                HeaderFieldKey.TCH to "Teacher: ..................",
                HeaderFieldKey.TYP to "Monthly Exam",
                HeaderFieldKey.YR to "Academic Year: 2026 - 2027",
                HeaderFieldKey.STU to "Student Name:",
                HeaderFieldKey.DAT to "Date: 2026/11/15",
                HeaderFieldKey.TIM to "Time: 45 min",
                HeaderFieldKey.SCR to "",
                HeaderFieldKey.FT to "With Best Wishes for Success"
            ),
            scoreLabel = "Mark:",
            qPrefix = "Q",
            defaultMark = "10",
            markWord = "Marks",
            mcqLabels = listOf("A)", "B)", "C)", "D)"),
            optionWord = "Option",
            defaultItem = "Write your question here...",
            defaultTf = "(    ) Write True or False statement here...",
            defaultBlank = "Fill in the blank ....................",
            defaultDef = "Define ............",
            defaultCount = "............",
            defaultEssay = "1- First topic...",
            defaultImla = "Write the dictation text here...",
            defaultCallig = "Rewrite the following sentence neatly.",
            defaultWrite = "Write your text here...",
            matchRightLabels = listOf("A-", "B-", "C-", "D-", "E-"),
            conceptCenter = "Main Topic",
            conceptBranches = listOf("Branch 1", "Branch 2", "Branch 3"),
            defaultTitles = mapOf(
                QuestionType.MCQ to "Choose the correct answer from the following:",
                QuestionType.COUNT to "List / Enumerate the following:",
                QuestionType.TF to "Put (✓) for True and (✗) for False:",
                QuestionType.BLANK to "Fill in the blanks with suitable words:",
                QuestionType.MATCH to "Match Column (A) with Column (B):",
                QuestionType.DEF to "Define the following terms:",
                QuestionType.IMG to "Look at the picture and answer the questions:",
                QuestionType.DIFF to "State the differences between:",
                QuestionType.WHY to "Give reasons for the following (Why):",
                QuestionType.CONCEPT to "Complete the following concept map:",
                QuestionType.FREE to "Answer the following questions:",
                QuestionType.ESSAY to "Write an essay on one of the following topics:",
                QuestionType.IMLA to "Dictation:",
                QuestionType.CALLIG to "Rewrite the following sentence neatly:"
            )
        )
    }

    fun defaultItemText(type: QuestionType, lang: ExamLanguage): String {
        val s = strings(lang)
        return when (type) {
            QuestionType.TF -> s.defaultTf
            QuestionType.BLANK -> s.defaultBlank
            QuestionType.DEF -> s.defaultDef
            QuestionType.COUNT -> s.defaultCount
            QuestionType.ESSAY -> s.defaultEssay
            QuestionType.IMLA -> s.defaultImla
            QuestionType.CALLIG -> s.defaultCallig
            QuestionType.WRITE -> s.defaultWrite
            QuestionType.SPACER -> "................................................................................"
            else -> s.defaultItem
        }
    }

    fun createQuestion(type: QuestionType, lang: ExamLanguage, mode: NumeralMode, pageIndex: Int): Question {
        val s = strings(lang)
        val noHdr = type.isNoHeader
        val mark = if (noHdr) "0" else formatNumber("10", mode, lang)
        val title = if (noHdr) "" else (s.defaultTitles[type] ?: s.defaultTitles[QuestionType.FREE].orEmpty())
        val items = when (type) {
            QuestionType.MATCH -> List(3) { QuestionItem(text = "............", matchRight = "............", isDefault = true) }
            QuestionType.CONCEPT -> emptyList()
            QuestionType.IMG -> listOf(QuestionItem(text = s.defaultItem, isDefault = true))
            QuestionType.ESSAY, QuestionType.IMLA, QuestionType.WRITE, QuestionType.SPACER ->
                listOf(QuestionItem(text = defaultItemText(type, lang), isDefault = true))
            else -> {
                val count = if (type == QuestionType.COUNT) 4 else 2
                List(count) {
                    QuestionItem(
                        text = defaultItemText(type, lang),
                        isDefault = true,
                        options = if (type == QuestionType.MCQ) {
                            List(4) { idx ->
                                McqOption(
                                    text = "${s.optionWord} ${formatNumber(idx + 1, mode, lang)}",
                                    isDefault = true
                                )
                            }
                        } else emptyList()
                    )
                }
            }
        }
        return Question(
            type = type,
            title = title,
            isDefaultTitle = !noHdr,
            marks = mark,
            isDefaultMarks = !noHdr,
            pageIndex = pageIndex,
            items = items,
            answerLines = when (type) {
                QuestionType.ESSAY -> 6
                QuestionType.IMLA -> 4
                QuestionType.SPACER -> 2
                else -> 0
            },
            spacerHeightDp = if (type == QuestionType.SPACER) 40 else 0,
            conceptCenter = if (type == QuestionType.CONCEPT) s.conceptCenter else "",
            conceptBranches = if (type == QuestionType.CONCEPT) s.conceptBranches else emptyList(),
            isDefaultConcept = true
        )
    }

    fun subjectPresets(): List<SubjectPreset> = listOf(
        SubjectPreset("kurdi", "📚 زمانێ کوردی", 0xFF0D9488) { lang, mode, pg ->
            val mk = formatNumber("10", mode, lang)
            listOf(
                Question(
                    type = QuestionType.FREE,
                    title = "ل سەر ڕێزمان و ئەدەبێ کوردی بەرسڤا ڤان پرسیاران بدە:",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(text = "ناڤ چییە و چەند جۆر هەنە؟", isDefault = false),
                        QuestionItem(text = "ڕستەیەکێ بنڤێسە کو کارێ ڕابردوو تێدا بیت.", isDefault = false)
                    )
                ),
                Question(
                    type = QuestionType.BLANK,
                    title = "ڤان بۆشاییان ب پەیڤێن گونجای پر بکە:",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(text = "پەیڤا کوردی ل گۆر واتایێ دبنە ............ و ............", isDefault = false),
                        QuestionItem(text = "هەلبەستڤانێ مەزن ئەحمەدێ خانی خودانێ داستانا ............ یە.", isDefault = false)
                    )
                ),
                Question(
                    type = QuestionType.ESSAY,
                    title = "داڕشتنەکێ ل سەر ئێک ژ ڤان بابەتان بنڤێسە:",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    answerLines = 5,
                    items = listOf(
                        QuestionItem(text = "١- ڕۆلێ مامۆستای د ئاڤاکرنا جڤاکی دا.", isDefault = false),
                        QuestionItem(text = "٢- گرنگیا خواندن و زانستی.", isDefault = false)
                    )
                )
            )
        },
        SubjectPreset("math", "🧮 بیرکاری", 0xFF7C3AED) { lang, mode, pg ->
            val mk = formatNumber("10", mode, lang)
            listOf(
                Question(
                    type = QuestionType.FREE,
                    title = "ئەنجامێ ڤان پرسیارێن خوارێ بدەستڤە بینە:",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(text = "بهایێ [sqrt:٢٥] + [sup:٣|٢] چەندە؟", isDefault = false),
                        QuestionItem(text = "شیتەڵا ژمارا ٢٤ بکە بۆ هۆکارێن سەرەتایی.", isDefault = false)
                    )
                ),
                Question(
                    type = QuestionType.MCQ,
                    title = "بەرسڤا ڕاست هەڵبژێرە:",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(
                            text = "ئەنجامێ کەرتێ [frac:١|٢] + [frac:١|٢] یەکسانە ب:",
                            isDefault = false,
                            options = listOf("٠", "١", "٢", "[frac:١|٤]").map { McqOption(text = it, isDefault = false) }
                        )
                    )
                )
            )
        },
        SubjectPreset("science", "🔬 زانست", 0xFF16A34A) { lang, mode, pg ->
            val mk = formatNumber("10", mode, lang)
            listOf(
                Question(
                    type = QuestionType.DEF,
                    title = "پێناسا ڤان تشتان بکە:",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(text = "خانە (الخلية): ............", isDefault = false),
                        QuestionItem(text = "ڕووناهیدروستکرن (التمثيل الضوئي): ............", isDefault = false)
                    )
                ),
                Question(
                    type = QuestionType.WHY,
                    title = "هۆکارێ (بۆچی) دیار بکە:",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(text = "بۆچی ڕووەک ب شەڤێ ئۆکسجینی هەڵدمژن؟", isDefault = false),
                        QuestionItem(text = "بۆچی دەریا شین دەردکەڤن؟", isDefault = false)
                    )
                ),
                Question(
                    type = QuestionType.TF,
                    title = "نیشانا (✓) بۆ ڕاست و (✗) بۆ شاش دانە:",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(text = "(    ) ئاڤ ژ دوو گەردێن هایدرۆجینی و ئێک ئۆکسجینی پێکدهێت.", isDefault = false),
                        QuestionItem(text = "(    ) ڕۆژ ل دۆر ئەردی دزڤڕیت.", isDefault = false)
                    )
                )
            )
        },
        SubjectPreset("english", "🔤 English", 0xFF2563EB) { lang, mode, pg ->
            val mk = formatNumber("10", mode, lang)
            listOf(
                Question(
                    type = QuestionType.BLANK,
                    title = "Fill in the blanks with (is, are, am):",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(text = "She ............ a clever student.", isDefault = false),
                        QuestionItem(text = "They ............ playing football now.", isDefault = false)
                    )
                ),
                Question(
                    type = QuestionType.MATCH,
                    title = "Match Column (A) with Column (B):",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(text = "Book", matchRight = "پەرتووک", isDefault = false),
                        QuestionItem(text = "School", matchRight = "قوتابخانە", isDefault = false),
                        QuestionItem(text = "Teacher", matchRight = "مامۆستا", isDefault = false)
                    )
                )
            )
        },
        SubjectPreset("arabic", "📖 عەرەبی", 0xFFB45309) { lang, mode, pg ->
            val mk = formatNumber("10", mode, lang)
            listOf(
                Question(
                    type = QuestionType.FREE,
                    title = "أجب عن الأسئلة الآتية:",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(text = "أعرب ما تحته خط: (قرأ الطالبُ الدرسَ).", isDefault = false),
                        QuestionItem(text = "استخرج الفاعل والمفعول به من الجملة الآتية.", isDefault = false)
                    )
                ),
                Question(
                    type = QuestionType.IMLA,
                    title = "الإملاء (اكتب ما يُملى عليك بخط واضح):",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    answerLines = 4,
                    items = listOf(
                        QuestionItem(text = "اكتب الفقرة بخط جميل خالٍ من الأخطاء الإملائية.", isDefault = false)
                    )
                )
            )
        },
        SubjectPreset("social", "🌍 مێژوو / جوگرافیا / ئایین", 0xFF9A3412) { lang, mode, pg ->
            val mk = formatNumber("10", mode, lang)
            listOf(
                Question(
                    type = QuestionType.COUNT,
                    title = "ئەڤان خالێن خوارێ بژمێرە:",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(text = "مەرجێن سەرەکی یێن نڤێژێ چنە؟", isDefault = false),
                        QuestionItem(text = "ئەگەرێن گەشەسەندنا شارستانیەتا میزۆپۆتامیا چ بوون؟", isDefault = false),
                        QuestionItem(text = "چوار چیایێن گرنگ ل کوردستانێ بنڤێسە.", isDefault = false),
                        QuestionItem(text = "پێنج پایتەختێن وەڵاتان بنڤێسە.", isDefault = false)
                    )
                ),
                Question(
                    type = QuestionType.DIFF,
                    title = "جیاوازی د ناڤبەرا ڤان دا بنڤێسە:",
                    isDefaultTitle = false,
                    marks = mk,
                    pageIndex = pg,
                    items = listOf(
                        QuestionItem(text = "جیاوازی د ناڤبەرا کەشوهەوایێ گەرم و سار دا:", isDefault = false)
                    )
                )
            )
        }
    )

    fun certificateTemplates(): List<CertificateTemplate> = listOf(
        CertificateTemplate(
            id = "turq",
            icon = "🌸",
            title = "سوپاس و پێزانینی مامۆستا",
            subtitle = "گوڵداری فیروزەیی و زەرد (نەخشاندی)",
            accentHex = 0xFF0F766E,
            state = CertificateState(
                templateId = "turq",
                isLandscape = true,
                bgColorHex = "#F3FCFB",
                textColorHex = "#0F766E",
                borderStyle = CertBorderStyle.TURQ_GOLD,
                topLine1 = "حکومەتا هەرێما کوردستانێ — وەزارەتا پەروەردێ",
                topLine2 = "ڕێڤەبەریا گشتی یا پەروەردێ — قوتابخانا ....................",
                badgeTitle = "سوپاس و پێزانین",
                bodyTop = "ل هەمبەر ماندیبوون، دڵسۆزی و خزمەتا وی یا بەردەوام د پێگەهاندنا نەڤیێن نوو دا، ڕێڤەبەریا قوتابخانێ پڕ ب دڵ سوپاس و پێزانینێن خۆ ئاراستەی مامۆستایێ هێژا:",
                recipientName = "مامۆستا ( ناڤێ مامۆستای ل ڤێرە بنڤێسە )",
                bodyBottom = "دکەت. هیڤیا سەرکەفتن و بەردەوامیێ د کاروانێ زانست و پەروەردێ دا بۆ دخوازین.",
                footerLeft = "ژمارە: ......\nڕێکەوت: ٢٠٢٦/١١/١٥",
                footerCenter = "مۆرا فەرمی",
                footerRight = "ڕێڤەبەرێ قوتابخانێ\n...................."
            )
        ),
        CertificateTemplate(
            id = "cert",
            icon = "🏆",
            title = "باوەرنامەیا سەرکەفتنێ",
            subtitle = "باوەرناما زێڕین بۆ قوتابیان",
            accentHex = 0xFF92400E,
            state = CertificateState(
                templateId = "cert",
                isLandscape = true,
                bgColorHex = "#FFFDF5",
                textColorHex = "#92400E",
                borderStyle = CertBorderStyle.GOLD,
                topLine1 = "حکومەتا هەرێما کوردستانێ — وەزارەتا پەروەردێ",
                topLine2 = "قوتابخانا / پەیمانگەها ....................",
                badgeTitle = "باوەرنامە",
                bodyTop = "پشتی ب سەرکەفتیانە تەمامکرنا خول و تاقیکرنێن (....................)،\nئەڤ باوەرنامە هاتە پێشکێشکرن بۆ قوتابیێ زێڕەک و هێژا:",
                recipientName = "( ناڤێ قوتابی ل ڤێرە بنڤێسە )",
                bodyBottom = "ب پلەیا (..........) دگەل داخوازیا سەرکەفتنا بەردەوام د ژیانا زانستی دا.",
                footerLeft = "مامۆستایێ وانێ\n....................",
                footerCenter = "ڕێکەوت: ٢٠٢٦/١١/١٥",
                footerRight = "ڕێڤەبەر\n...................."
            )
        ),
        CertificateTemplate(
            id = "topstu",
            icon = "🥇",
            title = "باوەرنامەیا قوتابیێ پێشەنگ",
            subtitle = "یەکەمێ پۆلێ / قوتابیێ نموونەیی",
            accentHex = 0xFF1E3A8A,
            state = CertificateState(
                templateId = "topstu",
                isLandscape = true,
                bgColorHex = "#FFFCF2",
                textColorHex = "#1E3A8A",
                borderStyle = CertBorderStyle.CLASSIC_GEO,
                topLine1 = "وەزارەتا پەروەردێ — قوتابخانا ....................",
                topLine2 = "",
                badgeTitle = "🥇 باوەرنامەیا پێشەنگی و سەرکەفتنێ",
                bodyTop = "ب شانازی و دڵخۆشیا زۆر ڤە، کارگێڕیا قوتابخانێ ئەڤ باوەرناما پێشەنگیێ پێشکێش دکەت بۆ قوتابیێ بلیمەت:",
                recipientName = "( ناڤێ قوتابی ل ڤێرە بنڤێسە )",
                bodyBottom = "ژ بەر بدەستڤەئینانا پلەیا (ئێکێ) د پۆلا (..........) دا بۆ ساڵێ خواندنێ ٢٠٢٦ - ٢٠٢٧. هەر د سەرکەفتی و پێشەنگ بی!",
                footerLeft = "مامۆستایێ پۆلێ\n....................",
                footerCenter = "ڕێکەوت: ٢٠٢٦/١١/١٥",
                footerRight = "ڕێڤەبەرێ قوتابخانێ\n...................."
            )
        ),
        CertificateTemplate(
            id = "course",
            icon = "📜",
            title = "بەلگەناما بەشداربوونێ",
            subtitle = "بۆ خول و وۆرک شۆپ و چالاکیان",
            accentHex = 0xFF0F172A,
            state = CertificateState(
                templateId = "course",
                isLandscape = true,
                bgColorHex = "#F8FAFC",
                textColorHex = "#0F172A",
                borderStyle = CertBorderStyle.DOUBLE_ORNATE,
                topLine1 = "ب ناڤێ خودایێ مەزن و دلوڤان",
                topLine2 = "",
                badgeTitle = "بەلگەناما بەشداربوونێ",
                bodyTop = "ئەم ددەینە زانین کو بەڕێز:",
                recipientName = "( ناڤێ بەشداربوویی ل ڤێرە بنڤێسە )",
                bodyBottom = "ب سەرکەفتیانە بەشداری د خولا (....................) دا کرییە کو ژ لایێ (....................) ڤە هاتبوو ڕێکخستن ل ڕێکەوتا (....................).",
                footerLeft = "سەرپەرشتیارێ خولێ\n....................",
                footerCenter = "ژمارا بەلگەنامێ: ......",
                footerRight = "ڕێڤەبەر\n...................."
            )
        ),
        CertificateTemplate(
            id = "honor",
            icon = "🎖️",
            title = "ڕێزلێنان",
            subtitle = "ڕێزلێنانا شاهانە بۆ مامۆستا و خزمەتکاران",
            accentHex = 0xFF1E3A8A,
            state = CertificateState(
                templateId = "honor",
                isLandscape = true,
                bgColorHex = "#F8FBFF",
                textColorHex = "#1E3A8A",
                borderStyle = CertBorderStyle.ROYAL,
                topLine1 = "وەزارەتا پەروەردێ — قوتابخانا ....................",
                topLine2 = "",
                badgeTitle = "ڕێزلێنان و سوپاسی",
                bodyTop = "کارگێڕیا قوتابخانێ و دەستەیا مامۆستایان، ب شانازی ڤە ئەڤ نڤێسارا ڕێزلێنانێ پێشکێش دکەن بۆ بەڕێز:",
                recipientName = "( ناڤێ بەڕێزی ل ڤێرە بنڤێسە )",
                bodyBottom = "ژ بەر کار و خەباتا وی یا جوان و نموونەیی. هەر د سەرکەفتی بن.",
                footerLeft = "مامۆستایان\n....................",
                footerCenter = "٢٠٢٦/١١/١٥",
                footerRight = "ڕێڤەبەر\n...................."
            )
        ),
        CertificateTemplate(
            id = "admin",
            icon = "🏛️",
            title = "سوپاسناما کارگێڕی",
            subtitle = "نڤێسارا فەرمی یا دەستخۆشی و سوپاسیێ",
            accentHex = 0xFF065F46,
            state = CertificateState(
                templateId = "admin",
                isLandscape = true,
                bgColorHex = "#F4FBF7",
                textColorHex = "#065F46",
                borderStyle = CertBorderStyle.EMERALD,
                topLine1 = "حکومەتا هەرێما کوردستانێ — وەزارەتا پەروەردێ / قوتابخانا ....................",
                topLine2 = "ژمارە: ......   |   ڕێکەوت: ٢٠٢٦/١١/١٥",
                badgeTitle = "بابەت / سوپاس و پێزانین",
                bodyTop = "بۆ بەڕێز:",
                recipientName = "( ناڤێ مامۆستای یان فەرمانبەری )",
                bodyBottom = "ل هەمبەر دڵسۆزی و کارێ هەوە یێ بەردەوام و خزمەتا پەروەردەیی، سوپاس و پێزانینا کارگێڕیا قوتابخانێ ئاراستەی جەنابێ هەوە دکەین و هیڤیا سەرکەفتنا بەردەوام بۆ هەوە دخوازین.",
                footerLeft = "",
                footerCenter = "",
                footerRight = "ڕێڤەبەرێ قوتابخانێ\n...................."
            )
        ),
        CertificateTemplate(
            id = "thanks",
            icon = "👏",
            title = "دەستخۆشی",
            subtitle = "پێزانین و هاندان ب بادینی",
            accentHex = 0xFF047857,
            state = CertificateState(
                templateId = "thanks",
                isLandscape = true,
                bgColorHex = "#F4FBF7",
                textColorHex = "#047857",
                borderStyle = CertBorderStyle.EMERALD,
                topLine1 = "ب ناڤێ خودایێ مەزن و دلوڤان",
                topLine2 = "",
                badgeTitle = "دەستخۆشی",
                bodyTop = "ب هەلکەفتا سەرکەفتن و کارێ جوان، ئەم دەستخۆشیەکا گەرم ل بەڕێز:",
                recipientName = "( ناڤێ وی/وێ ل ڤێرە بنڤێسە )",
                bodyBottom = "دکەین و هیڤیا پێشکەفتنا پتر بۆ دخوازین. هەر بمینن د خۆشی و سەرکەفتنێ دا.",
                footerLeft = "",
                footerCenter = "",
                footerRight = "دگەل ڕێزگرتنێ\nم. نەدیار حسێن / کارگێڕی\n٢٠٢٦/١١/١٥"
            )
        ),
        CertificateTemplate(
            id = "invite",
            icon = "✉️",
            title = "داخازنامە",
            subtitle = "داخازناما ڕێورەسم و چالاکیان",
            accentHex = 0xFF6D28D9,
            state = CertificateState(
                templateId = "invite",
                isLandscape = true,
                bgColorHex = "#FCFAFF",
                textColorHex = "#6D28D9",
                borderStyle = CertBorderStyle.PURPLE,
                topLine1 = "ب ناڤێ خودایێ مەزن و دلوڤان",
                topLine2 = "",
                badgeTitle = "داخازنامە",
                bodyTop = "ب شانازی و خۆشحاڵی ڤە داخازا جەنابێ هەوە یێ بەڕێز دکەین بۆ ئامادەبوونێ د ڕێورەسمێ:",
                recipientName = "( ناڤێ ڕێورەسمێ ل ڤێرە بنڤێسە )",
                bodyBottom = "📅 ڕێکەوت: ......................   |   🕒 دەمژمێر: ............   |   📍 جهـ: ....................\nهاتنا هەوە جێگایێ ڕێز و خۆشحاڵیا مەیە.",
                footerLeft = "",
                footerCenter = "",
                footerRight = "کارگێڕیا ...................."
            )
        )
    )

    fun defaultDocumentState(): ExamDocumentState {
        val lang = ExamLanguage.BADINI
        val mode = NumeralMode.AUTO
        val s = strings(lang)
        return ExamDocumentState(
            language = lang,
            numeralMode = mode,
            header = HeaderState(values = s.headerDefaults),
            questions = listOf(createQuestion(QuestionType.MCQ, lang, mode, 0)),
            pages = listOf(ExamPage(showHeader = true))
        )
    }
}
