package com.example.schede

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class StoricoActivity : AppCompatActivity() {

    private lateinit var listContainer: LinearLayout
    private lateinit var spinnerMese: Spinner
    private lateinit var spinnerReparto: Spinner
    private lateinit var spinnerAnno: Spinner
    
    private val client = OkHttpClient()
    private var listaInterventiCorrente = mutableListOf<Intervento>()
    private var ordinamentoDecrescente = true

    private val mesi = arrayOf("Tutti", "Gen", "Feb", "Mar", "Apr", "Mag", "Giu", "Lug", "Ago", "Set", "Ott", "Nov", "Dic")
    private val reparti = arrayOf("Tutti", "AVL", "BHS", "TECNOLOGICI", "ELETTRICI", "SPECIALI", "MAN.GEN", "STP VERDE")
    private var anni = mutableListOf("Tutti")

    private val gson = GsonBuilder()
        .registerTypeAdapter(Intervento::class.java, object : JsonDeserializer<Intervento> {
            override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Intervento {
                val obj = json.asJsonObject
                val turnoElement = obj.get("turno")
                val turnoStr = when {
                    turnoElement == null || turnoElement.isJsonNull -> ""
                    turnoElement.isJsonPrimitive && turnoElement.asJsonPrimitive.isNumber -> turnoElement.asNumber.toString()
                    turnoElement.isJsonPrimitive && turnoElement.asJsonPrimitive.isString -> turnoElement.asString
                    else -> turnoElement.toString()
                }
                fun getDoubleOrZero(key: String): Double {
                    return try {
                        val element = obj.get(key)
                        when {
                            element == null || element.isJsonNull -> 0.0
                            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asDouble
                            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                                val str = element.asString
                                if (str.isEmpty()) 0.0 else str.toDoubleOrNull() ?: 0.0
                            }
                            else -> 0.0
                        }
                    } catch (e: Exception) { 0.0 }
                }
                return Intervento(
                    id = obj.get("id")?.asInt ?: 0,
                    data = obj.get("data")?.asString ?: "",
                    nome = obj.get("nome")?.asString ?: "",
                    reparto = obj.get("reparto")?.asString ?: "",
                    turno = turnoStr,
                    oreTotali = getDoubleOrZero("oreTotali"),
                    straordinario = getDoubleOrZero("straordinario"),
                    attivitaJson = obj.get("attivitaJson")?.asString ?: obj.get("attivita")?.asString ?: "[]"
                )
            }
        })
        .create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val backgroundColor = ContextCompat.getColor(this, R.color.main_background)
        val filterBarColor = ContextCompat.getColor(this, R.color.input_background)
        val textColor = ContextCompat.getColor(this, R.color.main_text)
        val labelColor = ContextCompat.getColor(this, R.color.label_color)
        val accentColor = ContextCompat.getColor(this, R.color.purple_500)
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        val filterBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 20, 10, 20)
            setBackgroundColor(filterBarColor)
            weightSum = 3f
        }

        fun createFilterColumn(title: String, weight: Float): Pair<LinearLayout, Spinner> {
            val col = LinearLayout(this@StoricoActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            col.addView(TextView(this@StoricoActivity).apply {
                text = title; textSize = 11f; setTypeface(null, Typeface.BOLD)
                setTextColor(labelColor); gravity = Gravity.CENTER
            })
            val spinner = Spinner(this@StoricoActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            col.addView(spinner)
            return Pair(col, spinner)
        }

        fun createAdapter(items: List<String>, spinner: Spinner): ArrayAdapter<String> {
            return object : ArrayAdapter<String>(this@StoricoActivity, android.R.layout.simple_spinner_item, items) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent) as TextView
                    v.gravity = Gravity.CENTER; v.textSize = 14f; v.setTextColor(textColor) 
                    return v
                }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getDropDownView(position, convertView, parent) as TextView
                    v.setBackgroundColor(filterBarColor)
                    if (position == spinner.selectedItemPosition) {
                        v.setTextColor(accentColor); v.setTypeface(null, Typeface.BOLD)
                        v.setBackgroundColor(Color.parseColor("#1A000000"))
                    } else { v.setTextColor(textColor) }
                    v.setPadding(30, 40, 30, 40); v.textSize = 16f
                    return v
                }
            }
        }

        val (colM, sM) = createFilterColumn("MESE", 1f); spinnerMese = sM
        spinnerMese.adapter = createAdapter(mesi.toList(), spinnerMese)

        val (colA, sA) = createFilterColumn("ANNO", 0.8f); spinnerAnno = sA
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (anni.size == 1) { anni.add(currentYear.toString()); anni.add((currentYear - 1).toString()) }
        spinnerAnno.adapter = createAdapter(anni, spinnerAnno)

        val (colR, sR) = createFilterColumn("REPARTO", 1.2f); spinnerReparto = sR
        spinnerReparto.adapter = createAdapter(reparti.toList(), spinnerReparto)

        filterBar.addView(colM); filterBar.addView(colA); filterBar.addView(colR)
        mainLayout.addView(filterBar)

        val scrollView = ScrollView(this).apply { setPadding(30, 10, 30, 30) }
        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollView.addView(listContainer)
        mainLayout.addView(scrollView)
        setContentView(mainLayout)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Registro Interventi"

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { 
                ordinaEMostra()
                (p0?.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        spinnerMese.onItemSelectedListener = listener
        spinnerAnno.onItemSelectedListener = listener
        spinnerReparto.onItemSelectedListener = listener
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val item = menu.add(0, 100, 0, "Ordina")
        item.setIcon(android.R.drawable.ic_menu_sort_by_size); item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        if (item.itemId == 100) { invertiOrdinamento(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun invertiOrdinamento() { ordinamentoDecrescente = !ordinamentoDecrescente; ordinaEMostra() }

    override fun onResume() { 
        super.onResume()
        aggiornaInterfacciaDaLocale()
        caricaDatiDalWeb() 
    }

    private fun aggiornaInterfacciaDaLocale() {
        thread {
            val currentUserName = getString(R.string.user_name)
            val dao = AppDatabase.getDatabase(this).interventoDao()
            val interventiDaDb = dao.getAll().filter { it.nome.equals(currentUserName, ignoreCase = true) }
            synchronized(listaInterventiCorrente) {
                listaInterventiCorrente.clear()
                interventiDaDb.forEach { it.isFromWeb = false; listaInterventiCorrente.add(it) }
            }
            runOnUiThread { aggiornaAnniDisponibili(); ordinaEMostra() }
        }
    }

    private fun parseDataSicura(dataStr: String): Date {
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val simpleParser = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
        return try {
            if (dataStr.contains("T")) {
                val d = isoParser.parse(dataStr)!!
                Calendar.getInstance().apply { time = d; add(Calendar.HOUR_OF_DAY, 2) }.time
            } else {
                simpleParser.parse(dataStr)!!
            }
        } catch (e: Exception) { Date(0) }
    }

    private fun aggiornaAnniDisponibili() {
        val setAnni = mutableSetOf<String>()
        synchronized(listaInterventiCorrente) {
            listaInterventiCorrente.forEach {
                val d = parseDataSicura(it.data)
                if (d.time > 0) setAnni.add(Calendar.getInstance().apply { time = d }.get(Calendar.YEAR).toString())
            }
        }
        val currentAnni = setAnni.toList().sortedDescending()
        val nuoviAnni = mutableListOf("Tutti"); nuoviAnni.addAll(currentAnni)
        if (nuoviAnni != anni) {
            anni.clear(); anni.addAll(nuoviAnni); (spinnerAnno.adapter as ArrayAdapter<String>).notifyDataSetChanged()
        }
    }

    private fun ordinaEMostra() {
        val meseS = spinnerMese.selectedItemPosition
        val annoS = spinnerAnno.selectedItem?.toString() ?: "Tutti"
        val repS = spinnerReparto.selectedItem?.toString() ?: "Tutti"
        val filtrata = synchronized(listaInterventiCorrente) {
            listaInterventiCorrente.filter { i ->
                val dI = parseDataSicura(i.data)
                val cal = Calendar.getInstance().apply { time = dI }
                val matchM = if (meseS == 0) true else (cal.get(Calendar.MONTH) + 1) == meseS
                val matchA = if (annoS == "Tutti") true else cal.get(Calendar.YEAR).toString() == annoS
                val matchR = if (repS == "Tutti") true else i.reparto.contains(repS, ignoreCase = true)
                matchM && matchA && matchR
            }
        }
        val ordinata = if (ordinamentoDecrescente) filtrata.sortedByDescending { parseDataSicura(it.data) } else filtrata.sortedBy { parseDataSicura(it.data) }
        mostraInterventi(ordinata)
    }

    private fun mostraInterventi(interventi: List<Intervento>) {
        listContainer.removeAllViews()
        val textColor = ContextCompat.getColor(this, R.color.main_text)
        if (interventi.isEmpty()) {
            listContainer.addView(TextView(this).apply { text = "Nessun intervento trovato."; gravity = Gravity.CENTER; setPadding(0, 100, 0, 0); setTextColor(textColor) })
            return
        }
        interventi.forEach { aggiungiRigaSemplice(it) }
    }

    private fun caricaDatiDalWeb() {
        thread {
            try {
                val url = "https://script.google.com/macros/s/AKfycbwa5cZnejXKZ_cV0CtyyXLiAUlIdiD9_U2NeSdmXH6K7WCBt8mkBLslY6flH1SXlbz5hA/exec?action=read&t=${System.currentTimeMillis()}"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val json = response.body?.string() ?: return@thread
                val type = object : TypeToken<List<Intervento>>() {}.type
                val interventiRaw: List<Intervento> = gson.fromJson(json, type)
                val currentUserName = getString(R.string.user_name)
                val interventiDalWeb = interventiRaw.filter { it.nome.equals(currentUserName, ignoreCase = true) }
                if (interventiDalWeb.isNotEmpty()) {
                    synchronized(listaInterventiCorrente) {
                        listaInterventiCorrente.clear()
                        interventiDalWeb.forEach { it.isFromWeb = true; listaInterventiCorrente.add(it) }
                    }
                    runOnUiThread { aggiornaAnniDisponibili(); ordinaEMostra() }
                }
            } catch (e: Exception) { Log.e("StoricoActivity", "Errore web", e) }
        }
    }

    private fun aggiungiRigaSemplice(intervento: Intervento) {
        val textColor = ContextCompat.getColor(this, R.color.main_text)
        val displayF = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
        val dataD = parseDataSicura(intervento.data)
        val dataDisplay = if (dataD.time > 0) displayF.format(dataD) else intervento.data

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(35, 35, 35, 35)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 20) }
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            backgroundTintList = ContextCompat.getColorStateList(this@StoricoActivity, R.color.input_background)
            setOnClickListener {
                val intent = Intent(this@StoricoActivity, DettaglioActivity::class.java)
                intent.putExtra("INTERVENTO_OBJ", intervento)
                startActivity(intent)
            }
        }
        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        topRow.addView(TextView(this).apply { text = if (intervento.isFromWeb) "☁️ " else "📱 "; textSize = 18f; setPadding(0, 0, 16, 0) })
        topRow.addView(TextView(this).apply { text = dataDisplay; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(textColor); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        topRow.addView(TextView(this).apply { text = if (intervento.nome.length > 15) intervento.nome.substring(0, 12) + "..." else intervento.nome.uppercase(); textSize = 11f; setTextColor(ContextCompat.getColor(this@StoricoActivity, R.color.label_color)) })
        row.addView(topRow)
        
        var turnoD = intervento.turno
        if (turnoD.contains("T")) { try { val isoT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }; val targetT = SimpleDateFormat("H.mm", Locale.ITALIAN).apply { timeZone = TimeZone.getDefault() }; turnoD = targetT.format(isoT.parse(turnoD)!!) } catch(e:Exception){} }
        row.addView(TextView(this).apply { text = "${intervento.reparto} - Turno: $turnoD (${intervento.oreTotali}h)"; textSize = 14f; setTextColor(Color.parseColor("#2E7D32")); setPadding(0, 8, 0, 0) })
        listContainer.addView(row)
    }
}
