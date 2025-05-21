package com.example.ventaplus.utils

import android.content.Context
import android.graphics.*
import android.os.Environment
import android.util.Log
import com.example.ventaplus.ui.screens.Venta
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PDFUtils {

    fun generatePDF(
        context: Context,
        venta: Venta,
        negocio: String,
        admin: String,
        logoResId: Int
    ): File? {
        // Ajusta el alto para que quepan todos los productos
        val pageWidth = 300
        val pageHeight = 700 + venta.productos.size * 30

        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint()

        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 16f
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = Color.BLACK
            textAlign = Paint.Align.LEFT
        }

        val smallPaint = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 12f
            color = Color.DKGRAY
            textAlign = Paint.Align.LEFT
        }

        val highlightPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            textSize = 13f
            color = Color.rgb(0, 100, 200) // Azul llamativo
            textAlign = Paint.Align.LEFT
        }

        var yPos = 30

        // Título principal centrado arriba
        canvas.drawText("COMPROBANTE DE COMPRA", (pageWidth / 2).toFloat(), yPos.toFloat(), headerPaint)
        yPos += 40

        // Logo (más grande)
        try {
            val bitmap = BitmapFactory.decodeResource(context.resources, logoResId)
            val scaledLogo = Bitmap.createScaledBitmap(bitmap, 80, 80, false)
            canvas.drawBitmap(scaledLogo, (pageWidth - 80) / 2f, yPos.toFloat(), paint)
            yPos += 90
        } catch (e: Exception) {
            Log.e("PDFUtils", "Error loading logo: ${e.message}")
        }

        // Datos del negocio y admin
        canvas.drawText("Negocio: $negocio", 10f, yPos.toFloat(), titlePaint)
        yPos += 20

        canvas.drawText("Administrador: $admin", 10f, yPos.toFloat(), smallPaint)
        yPos += 20

        canvas.drawText("Fecha: ${venta.fecha}", 10f, yPos.toFloat(), smallPaint)
        yPos += 25

        // Línea separadora
        paint.color = Color.GRAY
        paint.strokeWidth = 1f
        canvas.drawLine(0f, yPos.toFloat(), pageWidth.toFloat(), yPos.toFloat(), paint)
        yPos += 15

        // Productos con cantidad y precio
        for ((_, producto) in venta.productos) {
            canvas.drawText("${producto.nombre} x${producto.cantidad}", 10f, yPos.toFloat(), smallPaint)
            canvas.drawText("$${producto.precio * producto.cantidad}", 200f, yPos.toFloat(), smallPaint)
            yPos += 22
        }

        yPos += 10
        // Línea separadora antes del total
        paint.color = Color.GRAY
        paint.strokeWidth = 1.5f
        canvas.drawLine(0f, yPos.toFloat(), pageWidth.toFloat(), yPos.toFloat(), paint)
        yPos += 25

        // Total destacado
        canvas.drawText("TOTAL: $${venta.total}", 10f, yPos.toFloat(), titlePaint)
        yPos += 40

        // Frases llamativas de agradecimiento
        canvas.drawText("¡Gracias por su compra!", 10f, yPos.toFloat(), highlightPaint)
        yPos += 20
        canvas.drawText("Le agradece $negocio", 10f, yPos.toFloat(), highlightPaint)
        yPos += 20
        canvas.drawText("¡Vuelva pronto!", 10f, yPos.toFloat(), highlightPaint)
        yPos += 20

        pdfDocument.finishPage(page)

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "Venta_${sdf.format(Date())}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            Log.e("PDFUtils", "Error writing PDF: ${e.message}")
            pdfDocument.close()
            null
        }
    }
}