package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val ScheherazadeNewFont = FontFamily(Font(R.font.scheherazade_new, FontWeight.Normal))
val LateefFont = FontFamily(Font(R.font.lateef, FontWeight.Normal))
val NotoKufiArabicFont = FontFamily(Font(R.font.noto_kufi_arabic, FontWeight.Normal))
val NotoNaskhArabicFont = FontFamily(Font(R.font.noto_naskh_arabic, FontWeight.Normal))
val VazirmatnFont = FontFamily(Font(R.font.vazirmatn, FontWeight.Normal))
val AmiriFont = FontFamily(Font(R.font.amiri, FontWeight.Normal))
val CairoFont = FontFamily(Font(R.font.cairo, FontWeight.Normal))

enum class ExamFontOption(
    val key: String,
    val label: String,
    val fontFamily: FontFamily,
    val cssName: String
) {
    SCHEHERAZADE(
        key = "scheherazade",
        label = "وەزارەتا پەروەردێ (MOE)",
        fontFamily = ScheherazadeNewFont,
        cssName = "'Scheherazade New','Noto Naskh Arabic',serif"
    ),
    LATEEF(
        key = "lateef",
        label = "وەزارەتا پەروەردێ - ٢ (Lateef)",
        fontFamily = LateefFont,
        cssName = "'Lateef','Scheherazade New',serif"
    ),
    KUFI(
        key = "kufi",
        label = "کوردی - کوفی (Noto Kufi)",
        fontFamily = NotoKufiArabicFont,
        cssName = "'Noto Kufi Arabic',sans-serif"
    ),
    VAZIRMATN(
        key = "vazirmatn",
        label = "مۆدێرن (Vazirmatn)",
        fontFamily = VazirmatnFont,
        cssName = "'Vazirmatn',sans-serif"
    ),
    NASKH(
        key = "naskh",
        label = "نەسخ (Noto Naskh)",
        fontFamily = NotoNaskhArabicFont,
        cssName = "'Noto Naskh Arabic',serif"
    ),
    AMIRI(
        key = "amiri",
        label = "ئەمیری (Amiri)",
        fontFamily = AmiriFont,
        cssName = "'Amiri',serif"
    ),
    CAIRO(
        key = "cairo",
        label = "قاهیرە (Cairo)",
        fontFamily = CairoFont,
        cssName = "'Cairo',sans-serif"
    ),
    TIMES(
        key = "times",
        label = "English (Serif)",
        fontFamily = FontFamily.Serif,
        cssName = "'Times New Roman',Georgia,serif"
    );

    companion object {
        fun fromKey(key: String): ExamFontOption = entries.firstOrNull { it.key == key } ?: SCHEHERAZADE
    }
}

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = VazirmatnFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = VazirmatnFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = VazirmatnFont,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = VazirmatnFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = VazirmatnFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = VazirmatnFont,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
