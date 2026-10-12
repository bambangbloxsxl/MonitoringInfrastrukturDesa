package id.desa.monitoring

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF123B67)
private val Blue = Color(0xFF1769AA)
private val Pale = Color(0xFFF2F6FA)

class MainActivity : ComponentActivity() {
    private var pendingPhotoIndex = 0
    private lateinit var photoFiles: Array<File>
    private val fields = listOf(
        "Sumber Dana", "Tahun Anggaran", "Nama Desa",
        "Nama Kepala Desa", "Nomor HP Kepala Desa",
        "Nama Bendahara", "Nomor HP Bendahara",
        "Nama Ketua TPK", "Nomor HP Ketua TPK",
        "Nama Paket Pekerjaan", "Volume", "Pagu (Rp)",
        "Realisasi Anggaran (Rp)", "Waktu Pelaksanaan",
        "Persentase Fisik (%)", "Keterangan"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dir = File(filesDir, "foto_monitoring").apply { mkdirs() }
        photoFiles = Array(4) { File(dir, "foto_${it+1}.jpg") }
        setContent { MonitoringApp() }
    }

    @Composable
    private fun MonitoringApp() {
        val prefs = getSharedPreferences("monitoring_draft", MODE_PRIVATE)
        val values = remember { mutableStateMapOf<String, String>().apply {
            fields.forEach { put(it, prefs.getString(it, "") ?: "") }
        } }
        var selectedPhoto by remember { mutableIntStateOf(0) }
        var useFront by remember { mutableStateOf(false) }
        var refresh by remember { mutableIntStateOf(0) }
        var status by remember { mutableStateOf("Data disimpan lokal di perangkat ini.") }

        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                refresh++
                status = "Foto ${selectedPhoto + 1} berhasil disimpan."
            } else status = "Pengambilan foto dibatalkan."
        }
        val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            if (uri != null) {
                try {
                    val pdf = createPdf(values.toMap())
                    contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
                    pdf.close()
                    status = "PDF berhasil dibuat."
                    Toast.makeText(this, "PDF berhasil disimpan", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    status = "Gagal membuat PDF: ${e.localizedMessage}"
                }
            }
        }

        MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Navy)) {
            Column(Modifier.fillMaxSize().background(Pale)) {
                Column(Modifier.fillMaxWidth().background(Navy).padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Text("MONITORING", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Infrastruktur Desa", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Dokumentasi lapangan • Offline • PDF A4", color = Color(0xFFD7E8F8), fontSize = 13.sp)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitle("01", "Dokumentasi Foto")
                            Text("Ambil 4 foto. Pilihan kamera depan/belakang akan diteruskan ke aplikasi kamera perangkat; dukungan pemilihan kamera bergantung pada aplikasi kamera.", fontSize = 12.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(selected = !useFront, onClick = { useFront = false }, label = { Text("Belakang") })
                                FilterChip(selected = useFront, onClick = { useFront = true }, label = { Text("Depan") })
                            }
                            (0..3).forEach { i ->
                                val exists = photoFiles[i].exists()
                                Row(Modifier.fillMaxWidth().background(Pale, RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (exists) {
                                        val bmp = remember(refresh, exists) { BitmapFactory.decodeFile(photoFiles[i].absolutePath) }
                                        if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.size(72.dp, 58.dp))
                                    } else Box(Modifier.size(72.dp, 58.dp).background(Color(0xFFDDE6EF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Text("${i+1}", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("Foto ${i+1}", fontWeight = FontWeight.SemiBold)
                                        Text(if (exists) "Foto tersimpan" else "Belum ada foto", color = Color.Gray, fontSize = 12.sp)
                                    }
                                    Button(onClick = {
                                        selectedPhoto = i
                                        val uri = androidx.core.content.FileProvider.getUriForFile(this@MainActivity, "$packageName.provider", photoFiles[i])
                                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                            putExtra(MediaStore.EXTRA_OUTPUT, uri)
                                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            putExtra("android.intent.extras.CAMERA_FACING", if (useFront) 1 else 0)
                                            putExtra("android.intent.extras.LENS_FACING_FRONT", useFront)
                                        }
                                        try { cameraLauncher.launch(intent) } catch (e: Exception) { status = "Aplikasi kamera tidak tersedia." }
                                    }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Text("Ambil") }
                                }
                            }
                            Text("Catatan: resolusi 4080 × 1836 hanya dapat tercapai jika didukung sensor dan aplikasi kamera HP. Aplikasi menyimpan foto asli yang dihasilkan kamera.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitle("02", "Identitas Desa & Pengurus")
                            fields.take(9).forEach { field -> DataField(field, values[field] ?: "") { values[field] = it } }
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitle("03", "Data Paket Pekerjaan")
                            fields.drop(9).forEach { field -> DataField(field, values[field] ?: "", multiline = field == "Keterangan") { values[field] = it } }
                        }
                    }
                    Button(onClick = {
                        prefs.edit().apply { values.forEach { (k,v) -> putString(k,v) } }.apply()
                        status = "Draft tersimpan di perangkat."
                    }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Simpan Data Offline") }
                    Button(onClick = {
                        prefs.edit().apply { values.forEach { (k,v) -> putString(k,v) } }.apply()
                        pdfLauncher.launch("Laporan_Monitoring_Desa_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf")
                    }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Navy)) { Text("Ekspor Laporan PDF A4") }
                    Text(status, color = Blue, fontSize = 12.sp)
                    Text("Versi awal • Data dan foto tersimpan secara lokal. Ekspor PDF dilakukan saat diminta.", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }

    @Composable
    private fun SectionTitle(number: String, title: String) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).background(Navy, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Text(number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
        }
    }

    @Composable
    private fun DataField(label: String, value: String, multiline: Boolean = false, onChange: (String) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
                minLines = if (multiline) 3 else 1, maxLines = if (multiline) 5 else 1,
                placeholder = { Text("Masukkan $label", fontSize = 13.sp) }, shape = RoundedCornerShape(10.dp))
        }
    }

    private fun createPdf(data: Map<String, String>): PdfDocument {
        val pdf = PdfDocument()
        val pageW = 595
        val pageH = 842
        val margin = 38f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(18,59,103); textSize = 18f; isFakeBoldText = true }
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(18,59,103); textSize = 11f; isFakeBoldText = true }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.DKGRAY; textSize = 9.5f }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.GRAY; textSize = 8f }
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var y = 0f
        fun newPage() {
            page?.let { pdf.finishPage(it) }
            pageNo++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            canvas = page!!.canvas
            y = 42f
            canvas!!.drawColor(android.graphics.Color.WHITE)
            canvas!!.drawText("MONITORING INFRASTRUKTUR DESA", margin, y, titlePaint); y += 18
            canvas!!.drawText("Laporan hasil monitoring lapangan", margin, y, smallPaint); y += 22
            canvas!!.drawLine(margin, y, pageW-margin, y, headPaint); y += 18
        }
        fun line(label: String, value: String) {
            if (y > pageH - 65) newPage()
            canvas!!.drawText(label, margin, y, headPaint)
            val maxWidth = pageW - margin*2 - 150
            val safe = value.ifBlank { "-" }
            val chunks = mutableListOf<String>()
            var current = ""
            safe.split(Regex("\\s+")).forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (textPaint.measureText(candidate) > maxWidth && current.isNotEmpty()) { chunks.add(current); current = word } else current = candidate
            }
            if (current.isNotEmpty()) chunks.add(current)
            chunks.forEachIndexed { idx, chunk ->
                if (y > pageH - 65) newPage()
                canvas!!.drawText(chunk, margin + 150, y, textPaint)
                if (idx < chunks.lastIndex) y += 12
            }
            y += 17
        }
        newPage()
        listOf("Sumber Dana","Tahun Anggaran","Nama Desa","Nama Kepala Desa","Nomor HP Kepala Desa",
            "Nama Bendahara","Nomor HP Bendahara","Nama Ketua TPK","Nomor HP Ketua TPK",
            "Nama Paket Pekerjaan","Volume","Pagu (Rp)","Realisasi Anggaran (Rp)",
            "Waktu Pelaksanaan","Persentase Fisik (%)","Keterangan").forEach { line(it, data[it] ?: "") }
        y += 4
        if (y > pageH - 150) newPage()
        canvas!!.drawText("DOKUMENTASI FOTO", margin, y, headPaint); y += 14
        val photoW = 245f; val photoH = 150f; val gap = 20f
        photoFiles.forEachIndexed { i, file ->
            val col = i % 2
            val row = i / 2
            val x = margin + col * (photoW + gap)
            val top = y + row * (photoH + 24f)
            if (top + photoH + 30 > pageH - 35) {
                newPage()
            }
            val actualTop = if (top + photoH + 30 > pageH - 35) y else top
            canvas!!.drawRect(x, actualTop, x+photoW, actualTop+photoH, Paint().apply { color = android.graphics.Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 1f })
            canvas!!.drawText("Foto ${i+1}", x+5, actualTop+12, smallPaint)
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp != null) {
                    val dest = RectF(x+3, actualTop+16, x+photoW-3, actualTop+photoH-3)
                    canvas!!.drawBitmap(bmp, null, dest, Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
                    bmp.recycle()
                } else canvas!!.drawText("Gambar tidak dapat dibaca", x+8, actualTop+80, textPaint)
            } else canvas!!.drawText("Foto belum tersedia", x+8, actualTop+80, textPaint)
        }
        val lastY = pageH - 34f
        canvas!!.drawText("Dicetak: ${SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("id","ID")).format(Date())}", margin, lastY, smallPaint)
        canvas!!.drawText("Tanda tangan petugas: ____________________", pageW-220f, lastY, smallPaint)
        page?.let { pdf.finishPage(it) }
        return pdf
    }
}
