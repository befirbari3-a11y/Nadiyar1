package com.example.model

import java.util.UUID

enum class AppTab {
    HEADER,
    QUESTIONS,
    A4_PAPER,
    CERTIFICATES
}

enum class ExamLanguage(val key: String, val label: String, val isRtl: Boolean) {
    BADINI("badini", "بادینی", true),
    SORANI("sorani", "سۆرانی", true),
    ARABIC("arabic", "عەرەبی", true),
    ENGLISH("english", "English", false);

    companion object {
        fun fromKey(key: String): ExamLanguage = entries.firstOrNull { it.key == key } ?: BADINI
    }
}

enum class NumeralMode(val key: String, val label: String) {
    AUTO("auto", "ژمارە: خۆکار"),
    KU("ku", "ژمارە: ١٢٣"),
    EN("en", "ژمارە: 123");

    companion object {
        fun fromKey(key: String): NumeralMode = entries.firstOrNull { it.key == key } ?: AUTO
    }
}

enum class LogoPosition(val key: String, val label: String) {
    CENTER("center", "جهـ: ناڤەڕاست"),
    RIGHT("right", "جهـ: ڕاست"),
    LEFT("left", "جهـ: چەپ");

    companion object {
        fun fromKey(key: String): LogoPosition = entries.firstOrNull { it.key == key } ?: CENTER
    }
}

enum class HeaderFieldKey(val key: String, val label: String) {
    BIS("bis", "بسم الله"),
    MIN("min", "وەزارەت"),
    SCH("sch", "قوتابخانە"),
    SUB("sub", "بابەت"),
    GRD("grd", "پۆل"),
    TCH("tch", "مامۆستا"),
    TYP("typ", "جۆرێ ئەزموونێ"),
    YR("yr", "ساڵ"),
    STU("stu", "ناڤێ قوتابی"),
    DAT("dat", "ڕێکەوت"),
    TIM("tim", "کات"),
    SCR("scr", "نمرێ گشتی (بەتاڵ = خۆکار)"),
    FT("ft", "دوماهی")
}

data class HeaderState(
    val values: Map<HeaderFieldKey, String>,
    val visible: Map<HeaderFieldKey, Boolean> = HeaderFieldKey.entries.associateWith { true },
    val locked: Boolean = false,
    val logoBase64: String = "",
    val logoPos: LogoPosition = LogoPosition.CENTER,
    val logoSizeDp: Int = 54
)

enum class QuestionType(val key: String, val label: String, val colorHex: Long) {
    MCQ("mcq", "هەڵبژاردن", 0xFF3B5998),
    COUNT("count", "بژمێرە (ئاسۆیی)", 0xFF1E40AF),
    TF("tf", "ڕاست / هەڵە", 0xFF2E8B57),
    BLANK("blank", "بۆشایی", 0xFF2C7A7B),
    WRITE("write", "نڤێسین (بێ پرسیار)", 0xFF0D9488),
    SPACER("spacer", "بۆشایی (بێ پرسیار)", 0xFF475569),
    MATCH("match", "بەیەکگەیاندن", 0xFF6B46C1),
    DEF("def", "پێناسە", 0xFF8B4513),
    IMG("img", "وێنە", 0xFF9B2C2C),
    DIFF("diff", "جیاوازی", 0xFF975A16),
    WHY("why", "هۆکار / بۆچی", 0xFF7B2CBF),
    CONCEPT("concept", "نەخشەی چەمک", 0xFF15803D),
    FREE("free", "پرسیاری ئازاد", 0xFF831843),
    ESSAY("essay", "دارشتن", 0xFF65A30D),
    IMLA("imla", "ئیملا", 0xFF1D6F8A),
    CALLIG("callig", "خۆشنووسی", 0xFF9A3412),
    TABLE("table", "خشتە (بێ پرسیار)", 0xFF0F172A);

    val isNoHeader: Boolean get() = this == SPACER || this == WRITE

    companion object {
        fun fromKey(key: String): QuestionType = entries.firstOrNull { it.key == key } ?: FREE
    }
}

enum class LineSize(val key: String, val label: String, val heightDp: Int) {
    SM("sm", "بچوک", 18),
    MD("md", "ناڤنجی", 26),
    LG("lg", "مەزن", 36);

    companion object {
        fun fromKey(key: String): LineSize = entries.firstOrNull { it.key == key } ?: MD
    }
}

data class McqOption(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDefault: Boolean = true
)

data class QuestionItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDefault: Boolean = true,
    val options: List<McqOption> = emptyList(),
    val matchRight: String = ""
)

data class Question(
    val id: String = UUID.randomUUID().toString(),
    val type: QuestionType,
    val title: String,
    val isDefaultTitle: Boolean = true,
    val marks: String,
    val isDefaultMarks: Boolean = true,
    val pageIndex: Int = 0,
    val items: List<QuestionItem> = emptyList(),
    val answerLines: Int = 0,
    val lineSize: LineSize = LineSize.MD,
    val spacerHeightDp: Int = 0,
    val bgColorHex: String = "",
    val noSeparatorLine: Boolean = false,
    val imageBase64: String = "",
    val imageWidthDp: Int = 180,
    val conceptCenter: String = "",
    val conceptBranches: List<String> = emptyList(),
    val isDefaultConcept: Boolean = true
)

enum class BorderStyle(val key: String, val label: String) {
    SOLID("solid", "سادە (تۆخ)"),
    DOUBLE("double", "دوو خەت (جوان)"),
    ROUND("round", "گۆشە خڕ"),
    ORNATE("ornate", "دوو چوارچێوە"),
    DASHED("dashed", "پچڕپچڕ"),
    NONE("none", "بێ");

    companion object {
        fun fromKey(key: String): BorderStyle = entries.firstOrNull { it.key == key } ?: SOLID
    }
}

enum class FloatingElementType(val key: String) {
    TEXT_BOX("text_box"),
    MARK_CIRCLE("mark_circle"),
    MED_CIRCLE("med_circle"),
    TABLE("table"),
    IMAGE("image"),
    SHAPE("shape"),
    NUMBER_LINE("number_line"),
    COORD_GRID("coord_grid");

    companion object {
        fun fromKey(key: String): FloatingElementType = entries.firstOrNull { it.key == key } ?: TEXT_BOX
    }
}

enum class ShapeKind(val key: String, val label: String) {
    CIRCLE("circle", "◯ بازنە"),
    TRIANGLE("triangle", "△ سێگۆشە"),
    SQUARE("square", "□ مربع"),
    RECT("rect", "▭ لاکێشە"),
    RHOMBUS("rhombus", "◇ معین"),
    LINE("line", "―― هێڵا ئاسۆیی"),
    VLINE("vline", "│ هێڵا ستوونی"),
    DLINE("dline", "╱ هێڵا لار"),
    ARROW("arrow", "➔ سەهمێ ڕاست"),
    LARROW("larrow", "⬅ سەهمێ چەپ"),
    VARROW("varrow", "↕ سەهمێ ستوونی"),
    SLINE("sline", "↔ سەهمێ دووسەر"),
    RAY("ray", "●─➔ تیشک");

    companion object {
        fun fromKey(key: String): ShapeKind = entries.firstOrNull { it.key == key } ?: CIRCLE
    }
}

data class FloatingElement(
    val id: String = UUID.randomUUID().toString(),
    val type: FloatingElementType,
    val xFrac: Float = 0.1f,
    val yFrac: Float = 0.25f,
    val wFrac: Float = 0.35f,
    val hFrac: Float = 0.08f,
    val rotationDeg: Float = 0f,
    val locked: Boolean = false,
    val text: String = "",
    val secondaryText: String = "",
    val strokeColorHex: String = "#000000",
    val bgColorHex: String = "#FFFFFF",
    val showBorder: Boolean = true,
    val fontSizeSp: Float = 13f,
    val shapeKind: ShapeKind = ShapeKind.CIRCLE,
    val tableRows: List<List<String>> = emptyList(),
    val imageBase64: String = ""
)

enum class PenMode(val key: String, val label: String) {
    SMART("smart", "✨ خۆکار (زیرەک)"),
    LINE("line", "📏 هێڵا ڕاست"),
    ARROW("arrow", "➔ سەهمێ ڕاست"),
    FREE("free", "〰️ ئازاد");

    companion object {
        fun fromKey(key: String): PenMode = entries.firstOrNull { it.key == key } ?: SMART
    }
}

data class NormalizedPoint(val x: Float, val y: Float)

data class PenStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<NormalizedPoint>,
    val colorHex: String = "#000000",
    val widthDp: Float = 2f,
    val dashed: Boolean = false,
    val isArrow: Boolean = false
)

data class ExamPage(
    val id: String = UUID.randomUUID().toString(),
    val showHeader: Boolean = true,
    val freeWriteText: String = "",
    val floatingElements: List<FloatingElement> = emptyList(),
    val penStrokes: List<PenStroke> = emptyList()
)

data class ExamStyle(
    val fontKey: String = "scheherazade",
    val fontSizeSp: Int = 15,
    val headerScalePct: Int = 93,
    val headerColorHex: String = "#000000",
    val questionTitleColorHex: String = "#1E3A8A",
    val bodyTextColorHex: String = "#000000",
    val paperBgColorHex: String = "#FFFFFF",
    val borderStyle: BorderStyle = BorderStyle.SOLID,
    val borderColorHex: String = "#0F172A"
)

data class ExamDocumentState(
    val language: ExamLanguage = ExamLanguage.BADINI,
    val numeralMode: NumeralMode = NumeralMode.AUTO,
    val header: HeaderState,
    val questions: List<Question>,
    val pages: List<ExamPage>,
    val style: ExamStyle = ExamStyle()
)

enum class CertBorderStyle(val key: String, val label: String) {
    TURQ_GOLD("turq-gold", "🌸 گوڵداری فیروزەیی و زەرد"),
    GOLD("gold", "🏆 زێڕین و شاهانە"),
    ROYAL("royal", "🎖️ شین و زێڕین"),
    EMERALD("emerald", "🌿 کەسک و زێڕین"),
    PURPLE("purple", "💜 مۆر و زێڕین"),
    CLASSIC_GEO("classic-geo", "💠 ئەندازەیی و کلاسیک"),
    DOUBLE_ORNATE("double-ornate", "📜 دوو هێڵا فەرمی");

    companion object {
        fun fromKey(key: String): CertBorderStyle = entries.firstOrNull { it.key == key } ?: TURQ_GOLD
    }
}

data class CertificateState(
    val templateId: String = "turq",
    val isLandscape: Boolean = true,
    val fontKey: String = "scheherazade",
    val fontScale: Float = 1.0f,
    val bgColorHex: String = "#F3FCFB",
    val textColorHex: String = "#0F766E",
    val borderStyle: CertBorderStyle = CertBorderStyle.TURQ_GOLD,
    val topLine1: String = "",
    val topLine2: String = "",
    val badgeTitle: String = "",
    val bodyTop: String = "",
    val recipientName: String = "",
    val bodyBottom: String = "",
    val footerLeft: String = "",
    val footerCenter: String = "",
    val footerRight: String = "",
    val floatingElements: List<FloatingElement> = emptyList()
)
