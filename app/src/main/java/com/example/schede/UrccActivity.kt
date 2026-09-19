package com.example.schede

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color
import android.content.res.ColorStateList

class UrccActivity : AppCompatActivity() {

    private val circuiti = listOf(
        "A1", "A2", "BP1", "BP2", "AP1", "AP2", "SP1-2", "SP3-4", 
        "PAPI1-2", "PAPI3-4", "SB1-14", "SB2-14", "SB1-32", "SB2-32", 
        "LD1", "LD2", "RGL14", "RGL32", "TA14", "TAMID14", "TAMID32", 
        "TA32", "VR1", "VR2", "VR3", "VR4", "VR5", "VR6"
    )
    
    private var listaDati = circuiti.map { nome ->
        val ampereDefault = if (nome in listOf("TA14", "TA32", "TAMID14", "TAMID32")) "5.2" else ""
        UrccRow(nome, ampere = ampereDefault)
    }

    private val configurazione = mutableMapOf(
        "time" to "",
        "mode" to "",
        "area" to "",
        "step" to "",
        "fs" to "",
        "sb" to ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_urcc)

        supportActionBar?.title = "URCC Settimanale"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        impostaDataEOraAttuali()
        setupConfigButtons()

        if (savedInstanceState != null) {
            val savedCarico = savedInstanceState.getStringArray("carico") ?: emptyArray()
            val savedAmpere = savedInstanceState.getStringArray("ampere") ?: emptyArray()
            val savedVolt = savedInstanceState.getStringArray("volt") ?: emptyArray()
            val savedIsolamento = savedInstanceState.getStringArray("isolamento") ?: emptyArray()
            val savedUnitaIso = savedInstanceState.getStringArray("unita_iso") ?: emptyArray()
            
            val savedConfigKeys = savedInstanceState.getStringArray("config_keys") ?: emptyArray()
            val savedConfigValues = savedInstanceState.getStringArray("config_values") ?: emptyArray()
            for (i in savedConfigKeys.indices) {
                configurazione[savedConfigKeys[i]] = savedConfigValues[i]
            }

            listaDati.forEachIndexed { index, row ->
                if (index < savedCarico.size) row.carico = savedCarico[index]
                if (index < savedAmpere.size) row.ampere = savedAmpere[index]
                if (index < savedVolt.size) row.volt = savedVolt[index]
                if (index < savedIsolamento.size) row.isolamento = savedIsolamento[index]
                if (index < savedUnitaIso.size) row.unitaIsolamento = savedUnitaIso[index]
            }
            ripristinaColoriBottoni()
        }

        aggiornaDatiLista()
        aggiornaNoteConfig()

        val rv = findViewById<RecyclerView>(R.id.rv_urcc)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = UrccAdapter(listaDati)

        findViewById<Button>(R.id.btn_save_urcc).setOnClickListener {
            val mappaPdf = generaMappaPerPdf()
            
            val nomeTemplate = getString(R.string.template_name)
            val stamper = UrccPdfStamper(this, nomeTemplate)
            val dataPerNome = "${mappaPdf["Data_Anno"]}${mappaPdf["Data_Mese"]}${mappaPdf["Data_Giorno"]}"
            val nomeFile = "$dataPerNome MD 125 Scheda verifica settimanale unità regolatrici (URCC).pdf"
            
            lifecycleScope.launch(Dispatchers.IO) {
                val uri = stamper.generaPdfCompilato(nomeFile, mappaPdf)
                
                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        Toast.makeText(this@UrccActivity, "PDF Generato: $nomeFile", Toast.LENGTH_LONG).show()
                        apriPdf(uri)
                    } else {
                        Toast.makeText(this@UrccActivity, "Errore nella generazione del PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupConfigButtons() {
        val colorSelected = Color.parseColor("#4CAF50") // Verde
        val colorDefault = Color.parseColor("#E0E0E0")
        val colorStepDefault = Color.parseColor("#F5F5F5")

        val btnDay = findViewById<Button>(R.id.btn_day)
        val btnNight = findViewById<Button>(R.id.btn_night)
        val btnStr = findViewById<Button>(R.id.btn_str)
        val btnCati = findViewById<Button>(R.id.btn_cati)
        val btnArea14 = findViewById<Button>(R.id.btn_area14)
        val btnArea32 = findViewById<Button>(R.id.btn_area32)
        val btnSbOn = findViewById<Button>(R.id.btn_sbon)
        val btnSbOff = findViewById<Button>(R.id.btn_sboff)
        val btnFs = findViewById<Button>(R.id.btn_fs)

        val stepButtons = listOf(
            findViewById<Button>(R.id.btn_step0),
            findViewById<Button>(R.id.btn_step1),
            findViewById<Button>(R.id.btn_step2),
            findViewById<Button>(R.id.btn_step3),
            findViewById<Button>(R.id.btn_step4),
            findViewById<Button>(R.id.btn_step5)
        )

        fun updateGroup(selected: Button, group: List<Button>, key: String, value: String) {
            val isAlreadySelected = configurazione[key] == value
            group.forEach { it.backgroundTintList = ColorStateList.valueOf(colorDefault) }
            
            if (isAlreadySelected) {
                configurazione[key] = ""
            } else {
                selected.backgroundTintList = ColorStateList.valueOf(colorSelected)
                configurazione[key] = value
            }
            aggiornaNoteConfig()
            aggiornaDatiLista()
        }

        btnDay.setOnClickListener { updateGroup(btnDay, listOf(btnDay, btnNight), "time", "DAY") }
        btnNight.setOnClickListener { updateGroup(btnNight, listOf(btnDay, btnNight), "time", "NIGHT") }
        
        btnStr.setOnClickListener { updateGroup(btnStr, listOf(btnStr, btnCati), "mode", "STR") }
        btnCati.setOnClickListener { updateGroup(btnCati, listOf(btnStr, btnCati), "mode", "CAT I") }

        btnArea14.setOnClickListener { updateGroup(btnArea14, listOf(btnArea14, btnArea32), "area", "14") }
        btnArea32.setOnClickListener { updateGroup(btnArea32, listOf(btnArea14, btnArea32), "area", "32") }

        btnSbOn.setOnClickListener { updateGroup(btnSbOn, listOf(btnSbOn, btnSbOff), "sb", "SB ON") }
        btnSbOff.setOnClickListener { updateGroup(btnSbOff, listOf(btnSbOn, btnSbOff), "sb", "SB OFF") }

        btnFs.setOnClickListener {
            if (configurazione["fs"] == "FS") {
                configurazione["fs"] = ""
                btnFs.backgroundTintList = ColorStateList.valueOf(colorStepDefault)
            } else {
                configurazione["fs"] = "FS"
                btnFs.backgroundTintList = ColorStateList.valueOf(colorSelected)
            }
            aggiornaNoteConfig()
            aggiornaDatiLista()
        }

        stepButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val value = "STD $index"
                if (configurazione["step"] == value) {
                    configurazione["step"] = ""
                    button.backgroundTintList = ColorStateList.valueOf(colorStepDefault)
                } else {
                    stepButtons.forEach { it.backgroundTintList = ColorStateList.valueOf(colorStepDefault) }
                    button.backgroundTintList = ColorStateList.valueOf(colorSelected)
                    configurazione["step"] = value
                }
                aggiornaNoteConfig()
                aggiornaDatiLista()
            }
        }
    }

    private fun aggiornaDatiLista() {
        val isSbOff = configurazione["sb"] == "SB OFF"
        val isArea14 = configurazione["area"] == "14"
        val step = configurazione["step"] ?: ""
        val isStep1to5 = step in listOf("STD 1", "STD 2", "STD 3", "STD 4", "STD 5")
        val isStep0 = step == "STD 0"
        val isFs = configurazione["fs"] == "FS"
        val sbLdCircuits = listOf("SB1-14", "SB2-14", "SB1-32", "SB2-32", "LD1", "LD2")
        val taCircuits = listOf("TA14", "TA32", "TAMID14", "TAMID32")
        val a1a2Circuits = listOf("A1", "A2")

        listaDati.forEach { row ->
            val c = row.circuito
            
            // Logica SB OFF: Sempre attiva per i circuiti SB/LD, indipendentemente da FS
            val forcedBySb = isSbOff && c in sbLdCircuits

            // Logica Area 14: A1 e A2 prendono / tranne isolamento
            val forcedByArea14 = isArea14 && c in a1a2Circuits
            
            // Logica STD 0: Forza le barre a tutti, ma viene sbloccata da FS
            val forcedByStep0 = isStep0 && !isFs

            if (forcedBySb || forcedByArea14 || forcedByStep0) {
                row.carico = "/"
                row.ampere = "/"
                row.volt = "/"
            } else {
                // Se non dobbiamo forzare la barra (perché siamo in FS o perché la configurazione non lo richiede),
                // puliamo se c'è una barra che era stata forzata precedentemente.
                if (row.carico == "/") row.carico = ""
                if (row.ampere == "/") row.ampere = ""
                if (row.volt == "/") row.volt = ""

                // Logica specifica TA per impostare il 5.2 in STD 1-5 (solo se campo vuoto e FS OFF)
                if (c in taCircuits && isStep1to5 && !isFs) {
                    if (row.ampere.isEmpty()) row.ampere = "5.2"
                }
            }
        }
        findViewById<RecyclerView>(R.id.rv_urcc).adapter?.notifyDataSetChanged()
    }

    private fun aggiornaNoteConfig() {
        val parti = mutableListOf<String>()
        configurazione["time"]?.takeIf { it.isNotEmpty() }?.let { parti.add(it) }
        configurazione["mode"]?.takeIf { it.isNotEmpty() }?.let { parti.add(it) }
        configurazione["area"]?.takeIf { it.isNotEmpty() }?.let { parti.add(it) }
        
        val step = configurazione["step"] ?: ""
        val fs = configurazione["fs"] ?: ""
        if (step.isNotEmpty() || fs.isNotEmpty()) {
            val combo = listOfNotNull(step.takeIf { it.isNotEmpty() }, fs.takeIf { it.isNotEmpty() }).joinToString(" ")
            parti.add(combo)
        }
        
        configurazione["sb"]?.takeIf { it.isNotEmpty() }?.let { parti.add(it) }

        val stringaConfig = "Conf " + parti.joinToString(" ")
        
        val etNote = findViewById<EditText>(R.id.input_note)
        val noteAttuali = etNote.text.toString()
        
        val righe = noteAttuali.split("\n").toMutableList()
        val indexConfig = righe.indexOfFirst { riga ->
             riga.startsWith("Conf ") || 
             ((riga.contains("DAY") || riga.contains("NIGHT")) && (riga.contains("STD") || riga.contains("STEP")))
        }
        
        if (indexConfig != -1) {
            righe[indexConfig] = stringaConfig
        } else {
            if (noteAttuali.isEmpty()) {
                etNote.setText(stringaConfig)
            } else {
                etNote.setText(stringaConfig + "\n" + noteAttuali)
            }
            return
        }
        etNote.setText(righe.joinToString("\n"))
    }

    private fun apriPdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Nessuna app per visualizzare PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun impostaDataEOraAttuali() {
        val calendar = Calendar.getInstance()
        
        val gg = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val mm = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val aaaa = calendar.get(Calendar.YEAR).toString()
        val hh = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val min = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')

        findViewById<EditText>(R.id.input_gg).setText(gg)
        findViewById<EditText>(R.id.input_mm).setText(mm)
        findViewById<EditText>(R.id.input_aaaa).setText(aaaa)
        findViewById<EditText>(R.id.input_ora).setText(hh)
        findViewById<EditText>(R.id.input_min).setText(min)
    }

    private fun generaMappaPerPdf(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        
        // Recupero il nome dell'utente dalle risorse (stringa user_name definita nel flavor)
        val nomeUtente = getString(R.string.user_name)
        
        // Header
        val gg = findViewById<EditText>(R.id.input_gg).text.toString()
        val mm = findViewById<EditText>(R.id.input_mm).text.toString()
        val aaaa = findViewById<EditText>(R.id.input_aaaa).text.toString()
        val ora = findViewById<EditText>(R.id.input_ora).text.toString()
        val min = findViewById<EditText>(R.id.input_min).text.toString()
        val noteIntere = findViewById<EditText>(R.id.input_note).text.toString()

        map["Data_Giorno"] = gg
        map["Data_Mese"] = mm
        map["Data_Anno"] = aaaa
        map["Ora_Verifica"] = ora
        map["Minuti_Verifica"] = min
        map["Responsabile_Verifica"] = nomeUtente

        // Gestione Note (suddivisione su più campi Note 2...9)
        val maxCharsPerRow = 80
        val righeNote = noteIntere.split("\n").flatMap { riga ->
            if (riga.length > maxCharsPerRow) riga.chunked(maxCharsPerRow) else listOf(riga)
        }
        for (i in 0 until 8) { // Note 2...9
            val fieldName = "Note ${i + 2}"
            map[fieldName] = if (i < righeNote.size) righeNote[i] else " "
        }

        // Tabella Circuiti - Utilizza direttamente i dati della lista che sono già sincronizzati
        listaDati.forEach { row ->
            val c = row.circuito
            map["Carico_$c"] = if (row.carico.isEmpty()) " " else row.carico
            map["Ampere_$c"] = if (row.ampere.isEmpty()) " " else row.ampere
            map["Volt_$c"] = if (row.volt.isEmpty()) " " else row.volt
            
            val isoVal = row.isolamento
            map["Iso_$c"] = if (isoVal.isNotEmpty()) {
                if (row.unitaIsolamento == "M ohm") "$isoVal M ohm" else "$isoVal ${row.unitaIsolamento}"
            } else " "
        }
        
        return map
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray("carico", listaDati.map { it.carico }.toTypedArray())
        outState.putStringArray("ampere", listaDati.map { it.ampere }.toTypedArray())
        outState.putStringArray("volt", listaDati.map { it.volt }.toTypedArray())
        outState.putStringArray("isolamento", listaDati.map { it.isolamento }.toTypedArray())
        outState.putStringArray("unita_iso", listaDati.map { it.unitaIsolamento }.toTypedArray())
        
        outState.putStringArray("config_keys", configurazione.keys.toTypedArray())
        outState.putStringArray("config_values", configurazione.values.toTypedArray())
    }

    private fun ripristinaColoriBottoni() {
        val colorSelected = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        val colorDefault = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
        val colorStepDefault = ColorStateList.valueOf(Color.parseColor("#F5F5F5"))

        findViewById<Button>(R.id.btn_day).backgroundTintList = if (configurazione["time"] == "DAY") colorSelected else colorDefault
        findViewById<Button>(R.id.btn_night).backgroundTintList = if (configurazione["time"] == "NIGHT") colorSelected else colorDefault
        findViewById<Button>(R.id.btn_str).backgroundTintList = if (configurazione["mode"] == "STR") colorSelected else colorDefault
        findViewById<Button>(R.id.btn_cati).backgroundTintList = if (configurazione["mode"] == "CAT I") colorSelected else colorDefault
        findViewById<Button>(R.id.btn_area14).backgroundTintList = if (configurazione["area"] == "14") colorSelected else colorDefault
        findViewById<Button>(R.id.btn_area32).backgroundTintList = if (configurazione["area"] == "32") colorSelected else colorDefault
        findViewById<Button>(R.id.btn_sbon).backgroundTintList = if (configurazione["sb"] == "SB ON") colorSelected else colorDefault
        findViewById<Button>(R.id.btn_sboff).backgroundTintList = if (configurazione["sb"] == "SB OFF") colorSelected else colorDefault
        findViewById<Button>(R.id.btn_fs).backgroundTintList = if (configurazione["fs"] == "FS") colorSelected else colorStepDefault

        val stepButtons = listOf(
            findViewById<Button>(R.id.btn_step0),
            findViewById<Button>(R.id.btn_step1),
            findViewById<Button>(R.id.btn_step2),
            findViewById<Button>(R.id.btn_step3),
            findViewById<Button>(R.id.btn_step4),
            findViewById<Button>(R.id.btn_step5)
        )
        stepButtons.forEachIndexed { i, btn ->
            btn.backgroundTintList = if (configurazione["step"] == "STD $i") colorSelected else colorStepDefault
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
