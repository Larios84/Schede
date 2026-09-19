package com.example.schede

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class DettaglioActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var interventi: List<Intervento> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Dettaglio Intervento"

        // RECUPERO OGGETTO PASSATO (PRECISIONE MASSIMA)
        val interventoCliccato = intent.getSerializableExtra("INTERVENTO_OBJ") as? Intervento

        viewPager = findViewById(R.id.viewPager)

        thread {
            val currentUserName = getString(R.string.user_name)
            val allInterventi = AppDatabase.getDatabase(this).interventoDao().getAll()
                .filter { it.nome.equals(currentUserName, ignoreCase = true) }
                .toMutableList()
            
            // SE L'INTERVENTO È CLOUD (NUVOLETTA) E NON È NEL DB, LO AGGIUNGIAMO ALLA LISTA TEMPORANEA
            if (interventoCliccato != null && allInterventi.none { it.data == interventoCliccato.data && it.attivitaJson == interventoCliccato.attivitaJson }) {
                allInterventi.add(0, interventoCliccato)
            }

            // CERCHIAMO LA POSIZIONE ESATTA (usando il JSON delle attività come firma univoca)
            val startPos = if (interventoCliccato != null) {
                allInterventi.indexOfFirst { 
                    it.data == interventoCliccato.data && it.attivitaJson == interventoCliccato.attivitaJson 
                }
            } else 0

            interventi = allInterventi
            
            runOnUiThread {
                viewPager.adapter = DettaglioAdapter(interventi)
                if (startPos != -1) viewPager.setCurrentItem(startPos, false)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    inner class DettaglioAdapter(private val list: List<Intervento>) : RecyclerView.Adapter<DettaglioAdapter.ViewHolder>() {

        private val isoFullParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        private val displayFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
        private val timeFormatter = SimpleDateFormat("H.mm", Locale.ITALIAN).apply { timeZone = TimeZone.getDefault() }

        private fun parseDataSicura(dataStr: String): String {
            return try {
                if (dataStr.contains("T")) {
                    val d = isoFullParser.parse(dataStr)!!
                    val cal = Calendar.getInstance().apply { 
                        time = d
                        add(Calendar.HOUR_OF_DAY, 2) 
                    }
                    displayFormatter.format(cal.time)
                } else dataStr
            } catch (e: Exception) { dataStr }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtData: TextView = view.findViewById(R.id.det_data)
            val txtReparto: TextView = view.findViewById(R.id.det_reparto)
            val txtInfo: TextView = view.findViewById(R.id.det_info_generali)
            val containerAtt: LinearLayout = view.findViewById(R.id.det_container_attivita)
            val btnElimina: Button = view.findViewById(R.id.det_btn_elimina)
            val btnRigenera: Button = view.findViewById(R.id.det_btn_rigenera)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_dettaglio, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val textColor = ContextCompat.getColor(this@DettaglioActivity, R.color.main_text)

            val dataVisualizzata = parseDataSicura(item.data)
            holder.txtData.text = dataVisualizzata
            holder.txtReparto.text = item.reparto
            
            var turnoVisualizzato = item.turno
            if (turnoVisualizzato.contains("T")) {
                try {
                    val date = isoFullParser.parse(turnoVisualizzato)
                    if (date != null) turnoVisualizzato = timeFormatter.format(date)
                } catch (e: Exception) {}
            }

            var info = "Operatore: ${item.nome}\nTurno: $turnoVisualizzato\nOre Totali: ${String.format(Locale.ITALY, "%.2f", item.oreTotali)}"
            if (item.straordinario > 0) info += "\nStraordinario: ${String.format(Locale.ITALY, "%.2f", item.straordinario)}"
            holder.txtInfo.text = info

            holder.containerAtt.removeAllViews()
            if (!item.attivitaJson.isNullOrEmpty()) {
                try {
                    val listType = object : TypeToken<List<Pair<String, String>>>() {}.type
                    val attivita: List<Pair<String, String>> = Gson().fromJson(item.attivitaJson, listType)
                    attivita.forEach { (desc, ore) ->
                        holder.containerAtt.addView(TextView(holder.itemView.context).apply {
                            text = "• $desc (${ore.replace(".", ",")} h)"
                            setTextColor(textColor)
                            setPadding(0, 8, 0, 8)
                        })
                    }
                } catch (e: Exception) {}
            }

            holder.btnElimina.setOnClickListener { 
                AlertDialog.Builder(this@DettaglioActivity)
                    .setTitle("Elimina record")
                    .setMessage("Vuoi eliminare questo intervento dal registro?")
                    .setPositiveButton("Sì") { _, _ ->
                        thread {
                            AppDatabase.getDatabase(this@DettaglioActivity).interventoDao().delete(item)
                            runOnUiThread { finish() }
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }

            holder.btnRigenera.setOnClickListener {
                rigeneraPdf(item, turnoVisualizzato, dataVisualizzata)
            }
        }

        override fun getItemCount() = list.size

        private fun rigeneraPdf(intervento: Intervento, turnoCorretto: String, dataCorretta: String) {
            val context = this@DettaglioActivity
            val uriStr = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("folder_uri", null) ?: return
            val folderUri = Uri.parse(uriStr)

            val formData = mutableMapOf<String, String>()
            val mapReparto = mapOf("AVL" to "avl", "BHS" to "bhs", "IMP.TECNOLOGICI" to "tec", "IMP.ELETTRICI" to "elettr", "IMP.SPECIALI" to "spec", "MAN.GEN" to "gen", "STP VERDE" to "stp")
            mapReparto.forEach { (k, v) -> formData[v] = if (intervento.reparto.uppercase().contains(k)) "Yes" else "Off" }
            
            formData["turno"] = turnoCorretto
            formData["nome"] = intervento.nome
            formData["data"] = dataCorretta

            if (!intervento.attivitaJson.isNullOrEmpty()) {
                try {
                    val listType = object : TypeToken<List<Pair<String, String>>>() {}.type
                    val lista: List<Pair<String, String>> = Gson().fromJson(intervento.attivitaJson, listType)
                    lista.forEachIndexed { i, p -> 
                        val rowNum = i + 1
                        val fieldName = when(rowNum) {
                            2 -> "att2_2"
                            8 -> "att2"
                            else -> "att$rowNum"
                        }
                        formData[fieldName] = p.first
                        val oreFormat = try { String.format(Locale.ITALY, "%.2f", p.second.toDouble()) } catch(e:Exception) { p.second }
                        formData["ore$rowNum"] = oreFormat
                    }
                } catch (e: Exception) {}
            }

            formData["tot"] = String.format(Locale.ITALY, "%.2f", intervento.oreTotali)
            formData["str"] = if (intervento.straordinario > 0) String.format(Locale.ITALY, "%.2f", intervento.straordinario) else ""

            val dataParts = dataCorretta.split("/")
            val dataISO = if (dataParts.size == 3) {
                val giorno = dataParts[0].padStart(2, '0')
                val mese = dataParts[1].padStart(2, '0')
                val anno = if (dataParts[2].length == 2) "20" + dataParts[2] else dataParts[2]
                "$anno$mese$giorno"
            } else dataCorretta.replace("/", "")

            val template = when {
                intervento.nome.contains("Ilario") -> "templateiz.pdf"
                intervento.nome.contains("Angelo") -> "templateab.pdf"
                else -> "templatelg.pdf"
            }

            thread {
                val uri = PdfStamper(context).fillPdfFromAssets(template, "$dataISO ${intervento.nome}.pdf", formData, folderUri, null)
                runOnUiThread { 
                    if (uri != null) {
                        Toast.makeText(context, "✅ PDF Rigenerato correttamente!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "❌ Errore rigenerazione", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}