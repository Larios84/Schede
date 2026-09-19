package com.example.schede

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val listaAttivita = mutableListOf<Pair<String, String>>()
    private lateinit var textOreTotali: TextView
    private lateinit var textOreRestanti: TextView
    private lateinit var textStraordinario: TextView
    private lateinit var containerAttivita: LinearLayout
    private lateinit var inputDescrizione: AutoCompleteTextView
    private lateinit var inputOre: EditText
    private lateinit var labelOre: TextView
    private lateinit var inputStraordinarioAngelo: EditText
    private lateinit var labelStraordinarioAngelo: TextView
    
    private lateinit var inputGg: EditText
    private lateinit var inputMm: EditText
    private lateinit var inputAaaa: EditText
    private lateinit var loadYesterdayButton: Button
    private lateinit var mainScrollView: ScrollView

    private var selectedFolderUri: Uri? = null
    private lateinit var sheetsSync: GoogleSheetsSync

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            saveFolderUri(it)
            selectedFolderUri = it
            Toast.makeText(this, getString(R.string.msg_cartella_selezionata), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sheetsSync = GoogleSheetsSync()
        loadFolderUri()

        inputGg = findViewById(R.id.input_gg)
        inputMm = findViewById(R.id.input_mm)
        inputAaaa = findViewById(R.id.input_aaaa)
        inputDescrizione = findViewById(R.id.input_descrizione)
        inputOre = findViewById(R.id.input_ore)
        labelOre = findViewById(R.id.label_ore)
        inputStraordinarioAngelo = findViewById(R.id.input_straordinario_angelo)
        labelStraordinarioAngelo = findViewById(R.id.label_straordinario_angelo)
        containerAttivita = findViewById(R.id.container_attivita)
        textOreTotali = findViewById(R.id.text_ore_totali)
        textOreRestanti = findViewById(R.id.text_ore_restanti)
        textStraordinario = findViewById(R.id.text_straordinario)
        loadYesterdayButton = findViewById(R.id.load_yesterday_button)
        mainScrollView = findViewById(R.id.main_scroll_view)
        
        val spinnerScelte: Spinner = findViewById(R.id.spinner_scelte)
        val spinnerTurno: Spinner = findViewById(R.id.spinner_turno)
        val spinnerNome: Spinner = findViewById(R.id.spinner_nome)
        
        val repartiArray = arrayOf("AVL", "BHS", "IMP.TECNOLOGICI", "IMP.ELETTRICI", "IMP.SPECIALI", "MAN.GEN", "STP VERDE")
        val turniArray = resources.getStringArray(R.array.turni_array)
        
        spinnerScelte.adapter = createHighlightedAdapter(repartiArray, spinnerScelte)
        spinnerTurno.adapter = createHighlightedAdapter(turniArray, spinnerTurno)
        
        val currentUserName = getString(R.string.user_name)
        spinnerNome.adapter = createHighlightedAdapter(arrayOf(currentUserName), spinnerNome)
        spinnerNome.isEnabled = false

        // --- GESTIONE BANNER COMPLEANNO (UNA SOLA VOLTA) ---
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) // Marzo è 2 (0-indexed)
        if (day == 5 && month == 2 && currentUserName == "Garau Luca") {
            val prefs = getSharedPreferences("auguri_prefs", MODE_PRIVATE)
            val chiaveAuguri = "auguri_mostrati_5marzo"
            val giaMostrati = prefs.getBoolean(chiaveAuguri, false)
            
            if (!giaMostrati) {
                val banner = findViewById<View>(R.id.birthday_banner)
                banner.visibility = View.VISIBLE
                
                // Segniamo subito come mostrato, così non riappare alla prossima apertura
                prefs.edit().putBoolean(chiaveAuguri, true).apply()
                
                // Permettiamo di chiuderlo cliccandoci sopra
                banner.setOnClickListener {
                    banner.visibility = View.GONE
                }
            }
        }

        // --- SCORRIMENTO AUTOMATICO ALLA DESCRIZIONE ---
        inputDescrizione.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.postDelayed({
                    val labelInserimento = findViewById<TextView>(R.id.label_inserimento)
                    mainScrollView.smoothScrollTo(0, labelInserimento.top)
                }, 300)
            }
        }

        // --- SCORRIMENTO AUTOMATICO ALLO STRAORDINARIO ANGELO ---
        inputStraordinarioAngelo.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.postDelayed({
                    val labelInserimento = findViewById<TextView>(R.id.label_inserimento)
                    mainScrollView.smoothScrollTo(0, labelInserimento.top)
                }, 300)
            }
        }

        // --- PULSANTE GENERATORE RIMOSSO ---
        val btnOpenGenerator = findViewById<ImageButton>(R.id.btn_open_generator)
        btnOpenGenerator.visibility = View.GONE

        val btnStraordinarioVeloce = findViewById<Button>(R.id.btn_straordinario_veloce)
        if (currentUserName != "Angelo Boi") {
            btnStraordinarioVeloce.visibility = View.VISIBLE
            btnStraordinarioVeloce.setOnClickListener {
                if (listaAttivita.size == 1) {
                    val attivita = listaAttivita[0]
                    val desc = attivita.first
                    val oreStr = attivita.second.replace(',', '.').replace(':', '.')
                    try {
                        val parts = oreStr.split('.')
                        val orePart = parts[0].toInt()
                        val minPart = if (parts.size > 1) parts[1].padEnd(2, '0').substring(0, 2).toInt() else 0
                        
                        var nuoveOre = orePart
                        var nuoviMin = minPart + 30
                        
                        if (nuoviMin >= 60) {
                            nuoveOre += 1
                            nuoviMin -= 60
                        }
                        
                        val nuoveOreStr = String.format(Locale.ITALY, "%d:%02d", nuoveOre, nuoviMin)
                        listaAttivita[0] = Pair(desc, nuoveOreStr)
                        aggiornaListaVisiva()
                        aggiornaCampoOre()
                        salvaBozza()
                        Toast.makeText(this, "Aggiunti 30 minuti!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Errore calcolo +30min: ${e.message}")
                    }
                } else {
                    Toast.makeText(this, "Deve esserci esattamente un'attività in lista!", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            btnStraordinarioVeloce.visibility = View.GONE
        }

        if (currentUserName == "Angelo Boi") {
            val adapter = spinnerScelte.adapter as ArrayAdapter<String>
            val position = adapter.getPosition("IMP.TECNOLOGICI")
            if (position >= 0) {
                spinnerScelte.setSelection(position)
            }
            labelStraordinarioAngelo.visibility = View.VISIBLE
            inputStraordinarioAngelo.visibility = View.VISIBLE
            inputStraordinarioAngelo.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    aggiornaListaVisiva()
                }
            })
        }

        val immediateColorListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                (p0?.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
                caricaSuggerimenti() // Aggiorna suggerimenti in base al reparto
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        spinnerScelte.onItemSelectedListener = immediateColorListener
        spinnerTurno.onItemSelectedListener = immediateColorListener

        // --- DATA PICKER ---
        val dateClickListener = View.OnClickListener { mostraDatePicker() }
        inputGg.setOnClickListener(dateClickListener)
        inputMm.setOnClickListener(dateClickListener)
        inputAaaa.setOnClickListener(dateClickListener)

        findViewById<Button>(R.id.add_activity_button).setOnClickListener {
            val desc = inputDescrizione.text.toString().trim()
            val oreStr = inputOre.text.toString().trim()
            
            if (desc.isEmpty()) {
                Toast.makeText(this, "Inserisci una descrizione!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (oreStr.isEmpty()) {
                Toast.makeText(this, "Inserisci le ore!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Converto la virgola o i due punti in punto per il parsing
            try {
                oreStr.replace(',', '.').replace(':', '.').toDouble()
            } catch (e: Exception) {
                Toast.makeText(this, "Formato ore non valido!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // CONTROLLO DUPLICATI
            if (listaAttivita.any { it.first.equals(desc, ignoreCase = true) }) {
                Toast.makeText(this, "Attività già presente in lista!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            listaAttivita.add(Pair(desc, oreStr))
            inputDescrizione.setText(""); inputOre.setText("")
            aggiornaListaVisiva()
            aggiornaCampoOre()
            salvaBozza()
        }

        findViewById<Button>(R.id.btn_clear_all).setOnClickListener {
            inputDescrizione.setText("")
            inputOre.setText("")
        }

        findViewById<Button>(R.id.create_pdf_button).setOnClickListener {
            val isAngelo = getString(R.string.user_name) == "Angelo Boi"
            if (listaAttivita.isEmpty() && !isAngelo) {
                Toast.makeText(this, "Aggiungi almeno un'attività!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedFolderUri == null) { folderPickerLauncher.launch(null); return@setOnClickListener }

            var totaleOre = 0.0
            listaAttivita.forEach { pair ->
                try { totaleOre += pair.second.replace(',', '.').replace(':', '.').toDouble() } catch (e: Exception) {}
            }

            if (totaleOre < 7.42 && !isAngelo) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Attenzione")
                    .setMessage("Le ore totali sono ${String.format(Locale.ITALY, "%.2f", totaleOre).replace(',', ':')}h, minori di 7:42h. Vuoi continuare comunque?")
                    .setPositiveButton("Continua") { _, _ ->
                        effettuaCompilazione(spinnerScelte, spinnerTurno, spinnerNome)
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
            } else {
                effettuaCompilazione(spinnerScelte, spinnerTurno, spinnerNome)
            }
        }

        findViewById<Button>(R.id.history_button).setOnClickListener {
            startActivity(Intent(this, StoricoActivity::class.java))
        }
        
        findViewById<ImageButton>(R.id.btn_change_folder)?.setOnClickListener { folderPickerLauncher.launch(null) }
        findViewById<Button>(R.id.exit_button).setOnClickListener { finishAffinity() }
        loadYesterdayButton.setOnClickListener { caricaComeIeri() }

        aggiornaDataCorrente()
        caricaSuggerimenti()
        sincronizzaInSottofondo()
        caricaBozza()
        
        // Inizializza campo ore per Luca e Ilario
        if (getString(R.string.user_name) != "Angelo Boi") {
            inputOre.setText("7:42")
            impostaClickListenerOre()
        }
    }

    private fun effettuaCompilazione(spinnerScelte: Spinner, spinnerTurno: Spinner, spinnerNome: Spinner) {
        compilePdf(spinnerScelte.selectedItem.toString(), spinnerTurno.selectedItem.toString(), spinnerNome.selectedItem.toString(), "${inputGg.text}/${inputMm.text}/${inputAaaa.text}")
        cancellaBozza()
    }

    private fun mostraDatePicker() {
        val c = Calendar.getInstance()
        try {
            val d = inputGg.text.toString().toInt()
            val m = inputMm.text.toString().toInt() - 1
            val y = inputAaaa.text.toString().toInt()
            c.set(y, m, d)
        } catch (e: Exception) {}

        DatePickerDialog(this, { _, year, month, day ->
            inputGg.setText(String.format(Locale.ITALY, "%02d", day))
            inputMm.setText(String.format(Locale.ITALY, "%02d", month + 1))
            inputAaaa.setText(year.toString())
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun createHighlightedAdapter(items: Array<String>, spinner: Spinner): ArrayAdapter<String> {
        val textColor = ContextCompat.getColor(this, R.color.main_text)
        val accentColor = ContextCompat.getColor(this, R.color.purple_500)
        val filterBarColor = ContextCompat.getColor(this, R.color.input_background)

        return object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(accentColor)
                v.setTypeface(null, Typeface.BOLD)
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.setBackgroundColor(filterBarColor)
                if (position == spinner.selectedItemPosition) {
                    v.setTextColor(accentColor)
                    v.setTypeface(null, Typeface.BOLD)
                } else {
                    v.setTextColor(textColor)
                    v.setTypeface(null, Typeface.NORMAL)
                }
                v.setPadding(30, 40, 30, 40)
                return v
            }
        }
    }

    private fun sincronizzaInSottofondo() {
        loadYesterdayButton.isEnabled = false
        loadYesterdayButton.text = "Sincronizzazione..."
        sheetsSync.readInterventi { interventi, error ->
            if (error == null && interventi != null) {
                thread {
                    try {
                        val db = AppDatabase.getDatabase(this)
                        db.interventoDao().insertAll(interventi)
                        runOnUiThread { 
                            caricaSuggerimenti()
                            loadYesterdayButton.isEnabled = true 
                            loadYesterdayButton.text = "CARICA COME IERI"
                            Toast.makeText(this, "Sincronizzazione completata!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            loadYesterdayButton.isEnabled = true
                            loadYesterdayButton.text = "CARICA COME IERI"
                        }
                    }
                }
            } else {
                runOnUiThread {
                    loadYesterdayButton.isEnabled = true
                    loadYesterdayButton.text = "CARICA COME IERI"
                }
            }
        }
    }

    private fun compilePdf(reparto: String, turno: String, nome: String, data: String) {
        val formData = mutableMapOf<String, String>()
        val mapReparto = mapOf("AVL" to "avl", "BHS" to "bhs", "IMP.TECNOLOGICI" to "tec", "IMP.ELETTRICI" to "elettr", "IMP.SPECIALI" to "spec", "MAN.GEN" to "gen", "STP VERDE" to "stp")
        mapReparto.forEach { (k, v) -> formData[v] = if (k == reparto) "Yes" else "Off" }
        formData["turno"] = turno; formData["nome"] = nome; formData["data"] = data

        val isAngelo = getString(R.string.user_name) == "Angelo Boi"
        var oreTotali = 0.0
        val straordinarioAngelo = inputStraordinarioAngelo.text.toString().replace(',', '.').replace(':', '.').toDoubleOrNull() ?: 0.0

        if (isAngelo) {
            oreTotali = 7.42 + straordinarioAngelo
        } else {
            listaAttivita.forEachIndexed { i, pair ->
                if (i < 8) {
                    val rowNum = i + 1
                    val fieldName = when(rowNum) {
                        2 -> "att2_2"
                        8 -> "att2"
                        else -> "att$rowNum"
                    }
                    formData[fieldName] = pair.first
                    formData["ore$rowNum"] = pair.second.replace(',', ':').replace('.', ':')
                    try { oreTotali += pair.second.replace(',', '.').replace(':', '.').toDouble() } catch (e: Exception) {}
                }
            }
        }

        // Uso lo stesso calcolo corretto dell'interfaccia
        val str: Double = if (isAngelo) {
            straordinarioAngelo
        } else if (oreTotali > 7.42) {
            // Sottrazione in sessantesimi: 8,00 - 7,42 = 0,18
            val sommaFormatted = String.format(Locale.US, "%.2f", oreTotali)
            val sommaOre = sommaFormatted.split('.')
            val sogliaOre = "7.42".split('.')
            
            if (sommaOre.size >= 2 && sogliaOre.size >= 2) {
                val sommaOreInt = sommaOre[0].toInt()
                val sommaMin = sommaOre[1].padEnd(2, '0').substring(0, 2).toInt()
                val sogliaOreInt = sogliaOre[0].toInt()
                val sogliaMin = sogliaOre[1].toInt()
                
                var straordinarioOre = sommaOreInt - sogliaOreInt
                var straordinarioMin = sommaMin - sogliaMin
                
                // Gestisco i prestiti di minuti
                if (straordinarioMin < 0) {
                    straordinarioOre -= 1
                    straordinarioMin += 60
                }
                
                straordinarioOre + (straordinarioMin / 100.0)
            } else {
                oreTotali - 7.42
            }
        } else {
            0.0
        }
        formData["tot"] = String.format(Locale.ITALY, "%.2f", oreTotali).replace(',', ':')
        formData["str"] = if (str > 0.0) String.format(Locale.ITALY, "%.2f", str).replace(',', ':') else ""

        val dataShort = data.replace("/", "-").let { it.substring(0, 6) + it.takeLast(2) }
        val nomeFile = "$nome $dataShort.pdf"
        val assetTemplate = when (nome) {
            "Zanetti Ilario" -> "templateiz.pdf"
            "Angelo Boi" -> "templateab.pdf"
            else -> "templatelg.pdf"
        }

        val stamper = PdfStamper(this)
        val uri = stamper.fillPdfFromAssets(assetTemplate, nomeFile, formData, selectedFolderUri, null)

        if (uri != null) {
            thread {
                try {
                    val intervento = Intervento(data = data, nome = nome, reparto = reparto, turno = turno, oreTotali = oreTotali, straordinario = str, attivitaJson = Gson().toJson(listaAttivita))
                    AppDatabase.getDatabase(this).interventoDao().insert(intervento)
                    sheetsSync.sendIntervento(intervento) { success, _ ->
                        runOnUiThread {
                            if (success) Toast.makeText(this, "Scheda salvata e sincronizzata!", Toast.LENGTH_LONG).show()
                        }
                    }
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.msg_creato_successo), Toast.LENGTH_SHORT).show()
                        listaAttivita.clear(); aggiornaListaVisiva()
                    }
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            }
        } else {
            Toast.makeText(this, "Errore: Impossibile creare il file. Controlla la cartella selezionata.", Toast.LENGTH_LONG).show()
        }
    }

    private fun caricaSuggerimenti() {
        val spinnerScelte: Spinner = findViewById(R.id.spinner_scelte)
        val repartoCorrente = spinnerScelte.selectedItem?.toString() ?: ""

        thread {
            try {
                val interventi = AppDatabase.getDatabase(this).interventoDao().getAll()
                val gson = Gson()
                val type = object : TypeToken<List<Pair<String, String>>>() {}.type
                
                val suggerimenti = interventi
                    .filter { it.reparto.equals(repartoCorrente, ignoreCase = true) || repartoCorrente.isEmpty() } // FILTRO PER REPARTO
                    .flatMap {
                        try {
                            val list: List<Pair<String, String>> = gson.fromJson(it.attivitaJson, type)
                            list.mapNotNull { p -> p?.first }
                        } catch (e: Exception) { emptyList<String>() }
                    }.filter { it.isNotBlank() }.distinct().sorted()
                
                runOnUiThread {
                    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suggerimenti)
                    inputDescrizione.setAdapter(adapter)
                }
            } catch (e: Exception) {}
        }
    }

    private fun salvaBozza() {
        val json = Gson().toJson(listaAttivita)
        getSharedPreferences("bozza", MODE_PRIVATE).edit().putString("lista", json).apply()
    }

    private fun caricaBozza() {
        val json = getSharedPreferences("bozza", MODE_PRIVATE).getString("lista", null)
        if (json != null) {
            val type = object : TypeToken<List<Pair<String, String>>>() {}.type
            val bozza: List<Pair<String, String>> = Gson().fromJson(json, type)
            if (bozza.isNotEmpty()) {
                listaAttivita.clear()
                listaAttivita.addAll(bozza)
                aggiornaListaVisiva()
                aggiornaCampoOre()
                Toast.makeText(this, "Ripristinata bozza precedente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancellaBozza() {
        getSharedPreferences("bozza", MODE_PRIVATE).edit().remove("lista").apply()
    }

    private fun saveFolderUri(uri: Uri) { getSharedPreferences("settings", MODE_PRIVATE).edit().putString("folder_uri", uri.toString()).apply() }
    private fun loadFolderUri() { getSharedPreferences("settings", MODE_PRIVATE).getString("folder_uri", null)?.let { selectedFolderUri = Uri.parse(it) } }
    
    private fun impostaClickListenerOre() {
        inputOre.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                // Quando clicco sul campo, lo svuoto
                inputOre.setText("")
                
                // --- SCORRIMENTO AUTOMATICO ANCHE QUI ---
                v.postDelayed({
                    val labelInserimento = findViewById<TextView>(R.id.label_inserimento)
                    mainScrollView.smoothScrollTo(0, labelInserimento.top)
                }, 300)
            }
        }
    }
    
    private fun aggiornaCampoOre() {
        if (getString(R.string.user_name) != "Angelo Boi") {
            val totali = listaAttivita.sumOf { 
                try { it.second.replace(',', '.').replace(':', '.').toDouble() } catch (e: Exception) { 0.0 }
            }
            val rimanenti = 7.42 - totali
            
            if (rimanenti > 0) {
                inputOre.setText(String.format(Locale.ITALY, "%.2f", rimanenti).replace(',', ':'))
            } else {
                inputOre.setText("0:00")
            }
        }
    }

    private fun aggiornaDataCorrente() {
        val c = Calendar.getInstance()
        inputGg.setText(String.format(Locale.ITALY, "%02d", c.get(Calendar.DAY_OF_MONTH)))
        inputMm.setText(String.format(Locale.ITALY, "%02d", c.get(Calendar.MONTH) + 1))
        inputAaaa.setText(c.get(Calendar.YEAR).toString())
    }

    private fun aggiornaListaVisiva() {
        containerAttivita.removeAllViews()
        val isAngelo = getString(R.string.user_name) == "Angelo Boi"
        var somma = 0.0
        var sommaString = "0:00"

        val straordinarioAngelo = inputStraordinarioAngelo.text.toString().replace(',', '.').replace(':', '.').toDoubleOrNull() ?: 0.0

        if (isAngelo) {
            somma = 7.42 + straordinarioAngelo
            sommaString = String.format(Locale.ITALY, "%.2f", somma).replace(',', ':')
        } else {
            listaAttivita.forEach { p ->
                try { somma += p.second.replace(',', '.').replace(':', '.').toDouble() } catch (e: Exception) {}
            }
            sommaString = String.format(Locale.ITALY, "%.2f", somma).replace(',', ':')
        }

        listaAttivita.forEachIndexed { i, p ->
            val tv = TextView(this).apply {
                val oreString = if (p.second.isNotEmpty()) " (${p.second.replace(',', ':').replace('.', ':')}h)" else ""
                text = String.format(Locale.ITALY, "%d) %s%s", i + 1, p.first, oreString)
                setPadding(32, 24, 32, 24)
                val outValue = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.main_text))
                textSize = 15f
                
                // CLICK NORMALE: MODIFICA
                setOnClickListener { 
                    inputDescrizione.setText(p.first)
                    inputOre.setText(p.second.replace('.', ':').replace(',', ':'))
                    listaAttivita.removeAt(i)
                    aggiornaListaVisiva()
                    aggiornaCampoOre()
                    // Pulisci il campo ore per permettere nuova inserzione
                    if (getString(R.string.user_name) != "Angelo Boi") {
                        inputOre.setText("")
                    }
                    salvaBozza()
                }
                
                // CLICK PROLUNGATO: ELIMINA DIRETTAMENTE
                setOnLongClickListener {
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Elimina attività")
                        .setMessage("Vuoi eliminare questa riga?")
                        .setPositiveButton("Elimina") { _, _ ->
                            listaAttivita.removeAt(i)
                            aggiornaListaVisiva()
                            aggiornaCampoOre()
                            salvaBozza()
                        }
                        .setNegativeButton("Annulla", null)
                        .show()
                    true
                }
            }
            containerAttivita.addView(tv)
        }
        textOreTotali.text = getString(R.string.totale_ore_label, sommaString)
        
        // Aggiorna ore restanti per Luca e Ilario
        if (getString(R.string.user_name) != "Angelo Boi") {
            val rimanenti = 7.42 - somma
            if (rimanenti > 0) {
                val restantiString = String.format(Locale.ITALY, "%.2f", rimanenti).replace(',', ':')
                textOreRestanti.text = "Restanti: $restantiString"
                textOreRestanti.visibility = View.VISIBLE
            } else {
                textOreRestanti.text = "Restanti: 0:00"
                textOreRestanti.visibility = View.VISIBLE
            }
        } else {
            textOreRestanti.visibility = View.GONE
        }
        
        // Calcolo lo straordinario in sessantesimi
        val straordinario: Double = if (isAngelo) {
            straordinarioAngelo
        } else if (somma > 7.42) {
            // Sottrazione in sessantesimi: 8,00 - 7,42 = 0,18
            val sommaFormatted = String.format(Locale.US, "%.2f", somma)
            val sommaOre = sommaFormatted.split('.')
            val sogliaOre = "7.42".split('.')
            
            if (sommaOre.size >= 2 && sogliaOre.size >= 2) {
                val sommaOreInt = sommaOre[0].toInt()
                val sommaMin = sommaOre[1].padEnd(2, '0').substring(0, 2).toInt()
                val sogliaOreInt = sogliaOre[0].toInt()
                val sogliaMin = sogliaOre[1].toInt()
                
                var straordinarioOre = sommaOreInt - sogliaOreInt
                var straordinarioMin = sommaMin - sogliaMin
                
                // Gestisco i prestiti di minuti
                if (straordinarioMin < 0) {
                    straordinarioOre -= 1
                    straordinarioMin += 60
                }
                
                straordinarioOre + (straordinarioMin / 100.0)
            } else {
                somma - 7.42
            }
        } else {
            0.0
        }
        textStraordinario.visibility = if (straordinario > 0.0) View.VISIBLE else View.GONE
        
        // Visualizzo lo straordinario in formato sessantesimi simple
        val straordinarioString = String.format(Locale.ITALY, "%.2f", straordinario).replace(',', ':')
        textStraordinario.text = getString(R.string.straordinario_label, straordinarioString)
    }

    private fun caricaComeIeri() {
        val spinnerNome: Spinner = findViewById(R.id.spinner_nome)
        val nomeCorrente = spinnerNome.selectedItem.toString()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val ieri = calendar.time

        thread {
            val interventi = AppDatabase.getDatabase(this).interventoDao().getAll()
            val isoFullParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val simpleParser = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

            val interventoIeri = interventi.find { intervento ->
                val dataIntervento = try {
                    if (intervento.data.contains("T")) isoFullParser.parse(intervento.data)
                    else simpleParser.parse(intervento.data)
                } catch (e: Exception) { null }

                if (dataIntervento != null) {
                    val calIntervento = Calendar.getInstance().apply { time = dataIntervento }
                    val calIeri = Calendar.getInstance().apply { time = ieri }
                    calIntervento.get(Calendar.YEAR) == calIeri.get(Calendar.YEAR) &&
                    calIntervento.get(Calendar.DAY_OF_YEAR) == calIeri.get(Calendar.DAY_OF_YEAR) &&
                    intervento.nome == nomeCorrente
                } else false
            }

            runOnUiThread {
                if (interventoIeri != null) {
                    val spinnerScelte: Spinner = findViewById(R.id.spinner_scelte)
                    val posRep = (spinnerScelte.adapter as ArrayAdapter<String>).getPosition(interventoIeri.reparto)
                    if (posRep >= 0) spinnerScelte.setSelection(posRep)
                    val spinnerTurno: Spinner = findViewById(R.id.spinner_turno)
                    val posTur = (spinnerTurno.adapter as ArrayAdapter<String>).getPosition(interventoIeri.turno)
                    if (posTur >= 0) spinnerTurno.setSelection(posTur)
                    try {
                        val type = object : TypeToken<List<Pair<String, String>>>() {}.type
                        val att: List<Pair<String, String>> = Gson().fromJson(interventoIeri.attivitaJson, type)
                        listaAttivita.clear(); listaAttivita.addAll(att); aggiornaListaVisiva()
                        aggiornaCampoOre()
                        salvaBozza()
                        Toast.makeText(this, "Dati di ieri caricati!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {}
                } else {
                    Toast.makeText(this, "Nessun dato trovato per ieri", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
