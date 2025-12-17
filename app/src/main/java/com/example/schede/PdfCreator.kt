package com.example.schede

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.io.IOException

class PdfCreator(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Crea un nuovo file PDF e lo salva nella cartella pubblica Documenti.
     * @return L'Uri del file creato, o null in caso di errore.
     */
    @Throws(IOException::class)
    fun createPdf(outputName: String, title: String, content: String): Uri? {
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page)

        val contentStream = PDPageContentStream(document, page)

        // ... (il codice per aggiungere il contenuto rimane invariato)
        contentStream.beginText()
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20f)
        contentStream.setLeading(14.5f)
        contentStream.newLineAtOffset(50f, 700f)
        contentStream.showText(title)
        contentStream.endText()
        contentStream.beginText()
        contentStream.setFont(PDType1Font.HELVETICA, 12f)
        contentStream.newLineAtOffset(50f, 650f)
        val lines = content.split('\n').flatMap { line ->
            val words = line.split(' ')
            val lines = mutableListOf<String>()
            var currentLine = ""
            for (word in words) {
                if (currentLine.isEmpty()) {
                    currentLine = word
                } else if (currentLine.length + word.length + 1 < 80) {
                    currentLine += " $word"
                } else {
                    lines.add(currentLine)
                    currentLine = word
                }
            }
            lines.add(currentLine)
            lines
        }
        for (line in lines) {
            contentStream.showText(line)
            contentStream.newLine()
        }
        contentStream.endText()
        contentStream.close()

        // --- NUOVA LOGICA DI SALVATAGGIO ---
        var resultUri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Per Android 10 e versioni successive (usa MediaStore)
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, outputName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            }
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    document.save(outputStream)
                }
                resultUri = it
            }
        } else {
            // Per versioni di Android più vecchie
            @Suppress("DEPRECATION")
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            documentsDir.mkdirs()
            val outputFile = File(documentsDir, outputName)
            document.save(outputFile)
            resultUri = Uri.fromFile(outputFile)
        }
        document.close()
        return resultUri
    }
}
