package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max

object MediaUtils {

    fun parseColor(hex: String, fallback: Color = Color.Black): Color {
        if (hex.isBlank()) return fallback
        return runCatching {
            Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
        }.getOrDefault(fallback)
    }

    fun parseColorInt(hex: String, fallback: Int = android.graphics.Color.BLACK): Int {
        if (hex.isBlank()) return fallback
        return runCatching {
            android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        }.getOrDefault(fallback)
    }

    fun colorToHex(color: Color): String {
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    fun uriToBase64(context: Context, uri: Uri, maxDim: Int = 720): String? {
        return runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val scale = max(raw.width, raw.height).let { if (it > maxDim) maxDim.toFloat() / it else 1f }
            val bmp = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    raw,
                    (raw.width * scale).toInt().coerceAtLeast(1),
                    (raw.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else raw
            val out = ByteArrayOutputStream()
            val format = if (bmp.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            bmp.compress(format, 85, out)
            val mime = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
            "data:$mime;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    fun decodeBase64Bitmap(dataUri: String): Bitmap? {
        if (dataUri.isBlank()) return null
        return runCatching {
            val rawB64 = dataUri.substringAfter("base64,", dataUri)
            val bytes = Base64.decode(rawB64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun decodeBase64ImageBitmap(dataUri: String): ImageBitmap? {
        return decodeBase64Bitmap(dataUri)?.asImageBitmap()
    }
}
