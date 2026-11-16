package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ShapeKind
import com.example.ui.theme.*
import com.example.util.MediaUtils

@Composable
fun ColorPickerDialog(
    title: String,
    currentHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val palette = listOf(
        "#000000", "#0F172A", "#1E3A8A", "#1D4ED8", "#0284C7", "#0D9488",
        "#16A34A", "#047857", "#B45309", "#D97706", "#DC2626", "#7C3AED",
        "#6D28D9", "#BE185D", "#475569", "#FFFFFF", "#F8FAFC", "#F3FCFB",
        "#FFFDF5", "#FEF08A", "#BAE6FD", "#BBF7D0", "#FBCFE8", "#E9D5FF"
    )
    var hexInput by remember { mutableStateOf(currentHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    palette.forEach { hex ->
                        val col = MediaUtils.parseColor(hex)
                        val selected = hex.equals(hexInput, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) RoyalBlue else SubtleBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    hexInput = hex
                                    onColorSelected(hex)
                                    onDismiss()
                                }
                        )
                    }
                }
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { hexInput = it },
                    label = { Text("کۆدێ ڕەنگی (Hex)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onColorSelected(hexInput.trim().ifEmpty { currentHex })
                onDismiss()
            }) {
                Text("هەڵبژارتن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لابردن") }
        }
    )
}

enum class MathInsertKind(val label: String) {
    FRAC("کەرت a/b"),
    MIX("تێکەڵ c a/b"),
    SQRT("√x ڕەگ"),
    CUBE_ROOT("∛x ڕەگێ ٣"),
    N_ROOT("ⁿ√x ڕەگێ n"),
    POW2("x² توان"),
    POWN("xⁿ توان"),
    SUB("x₁ ژێر"),
    RASTA("↔ ڕاستەهێڵ AB"),
    OVER("AB̄ هێڵ")
}

@Composable
fun MathInsertDialog(
    initialKind: MathInsertKind,
    onDismiss: () -> Unit,
    onInsertToken: (String) -> Unit
) {
    var kind by remember { mutableStateOf(initialKind) }
    var a by remember { mutableStateOf(if (initialKind == MathInsertKind.POW2 || initialKind == MathInsertKind.POWN || initialKind == MathInsertKind.SUB || initialKind == MathInsertKind.SQRT || initialKind == MathInsertKind.CUBE_ROOT || initialKind == MathInsertKind.N_ROOT) "x" else if (initialKind == MathInsertKind.RASTA || initialKind == MathInsertKind.OVER) "AB" else "١") }
    var b by remember { mutableStateOf(if (initialKind == MathInsertKind.CUBE_ROOT) "٣" else if (initialKind == MathInsertKind.POW2) "٢" else if (initialKind == MathInsertKind.SUB) "١" else "٢") }
    var c by remember { mutableStateOf("٣") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🧮 بەشێ بیرکاری", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MathInsertKind.entries.forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = {
                                kind = k
                                when (k) {
                                    MathInsertKind.FRAC -> { a = "١"; b = "٢" }
                                    MathInsertKind.MIX -> { c = "٣"; a = "١"; b = "٢" }
                                    MathInsertKind.SQRT -> { a = "٢٥" }
                                    MathInsertKind.CUBE_ROOT -> { b = "٣"; a = "٨" }
                                    MathInsertKind.N_ROOT -> { b = "n"; a = "x" }
                                    MathInsertKind.POW2 -> { a = "x"; b = "٢" }
                                    MathInsertKind.POWN -> { a = "x"; b = "n" }
                                    MathInsertKind.SUB -> { a = "x"; b = "١" }
                                    MathInsertKind.RASTA, MathInsertKind.OVER -> { a = "AB" }
                                }
                            },
                            label = { Text(k.label, fontSize = 11.sp) }
                        )
                    }
                }

                when (kind) {
                    MathInsertKind.FRAC -> {
                        OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("سەرە (بسط)") }, singleLine = true)
                        OutlinedTextField(value = b, onValueChange = { b = it }, label = { Text("ژێرە (مقام)") }, singleLine = true)
                    }
                    MathInsertKind.MIX -> {
                        OutlinedTextField(value = c, onValueChange = { c = it }, label = { Text("ژمارا ساخ") }, singleLine = true)
                        OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("سەرە") }, singleLine = true)
                        OutlinedTextField(value = b, onValueChange = { b = it }, label = { Text("ژێرە") }, singleLine = true)
                    }
                    MathInsertKind.SQRT -> {
                        OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("ژمارە یان پیت د بن ڕەگی دا") }, singleLine = true)
                    }
                    MathInsertKind.CUBE_ROOT, MathInsertKind.N_ROOT -> {
                        OutlinedTextField(value = b, onValueChange = { b = it }, label = { Text("پلەیا ڕەگی (بۆ نموونە ٣ یان n)") }, singleLine = true)
                        OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("ژمارە یان پیت") }, singleLine = true)
                    }
                    MathInsertKind.POW2, MathInsertKind.POWN -> {
                        OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("بنەما (الأساس)") }, singleLine = true)
                        OutlinedTextField(value = b, onValueChange = { b = it }, label = { Text("توان (الأس)") }, singleLine = true)
                    }
                    MathInsertKind.SUB -> {
                        OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("پیت") }, singleLine = true)
                        OutlinedTextField(value = b, onValueChange = { b = it }, label = { Text("نڤێسینا ژێر") }, singleLine = true)
                    }
                    MathInsertKind.RASTA, MathInsertKind.OVER -> {
                        OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("پیتێن هێڵێ (بۆ نموونە AB)") }, singleLine = true)
                    }
                }

                val previewToken = buildMathToken(kind, a, b, c)
                Surface(
                    color = VioletContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RichMathText(
                            text = previewToken,
                            color = Color(0xFF4C1D95),
                            fontSize = 18.sp,
                            fontFamily = ScheherazadeNewFont
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onInsertToken(buildMathToken(kind, a, b, c))
                onDismiss()
            }) {
                Text("زێدەکرن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لابردن") }
        }
    )
}

private fun buildMathToken(kind: MathInsertKind, a: String, b: String, c: String): String = when (kind) {
    MathInsertKind.FRAC -> "[frac:${a.trim()}|${b.trim()}]"
    MathInsertKind.MIX -> "[mix:${c.trim()}|${a.trim()}|${b.trim()}]"
    MathInsertKind.SQRT -> "[sqrt:${a.trim()}]"
    MathInsertKind.CUBE_ROOT, MathInsertKind.N_ROOT -> "[root:${b.trim()}|${a.trim()}]"
    MathInsertKind.POW2, MathInsertKind.POWN -> "[sup:${a.trim()}|${b.trim()}]"
    MathInsertKind.SUB -> "[sub:${a.trim()}|${b.trim()}]"
    MathInsertKind.RASTA -> "[rasta:${a.trim()}]"
    MathInsertKind.OVER -> "[over:${a.trim()}]"
}

@Composable
fun MathAndShapeToolbox(
    showPaperItems: Boolean,
    onInsertTextToFocused: (String) -> Unit,
    onOpenMathDialog: (MathInsertKind) -> Unit,
    onAddShape: (ShapeKind) -> Unit = {},
    onAddNumberLine: () -> Unit = {},
    onAddCoordGrid: () -> Unit = {},
    onAddMedCircle: () -> Unit = {},
    onAddMiniTable: (Int, Int) -> Unit = { _, _ -> }
) {
    var mathOpen by remember { mutableStateOf(false) }
    var arrowsOpen by remember { mutableStateOf(false) }
    var extraOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Math section
        Surface(
            color = Color(0xFFFAF5FF),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.2.dp, Color(0xFFC084FC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable { mathOpen = !mathOpen },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🧮 بەشێ بیرکاری (کەرت، ڕەگ، توان، ڕاستەهێڵ، نەخشێ ژماران)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = Color(0xFF6B21A8),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (mathOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle math tools",
                        tint = Color(0xFF6B21A8)
                    )
                }
                AnimatedVisibility(visible = mathOpen) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                    ) {
                        MathInsertKind.entries.forEach { mk ->
                            OutlinedButton(
                                onClick = { onOpenMathDialog(mk) },
                                border = BorderStroke(1.dp, Color(0xFFC084FC)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(mk.label, color = Color(0xFF4C1D95), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        listOf(
                            "[b:دەق]" to "B توخ",
                            "[i:دەق]" to "I لار",
                            "[u:دەق]" to "U هێڵ",
                            "[hi:#FEF08A|دەق]" to "🟨 ڕەنگ",
                            "[hi:#BAE6FD|دەق]" to "🟦 ڕەنگ",
                            "[hi:#BBF7D0|دەق]" to "🟩 ڕەنگ"
                        ).forEach { (tok, lbl) ->
                            FilledTonalButton(
                                onClick = { onInsertTextToFocused(tok) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(lbl, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                        listOf(" ________ ", "×", "÷", "±", "≠", "≤", "≥", "°", "∠", "△", "π", "∑").forEach { sym ->
                            FilledTonalButton(
                                onClick = { onInsertTextToFocused(sym) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(sym.trim().ifEmpty { "____" }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        if (showPaperItems) {
                            OutlinedButton(
                                onClick = onAddNumberLine,
                                border = BorderStroke(1.dp, Color(0xFFC084FC)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) { Text("📏 نەخشێ ژماران", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = onAddCoordGrid,
                                border = BorderStroke(1.dp, Color(0xFFC084FC)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) { Text("➕ نەخشێ بیرکاری (X/Y)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        if (showPaperItems) {
            // Lines & Arrows
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.2.dp, Color(0xFF60A5FA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { arrowsOpen = !arrowsOpen },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📏 بەشێ هێڵ، ئاراستە و سەهم (ئاسۆیی، ستوونی، لار، زڤڕاندن)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFF1E40AF),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (arrowsOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle line tools",
                            tint = Color(0xFF1E40AF)
                        )
                    }
                    AnimatedVisibility(visible = arrowsOpen) {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        ) {
                            listOf(
                                ShapeKind.LINE, ShapeKind.VLINE, ShapeKind.DLINE,
                                ShapeKind.ARROW, ShapeKind.LARROW, ShapeKind.VARROW,
                                ShapeKind.SLINE, ShapeKind.RAY
                            ).forEach { sk ->
                                OutlinedButton(
                                    onClick = { onAddShape(sk) },
                                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(sk.label, color = Color(0xFF1E40AF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Highlight / Circles / Mini Tables
            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.2.dp, Color(0xFF4ADE80)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { extraOpen = !extraOpen },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🎨 بازنا ناڤەڕاست و جەدوەلێ بچوک",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFF166534),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (extraOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle extra tools",
                            tint = Color(0xFF166534)
                        )
                    }
                    AnimatedVisibility(visible = extraOpen) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 6.dp, bottom = 4.dp)
                        ) {
                            OutlinedButton(onClick = onAddMedCircle) { Text("⭕ بازنا ناڤەڕاست", fontSize = 12.sp) }
                            OutlinedButton(onClick = { onAddMiniTable(2, 2) }) { Text("⊞ جەدوەلێ بچوک (٢×٢)", fontSize = 12.sp) }
                            OutlinedButton(onClick = { onAddMiniTable(3, 2) }) { Text("⊞ جەدوەلێ بچوک (٣×٢)", fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}
