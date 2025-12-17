package com.example.schede

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinnerScelte: Spinner = findViewById(R.id.spinner_scelte)
        val inputGg: EditText = findViewById(R.id.input_gg)
        val inputMm: EditText = findViewById(R.id.input_mm)
        val inputAaaa: EditText = findViewById(R.id.input_aaaa)
        val inputDescrizione: EditText = findViewById(R.id.input_descrizione)
        val createPdfButton: Button = findViewById(R.id.create_pdf_button)

        // Opzioni Reparto
        val opzioni = arrayOf("AVL", "BHS", "IMP.TECNOLOGICI", "IMP.ELETTRICI", "IMP.SPECIALI", "MAN.GEN", "STP VERDE")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opzioni)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerScelte.adapter = adapter

        createPdfButton.setOnClickListener {
            val sceltaSelezionata = spinnerScelte.selectedItem.toString()
            
            // Uniamo i tre campi in una stringa unica "GG/MM/AAAA"
            val gg = inputGg.text.toString()
            val mm = inputMm.text.toString()
            val aaaa = inputAaaa.text.toString()
            val dataCompleta = "$gg/$mm/$aaaa"
            
            val descrizione = inputDescrizione.text.toString()
            
            compilePdf(sceltaSelezionata, dataCompleta, descrizione)
        }
    }

    private fun compilePdf(scelta: String, data: String, descrizione: String) {
        val templateFileName = "template2.pdf"
        val outputFileName = "scheda_intervento.pdf"

        val mapSceltePdf = mapOf(
            "AVL" to "avl",
            "BHS" to "bhs",
            "IMP.TECNOLOGICI" to "tec",
            "IMP.ELETTRICI" to "elettr",
            "IMP.SPECIALI" to "spec",
            "MAN.GEN" to "gen",
            "STP VERDE" to "stp"
        )

        val formData = mutableMapOf<String, String>()
        
        // Mappatura caselle reparto
        mapSceltePdf.forEach { (label, pdfFieldName) ->
            formData[pdfFieldName] = if (label == scelta) "Yes" else "Off"
        }

        // Inviamo la data formattata all'unico campo "data" del PDF
        formData["data"] = data
        formData["descrizione"] = descrizione

        try {
            val pdfStamper = PdfStamper(this)
            val compiledPdfUri = pdfStamper.fillPdfFromAssets(templateFileName, outputFileName, formData)
            if (compiledPdfUri != null) {
                Toast.makeText(this, "PDF creato con successo!", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "Errore PDF", e)
        }
    }
}
