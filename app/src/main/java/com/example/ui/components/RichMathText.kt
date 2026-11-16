package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.MathMarkup
import com.example.util.MediaUtils
import com.example.util.RichSegment

@Composable
fun RichMathText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = (fontSize.value * 1.45f).sp
) {
    val segments = remember(text) { MathMarkup.parse(text) }
    val hasRich = remember(segments) { segments.any { it !is RichSegment.Plain } }

    if (!hasRich) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight,
            modifier = modifier
        )
        return
    }

    val inlineContent = remember(segments, color, fontSize, fontFamily) {
        val map = mutableMapOf<String, InlineTextContent>()
        segments.forEachIndexed { idx, seg ->
            if (seg !is RichSegment.Plain && seg !is RichSegment.Styled) {
                val id = "m_$idx"
                val (wSp, hSp) = when (seg) {
                    is RichSegment.Fraction -> {
                        val chars = maxOf(seg.num.length, seg.den.length).coerceAtLeast(1)
                        ((chars * 0.65f + 0.9f) * fontSize.value).sp to (fontSize.value * 2.1f).sp
                    }
                    is RichSegment.Mixed -> {
                        val chars = maxOf(seg.num.length, seg.den.length).coerceAtLeast(1)
                        (((seg.whole.length + chars) * 0.7f + 1.1f) * fontSize.value).sp to (fontSize.value * 2.1f).sp
                    }
                    is RichSegment.Root -> {
                        val chars = (seg.index.length + seg.radicand.length).coerceAtLeast(2)
                        ((chars * 0.65f + 1.3f) * fontSize.value).sp to (fontSize.value * 1.55f).sp
                    }
                    is RichSegment.Power, is RichSegment.Subscript -> {
                        val chars = when (seg) {
                            is RichSegment.Power -> seg.base.length + seg.exp.length
                            is RichSegment.Subscript -> seg.base.length + seg.sub.length
                            else -> 2
                        }
                        ((chars * 0.65f + 0.5f) * fontSize.value).sp to (fontSize.value * 1.45f).sp
                    }
                    is RichSegment.RastaLine -> {
                        ((seg.label.length * 0.7f + 0.8f) * fontSize.value).sp to (fontSize.value * 1.75f).sp
                    }
                    is RichSegment.Overline -> {
                        ((seg.label.length * 0.7f + 0.7f) * fontSize.value).sp to (fontSize.value * 1.4f).sp
                    }
                    else -> 24.sp to 24.sp
                }
                map[id] = InlineTextContent(
                    Placeholder(
                        width = wSp,
                        height = hSp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    MathSegmentBox(seg = seg, color = color, fontSize = fontSize, fontFamily = fontFamily)
                }
            }
        }
        map
    }

    val annotated = remember(segments) {
        buildAnnotatedString {
            segments.forEachIndexed { idx, seg ->
                when (seg) {
                    is RichSegment.Plain -> append(seg.text)
                    is RichSegment.Styled -> {
                        val bg = if (seg.highlightHex.isNotBlank()) {
                            MediaUtils.parseColor(seg.highlightHex, Color.Unspecified)
                        } else Color.Unspecified
                        withStyle(
                            SpanStyle(
                                fontWeight = if (seg.bold) FontWeight.ExtraBold else null,
                                fontStyle = if (seg.italic) FontStyle.Italic else null,
                                textDecoration = if (seg.underline) TextDecoration.Underline else null,
                                background = bg
                            )
                        ) {
                            append(seg.text)
                        }
                    }
                    else -> appendInlineContent("m_$idx", "[math]")
                }
            }
        }
    }

    Text(
        text = annotated,
        inlineContent = inlineContent,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight
        ),
        modifier = modifier
    )
}

@Composable
private fun MathSegmentBox(
    seg: RichSegment,
    color: Color,
    fontSize: TextUnit,
    fontFamily: FontFamily
) {
    val smallFs = (fontSize.value * 0.8f).coerceAtLeast(8f).sp
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (seg) {
            is RichSegment.Fraction -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(seg.num, color = color, fontSize = smallFs, fontFamily = fontFamily, fontWeight = FontWeight.Bold, lineHeight = smallFs)
                Box(modifier = Modifier.fillMaxWidth(0.85f).height(1.3.dp).background(color))
                Text(seg.den, color = color, fontSize = smallFs, fontFamily = fontFamily, fontWeight = FontWeight.Bold, lineHeight = smallFs)
            }
            is RichSegment.Mixed -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(seg.whole, color = color, fontSize = fontSize, fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(2.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(seg.num, color = color, fontSize = smallFs, fontFamily = fontFamily, fontWeight = FontWeight.Bold, lineHeight = smallFs)
                    Box(modifier = Modifier.widthIn(min = 14.dp).height(1.3.dp).background(color))
                    Text(seg.den, color = color, fontSize = smallFs, fontFamily = fontFamily, fontWeight = FontWeight.Bold, lineHeight = smallFs)
                }
            }
            is RichSegment.Root -> Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                if (seg.index.isNotBlank()) {
                    Text(
                        seg.index,
                        color = color,
                        fontSize = (fontSize.value * 0.65f).coerceAtLeast(7f).sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
                Text("√", color = color, fontSize = (fontSize.value * 1.15f).sp, fontWeight = FontWeight.Bold)
                Text(
                    seg.radicand,
                    color = color,
                    fontSize = smallFs,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .drawBehind {
                            drawLine(color, Offset(0f, 1f), Offset(size.width, 1f), strokeWidth = 2.2f)
                        }
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
            is RichSegment.Power -> Row(verticalAlignment = Alignment.Top) {
                Text(seg.base, color = color, fontSize = fontSize, fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                Text(
                    seg.exp,
                    color = color,
                    fontSize = (fontSize.value * 0.68f).coerceAtLeast(7f).sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            is RichSegment.Subscript -> Row(verticalAlignment = Alignment.Bottom) {
                Text(seg.base, color = color, fontSize = fontSize, fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                Text(
                    seg.sub,
                    color = color,
                    fontSize = (fontSize.value * 0.68f).coerceAtLeast(7f).sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            is RichSegment.RastaLine -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("↔", color = color, fontSize = smallFs, lineHeight = smallFs)
                Text(seg.label, color = color, fontSize = smallFs, fontFamily = fontFamily, fontWeight = FontWeight.Bold, lineHeight = smallFs)
            }
            is RichSegment.Overline -> Text(
                seg.label,
                color = color,
                fontSize = smallFs,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .drawBehind {
                        drawLine(color, Offset(0f, 1f), Offset(size.width, 1f), strokeWidth = 2.2f)
                    }
                    .padding(horizontal = 3.dp)
            )
            is RichSegment.Plain, is RichSegment.Styled -> {}
        }
    }
}
