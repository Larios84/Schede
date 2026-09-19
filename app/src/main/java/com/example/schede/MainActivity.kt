package com.example.schede

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
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
    private lateinit var textStraordinario: TextView
    private lateinit var containerAttivita: LinearLayout
    private lateinit var inputDescrizione: AutoCompleteTextView
    private lateinit var inputOre: EditText
    
    private lateinit var inputGg: EditText
    private lateinit var inputMm: EditText
    private lateinit var inputAaaa: EditText
    private lateinit var loadYesterdayButton: Button

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
        containerAttivita = findViewById(R.id.container_attivita)
        textOreTotali = findViewById(R.id.text_ore_totali)
        textStraordinario = findViewById(R.id.text_straordinario)
        loadYesterdayButton = findViewById(R.id.load_yesterday_button)
        
        val spinnerScelte: Spinner = findViewById(R.id.spinner_scelte)
        val spinnerTurno: Spinner = findViewById(R.id.spinner_turno)
        val spinnerNome: Spinner = findViewById(R.id.spinner_nome)
        
        val repartiArray = arrayOf("AVL", "BHS", "IMP.TECNOLOGICI", "IMP.ELETTRICI", "IMP.SPECIALI", "MAN.GEN", "STP VERDE")
        val turniArray = arrayOf("1", "2", "3", "4", "5", "5.48", "6", "6+", "6.48", "7", "7+", "8", "9", "13", "13+", "13.48", "14", "14.48", "15", "21+", "21.48", "22")
        
        spinnerScelte.adapter = createHighlightedAdapter(repartiArray, spinnerScelte)
        spinnerTurno.adapter = createHighlightedAdapter(turniArray, spinnerTurno)
        
        val currentUserName = getString(R.string.user_name)
        spinnerNome.adapter = createHighlightedAdapter(arrayOf(currentUserName), spinnerNome)
        spinnerNome.isEnabled = false

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
            val ore = inputOre.text.toString().trim().replace(",", ".")
            if (desc.isNotEmpty() && ore.isNotEmpty()) {
                // CONTROLLO DUPLICATI
                if (listaAttivita.any { it.first.equals(desc, ignoreCase = true) }) {
                    Toast.makeText(this, "Attività già presente in lista!", Toast.LENGTH_SHORT).show()
                }
                
                listaAttivita.add(Pair(desc, ore))
                inputDescrizione.setText(""); inputOre.setText("")
                aggiornaListaVisiva()
                salvaBozza()
            } else {
                Toast.makeText(this, "Inserisci descrizione e ore!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btn_clear_all).setOnClickListener {
            inputDescrizione.setText("")
            inputOre.setText("")
        }

        findViewById<Button>(R.id.create_pdf_button).setOnClickListener {
            if (listaAttivita.isEmpty()) {
                Toast.makeText(this, "Aggiungi almeno un'attività!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedFolderUri == null) { folderPickerLauncher.launch(null); return@setOnClickListener }

            var totaleOre = 0.0
            listaAttivita.forEach { pair ->
                try { totaleOre += pair.second.toDouble() } catch (e: Exception) {}
            }

            if (totaleOre < 7.5) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Attenzione")
                    .setMessage("Le ore totali sono ${String.format(Locale.ITALY, "%.2f", totaleOre)}h, minori di 7,5h. Vuoi continuare comunque?")
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

        var oreTotali = 0.0
        listaAttivita.forEachIndexed { i, pair ->
            if (i < 8) {
                val rowNum = i + 1
                val fieldName = when(rowNum) {
                    2 -> "att2_2"
                    8 -> "att2"
                    else -> "att$rowNum"
                }
                formData[fieldName] = pair.first
                formData["ore$rowNum"] = pair.second
                try { oreTotali += pair.second.toDouble() } catch (e: Exception) {}
            }
        }
        val str = if (oreTotali > 7.5) oreTotali - 7.5 else 0.0
        formData["tot"] = String.format(Locale.ITALY, "%.2f", oreTotali)
        formData["str"] = if (str > 0) String.format(Locale.ITALY, "%.2f", str) else ""

        val dataShort = data.replace("/", "-").let { it.substring(0, 6) + it.takeLast(2) }
        val nomeFile = "$nome $dataShort.pdf"
        val assetTemplate = if (nome == "Zanetti Ilario") "templateiz.pdf" else "templatelg.pdf"

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
                Toast.makeText(this, "Ripristinata bozza precedente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancellaBozza() {
        getSharedPreferences("bozza", MODE_PRIVATE).edit().remove("lista").apply()
    }

    private fun saveFolderUri(uri: Uri) { getSharedPreferences("settings", MODE_PRIVATE).edit().putString("folder_uri", uri.toString()).apply() }
    private fun loadFolderUri() { getSharedPreferences("settings", MODE_PRIVATE).getString("folder_uri", null)?.let { selectedFolderUri = Uri.parse(it) } }
    
    private fun aggiornaDataCorrente() {
        val c = Calendar.getInstance()
        inputGg.setText(String.format(Locale.ITALY, "%02d", c.get(Calendar.DAY_OF_MONTH)))
        inputMm.setText(String.format(Locale.ITALY, "%02d", c.get(Calendar.MONTH) + 1))
        inputAaaa.setText(c.get(Calendar.YEAR).toString())
    }

    private fun aggiornaListaVisiva() {
        containerAttivita.removeAllViews()
        var somma = 0.0
        listaAttivita.forEachIndexed { i, p ->
            val tv = TextView(this).apply {
                text = String.format(Locale.ITALY, "%d) %s (%sh)", i + 1, p.first, p.second)
                setPadding(32, 24, 32, 24)
                val outValue = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.main_text))
                textSize = 15f
                
                // CLICK NORMALE: MODIFICA
                setOnClickListener { 
                    inputDescrizione.setText(p.first)
                    inputOre.setText(p.second)
                    listaAttivita.removeAt(i)
                    aggiornaListaVisiva()
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
                            salvaBozza()
                        }
                        .setNegativeButton("Annulla", null)
                        .show()
                    true
                }
            }
            containerAttivita.addView(tv)
            try { somma += p.second.toDouble() } catch (e: Exception) {}
        }
        textOreTotali.text = getString(R.string.totale_ore_label, String.format(Locale.ITALY, "%.2f", somma))
        textStraordinario.visibility = if (somma > 7.5) View.VISIBLE else View.GONE
        textStraordinario.text = getString(R.string.straordinario_label, String.format(Locale.ITALY, "%.2f", somma - 7.5))
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
