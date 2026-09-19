package com.example.schede

import android.content.Context
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

class KmPdfManager(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    private fun cleanText(text: String?): String {
        if (text == null) return ""
        return text.replace(Regex("[^\\x20-\\x7E]"), "")
    }

    fun generaPdfRimborso(
        auto: Auto,
        rimborso: RimborsoMese,
        viaggi: List<Viaggio>,
        userName: String
    ): File? {
        var document: PDDocument? = null
        try {
            // 1. Carica il template corretto dagli assets
            val inputStream: InputStream = context.assets.open("RIMBORSO Km Ilario ultimo.pdf")
            document = PDDocument.load(inputStream)
            
            val catalog = document.documentCatalog
            val acroForm: PDAcroForm? = catalog.acroForm

            if (acroForm != null) {
                // Forza la visualizzazione dei valori inseriti
                acroForm.needAppearances = true

                // 2. Riempimento campi fissi intestazione (nomi esatti richiesti)
                acroForm.getField("auto")?.setValue(cleanText(auto.modello))
                acroForm.getField("targa")?.setValue(cleanText(auto.targa))
                acroForm.getField("nome")?.setValue(cleanText(userName))
                acroForm.getField("combustibile")?.setValue(cleanText(auto.combustibile))

                // 3. Ciclo per la tabella (data1, percorso1, km1, motivo1...)
                viaggi.take(30).forEachIndexed { i, viaggio ->
                    val n = i + 1
                    acroForm.getField("data$n")?.setValue(viaggio.giorno.toString())
                    val percorsoStr = "${viaggio.partenza} - ${viaggio.destinazione}"
                    acroForm.getField("percorso$n")?.setValue(cleanText(percorsoStr))
                    acroForm.getField("km$n")?.setValue(String.format(Locale.US, "%.1f", viaggio.km))
                    acroForm.getField("motivo$n")?.setValue(cleanText(viaggio.causale))
                }

                // 4. Campo Totale (kmtot)
                val totaleKm = viaggi.sumOf { it.km }
                acroForm.getField("kmtot")?.setValue(String.format(Locale.US, "%.2f", totaleKm))

                // 5. Appiattimento (Flatten) per fissare i dati nel foglio
                acroForm.flatten()
                Log.d("KmPdfManager", "Campi compilati e modulo appiattito con successo.")
            } else {
                Log.e("KmPdfManager", "ERRORE: AcroForm non trovato nel template 'RIMBORSO Km Ilario ultimo.pdf'!")
            }

            // 6. Salvataggio finale
            val fileName = "RIMBORSO_KM_FINALE_${rimborso.mese}_${rimborso.anno}.pdf"
            val publicDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS), "Schede")
            if (!publicDir.exists()) publicDir.mkdirs()
            
            val outputFile = File(publicDir, fileName)
            FileOutputStream(outputFile).use { fos ->
                document.save(fos)
            }
            
            Log.d("KmPdfManager", "PDF salvato in: ${outputFile.absolutePath}")
            return outputFile

        } catch (e: Exception) {
            Log.e("KmPdfManager", "Errore generazione PDF: ${e.message}")
            return null
        } finally {
            document?.close()
        }
    }
}
