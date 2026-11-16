package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExamDocumentState
import com.example.model.HeaderFieldKey
import com.example.model.LogoPosition
import com.example.ui.theme.*
import com.example.util.MediaUtils

@Composable
fun ExamHeaderScreen(
    doc: ExamDocumentState,
    onToggleLock: () -> Unit,
    onSavePreset: () -> Unit,
    onLoadPreset: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit,
    onResetHeader: () -> Unit,
    onUpdateField: (HeaderFieldKey, String) -> Unit,
    onToggleFieldVisibility: (HeaderFieldKey, Boolean) -> Unit,
    onSetLogoBase64: (String) -> Unit,
    onSetLogoPos: (LogoPosition) -> Unit,
    onChangeLogoSize: (Int) -> Unit
) {
    val context = LocalContext.current
    val hdr = doc.header

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            MediaUtils.uriToBase64(context, it, maxDim = 480)?.let(onSetLogoBase64)
        }
    }

    val jsonImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            }.getOrNull()?.let(onImportJson)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .widthIn(max = 860.dp)
    ) {
        // Lock & Preset Bar
        Surface(
            color = if (hdr.locked) EmeraldContainer else AmberContainer,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, if (hdr.locked) EmeraldSuccess else AmberWarm),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = if (hdr.locked) {
                        "🔒 کلێشە یا جێگیرە (قفل) — هەتا تە خلاس نەکربیت ناهێتە گوهۆڕین"
                    } else {
                        "🔓 کلێشە یا ڤەکرییە بۆ گوهۆڕینێ (دشێی کلێشەی سابت بکەی تا ئەسیلا خۆ خلاس دکەی)"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = if (hdr.locked) Color(0xFF14532D) else SlateNavy
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onToggleLock,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hdr.locked) EmeraldSuccess else AmberWarm
                        ),
                        modifier = Modifier.testTag("header_lock_button")
                    ) {
                        Icon(
                            if (hdr.locked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (hdr.locked) "ڤەکرنا کلێشەی" else "سابتکرنا کلێشەی (قفل)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onSavePreset,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier.testTag("save_header_preset_button")
                    ) {
                        Text("📌 خەزنکرنا کلێشێ بۆ هەردەم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onLoadPreset,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.testTag("load_header_preset_button")
                    ) {
                        Text("↺ گەڕاندنەڤا کلێشێ خەزنکری", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // File Export/Import & Reset Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⚙️ زانیاریێن کلێشە (بەتاڵ یا نمرێ گشتی = خۆکار کۆم دکەت)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = onExportJson,
                        modifier = Modifier.testTag("export_json_button")
                    ) {
                        Text("📥 فایل (JSON)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { jsonImporter.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.testTag("import_json_button")
                    ) {
                        Text("📂 ڤەکرنا فایل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onResetHeader,
                        modifier = Modifier.testTag("reset_header_button")
                    ) {
                        Text("↺ ڤەگەڕاندنا کلێشەی", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // 13 Header Fields (2-column grid on wide screens, 1-column on phones)
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val twoCols = maxWidth >= 560.dp
                    val rows = if (twoCols) HeaderFieldKey.entries.chunked(2) else HeaderFieldKey.entries.map { listOf(it) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowKeys ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowKeys.forEach { key ->
                                    val visible = hdr.visible[key] != false
                                    val value = hdr.values[key].orEmpty()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = visible,
                                            enabled = !hdr.locked,
                                            onCheckedChange = { onToggleFieldVisibility(key, it) }
                                        )
                                        OutlinedTextField(
                                            value = value,
                                            onValueChange = { onUpdateField(key, it) },
                                            enabled = !hdr.locked,
                                            label = { Text(key.label, fontSize = 12.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                if (twoCols && rowKeys.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // School / Ministry Logo Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    "🖼️ لۆگۆیێ قوتابخانێ / وەزارەتێ د ناڤ کلێشەی دا:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )

                val logoBmp = remember(hdr.logoBase64) { MediaUtils.decodeBase64ImageBitmap(hdr.logoBase64) }
                if (logoBmp != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Image(
                            bitmap = logoBmp,
                            contentDescription = "Selected school logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(hdr.logoSizeDp.dp)
                        )
                    }
                }

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !hdr.locked,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier.testTag("pick_header_logo_button")
                    ) {
                        Text("+ هەڵبژارتنا لۆگۆی", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    LogoPosition.entries.forEach { pos ->
                        FilterChip(
                            selected = hdr.logoPos == pos,
                            enabled = !hdr.locked,
                            onClick = { onSetLogoPos(pos) },
                            label = { Text(pos.label, fontSize = 11.5.sp) }
                        )
                    }
                    OutlinedButton(
                        onClick = { onChangeLogoSize(-8) },
                        enabled = !hdr.locked
                    ) { Text("لۆگۆ －", fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { onChangeLogoSize(8) },
                        enabled = !hdr.locked
                    ) { Text("لۆگۆ ＋", fontWeight = FontWeight.Bold) }
                    if (hdr.logoBase64.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onSetLogoBase64("") },
                            enabled = !hdr.locked,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDanger)
                        ) { Text("✕ لادانا لۆگۆی", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
