package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.CertificateOrnamentCanvas
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.FloatingElementEditDialog
import com.example.ui.components.FloatingElementView
import com.example.ui.theme.*
import com.example.util.MediaUtils

@Composable
fun CertificateStudioScreen(
    cert: CertificateState,
    onApplyTemplate: (CertificateTemplate) -> Unit,
    onUpdateCert: ((CertificateState) -> CertificateState) -> Unit,
    onAddImage: (String) -> Unit,
    onAddSnippet: (Boolean) -> Unit,
    onUpdateFloating: (String, (FloatingElement) -> FloatingElement) -> Unit,
    onDeleteFloating: (String) -> Unit,
    onPrintPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onExportWord: () -> Unit
) {
    val context = LocalContext.current
    val templates = remember { LocalizationPresets.certificateTemplates() }

    var fontMenuOpen by remember { mutableStateOf(false) }
    var borderMenuOpen by remember { mutableStateOf(false) }
    var colorTarget by remember { mutableStateOf<String?>(null) }
    var selectedFlId by remember { mutableStateOf<String?>(null) }
    var editTextDialog by remember { mutableStateOf(false) }
    var editingFloating by remember { mutableStateOf<FloatingElement?>(null) }
    var floatingColorTargetId by remember { mutableStateOf<String?>(null) }

    val imgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { MediaUtils.uriToBase64(context, it, maxDim = 480)?.let(onAddImage) }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        // Header & Export Actions
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    "🌸 بەشێ سەربەخۆ یێ باوەرنامە و قاڵبان (بێ تێکدانا ئەسیلێ)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TealPrimary
                )

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onUpdateCert { it.copy(isLandscape = !it.isLandscape) } },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.testTag("toggle_cert_orient_btn")
                    ) {
                        Text(
                            if (cert.isLandscape) "🔄 ئاسۆیی (Landscape)" else "🔄 ستوونی (Portrait)",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onPrintPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
                        modifier = Modifier.testTag("print_cert_pdf_btn")
                    ) {
                        Text("📄 چاپ / PDF باوەرنامێ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(onClick = onSharePdf) {
                        Text("📤 ناردنا PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onExportWord,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier.testTag("export_cert_word_btn")
                    ) {
                        Text("📘 Word", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 8 Template Cards
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { tpl ->
                        val active = cert.templateId == tpl.id
                        Surface(
                            color = if (active) Color(tpl.accentHex).copy(alpha = 0.08f) else CardSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(if (active) 2.5.dp else 1.2.dp, Color(tpl.accentHex)),
                            modifier = Modifier
                                .widthIn(min = 150.dp, max = 220.dp)
                                .clickable { onApplyTemplate(tpl) }
                                .testTag("cert_tpl_${tpl.id}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Text(tpl.icon, fontSize = 24.sp)
                                Text(tpl.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(tpl.accentHex), textAlign = TextAlign.Center)
                                Text(tpl.subtitle, fontSize = 10.5.sp, color = MutedText, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Certificate Styling & Snippets
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box {
                        OutlinedButton(onClick = { fontMenuOpen = true }) {
                            Text("فۆنت: ${ExamFontOption.fromKey(cert.fontKey).label}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = fontMenuOpen, onDismissRequest = { fontMenuOpen = false }) {
                            ExamFontOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label, fontFamily = opt.fontFamily) },
                                    onClick = { onUpdateCert { it.copy(fontKey = opt.key) }; fontMenuOpen = false }
                                )
                            }
                        }
                    }

                    Box {
                        OutlinedButton(onClick = { borderMenuOpen = true }) {
                            Text(cert.borderStyle.label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = borderMenuOpen, onDismissRequest = { borderMenuOpen = false }) {
                            CertBorderStyle.entries.forEach { bs ->
                                DropdownMenuItem(
                                    text = { Text(bs.label) },
                                    onClick = { onUpdateCert { it.copy(borderStyle = bs) }; borderMenuOpen = false }
                                )
                            }
                        }
                    }

                    OutlinedButton(onClick = {
                        onUpdateCert { it.copy(fontScale = (it.fontScale + 0.1f).coerceIn(0.6f, 1.8f)) }
                    }) { Text("A+", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    OutlinedButton(onClick = {
                        onUpdateCert { it.copy(fontScale = (it.fontScale - 0.1f).coerceIn(0.6f, 1.8f)) }
                    }) { Text("A-", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    OutlinedButton(onClick = { colorTarget = "bg" }) { Text("🎨 ڕەنگێ پەڕێ", fontSize = 11.5.sp) }
                    OutlinedButton(onClick = { colorTarget = "txt" }) { Text("🎨 ڕەنگێ نڤێسینێ", fontSize = 11.5.sp) }
                    FilledTonalButton(onClick = { editTextDialog = true }) {
                        Text("✍️ دەستکاریا دەقێ باوەرنامێ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = {
                        imgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("🖼️ + لۆگۆ / وێنە", fontSize = 11.5.sp) }
                    OutlinedButton(onClick = { onAddSnippet(true) }) { Text("⭕ + مۆرا فەرمی", fontSize = 11.5.sp) }
                    OutlinedButton(onClick = { onAddSnippet(false) }) { Text("✍️ + ئیمزا", fontSize = 11.5.sp) }
                }
            }
        }

        // Live Ornamental Certificate Canvas
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val wDp = (maxWidth.value - 8f).coerceIn(280f, 720f)
            val aspect = if (cert.isLandscape) 210f / 297f else 297f / 210f
            val hDp = wDp * aspect
            val scale = (wDp / 760f).coerceIn(0.42f, 1.3f)
            val txtScale = scale * cert.fontScale

            val certBg = MediaUtils.parseColor(cert.bgColorHex, Color(0xFFF3FCFB))
            val certTxt = MediaUtils.parseColor(cert.textColorHex, Color(0xFF0F766E))
            val fontFamily = ExamFontOption.fromKey(cert.fontKey).fontFamily
            var certPxSize by remember { mutableStateOf(IntSize(1, 1)) }

            Surface(
                shadowElevation = 6.dp,
                color = certBg,
                modifier = Modifier
                    .width(wDp.dp)
                    .height(hDp.dp)
                    .clipToBounds()
                    .onSizeChanged { certPxSize = it }
                    .clickable { selectedFlId = null }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CertificateOrnamentCanvas(borderStyle = cert.borderStyle)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = (34f * scale).dp, vertical = (26f * scale).dp)
                            .clickable { editTextDialog = true }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (cert.topLine1.isNotBlank()) {
                                Text(
                                    cert.topLine1,
                                    color = certTxt, fontWeight = FontWeight.Bold,
                                    fontSize = (14f * txtScale).coerceAtLeast(8.5f).sp,
                                    fontFamily = fontFamily, textAlign = TextAlign.Center
                                )
                            }
                            if (cert.topLine2.isNotBlank()) {
                                Text(
                                    cert.topLine2,
                                    color = Color(0xFF475569),
                                    fontSize = (13f * txtScale).coerceAtLeast(8f).sp,
                                    fontFamily = fontFamily, textAlign = TextAlign.Center
                                )
                            }
                            Spacer(Modifier.height((10f * scale).dp))
                            if (cert.badgeTitle.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Brush.linearGradient(listOf(certTxt, Color(0xFF0F766E))),
                                            RoundedCornerShape(99.dp)
                                        )
                                        .border(1.5.dp, Color(0xFFEAB308), RoundedCornerShape(99.dp))
                                        .padding(horizontal = (24f * scale).dp, vertical = (4f * scale).dp)
                                ) {
                                    Text(
                                        cert.badgeTitle,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = (22f * txtScale).coerceAtLeast(12f).sp,
                                        fontFamily = fontFamily,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((6f * scale).dp),
                            modifier = Modifier.padding(horizontal = (12f * scale).dp)
                        ) {
                            if (cert.bodyTop.isNotBlank()) {
                                Text(
                                    cert.bodyTop,
                                    color = Color(0xFF1E293B),
                                    fontSize = (16f * txtScale).coerceAtLeast(9f).sp,
                                    lineHeight = (26f * txtScale).coerceAtLeast(13f).sp,
                                    fontFamily = fontFamily,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (cert.recipientName.isNotBlank()) {
                                Text(
                                    cert.recipientName,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = (21f * txtScale).coerceAtLeast(11f).sp,
                                    fontFamily = fontFamily,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (cert.bodyBottom.isNotBlank()) {
                                Text(
                                    cert.bodyBottom,
                                    color = Color(0xFF1E293B),
                                    fontSize = (16f * txtScale).coerceAtLeast(9f).sp,
                                    lineHeight = (26f * txtScale).coerceAtLeast(13f).sp,
                                    fontFamily = fontFamily,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(cert.footerLeft, cert.footerCenter, cert.footerRight).forEach { ft ->
                                Text(
                                    ft,
                                    color = certTxt,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (13.5f * txtScale).coerceAtLeast(8f).sp,
                                    fontFamily = fontFamily,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Floating Logo / Stamp / Signatures on Certificate
                    cert.floatingElements.forEach { fl ->
                        FloatingElementView(
                            element = fl,
                            paperPxSize = certPxSize,
                            isSelected = selectedFlId == fl.id,
                            fontFamily = fontFamily,
                            scale = scale,
                            onSelect = { selectedFlId = fl.id },
                            onMoveBy = { dx, dy ->
                                if (!fl.locked && certPxSize.width > 1 && certPxSize.height > 1) {
                                    onUpdateFloating(fl.id) {
                                        it.copy(
                                            xFrac = (it.xFrac + dx / certPxSize.width).coerceIn(0f, 0.9f),
                                            yFrac = (it.yFrac + dy / certPxSize.height).coerceIn(0f, 0.9f)
                                        )
                                    }
                                }
                            },
                            onTogglePin = { onUpdateFloating(fl.id) { it.copy(locked = !it.locked) } },
                            onResize = { f ->
                                if (!fl.locked) {
                                    onUpdateFloating(fl.id) {
                                        it.copy(
                                            wFrac = (it.wFrac * f).coerceIn(0.08f, 0.8f),
                                            hFrac = (it.hFrac * f).coerceIn(0.08f, 0.8f)
                                        )
                                    }
                                }
                            },
                            onRotate = { deg ->
                                if (!fl.locked) {
                                    onUpdateFloating(fl.id) { it.copy(rotationDeg = (it.rotationDeg + deg + 360f) % 360f) }
                                }
                            },
                            onEdit = { editingFloating = fl },
                            onPickColor = { floatingColorTargetId = fl.id },
                            onDelete = { onDeleteFloating(fl.id) }
                        )
                    }
                }
            }
        }
    }

    colorTarget?.let { target ->
        ColorPickerDialog(
            title = if (target == "bg") "ڕەنگێ پەڕێ باوەرنامێ" else "ڕەنگێ نڤێسینێ",
            currentHex = if (target == "bg") cert.bgColorHex else cert.textColorHex,
            onDismiss = { colorTarget = null },
            onColorSelected = { hex ->
                onUpdateCert {
                    if (target == "bg") it.copy(bgColorHex = hex) else it.copy(textColorHex = hex)
                }
            }
        )
    }

    if (editTextDialog) {
        var t1 by remember { mutableStateOf(cert.topLine1) }
        var t2 by remember { mutableStateOf(cert.topLine2) }
        var badge by remember { mutableStateOf(cert.badgeTitle) }
        var bTop by remember { mutableStateOf(cert.bodyTop) }
        var rec by remember { mutableStateOf(cert.recipientName) }
        var bBot by remember { mutableStateOf(cert.bodyBottom) }
        var fL by remember { mutableStateOf(cert.footerLeft) }
        var fC by remember { mutableStateOf(cert.footerCenter) }
        var fR by remember { mutableStateOf(cert.footerRight) }

        AlertDialog(
            onDismissRequest = { editTextDialog = false },
            title = { Text("✍️ دەستکاریا دەقێ باوەرنامێ", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = t1, onValueChange = { t1 = it }, label = { Text("دێڕا سەری ١") })
                    OutlinedTextField(value = t2, onValueChange = { t2 = it }, label = { Text("دێڕا سەری ٢") })
                    OutlinedTextField(value = badge, onValueChange = { badge = it }, label = { Text("ناڤنیشانێ سەرەکی") })
                    OutlinedTextField(value = bTop, onValueChange = { bTop = it }, label = { Text("دەقێ بەراهیێ") }, minLines = 2)
                    OutlinedTextField(value = rec, onValueChange = { rec = it }, label = { Text("ناڤێ بەڕێز / قوتابی") })
                    OutlinedTextField(value = bBot, onValueChange = { bBot = it }, label = { Text("دەقێ دوماهیێ") }, minLines = 2)
                    OutlinedTextField(value = fL, onValueChange = { fL = it }, label = { Text("خوارێ - لایێ چەپ/ئێک") })
                    OutlinedTextField(value = fC, onValueChange = { fC = it }, label = { Text("خوارێ - ناڤەڕاست") })
                    OutlinedTextField(value = fR, onValueChange = { fR = it }, label = { Text("خوارێ - لایێ ڕاست/سێ") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateCert {
                        it.copy(
                            topLine1 = t1, topLine2 = t2, badgeTitle = badge,
                            bodyTop = bTop, recipientName = rec, bodyBottom = bBot,
                            footerLeft = fL, footerCenter = fC, footerRight = fR
                        )
                    }
                    editTextDialog = false
                }) { Text("خەزن") }
            },
            dismissButton = { TextButton(onClick = { editTextDialog = false }) { Text("لابردن") } }
        )
    }

    editingFloating?.let { fl ->
        FloatingElementEditDialog(
            element = fl,
            onDismiss = { editingFloating = null },
            onSave = { updated ->
                onUpdateFloating(fl.id) { updated }
                editingFloating = null
            }
        )
    }

    floatingColorTargetId?.let { targetId ->
        val cur = cert.floatingElements.firstOrNull { it.id == targetId }?.strokeColorHex ?: "#B45309"
        ColorPickerDialog(
            title = "ڕەنگێ توخمێ",
            currentHex = cur,
            onDismiss = { floatingColorTargetId = null },
            onColorSelected = { hex ->
                onUpdateFloating(targetId) { it.copy(strokeColorHex = hex) }
            }
        )
    }
}
