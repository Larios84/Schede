package com.example.schede

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RimborsoZiActivity : AppCompatActivity() {

    private data class Trasferta(
        var giorno: String = "",
        var percorso: String = "",
        var km: String = "",
        var motivazione: String = ""
    )

    private data class BozzaRimborsoZi(
        val mese: String = "",
        val anno: String = "",
        val autovettura: String = "",
        val combustibile: String = "",
        val targa: String = "",
        val nome: String = "",
        val trasferte: List<Trasferta> = emptyList()
    )

    private data class Veicolo(
        val modello: String,
        val combustibile: String,
        val targa: String
    ) {
        override fun toString(): String {
            return if (targa.isNotEmpty()) "[$targa] $modello ($combustibile)" else "$modello ($combustibile)"
        }
    }

    private val mesi = arrayOf(
        "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
        "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
    )

    private val anni = arrayOf("2024", "2025", "2026", "2027", "2028")

    private val suggerimentiPercorsiDefault = arrayOf(
        "Residenza - Aeroporto - Residenza",
        "Residenza - APT - Residenza",
        "Cagliari - Elmas - Cagliari"
    )

    private val suggerimentiMotivazioniDefault = arrayOf(
        "Servizio di manutenzione impianti",
        "Servizio di reperibilità e intervento",
        "Turno di servizio"
    )

    private val listaTrasferte = mutableListOf<Trasferta>()
    private var trasfertaInModificaIndex: Int? = null

    private val gson = Gson()
    private var isCaricamentoBozzaInCorso = false
    private var isProgrammaticSpinnerChange = false
    private var veicoloSelezionatoIndex: Int? = null

    private lateinit var editAutovettura: EditText
    private lateinit var editCombustibile: EditText
    private lateinit var editTarga: EditText
    private lateinit var editNome: EditText
    private lateinit var spinnerMese: Spinner
    private lateinit var spinnerAnno: Spinner
    private lateinit var spinnerVeicoli: Spinner
    private lateinit var btnSalvaVeicolo: Button
    private lateinit var btnEliminaVeicolo: ImageButton

    // Form Inserimento Trasferta
    private lateinit var tvTitoloFormTrasferta: TextView
    private lateinit var inputGiorno: EditText
    private lateinit var inputKm: EditText
    private lateinit var inputPercorso: AutoCompleteTextView
    private lateinit var inputMotivazione: AutoCompleteTextView
    private lateinit var btnOggi: Button
    private lateinit var btnPercorsoPredefinito: Button
    private lateinit var btnAggiungiTrasferta: Button
    private lateinit var btnAnnullaModifica: Button

    // Container Riepilogo
    private lateinit var tvTitoloRiepilogo: TextView
    private lateinit var containerTrasferteInserite: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rimborso_zi)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Rimborso KM"

        editNome = findViewById(R.id.edit_nome_zi)
        editAutovettura = findViewById(R.id.edit_autovettura_zi)
        editCombustibile = findViewById(R.id.edit_combustibile_zi)
        editTarga = findViewById(R.id.edit_targa_zi)
        spinnerMese = findViewById(R.id.spinner_mese_zi)
        spinnerAnno = findViewById(R.id.spinner_anno_zi)
        spinnerVeicoli = findViewById(R.id.spinner_veicoli_salvati)
        btnSalvaVeicolo = findViewById(R.id.btn_salva_veicolo)
        btnEliminaVeicolo = findViewById(R.id.btn_elimina_veicolo)

        tvTitoloFormTrasferta = findViewById(R.id.tv_titolo_form_trasferta)
        inputGiorno = findViewById(R.id.input_giorno_zi)
        inputKm = findViewById(R.id.input_km_zi)
        inputPercorso = findViewById(R.id.input_percorso_zi)
        inputMotivazione = findViewById(R.id.input_motivazione_zi)
        btnOggi = findViewById(R.id.btn_oggi_trasferta)
        btnPercorsoPredefinito = findViewById(R.id.btn_percorso_predefinito)
        btnAggiungiTrasferta = findViewById(R.id.btn_aggiungi_trasferta)
        btnAnnullaModifica = findViewById(R.id.btn_annulla_modifica_trasferta)

        tvTitoloRiepilogo = findViewById(R.id.tv_titolo_riepilogo_trasferte)
        containerTrasferteInserite = findViewById(R.id.container_trasferte_inserite)

        // Nome operatore bloccato e non modificabile
        editNome.setText(getString(R.string.user_name))
        editNome.isEnabled = false

        setupMeseAnnoSpinners()
        setupFormTrasferta()
        setupAutoSaveListeners()
        aggiornaSpinnerVeicoli()
        caricaBozza()

        btnSalvaVeicolo.setOnClickListener {
            salvaOAggiornaVeicolo()
        }

        btnEliminaVeicolo.setOnClickListener {
            eliminaVeicoloSelezionato()
        }

        findViewById<Button>(R.id.btn_generate_rimborso_zi)
            .setOnClickListener {
                generaPdf()
            }

        findViewById<Button>(R.id.btn_clear_rimborso_zi)
            .setOnClickListener {
                confermaEAnnullaBozza()
            }
    }

    private fun getTrattaStandard(): String {
        val prefs = getSharedPreferences("veicoli_salvati_prefs", MODE_PRIVATE)
        return prefs.getString("tratta_standard", "Residenza - Aeroporto - Residenza") ?: "Residenza - Aeroporto - Residenza"
    }

    private fun salvaTrattaStandard(tratta: String) {
        getSharedPreferences("veicoli_salvati_prefs", MODE_PRIVATE)
            .edit()
            .putString("tratta_standard", tratta)
            .apply()
    }

    private fun mostraDialogModificaTrattaStandard() {
        val input = EditText(this).apply {
            setText(getTrattaStandard())
            hint = "Es. Residenza - Aeroporto - Residenza"
            setSelection(text.length)
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Modifica Tratta Standard")
            .setMessage("Inserisci il percorso predefinito che preferisci (puoi anche tenere premuto sul tasto per modificarlo in ogni momento):")
            .setView(input)
            .setPositiveButton("Salva") { _, _ ->
                val nuovaTratta = input.text.toString().trim()
                if (nuovaTratta.isNotEmpty()) {
                    salvaTrattaStandard(nuovaTratta)
                    Toast.makeText(this, "Tratta standard aggiornata!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun setupFormTrasferta() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)

        btnOggi.setOnClickListener {
            inputGiorno.setText(dateFormat.format(Calendar.getInstance().time))
        }

        btnPercorsoPredefinito.setOnClickListener {
            inputPercorso.setText(getTrattaStandard())
        }

        btnPercorsoPredefinito.setOnLongClickListener {
            mostraDialogModificaTrattaStandard()
            true
        }

        // Suggerimenti AutoComplete per Percorso
        val adapterPercorsi = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suggerimentiPercorsiDefault)
        inputPercorso.setAdapter(adapterPercorsi)
        inputPercorso.threshold = 1
        inputPercorso.setOnClickListener { inputPercorso.showDropDown() }

        // Suggerimenti AutoComplete per Motivazione
        val adapterMotivazioni = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suggerimentiMotivazioniDefault)
        inputMotivazione.setAdapter(adapterMotivazioni)
        inputMotivazione.threshold = 1
        inputMotivazione.setOnClickListener { inputMotivazione.showDropDown() }

        inputGiorno.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    cal.set(year, month, dayOfMonth)
                    inputGiorno.setText(dateFormat.format(cal.time))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnAggiungiTrasferta.setOnClickListener {
            aggiungiOModificaTrasferta()
        }

        btnAnnullaModifica.setOnClickListener {
            resetFormTrasferta()
        }
    }

    private fun aggiungiOModificaTrasferta() {
        val giorno = inputGiorno.text.toString().trim()
        val km = inputKm.text.toString().replace(",", ".").trim()
        val percorso = inputPercorso.text.toString().trim()
        val motivazione = inputMotivazione.text.toString().trim()

        if (giorno.isEmpty() || km.isEmpty() || percorso.isEmpty() || motivazione.isEmpty()) {
            Toast.makeText(this, "Compila tutti i campi (Data, Km, Percorso e Motivazione)!", Toast.LENGTH_LONG).show()
            return
        }

        val idx = trasfertaInModificaIndex
        if (idx != null && idx >= 0 && idx < listaTrasferte.size) {
            // Aggiorna trasferta esistente
            listaTrasferte[idx] = Trasferta(giorno, percorso, km, motivazione)
            Toast.makeText(this, "Trasferta modificata!", Toast.LENGTH_SHORT).show()
        } else {
            // Nuova trasferta
            if (listaTrasferte.size >= 13) {
                Toast.makeText(this, "Hai raggiunto il limite massimo di 13 trasferte per scheda!", Toast.LENGTH_LONG).show()
                return
            }
            listaTrasferte.add(Trasferta(giorno, percorso, km, motivazione))
            Toast.makeText(this, "Trasferta aggiunta!", Toast.LENGTH_SHORT).show()
        }

        resetFormTrasferta()
        aggiornaRiepilogoTrasferte()
        salvaBozza()
    }

    private fun avviaModificaTrasferta(index: Int) {
        if (index in 0 until listaTrasferte.size) {
            val t = listaTrasferte[index]
            trasfertaInModificaIndex = index
            inputGiorno.setText(t.giorno)
            inputKm.setText(t.km)
            inputPercorso.setText(t.percorso)
            inputMotivazione.setText(t.motivazione)

            tvTitoloFormTrasferta.text = "MODIFICA TRASFERTA #${index + 1}"
            btnAggiungiTrasferta.text = "✏️ SALVA MODIFICA"
            btnAnnullaModifica.visibility = View.VISIBLE
        }
    }

    private fun resetFormTrasferta() {
        trasfertaInModificaIndex = null
        inputGiorno.setText("")
        inputKm.setText("")
        inputPercorso.setText("")
        inputMotivazione.setText("")

        tvTitoloFormTrasferta.text = "AGGIUNGI TRASFERTA"
        btnAggiungiTrasferta.text = "➕ AGGIUNGI TRASFERTA"
        btnAnnullaModifica.visibility = View.GONE
    }

    private fun eliminaTrasferta(index: Int) {
        if (index in 0 until listaTrasferte.size) {
            val t = listaTrasferte[index]
            val desc = if (t.giorno.isNotEmpty()) "del ${t.giorno}" else "#${index + 1}"

            AlertDialog.Builder(this)
                .setTitle("Elimina Trasferta")
                .setMessage("Sei sicuro di voler eliminare la trasferta $desc?")
                .setPositiveButton("Elimina") { _, _ ->
                    listaTrasferte.removeAt(index)
                    if (trasfertaInModificaIndex == index) {
                        resetFormTrasferta()
                    } else if (trasfertaInModificaIndex != null && trasfertaInModificaIndex!! > index) {
                        trasfertaInModificaIndex = trasfertaInModificaIndex!! - 1
                    }
                    aggiornaRiepilogoTrasferte()
                    salvaBozza()
                    Toast.makeText(this, "Trasferta rimossa", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    private fun ordinaTrasfertePerData() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        listaTrasferte.sortWith { t1, t2 ->
            val d1 = try { dateFormat.parse(t1.giorno) } catch (_: Exception) { null }
            val d2 = try { dateFormat.parse(t2.giorno) } catch (_: Exception) { null }

            when {
                d1 == null && d2 == null -> 0
                d1 == null -> 1
                d2 == null -> -1
                else -> d1.compareTo(d2)
            }
        }
    }

    private fun aggiornaRiepilogoTrasferte() {
        ordinaTrasfertePerData()
        containerTrasferteInserite.removeAllViews()
        val layoutInflater = LayoutInflater.from(this)

        tvTitoloRiepilogo.text = "RIEPILOGO TRASFERTE (${listaTrasferte.size} / 13)"

        listaTrasferte.forEachIndexed { index, t ->
            val itemView = layoutInflater.inflate(R.layout.item_trasferta_inserita, containerTrasferteInserite, false)

            val tvNum = itemView.findViewById<TextView>(R.id.tv_item_num)
            val tvData = itemView.findViewById<TextView>(R.id.tv_item_data)
            val tvKm = itemView.findViewById<TextView>(R.id.tv_item_km)
            val tvPercorso = itemView.findViewById<TextView>(R.id.tv_item_percorso)
            val tvMotivazione = itemView.findViewById<TextView>(R.id.tv_item_motivazione)
            val btnModifica = itemView.findViewById<ImageButton>(R.id.btn_item_modifica)
            val btnElimina = itemView.findViewById<ImageButton>(R.id.btn_item_elimina)

            tvNum.text = "#${index + 1}"
            tvData.text = if (t.giorno.isNotEmpty()) t.giorno else "Data non spec."
            tvKm.text = if (t.km.isNotEmpty()) "${t.km} km" else "0 km"
            tvPercorso.text = if (t.percorso.isNotEmpty()) "Percorso: ${t.percorso}" else "Percorso: -"
            tvMotivazione.text = if (t.motivazione.isNotEmpty()) "Motivazione: ${t.motivazione}" else "Motivazione: -"

            btnModifica.setOnClickListener {
                avviaModificaTrasferta(index)
            }

            btnElimina.setOnClickListener {
                eliminaTrasferta(index)
            }

            containerTrasferteInserite.addView(itemView)
        }

        aggiornaTotale()
    }

    private fun setupMeseAnnoSpinners() {
        val adapterMesi = ArrayAdapter(this, android.R.layout.simple_spinner_item, mesi)
        adapterMesi.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMese.adapter = adapterMesi

        val adapterAnni = ArrayAdapter(this, android.R.layout.simple_spinner_item, anni)
        adapterAnni.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAnno.adapter = adapterAnni

        val cal = Calendar.getInstance()
        val meseCorrenteIdx = cal.get(Calendar.MONTH) // 0-based
        val annoCorrenteStr = cal.get(Calendar.YEAR).toString()

        spinnerMese.setSelection(meseCorrenteIdx)
        val annoIdx = anni.indexOf(annoCorrenteStr)
        if (annoIdx >= 0) {
            spinnerAnno.setSelection(annoIdx)
        }

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isCaricamentoBozzaInCorso) {
                    salvaBozza()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerMese.onItemSelectedListener = listener
        spinnerAnno.onItemSelectedListener = listener
    }

    private fun getVeicoliSalvati(): MutableList<Veicolo> {
        val prefs = getSharedPreferences("veicoli_salvati_prefs", MODE_PRIVATE)
        val json = prefs.getString("lista_veicoli", null) ?: return mutableListOf()
        val type = object : TypeToken<List<Veicolo>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun salvaVeicoliInPrefs(lista: List<Veicolo>) {
        val json = gson.toJson(lista)
        getSharedPreferences("veicoli_salvati_prefs", MODE_PRIVATE)
            .edit()
            .putString("lista_veicoli", json)
            .apply()
    }

    private fun impostaCampiVeicoloBloccati(bloccati: Boolean) {
        editAutovettura.isEnabled = !bloccati
        editCombustibile.isEnabled = !bloccati
        editTarga.isEnabled = !bloccati
    }

    private fun salvaOAggiornaVeicolo() {
        val modello = editAutovettura.text.toString().trim()
        val combustibile = editCombustibile.text.toString().trim()
        val targa = editTarga.text.toString().trim()

        if (modello.isEmpty() && targa.isEmpty()) {
            Toast.makeText(this, "Inserisci il modello o la targa del veicolo", Toast.LENGTH_SHORT).show()
            return
        }

        val veicoli = getVeicoliSalvati()
        val idx = veicoloSelezionatoIndex

        if (idx != null && idx >= 0 && idx < veicoli.size) {
            // Aggiorna veicolo esistente
            veicoli[idx] = Veicolo(modello, combustibile, targa)
            salvaVeicoliInPrefs(veicoli)
            aggiornaSpinnerVeicoli(selezionaIndex = idx + 1)
            impostaCampiVeicoloBloccati(true)
            Toast.makeText(this, "Veicolo aggiornato nei preferiti!", Toast.LENGTH_SHORT).show()
        } else {
            // Inserisci nuovo veicolo
            veicoli.removeAll {
                (it.targa.equals(targa, ignoreCase = true) && targa.isNotEmpty()) ||
                (it.modello.equals(modello, ignoreCase = true) && targa.isEmpty())
            }
            veicoli.add(0, Veicolo(modello, combustibile, targa))
            salvaVeicoliInPrefs(veicoli)
            aggiornaSpinnerVeicoli(selezionaIndex = 1)
            impostaCampiVeicoloBloccati(true)
            Toast.makeText(this, "Nuovo veicolo salvato nei preferiti!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eliminaVeicoloSelezionato() {
        val veicoli = getVeicoliSalvati()
        val idx = veicoloSelezionatoIndex
        if (idx != null && idx >= 0 && idx < veicoli.size) {
            val veicoloDaEliminare = veicoli[idx]
            AlertDialog.Builder(this)
                .setTitle("Elimina Veicolo")
                .setMessage("Vuoi rimuovere '${veicoloDaEliminare.modello}' dai veicoli salvati?")
                .setPositiveButton("Elimina") { _, _ ->
                    veicoli.removeAt(idx)
                    salvaVeicoliInPrefs(veicoli)
                    veicoloSelezionatoIndex = null
                    impostaCampiVeicoloBloccati(false)
                    aggiornaSpinnerVeicoli()
                    Toast.makeText(this, "Veicolo rimosso dai preferiti", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    private fun aggiornaSpinnerVeicoli(selezionaIndex: Int = 0) {
        val veicoli = getVeicoliSalvati()
        val opzioni = mutableListOf("Seleziona veicolo salvato...")
        opzioni.addAll(veicoli.map { it.toString() })
        opzioni.add("➕ Inserisci nuovo veicolo")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opzioni)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        isProgrammaticSpinnerChange = true
        spinnerVeicoli.adapter = adapter
        if (selezionaIndex in 0 until opzioni.size) {
            spinnerVeicoli.setSelection(selezionaIndex)
        }
        isProgrammaticSpinnerChange = false

        spinnerVeicoli.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isProgrammaticSpinnerChange) return

                if (position > 0 && position <= veicoli.size) {
                    veicoloSelezionatoIndex = position - 1
                    val v = veicoli[position - 1]
                    editAutovettura.setText(v.modello)
                    editCombustibile.setText(v.combustibile)
                    editTarga.setText(v.targa)
                    impostaCampiVeicoloBloccati(true)
                    btnSalvaVeicolo.text = "✏️ AGGIORNA VEICOLO"
                    btnEliminaVeicolo.visibility = View.VISIBLE
                    salvaBozza()
                } else {
                    veicoloSelezionatoIndex = null
                    impostaCampiVeicoloBloccati(false)
                    btnSalvaVeicolo.text = "💾 SALVA VEICOLO"
                    btnEliminaVeicolo.visibility = View.GONE
                    if (position == veicoli.size + 1) {
                        editAutovettura.setText("")
                        editCombustibile.setText("")
                        editTarga.setText("")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                veicoloSelezionatoIndex = null
                impostaCampiVeicoloBloccati(false)
                btnSalvaVeicolo.text = "💾 SALVA VEICOLO"
                btnEliminaVeicolo.visibility = View.GONE
            }
        }
    }

    private fun setupAutoSaveListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isCaricamentoBozzaInCorso) {
                    salvaBozza()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        editNome.addTextChangedListener(watcher)
        editAutovettura.addTextChangedListener(watcher)
        editCombustibile.addTextChangedListener(watcher)
        editTarga.addTextChangedListener(watcher)
    }

    private fun salvaBozza() {
        val autovettura = editAutovettura.text.toString()
        val combustibile = editCombustibile.text.toString()
        val targa = editTarga.text.toString()
        val nome = getString(R.string.user_name)
        val mese = spinnerMese.selectedItem?.toString() ?: ""
        val anno = spinnerAnno.selectedItem?.toString() ?: ""

        val bozza = BozzaRimborsoZi(
            mese = mese,
            anno = anno,
            autovettura = autovettura,
            combustibile = combustibile,
            targa = targa,
            nome = nome,
            trasferte = listaTrasferte
        )

        val json = gson.toJson(bozza)
        getSharedPreferences("bozza_rimborso_zi", MODE_PRIVATE)
            .edit()
            .putString("dati_bozza", json)
            .apply()
    }

    private fun caricaBozza() {
        val prefs = getSharedPreferences("bozza_rimborso_zi", MODE_PRIVATE)
        val json = prefs.getString("dati_bozza", null) ?: return

        isCaricamentoBozzaInCorso = true
        try {
            val bozza = gson.fromJson(json, BozzaRimborsoZi::class.java) ?: return

            if (bozza.mese.isNotEmpty()) {
                val idxMese = mesi.indexOf(bozza.mese)
                if (idxMese >= 0) spinnerMese.setSelection(idxMese)
            }
            if (bozza.anno.isNotEmpty()) {
                val idxAnno = anni.indexOf(bozza.anno)
                if (idxAnno >= 0) spinnerAnno.setSelection(idxAnno)
            }

            if (bozza.autovettura.isNotEmpty()) editAutovettura.setText(bozza.autovettura)
            if (bozza.combustibile.isNotEmpty()) editCombustibile.setText(bozza.combustibile)
            if (bozza.targa.isNotEmpty()) editTarga.setText(bozza.targa)
            
            // Se c'è una targa caricata e corrisponde a un veicolo salvato, blocca i campi per sicurezza
            val veicoli = getVeicoliSalvati()
            val matchIdx = veicoli.indexOfFirst { it.targa.equals(bozza.targa, ignoreCase = true) && bozza.targa.isNotEmpty() }
            if (matchIdx >= 0) {
                veicoloSelezionatoIndex = matchIdx
                spinnerVeicoli.setSelection(matchIdx + 1)
                impostaCampiVeicoloBloccati(true)
            } else {
                impostaCampiVeicoloBloccati(false)
            }

            // Sempre legato al nome operatore del flavor
            editNome.setText(getString(R.string.user_name))
            editNome.isEnabled = false

            listaTrasferte.clear()
            listaTrasferte.addAll(bozza.trasferte.filter { 
                it.giorno.isNotEmpty() || it.percorso.isNotEmpty() || it.km.isNotEmpty() || it.motivazione.isNotEmpty() 
            })
            aggiornaRiepilogoTrasferte()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isCaricamentoBozzaInCorso = false
        }
    }

    private fun confermaEAnnullaBozza() {
        AlertDialog.Builder(this)
            .setTitle("Svuota Scheda")
            .setMessage("Sei sicuro di voler cancellare tutti i dati della scheda e iniziare un nuovo mese?")
            .setPositiveButton("Svuota") { _, _ ->
                svuotaScheda()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun svuotaScheda() {
        isCaricamentoBozzaInCorso = true
        getSharedPreferences("bozza_rimborso_zi", MODE_PRIVATE)
            .edit()
            .remove("dati_bozza")
            .apply()

        editAutovettura.setText("")
        editCombustibile.setText("")
        editTarga.setText("")
        editNome.setText(getString(R.string.user_name))
        editNome.isEnabled = false

        listaTrasferte.clear()
        resetFormTrasferta()
        aggiornaRiepilogoTrasferte()

        veicoloSelezionatoIndex = null
        impostaCampiVeicoloBloccati(false)
        aggiornaSpinnerVeicoli()
        isCaricamentoBozzaInCorso = false
        Toast.makeText(this, "Scheda svuotata", Toast.LENGTH_SHORT).show()
    }

    private fun aggiornaTotale() {
        val totale = listaTrasferte.sumOf {
            it.km.replace(",", ".")
                .toDoubleOrNull() ?: 0.0
        }

        findViewById<TextView>(R.id.text_totale_km_zi).text =
            if (totale % 1.0 == 0.0) {
                "Totale km: ${totale.toInt()}"
            } else {
                "Totale km: ${
                    String.format(Locale.US, "%.1f", totale)
                }"
            }
    }

    private fun generaPdf() {
        currentFocus?.clearFocus()

        val autovettura = editAutovettura.text.toString().trim()
        val combustibile = editCombustibile.text.toString().trim()
        val targa = editTarga.text.toString().trim()
        val nome = getString(R.string.user_name)
        val mese = spinnerMese.selectedItem?.toString() ?: ""
        val anno = spinnerAnno.selectedItem?.toString() ?: ""

        if (autovettura.isEmpty() ||
            combustibile.isEmpty() ||
            targa.isEmpty() ||
            nome.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Compila i dati dell'autovettura",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (listaTrasferte.isEmpty()) {
            Toast.makeText(this, "Inserisci almeno una trasferta prima di generare il PDF!", Toast.LENGTH_SHORT).show()
            return
        }

        val dati = mutableMapOf<String, String>()

        dati["Autovettura  modello  anno immatricolazione"] = autovettura
        dati["Combustibile"] = combustibile
        dati["Categoria"] = targa
        
        // Solo nome e cognome
        dati["Nome e CognomeRow1"] = nome

        var totaleKm = 0.0

        listaTrasferte.forEachIndexed { indice, trasferta ->
            if (indice < 13) {
                val numero = indice + 1

                val giorno = trasferta.giorno.trim()
                val percorso = trasferta.percorso.trim()
                val km = trasferta.km.replace(",", ".").trim()
                val motivazione = trasferta.motivazione.trim()

                if (giorno.isNotEmpty()) {
                    dati["GiornoRow$numero"] = giorno
                }

                if (percorso.isNotEmpty()) {
                    dati["Percorso APT X  APTRow$numero"] = percorso
                }

                if (km.isNotEmpty()) {
                    dati["KmRow$numero"] = km
                    totaleKm += km.toDoubleOrNull() ?: 0.0
                }

                if (motivazione.isNotEmpty()) {
                    dati["MotivazioneRow$numero"] = motivazione
                }
            }
        }

        dati["KmTotale km"] =
            if (totaleKm % 1.0 == 0.0) {
                totaleKm.toInt().toString()
            } else {
                String.format(Locale.US, "%.1f", totaleKm)
            }

        // Nome file esatto come richiesto: Rimborso Km <Mese> <Anno> <Nome Operatore>.pdf
        val nomeFile = "Rimborso Km $mese $anno $nome.pdf"

        val stamper = RimborsoZiPdfStamper(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val uri = stamper.generaPdfCompilato(
                nomeFile,
                dati
            )

            withContext(Dispatchers.Main) {
                if (uri != null) {
                    Toast.makeText(
                        this@RimborsoZiActivity,
                        "PDF Generato: $nomeFile",
                        Toast.LENGTH_LONG,
                    ).show()

                    apriPdf(uri)
                } else {
                    Toast.makeText(
                        this@RimborsoZiActivity,
                        "Errore nella generazione del PDF",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private fun apriPdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Nessuna app per aprire PDF",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onPause() {
        super.onPause()
        salvaBozza()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
