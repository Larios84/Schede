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

class PdfStamper(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    @Throws(IOException::class)
    fun fillPdfFromAssets(assetName: String, outputName: String, fields: Map<String, String>): Uri? {
        val document = PDDocument.load(context.assets.open(assetName))
        val acroForm: PDAcroForm? = document.documentCatalog.acroForm

        acroForm?.let { form ->
            // Impostiamo NeedAppearances a false per evitare che il lettore PDF sovrascriva lo stile
            form.setNeedAppearances(false)

            fields.forEach { (fieldName, value) ->
                val field = form.getField(fieldName)
                if (field != null) {
                    field.setValue(value)
                    Log.d("PdfStamper", "Compilato campo: $fieldName")
                }
            }
            
            // "Appiattisce" il modulo: i campi diventano testo/grafica fissa
            // e lo sfondo azzurro di selezione scompare.
            form.flatten() 
        }

        var resultUri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
