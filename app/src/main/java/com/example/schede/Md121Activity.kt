package com.example.schede

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class Md121Activity : AppCompatActivity() {

    // Strutture dati: 16 unità totali. Ogni unità ha 4 controlli C/NC (punti 02, 03, 04, 05)
    // Usiamo un Array di 4 elementi per gestire gli indici da 0 a 3 (mappati sui punti 02-05)
    private val statiUnita = mutableMapOf<String, Array<String>>()
    private val gradiMisurati = mutableMapOf<String, String>()

    // CONFIGURAZIONE GEOMETRICA REALE IN BASE ALLE TESTATE E AI LATI (MIL / CIV)
    // Ordine: da sinistra a destra come appaiono a schermo rispetto alla pista
    private val unitaTestata32Mil = listOf("1" to "2°25'", "2" to "2°45'", "3" to "3°15'", "4" to "3°35'")
    private val unitaTestata32Civ = listOf("14" to "3°35'", "13" to "3°15'", "12" to "2°45'", "11" to "2°25'")
    
    private val unitaTestata14Civ = listOf("1" to "2°30'", "2" to "2°50'", "3" to "3°10'", "4" to "3°30'")
    private val unitaTestata14Mil = listOf("14" to "3°30'", "13" to "3°10'", "12" to "2°50'", "11" to "2°30'")

    // Testi descrittivi per la guida pop-up (Long Click) associati ai punti 02, 03, 04, 05
    private val descrizioniAttivita = listOf(
        "02 - Controllo della transizione rosso-bianco di tutte e tre le lenti contemporaneamente",
        "03 - Ispezione interna e pulizia dell'unità e applicazione liquido antiappannante alle lenti",
        "04 - Controllo stato vegetazione",
        "05 - Verifica del buon serraggio dei fermi del coperchio di ogni unità"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_md121)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "PAPI Settimanale"

        // Inizializzazione in memoria a 4 pulsanti per ciascuna delle 16 unità
        listOf("14", "32").forEach { testata ->
            listOf("1", "2", "3", "4", "11", "12", "13", "14").forEach { unita ->
                val chiave = "${testata}_${unita}"
                statiUnita[chiave] = Array(4) { "C" } // 4 elementi per i punti 02, 03, 04, 05
                gradiMisurati[chiave] = ""
            }
        }

        val editData = findViewById<EditText>(R.id.edit_data_papi)
        val editOra = findViewById<EditText>(R.id.edit_ora_papi)
        val editResponsabile = findViewById<EditText>(R.id.edit_responsabile_papi)

        val cal = Calendar.getInstance()
        editData.setText(SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(cal.time))
        editOra.setText(SimpleDateFormat("HH:mm", Locale.ITALY).format(cal.time))
        editResponsabile.setText(getString(R.string.user_name))
        editResponsabile.isEnabled = false // Rendi il nome non modificabile

        aggiornaSchermataDinamica()

        findViewById<RadioGroup>(R.id.rg_testata).setOnCheckedChangeListener { _, _ ->
            currentFocus?.clearFocus()
            aggiornaSchermataDinamica()
        }

        findViewById<RadioGroup>(R.id.rg_lato).setOnCheckedChangeListener { _, _ ->
            currentFocus?.clearFocus()
            aggiornaSchermataDinamica()
        }

        findViewById<Button>(R.id.btn_generate_md121).setOnClickListener {
            generaPdf()
        }
    }

    private fun aggiornaSchermataDinamica() {
        val isTestata32 = findViewById<RadioGroup>(R.id.rg_testata).checkedRadioButtonId == R.id.rb_t32
        val isLatoCiv = findViewById<RadioGroup>(R.id.rg_lato).checkedRadioButtonId == R.id.rb_lato_dx
        
        val testataAttiva = if (isTestata32) "32" else "14"
        findViewById<TextView>(R.id.txt_pista_label).text = testataAttiva

        val pistaASinistra = if (isTestata32) isLatoCiv else !isLatoCiv

        val layoutPrincipale = findViewById<LinearLayout>(R.id.layout_griglia_dinamica)
        val containerLuci = findViewById<LinearLayout>(R.id.container_quattro_colonne)
        val containerPista = findViewById<LinearLayout>(R.id.container_pista_mobile)

        layoutPrincipale.removeAllViews()
        if (pistaASinistra) {
            layoutPrincipale.addView(containerPista)
            layoutPrincipale.addView(containerLuci)
        } else {
            layoutPrincipale.addView(containerLuci)
            layoutPrincipale.addView(containerPista)
        }

        val txtAereo = containerPista.findViewById<TextView>(R.id.txt_pista_label)?.parent?.let { parentView ->
            if (parentView is LinearLayout && parentView.childCount > 3) {
                parentView.getChildAt(3) as? TextView
            } else null
        }
        if (isTestata32) {
            txtAereo?.rotation = -90f
        } else {
            txtAereo?.rotation = 270f
        }

        val listaUnitaAttuale = when {
            isTestata32 && !isLatoCiv -> unitaTestata32Mil // MIL sulla 32: 1-4 (4 vicino pista)
            isTestata32 && isLatoCiv -> unitaTestata32Civ  // CIV sulla 32: 14-11 (14 vicino pista)
            !isTestata32 && isLatoCiv -> unitaTestata14Civ // CIV sulla 14: 1-4 (4 vicino pista)
            else -> unitaTestata14Mil // MIL sulla 14: 14-11 (14 vicino pista)
        }

        val colIds = listOf(R.id.col_papi_1, R.id.col_papi_2, R.id.col_papi_3, R.id.col_papi_4)

        listaUnitaAttuale.forEachIndexed { index, pair ->
            val unitName = pair.first
            val angoloNominale = pair.second
            val chiaveMemoria = "${testataAttiva}_${unitName}"

            val colContainer = findViewById<LinearLayout>(colIds[index])
            colContainer.removeAllViews()

            // 1. Numero Unità
            colContainer.addView(TextView(this).apply {
                text = unitName
                textSize = 15f
                paint.isFakeBoldText = true
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#1b5e20"))
            })

            // 2. Angolo Nominale
            colContainer.addView(TextView(this).apply {
                text = angoloNominale
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
            })

            // 3. Campo inserimento gradi strumento (Punto 01)
            val editGradi = EditText(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(2, 4, 2, 4)
                }
                hint = "" // Rimossa la parola "Grado" lasciandola vuota o pulita
                textSize = 12f
                gravity = Gravity.CENTER
                // Impostiamo la tastiera numerica ma permettiamo l'inserimento dei nostri simboli speciali
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                keyListener = DigitsKeyListener.getInstance("0123456789.°', ")
                setTextColor(Color.BLACK) // Forza colore del testo nero ben visibile
                setHintTextColor(Color.GRAY) // Colore dell'indizio in grigio
                setBackgroundResource(android.R.drawable.edit_text)
                setText(gradiMisurati[chiaveMemoria])
                
                // Formattazione automatica intelligente: 2.25 -> 2° 25'
                addTextChangedListener(object : TextWatcher {
                    private var isUpdating = false
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (isUpdating) return
                        val original = s.toString()
                        
                        // Sostituiamo il punto o la virgola con il simbolo dei gradi (CON SPAZIO)
                        if (original.contains(".") || original.contains(",")) {
                            isUpdating = true
                            val formatted = original.replace(".", "° ").replace(",", "° ")
                            s?.replace(0, s.length, formatted)
                            isUpdating = false
                        }
                        
                        // Se ci sono 2 numeri dopo il simbolo dei gradi, aggiungiamo i primi '
                        val current = s.toString()
                        if (current.contains("° ")) {
                            val parts = current.split("° ")
                            if (parts.size > 1) {
                                val minutes = parts[1].replace("'", "")
                                if (minutes.length == 2 && !current.endsWith("'")) {
                                    isUpdating = true
                                    s?.append("'")
                                    isUpdating = false
                                }
                            }
                        }
                        gradiMisurati[chiaveMemoria] = s.toString()
                    }
                })

                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        gradiMisurati[chiaveMemoria] = text.toString()
                    }
                }
            }
            colContainer.addView(editGradi)

            // 4. I 4 bottoni verticali (Punti 02, 03, 04, 05)
            val arrayStati = statiUnita[chiaveMemoria]!!
            for (i in 0 until 4) {
                val numeroAttivita = "0${i + 2}" // Genera i testi delle etichette "02", "03", "04", "05"
                val btn = Button(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        80
                    ).apply {
                        setMargins(2, 4, 2, 4)
                    }
                    setPadding(0, 0, 0, 0)
                    // Il testo mostra sia il numero dell'attività che lo stato C/NC in piccolo o solo lo stato
                    text = "$numeroAttivita: ${arrayStati[i]}"
                    textSize = 10f
                    updateButtonStyle(this, arrayStati[i])

                    setOnClickListener {
                        arrayStati[i] = if (arrayStati[i] == "C") "NC" else "C"
                        text = "$numeroAttivita: ${arrayStati[i]}"
                        updateButtonStyle(this, arrayStati[i])
                    }

                    // GUIDA INFORMATIVA POP-UP AL TOCCO PROLUNGATO (Long Click)
                    setOnLongClickListener {
                        Toast.makeText(this@Md121Activity, descrizioniAttivita[i], Toast.LENGTH_LONG).show()
                        true // Consuma l'evento evitando il click standard
                    }
                }
                colContainer.addView(btn)
            }
        }
    }

    private fun updateButtonStyle(button: Button, state: String) {
        if (state == "C") {
            button.setBackgroundColor(Color.parseColor("#4CAF50"))
            button.setTextColor(Color.WHITE)
        } else {
            button.setBackgroundColor(Color.parseColor("#F44336"))
            button.setTextColor(Color.WHITE)
        }
    }

    private fun generaPdf() {
        currentFocus?.clearFocus()
        val data = findViewById<EditText>(R.id.edit_data_papi).text.toString()
        val ora = findViewById<EditText>(R.id.edit_ora_papi).text.toString()
        val responsabile = findViewById<EditText>(R.id.edit_responsabile_papi).text.toString()

        val mappaPdf = mutableMapOf<String, String>()
        
        // Mappatura Intestazione (confermata dall'utente)
        val dataParts = data.split("/")
        if (dataParts.size == 3) {
            mappaPdf["Angolo nominale di pendenza di avvicinamento 3 00 Altezza minima occhi pilota  soglia H min 16 m  ILS.0"] = dataParts[0] // Giorno
            mappaPdf["undefined.0"] = dataParts[1] // Mese
            mappaPdf["undefined_2"] = dataParts[2] // Anno
        }
        
        val oraParts = ora.split(":")
        if (oraParts.size == 2) {
            mappaPdf["Angolo nominale di pendenza di avvicinamento 3 00 Altezza minima occhi pilota  soglia H min 16 m  ILS.1"] = oraParts[0] // Ore
            mappaPdf["undefined.1"] = oraParts[1] // Minuti
        }
        mappaPdf["Responsabile verifica"] = responsabile

        // MAPPATURA UNITA' -> INDICE PDF (Fissa per entrambe le testate 14 e 32)
        val unitToPdfIdx = mapOf(
            "1" to 0, "2" to 1, "3" to 2, "4" to 3,
            "14" to 4, "13" to 5, "12" to 6, "11" to 7
        )

        // Ciclo su ENTRAMBE le testate (32 e 14) per compilare l'intero PDF
        listOf("32" to "0", "14" to "1").forEach { (tAttiva, tIdx) ->
            
            unitToPdfIdx.forEach { (unitName, pdfIdx) ->
                val chiaveMem = "${tAttiva}_$unitName"
                val valoreGrado = gradiMisurati[chiaveMem] ?: ""
                
                // 1. Scrittura Gradi: Trasformiamo "2° 25'" in "2º25'" (rimossi tutti gli spazi per precisione)
                val fieldGrado = "1.$pdfIdx.$tIdx"
                val pulito = valoreGrado.replace(" ", "").replace("°", "º").trim()
                mappaPdf[fieldGrado] = pulito
                
                // 2. Scrittura Check Box (Punti 02, 03, 04, 05)
                // Logica: Check Box7.<RigaAct>.<UnitSideIdx*2 + State>.<SideIdx>.<TestataIdx>
                val stati = statiUnita[chiaveMem] ?: Array(4) { "C" }
                val sideIdx = if (unitName.toInt() <= 4) "0" else "1" // MIL=0, CIV=1
                
                val unitIdxInSide = when(unitName) {
                    "1", "14" -> 0
                    "2", "13" -> 1
                    "3", "12" -> 2
                    "4", "11" -> 3
                    else -> 0
                }

                stati.forEachIndexed { actIdx, stato ->
                    val colOffset = if (stato == "C") 0 else 1
                    val colCombined = unitIdxInSide * 2 + colOffset
                    val fieldCB = "Check Box7.$actIdx.$colCombined.$sideIdx.$tIdx"
                    mappaPdf[fieldCB] = "Sì"
                }
            }
        }

        val resId = resources.getIdentifier("md121_template_name", "string", packageName)
        if (resId == 0) return
        val templateName = getString(resId)
        
        val stamper = Md121PdfStamper(this, templateName)

        // Formattazione nome file: aaaammgg MD-121 Scheda verifica periodica PAPI.pdf
        val dataPerNome = if (dataParts.size == 3) {
            "${dataParts[2]}${dataParts[1]}${dataParts[0]}" // aaaammgg
        } else {
            data.replace("/", "")
        }
        val nomeFile = "$dataPerNome MD-121 Scheda verifica periodica PAPI.pdf"

        lifecycleScope.launch(Dispatchers.IO) {
            val uri = stamper.generaPdfCompilato(nomeFile, mappaPdf)
            withContext(Dispatchers.Main) {
                if (uri != null) {
                    Toast.makeText(this@Md121Activity, "PDF Generato: $nomeFile", Toast.LENGTH_LONG).show()
                    apriPdf(uri)
                } else {
                    Toast.makeText(this@Md121Activity, "Errore nella generazione del PDF", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Nessuna app per aprire PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
