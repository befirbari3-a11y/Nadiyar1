package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.model.CertBorderStyle

@Composable
fun CertificateOrnamentCanvas(
    borderStyle: CertBorderStyle,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val (c1, c2, c3) = when (borderStyle) {
            CertBorderStyle.TURQ_GOLD -> Triple(Color(0xFF0D9488), Color(0xFFEAB308), Color(0xFF14B8A6))
            CertBorderStyle.GOLD -> Triple(Color(0xFFB45309), Color(0xFFF59E0B), Color(0xFFD97706))
            CertBorderStyle.ROYAL -> Triple(Color(0xFF1E3A8A), Color(0xFFEAB308), Color(0xFF3B82F6))
            CertBorderStyle.EMERALD -> Triple(Color(0xFF047857), Color(0xFFEAB308), Color(0xFF10B981))
            CertBorderStyle.PURPLE -> Triple(Color(0xFF6D28D9), Color(0xFFEAB308), Color(0xFF8B5CF6))
            CertBorderStyle.CLASSIC_GEO -> Triple(Color(0xFF1E3A8A), Color(0xFFB45309), Color(0xFF0284C7))
            CertBorderStyle.DOUBLE_ORNATE -> Triple(Color(0xFF0F172A), Color(0xFF0D9488), Color(0xFF475569))
        }

        val m1 = 10.dp.toPx()
        val m2 = 17.dp.toPx()
        drawRoundRect(
            color = c1,
            topLeft = Offset(m1, m1),
            size = Size(size.width - m1 * 2, size.height - m1 * 2),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = 3.5.dp.toPx())
        )
        drawRoundRect(
            color = c2,
            topLeft = Offset(m2, m2),
            size = Size(size.width - m2 * 2, size.height - m2 * 2),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        val floral = borderStyle == CertBorderStyle.TURQ_GOLD ||
            borderStyle == CertBorderStyle.EMERALD ||
            borderStyle == CertBorderStyle.PURPLE

        val corners = listOf(
            Offset(0f, 0f) to 0f,
            Offset(size.width, 0f) to 90f,
            Offset(size.width, size.height) to 180f,
            Offset(0f, size.height) to 270f
        )

        corners.forEach { (origin, deg) ->
            rotate(degrees = deg, pivot = origin) {
                drawCornerOrnament(
                    origin = origin,
                    floral = floral,
                    isGeo = borderStyle == CertBorderStyle.CLASSIC_GEO,
                    c1 = c1,
                    c2 = c2,
                    c3 = c3
                )
            }
        }
    }
}

private fun DrawScope.drawCornerOrnament(
    origin: Offset,
    floral: Boolean,
    isGeo: Boolean,
    c1: Color,
    c2: Color,
    c3: Color
) {
    val ox = origin.x
    val oy = origin.y
    val u = 1.dp.toPx()
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
        drawPath(petal1, c3.copy(alpha = 0.35f))
        drawPath(petal2, c1.copy(alpha = 0.30f))
        drawCircle(c1.copy(alpha = 0.85f), radius = 7.5f * u, center = Offset(ox + 21 * u, oy + 21 * u))
        drawCircle(c3.copy(alpha = 0.80f), radius = 5f * u, center = Offset(ox + 32 * u, oy + 17 * u))
        drawCircle(c3.copy(alpha = 0.80f), radius = 5f * u, center = Offset(ox + 17 * u, oy + 32 * u))
        drawCircle(c2, radius = 3f * u, center = Offset(ox + 21 * u, oy + 21 * u))
        val arc = Path().apply {
            moveTo(ox + 12 * u, oy + 56 * u)
            cubicTo(ox + 12 * u, oy + 24 * u, ox + 24 * u, oy + 12 * u, ox + 56 * u, oy + 12 * u)
        }
        drawPath(arc, c2, style = Stroke(width = 1.8f * u))
    } else if (isGeo) {
        drawRect(
            color = c2,
            topLeft = Offset(ox + 12 * u, oy + 12 * u),
            size = Size(28 * u, 28 * u),
            style = Stroke(width = 1.8f * u)
        )
        drawLine(c2, Offset(ox + 12 * u, oy + 52 * u), Offset(ox + 52 * u, oy + 12 * u), strokeWidth = 1.4f * u)
        drawCircle(c1.copy(alpha = 0.25f), radius = 10 * u, center = Offset(ox + 26 * u, oy + 26 * u))
    } else {
        val cornerPath = Path().apply {
            moveTo(ox + 12 * u, oy + 46 * u)
            lineTo(ox + 12 * u, oy + 12 * u)
            lineTo(ox + 46 * u, oy + 12 * u)
        }
        drawPath(cornerPath, c1, style = Stroke(width = 2.4f * u))
        drawCircle(c2, radius = 4f * u, center = Offset(ox + 19 * u, oy + 19 * u))
    }
}
