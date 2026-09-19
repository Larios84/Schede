package com.example.schede

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import java.io.File
import java.io.IOException

class Md121PdfStamper(private val context: Context, private val templateName: String) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun generaPdfCompilato(nomeFile: String, dati: Map<String, String>): Uri? {
        try {
            val inputStream = context.assets.open(templateName)
            val document = PDDocument.load(inputStream)
            val acroForm: PDAcroForm? = document.documentCatalog.acroForm

            if (acroForm != null) {
                for ((chiave, valore) in dati) {
                    try {
                        val field = acroForm.getField(chiave)
                        if (field != null) {
                            field.setValue(valore)
                        }
                    } catch (e: Exception) {
                        Log.e("Md121PdfStamper", "Errore nel campo $chiave: ${e.message}")
                    }
                }
                acroForm.flatten()
            }

            val resultUri = salvaPdf(document, nomeFile)
            document.close()
            return resultUri

        } catch (e: IOException) {
            Log.e("Md121PdfStamper", "Errore IO: ${e.message}")
            return null
        }
    }

    private fun salvaPdf(document: PDDocument, outputName: String): Uri? {
        var resultUri: Uri? = null
        val resolver = context.contentResolver
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, outputName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Schede")
            }
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    document.save(outputStream)
                }
                resultUri = it
            }
        } else {
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Schede")
            if (!documentsDir.exists()) documentsDir.mkdirs()
            val outputFile = File(documentsDir, outputName)
            document.save(outputFile)
            resultUri = Uri.fromFile(outputFile)
        }
        return resultUri
    }
}
