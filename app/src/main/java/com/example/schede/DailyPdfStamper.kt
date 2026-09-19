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

class DailyPdfStamper(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun generaPdfCompilato(nomeFile: String, dati: Map<String, String>): Uri? {
        try {
            // Cerchiamo il nome del template specifico per MD46 nelle risorse
            // Se non trovato, usiamo il nome file standard come fallback
            val resId = context.resources.getIdentifier("md46_template_name", "string", context.packageName)
            val templateName = if (resId != 0) context.getString(resId) else "template_md46_iz.pdf"
            
            val inputStream = context.assets.open(templateName)
            val document = PDDocument.load(inputStream)
            val acroForm: PDAcroForm? = document.documentCatalog.acroForm

            if (acroForm != null) {
                Log.d("DailyPdfStamper", "--- ISPEZIONE CAMPI PDF MD-46 ---")
                acroForm.fields.forEach { f ->
                    val type = try { f.fieldType } catch(e:Exception) { "unknown" }
                    Log.d("DailyPdfStamper", "Field: ${f.fullyQualifiedName} | Type: $type")
                }

                for ((chiave, valore) in dati) {
                    try {
                        val field = acroForm.getField(chiave)
                        if (field != null) {
                            Log.d("DailyPdfStamper", "Scrittura: $chiave = $valore")
                            field.setValue(valore)
                        } else {
                            Log.w("DailyPdfStamper", "Campo NON trovato: $chiave")
                        }
                    } catch (e: Exception) {
                        Log.e("DailyPdfStamper", "Errore campo $chiave: ${e.message}")
                    }
                }
                
                // Forza la generazione dell'aspetto grafico per le checkbox e i campi
                try {
                    acroForm.refreshAppearances()
                } catch (e: Exception) {
                    Log.e("DailyPdfStamper", "Errore refreshAppearances: ${e.message}")
                }

                // RIMOZIONE ICONE GIALLO (Commenti/Popup associati al timbro della firma)
                try {
                    document.pages.forEach { page ->
                        val annots = page.annotations
                        val iterator = annots.iterator()
                        while (iterator.hasNext()) {
                            val annot = iterator.next()
                            
                            // 1. Rimuoviamo i Popup (i rettangolini fisici)
                            if (annot.subtype == "Popup" || annot.subtype == "Text") {
                                iterator.remove()
                                continue
                            }

                            // 2. Puliamo il testo interno di qualsiasi altra annotazione (come il Timbro)
                            // Spesso è il testo "Timbro" o "Firma" che genera l'icona del commento
                            annot.contents = null
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DailyPdfStamper", "Errore rimozione icone gialle: ${e.message}")
                }
                
                // Appiattiamo il modulo per rendere i dati visibili su tutti i lettori
                acroForm.flatten() 
            }

            val resultUri = salvaPdf(document, nomeFile)
            document.close()
            return resultUri

        } catch (e: IOException) {
            Log.e("DailyPdfStamper", "Errore IO: ${e.message}")
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
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Schede/Giornaliere")
            }
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    document.save(outputStream)
                }
                resultUri = it
            }
        } else {
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Schede/Giornaliere")
            if (!documentsDir.exists()) documentsDir.mkdirs()
            val outputFile = File(documentsDir, outputName)
            document.save(outputFile)
            resultUri = Uri.fromFile(outputFile)
        }
        return resultUri
    }
}
