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

class RimborsoZiPdfStamper(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun generaPdfCompilato(nomeFile: String, dati: Map<String, String>): Uri? {
        var document: PDDocument? = null

        try {
            val templateName = try {
                context.getString(R.string.rimborso_zi_template_name)
            } catch (_: Exception) {
                "RIMBORSO_ZI.pdf"
            }

            val inputStream = try {
                context.assets.open(templateName)
            } catch (_: Exception) {
                context.assets.open("RIMBORSO_ZI.pdf")
            }

            document = PDDocument.load(inputStream)

            val acroForm: PDAcroForm? = document.documentCatalog.acroForm

            if (acroForm != null) {
                for ((chiave, valore) in dati) {
                    try {
                        val field = acroForm.getField(chiave)

                        if (field != null) {
                            field.setValue(valore)
                        } else {
                            Log.w(
                                "RimborsoZiPdfStamper",
                                "Campo PDF non trovato: $chiave"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "RimborsoZiPdfStamper",
                            "Errore nel campo $chiave: ${e.message}"
                        )
                    }
                }

                acroForm.flatten()
            } else {
                Log.e(
                    "RimborsoZiPdfStamper",
                    "Il PDF non contiene un modulo AcroForm"
                )
            }

            return salvaPdf(document, nomeFile)

        } catch (e: IOException) {
            Log.e(
                "RimborsoZiPdfStamper",
                "Errore IO: ${e.message}"
            )
            return null
        } catch (e: Exception) {
            Log.e(
                "RimborsoZiPdfStamper",
                "Errore: ${e.message}"
            )
            return null
        } finally {
            try {
                document?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun salvaPdf(document: PDDocument, outputName: String): Uri? {
        val resolver = context.contentResolver

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, outputName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/Schede"
                )
            }

            resolver.insert(
                MediaStore.Files.getContentUri("external"),
                contentValues
            )?.also { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    document.save(outputStream)
                }
            }
        } else {
            val documentsDir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                "Schede"
            )

            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            val outputFile = File(documentsDir, outputName)
            document.save(outputFile)
            Uri.fromFile(outputFile)
        }
    }
}
