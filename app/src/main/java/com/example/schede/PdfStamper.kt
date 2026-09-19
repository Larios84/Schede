package com.example.schede

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import java.io.IOException

class PdfStamper(private val context: Context) {

    init {
        try {
            PDFBoxResourceLoader.init(context)
        } catch (e: Exception) {
            Log.e("PdfStamper", "Errore inizializzazione PDFBox: ${e.message}")
        }
    }

    @Throws(IOException::class)
    fun fillPdfFromAssets(
        assetName: String, 
        outputName: String, 
        fields: Map<String, String>, 
        treeUri: Uri?,
        signature: Bitmap? = null
    ): Uri? {
        var document: PDDocument? = null
        try {
            val finalFileName = if (outputName.endsWith(".pdf")) outputName else "$outputName.pdf"
            val inputStream = context.assets.open(assetName)
            document = PDDocument.load(inputStream)
            
            val acroForm: PDAcroForm = document.documentCatalog.acroForm ?: return null

            // DEBUG: Ispezione nomi campi reali nel PDF
            Log.d("ISPEZIONE_PDF", "--- INIZIO ELENCO CAMPI NEL TEMPLATE: $assetName ---")
            acroForm.fields.forEach { field ->
                Log.d("ISPEZIONE_PDF", "Nome campo: ${field.fullyQualifiedName}")
            }
            Log.d("ISPEZIONE_PDF", "--- FINE ELENCO CAMPI ---")

            // 1. Inserimento valori
            fields.forEach { (key, value) ->
                try {
                    acroForm.getField(key)?.setValue(value)
                } catch (e: Exception) {
                    Log.e("PdfStamper", "Errore campo $key: ${e.message}")
                }
            }

            // 2. Refresh grafico
            try { acroForm.refreshAppearances() } catch (e: Exception) {}
            
            // 3. Firma
            if (signature != null) {
                try {
                    val pdImage = LosslessFactory.createFromImage(document, signature)
                    val page = document.getPage(0)
                    PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { contentStream ->
                        contentStream.drawImage(pdImage, 420f, 60f, 130f, 40f)
                    }
                } catch (e: Exception) {
                    Log.e("PdfStamper", "Errore firma: ${e.message}")
                }
            }

            // 4. Appiattimento
            try { acroForm.flatten() } catch (e: Exception) {}

            // 5. Salvataggio
            if (treeUri != null) {
                val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                val newFile = pickedDir?.createFile("application/pdf", finalFileName)
                newFile?.uri?.let { uri ->
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        document.save(out)
                    }
                    return uri
                }
            }
            
            return null

        } catch (e: Exception) {
            Log.e("PdfStamper", "Errore generazione PDF: ${e.message}")
            return null
        } finally {
            try {
                document?.close()
            } catch (e: Exception) {
                Log.e("PdfStamper", "Errore chiusura documento: ${e.message}")
            }
        }
    }
}
