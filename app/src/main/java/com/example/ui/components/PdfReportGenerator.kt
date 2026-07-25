package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.model.Expense
import com.example.data.model.Group
import com.example.data.model.Member
import com.example.ui.theme.Localization
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private fun createPdfDocument(
        group: Group?,
        members: List<Member>,
        allMembersList: List<Member>,
        expenses: List<Expense>,
        balances: Map<Int, Double>,
        costPerPerson: Double,
        isFarsi: Boolean,
        customCurrency: String?,
        reportTitle: String? = null
    ): Pair<PdfDocument, String> {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 width=595, height=842
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = 50f

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1E3A8A") // Dark blue
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#4B5563") // Gray
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.parseColor("#1F2937") // Dark gray
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#374151")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldBodyPaint = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val greenPaint = Paint().apply {
            color = Color.parseColor("#059669")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val redPaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1.5f
        }

        fun checkPageBreak(neededHeight: Float) {
            if (y + neededHeight > 780f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
        }

        // Draw Header
        val title = reportTitle ?: if (group != null) {
            if (isFarsi) "گزارش مالی گروه: ${group.name}" else "Financial Report: ${group.name}"
        } else {
            if (isFarsi) "گزارش جامع هزینه‌ها و تفکیک حساب‌ها" else "Comprehensive Expense & Split Report"
        }
        canvas.drawText(title, 40f, y, titlePaint)
        y += 24f

        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
        val dateLabel = if (isFarsi) "تاریخ تولید گزارش: $dateStr" else "Report Generated: $dateStr"
        canvas.drawText(dateLabel, 40f, y, subtitlePaint)
        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Summary Section
        checkPageBreak(80f)
        canvas.drawText(if (isFarsi) " خلاصه آمار و سرانه:" else " Summary Statistics:", 40f, y, headerPaint)
        y += 22f

        val totalSpending = expenses.sumOf { it.amount }
        val totalHc = members.sumOf { it.headcount.coerceAtLeast(1) }.coerceAtLeast(1)
        val spendingTxt = (if (isFarsi) "مجموع مخارج: " else "Total Spending: ") + Localization.formatCurrency(totalSpending, isFarsi, customCurrency)
        val hcTxt = (if (isFarsi) "تعداد کل نفرات: " else "Total Headcount: ") + "$totalHc " + (if (isFarsi) "نفر" else "people")
        val cppTxt = (if (isFarsi) "سرانه هر نفر: " else "Cost Per Person: ") + Localization.formatCurrency(costPerPerson, isFarsi, customCurrency)

        canvas.drawText("• $spendingTxt", 50f, y, boldBodyPaint)
        y += 18f
        canvas.drawText("• $hcTxt", 50f, y, bodyPaint)
        y += 18f
        canvas.drawText("• $cppTxt", 50f, y, greenPaint)
        y += 25f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Member & Group Breakdown Section
        if (members.isNotEmpty()) {
            checkPageBreak(40f)
            val breakTitle = if (group?.groupType == "MULTI_GROUP_TRIP") {
                if (isFarsi) " تفکیک سهم و وضعیت پرداخت هر گروه (مرحله اول و دوم):" else " Group & Per-Person Breakdown (Stages 1 & 2):"
            } else {
                if (isFarsi) " وضعیت پرداخت و تسویه حساب اعضا:" else " Member Balance & Settlement Breakdown:"
            }
            canvas.drawText(breakTitle, 40f, y, headerPaint)
            y += 22f

            members.forEach { m ->
                checkPageBreak(70f)
                val grpId = m.userId?.removePrefix("GROUP_")?.toIntOrNull()
                val masterPayer = if (grpId != null) allMembersList.find { it.groupId == grpId }?.name else null
                val grpShare = costPerPerson * m.headcount.coerceAtLeast(1)
                val grpPaid = expenses.filter { it.payerId == m.id }.sumOf { it.amount }
                val netBal = balances[m.id] ?: (grpPaid - grpShare)

                val nameLine = m.name + if (masterPayer != null) " (👑 مادر خرج: $masterPayer)" else ""
                canvas.drawText("▶ $nameLine (${m.headcount} ${if (isFarsi) "نفر" else "p"})", 50f, y, boldBodyPaint)
                y += 18f

                val shareStr = (if (isFarsi) "سهم کل: " else "Total Share: ") + Localization.formatCurrency(grpShare, isFarsi, customCurrency)
                val paidStr = (if (isFarsi) "پرداخت شده: " else "Paid: ") + Localization.formatCurrency(grpPaid, isFarsi, customCurrency)
                canvas.drawText("    $shareStr | $paidStr", 55f, y, bodyPaint)
                y += 18f

                val balTxt = if (kotlin.math.abs(netBal) < 1.0) {
                    if (isFarsi) "وضعیت: تسویه کامل" else "Status: Settled"
                } else if (netBal > 0) {
                    (if (isFarsi) "بستانکار / دریافت از جمع: +" else "Receives: +") + Localization.formatCurrency(netBal, isFarsi, customCurrency)
                } else {
                    (if (isFarsi) "بدهکار / پرداخت به جمع: -" else "Pays: -") + Localization.formatCurrency(kotlin.math.abs(netBal), isFarsi, customCurrency)
                }
                val balPaint = if (netBal > 0) greenPaint else if (netBal < -1.0) redPaint else bodyPaint
                canvas.drawText("    $balTxt", 55f, y, balPaint)
                y += 20f

                // Stage 2: Inner members if multi-group trip
                val innerMems = if (grpId != null) allMembersList.filter { it.groupId == grpId } else emptyList()
                if (innerMems.isNotEmpty()) {
                    checkPageBreak(innerMems.size * 20f + 25f)
                    canvas.drawText("    👥 " + (if (isFarsi) "تفکیک نفرات داخلی گروه:" else "Inner Family Members Breakdown:"), 65f, y, subtitlePaint)
                    y += 16f
                    innerMems.forEachIndexed { idx, ind ->
                        val indShare = costPerPerson * ind.headcount.coerceAtLeast(1)
                        val indPaid = expenses.filter { it.payerId == m.id && (it.actualPayerName == ind.name || (it.actualPayerName == null && idx == 0)) }.sumOf { it.amount }
                        val indNet = indPaid - indShare

                        val indBalTxt = if (kotlin.math.abs(indNet) < 1.0) {
                            if (isFarsi) "تسویه" else "Settled"
                        } else if (indNet > 0) {
                            (if (isFarsi) "طالب: +" else "+") + Localization.formatCurrency(indNet, isFarsi, customCurrency)
                        } else {
                            (if (isFarsi) "بدهی: -" else "-") + Localization.formatCurrency(kotlin.math.abs(indNet), isFarsi, customCurrency)
                        }
                        val indLine = "      • ${ind.name} (${ind.headcount} ${if (isFarsi) "نفر" else "p"})" + if (idx == 0) " (👑)" else ""
                        val indDetail = "سهم: ${Localization.formatCurrency(indShare, isFarsi, customCurrency)} | $indBalTxt"
                        canvas.drawText("$indLine -> $indDetail", 70f, y, if (indNet > 0) greenPaint else if (indNet < -1.0) redPaint else bodyPaint)
                        y += 18f
                    }
                    y += 6f
                }
            }
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 25f
        }

        // Expense History Table Section
        checkPageBreak(60f)
        canvas.drawText(if (isFarsi) " لیست جزئیات تمام هزینه‌ها و خریدها:" else " Detailed Expense History:", 40f, y, headerPaint)
        y += 22f

        if (expenses.isEmpty()) {
            canvas.drawText(if (isFarsi) "هیچ هزینه‌ای ثبت نشده است." else "No expenses recorded yet.", 50f, y, subtitlePaint)
            y += 20f
        } else {
            // Table Header
            canvas.drawRect(40f, y - 14f, 555f, y + 8f, Paint().apply { color = Color.parseColor("#F3F4F6") })
            canvas.drawText(if (isFarsi) "عنوان هزینه" else "Title", 45f, y, boldBodyPaint)
            canvas.drawText(if (isFarsi) "خریدار / پرداخت‌کننده" else "Payer", 200f, y, boldBodyPaint)
            canvas.drawText(if (isFarsi) "دسته‌بندی" else "Category", 380f, y, boldBodyPaint)
            canvas.drawText(if (isFarsi) "مبلغ" else "Amount", 480f, y, boldBodyPaint)
            y += 22f

            expenses.forEach { exp ->
                checkPageBreak(25f)
                val payerMem = members.find { it.id == exp.payerId }
                val payerName = if (!exp.actualPayerName.isNullOrBlank() && exp.actualPayerName != payerMem?.name) {
                    "${exp.actualPayerName} (${payerMem?.name ?: ""})"
                } else {
                    payerMem?.name ?: (if (isFarsi) "ناشناس" else "Unknown")
                }
                val catName = Localization.getString("category_${exp.category.lowercase()}", isFarsi)
                val amtStr = Localization.formatCurrency(exp.amount, isFarsi, customCurrency)

                // Truncate long strings to fit columns
                val truncTitle = if (exp.title.length > 18) exp.title.take(16) + ".." else exp.title
                val truncPayer = if (payerName.length > 20) payerName.take(18) + ".." else payerName

                canvas.drawText(truncTitle, 45f, y, bodyPaint)
                canvas.drawText(truncPayer, 200f, y, bodyPaint)
                canvas.drawText(catName, 380f, y, subtitlePaint)
                canvas.drawText(amtStr, 480f, y, boldBodyPaint)
                y += 20f
                canvas.drawLine(40f, y - 4f, 555f, y - 4f, Paint().apply { color = Color.parseColor("#F9FAFB"); strokeWidth = 1f })
            }
        }

        pdfDocument.finishPage(page)
        return Pair(pdfDocument, title)
    }

    fun generateAndSharePdf(
        context: Context,
        group: Group?,
        members: List<Member>,
        allMembersList: List<Member>,
        expenses: List<Expense>,
        balances: Map<Int, Double>,
        costPerPerson: Double,
        isFarsi: Boolean,
        customCurrency: String?,
        reportTitle: String? = null
    ) {
        try {
            val (pdfDocument, title) = createPdfDocument(group, members, allMembersList, expenses, balances, costPerPerson, isFarsi, customCurrency, reportTitle)
            val fileName = "SplitEase_Report_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, if (isFarsi) "اشتراک‌گذاری گزارش PDF" else "Share PDF Report"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, if (isFarsi) "خطا در ایجاد خروجی PDF: ${e.message}" else "Error creating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun savePdfLocally(
        context: Context,
        group: Group?,
        members: List<Member>,
        allMembersList: List<Member>,
        expenses: List<Expense>,
        balances: Map<Int, Double>,
        costPerPerson: Double,
        isFarsi: Boolean,
        customCurrency: String?,
        reportTitle: String? = null
    ) {
        try {
            val (pdfDocument, _) = createPdfDocument(group, members, allMembersList, expenses, balances, costPerPerson, isFarsi, customCurrency, reportTitle)
            val fileName = "SplitEase_Report_${System.currentTimeMillis()}.pdf"
            var savedPath = ""

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/SplitEase")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    savedPath = "Downloads/SplitEase/$fileName"
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "SplitEase")
                if (!appDir.exists()) appDir.mkdirs()
                val file = File(appDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                savedPath = file.absolutePath
            }
            pdfDocument.close()

            Toast.makeText(
                context,
                if (isFarsi) "✅ فایل PDF در پوشه $savedPath ذخیره شد." else "✅ PDF saved to $savedPath",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, if (isFarsi) "خطا در ذخیره محلی PDF: ${e.message}" else "Error saving PDF locally: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun PdfExportDialog(
    onDismiss: () -> Unit,
    onSaveLocally: () -> Unit,
    onShareDirectly: () -> Unit,
    isFarsi: Boolean,
    title: String = if (isFarsi) "انتخاب روش دریافت گزارش PDF" else "Select PDF Export Option"
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isFarsi) "می‌توانید گزارش را در حافظه گوشی (پوشه Downloads) ذخیره کنید یا مستقیم از طریق تلگرام، واتساپ و ایمیل به اشتراک بگذارید." else "You can save the report locally to your device storage (Downloads folder) or share it directly via messaging apps or email.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onSaveLocally(); onDismiss() },
                        modifier = Modifier.fillMaxWidth().testTag("pdf_save_local_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (isFarsi) "💾 ذخیره محلی در حافظه گوشی (Downloads)" else "💾 Save Locally to Device Storage",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { onShareDirectly(); onDismiss() },
                        modifier = Modifier.fillMaxWidth().testTag("pdf_share_direct_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (isFarsi) "📤 اشتراک‌گذاری مستقیم (تلگرام، واتساپ و...)" else "📤 Share Directly via Apps",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = if (isFarsi) "انصراف" else "Cancel")
                }
            }
        }
    }
}
