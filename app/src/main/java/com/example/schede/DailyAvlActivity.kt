package com.example.schede

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class DailyAvlActivity : AppCompatActivity() {

    private val checklistItems = listOf(
        DailyAvlRow("01", "SISTEMA DI TELECONTROLLO - Controllo efficienza impianto apparati di cabina"),
        DailyAvlRow("02", "UNITA' REGOLATRICI (URCC) - Controllo efficienza e verifica del corretto funzionamento"),
        DailyAvlRow("03", "QUADRO POWER CENTER (QPCVN) - Controllo a vista spie, segnalazioni e strumenti"),
        DailyAvlRow("04", "UPS - Verifica funzionalità e ispezione a vista stato UPS"),
        DailyAvlRow("05", "GRUPPI ELETTROGENI - Ispezione a vista locale GGEE e quadro comando e controllo (QGE5)"),
        DailyAvlRow("06", "CABINA MT - Ispezione a vista locale MT"),
        DailyAvlRow("07", "ATTREZZATURA OFFICINA AVL - Verifica attrezzatura"),
        DailyAvlRow("08", "ATTREZZATURA FURGONE AVL - Verifica attrezzatura"),
        DailyAvlRow("09", "OFFICINA E FURGONE AVL - Verifica ordine e pulizia")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_avl)

        supportActionBar?.title = getString(R.string.title_daily_avl)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        impostaDataEOraAttuali()

        val rv = findViewById<RecyclerView>(R.id.rv_daily)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = DailyAvlAdapter(checklistItems)

        findViewById<Button>(R.id.btn_save_daily).setOnClickListener {
            salvaEGeneraPdf()
        }
    }

    private fun impostaDataEOraAttuali() {
        val calendar = Calendar.getInstance()
        findViewById<EditText>(R.id.input_gg).setText(String.format(Locale.ITALY, "%02d", calendar.get(Calendar.DAY_OF_MONTH)))
        findViewById<EditText>(R.id.input_mm).setText(String.format(Locale.ITALY, "%02d", calendar.get(Calendar.MONTH) + 1))
        findViewById<EditText>(R.id.input_aaaa).setText(calendar.get(Calendar.YEAR).toString())
        findViewById<EditText>(R.id.input_ora).setText(String.format(Locale.ITALY, "%02d", calendar.get(Calendar.HOUR_OF_DAY)))
        findViewById<EditText>(R.id.input_min).setText(String.format(Locale.ITALY, "%02d", calendar.get(Calendar.MINUTE)))
    }

    private fun salvaEGeneraPdf() {
        // --- CONTROLLO VALIDAZIONE ---
        val righeIncomplete = checklistItems.filter { it.stato.isEmpty() }
        if (righeIncomplete.isNotEmpty()) {
            val nomiRighe = righeIncomplete.joinToString(", ") { it.numero }
            Toast.makeText(this, "Attenzione! Seleziona lo stato per le attività: $nomiRighe", Toast.LENGTH_LONG).show()
            return
        }

        val dati = mutableMapOf<String, String>()
        
        val gg = findViewById<EditText>(R.id.input_gg).text.toString()
        val mm = findViewById<EditText>(R.id.input_mm).text.toString()
        val aaaa = findViewById<EditText>(R.id.input_aaaa).text.toString()
        val ora = findViewById<EditText>(R.id.input_ora).text.toString()
        val min = findViewById<EditText>(R.id.input_min).text.toString()
        val noteGen = findViewById<EditText>(R.id.input_note_generali).text.toString()

        // Mappatura campi intestazione
        dati["undefined_2.0"] = gg
        dati["undefined_3.0"] = mm
        dati["undefined_4"] = aaaa
        dati["undefined_2.1"] = ora
        dati["undefined_3.1"] = min
        dati["Responsabile verifica"] = "Zanetti Ilario"
        
        // Pulizia campi extra che potrebbero causare icone gialle (commenti) nel PDF
        dati["undefined"] = " "
        dati["Check Box1"] = "Off"

        // Mappatura note per riga
        val fieldNamesNotes = listOf(
            "NOTESISTEMA DI TELECONTROLLO Controllo efficienza impianto apparati di cabina",
            "NOTEUNITA REGOLATRICI URCC Controllo efficienza e verifica del corretto funzionamento",
            "NOTEQUADRO POWER CENTER QPCVN Controllo a vista spie segnalazioni e strumenti",
            "NOTEUPS Verifica funzionalità e ispezione a vista stato UPS",
            "NOTEGRUPPI ELETTROGENI Ispezione a vista locale GGEE e quadro comando e controllo QGE5",
            "NOTECABINA MT Ispezione a vista locale MT",
            "NOTEATTREZZATURA OFFICINA AVL Verifica attrezzatura",
            "NOTEATTREZZATURA FURGONE AVL Verifica attrezzatura",
            "NOTEOFFICINA E FURGONE AVL Verifica ordine e pulizia"
        )

        checklistItems.forEachIndexed { index, row ->
            if (index < fieldNamesNotes.size) {
                dati[fieldNamesNotes[index]] = row.note
            }
            
            // Mappatura CheckBox basata sullo schema Check Box1.[Colonna].[RigaIndex]
            // Colonna 0 = C, 1 = NC, 2 = NA
            // RigaIndex (index) = da 0 a 8
            val valoreSpunta = "Sì" 
            when (row.stato) {
                "C" -> dati["Check Box1.0.$index"] = valoreSpunta
                "NC" -> dati["Check Box1.1.$index"] = valoreSpunta
                "NA" -> dati["Check Box1.2.$index"] = valoreSpunta
            }
        }

        // Note generali a fondo pagina
        val righeNote = noteGen.split("\n")
        for (i in 0 until 9) {
            val fieldName = "Note ${i + 1}"
            dati[fieldName] = if (i < righeNote.size) righeNote[i] else ""
        }

        val nomeFile = "${aaaa}${mm}${gg} MD-46 Scheda verifica giornaliera cabina AVL rev01 del 06.06.2017.pdf"
        val stamper = DailyPdfStamper(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val uri = stamper.generaPdfCompilato(nomeFile, dati)
            withContext(Dispatchers.Main) {
                if (uri != null) {
                    Toast.makeText(this@DailyAvlActivity, "PDF Generato!", Toast.LENGTH_LONG).show()
                    apriPdf(uri)
                } else {
                    Toast.makeText(this@DailyAvlActivity, getString(R.string.msg_errore_pdf), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun apriPdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Nessuna app per PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
