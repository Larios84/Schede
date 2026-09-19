package com.example.schede

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import java.util.*
import kotlin.concurrent.thread

class KmActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var selectedAuto: Auto? = null
    private var listaViaggi = mutableListOf<Viaggio>()
    
    private lateinit var spinnerAuto: Spinner
    private lateinit var containerViaggi: LinearLayout
    private lateinit var txtMeseAnno: TextView

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            saveFolderUri(it)
            generaExcel(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getDatabase(this)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(ContextCompat.getColor(this@KmActivity, R.color.main_background))
        }

        // --- SEZIONE AUTO ---
        mainLayout.addView(TextView(this).apply { text = "Seleziona Auto:"; setTextColor(ContextCompat.getColor(context, R.color.main_text)) })
        spinnerAuto = Spinner(this).apply { 
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120)
        }
        mainLayout.addView(spinnerAuto)

        val btnAddAuto = Button(this).apply { text = "AGGIUNGI AUTO" }
        btnAddAuto.setOnClickListener { mostraDialogAggiungiAuto() }
        mainLayout.addView(btnAddAuto)

        // --- DATA ---
        txtMeseAnno = TextView(this).apply { 
            val cal = Calendar.getInstance()
            text = "Periodo: ${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}"
            textSize = 18f
            setPadding(0, 32, 0, 32)
            setTextColor(ContextCompat.getColor(context, R.color.main_text))
        }
        mainLayout.addView(txtMeseAnno)

        // --- LISTA VIAGGI ---
        mainLayout.addView(TextView(this).apply { text = "Viaggi:"; setTextColor(ContextCompat.getColor(context, R.color.main_text)) })
        val scroll = ScrollView(this).apply { 
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        containerViaggi = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(containerViaggi)
        mainLayout.addView(scroll)

        val btnAddViaggio = Button(this).apply { text = "AGGIUNGI VIAGGIO" }
        btnAddViaggio.setOnClickListener { mostraDialogAggiungiViaggio() }
        mainLayout.addView(btnAddViaggio)

        val btnGenera = Button(this).apply { 
            text = "GENERA EXCEL"
            setBackgroundColor(ContextCompat.getColor(context, R.color.purple_500))
            setTextColor(android.graphics.Color.WHITE)
        }
        btnGenera.setOnClickListener { 
            val uri = loadFolderUri()
            if (uri == null) folderPickerLauncher.launch(null)
            else generaExcel(uri)
        }
        mainLayout.addView(btnGenera)

        val btnPdf = Button(this).apply {
            text = "GENERA PDF"
            setBackgroundColor(android.graphics.Color.RED)
            setTextColor(android.graphics.Color.WHITE)
        }
        btnPdf.setOnClickListener {
            val uri = loadFolderUri()
            if (uri == null) folderPickerLauncher.launch(null)
            else generaPdf(uri)
        }
        mainLayout.addView(btnPdf)

        setContentView(mainLayout)
        supportActionBar?.title = "Gestione KM"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        caricaAuto()
    }

    private fun caricaAuto() {
        thread {
            val autos = db.autoDao().getAll()
            runOnUiThread {
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, autos.map { "${it.modello} (${it.targa})" })
                spinnerAuto.adapter = adapter
                if (autos.isNotEmpty()) {
                    selectedAuto = autos[0]
                    spinnerAuto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                            selectedAuto = autos[p2]
                        }
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                    }
                }
            }
        }
    }

    private fun mostraDialogAggiungiAuto() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        val editModello = EditText(this).apply { hint = "Modello (es. Fiat Panda)" }
        val editTarga = EditText(this).apply { hint = "Targa" }
        val editAnno = EditText(this).apply { hint = "Anno Immatricolazione"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val editCombustibile = EditText(this).apply { hint = "Combustibile (es. Benzina)" }
        
        layout.addView(editModello)
        layout.addView(editTarga)
        layout.addView(editAnno)
        layout.addView(editCombustibile)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Nuova Auto")
            .setView(layout)
            .setPositiveButton("Salva") { _, _ ->
                val m = editModello.text.toString()
                val t = editTarga.text.toString()
                val a = editAnno.text.toString()
                val c = editCombustibile.text.toString()
                if (m.isNotEmpty() && t.isNotEmpty()) {
                    thread {
                        db.autoDao().insert(Auto(modello = m, targa = t, annoImmatricolazione = a, combustibile = c))
                        caricaAuto()
                    }
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun mostraDialogAggiungiViaggio() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        val editGg = EditText(this).apply { hint = "Giorno"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val editPartenza = EditText(this).apply { hint = "Partenza (es. Sestu)" }
        val editDestinazione = EditText(this).apply { hint = "Destinazione (es. Aeroporto)" }
        val editCausale = EditText(this).apply { hint = "Causale/Motivazione" }
        val editKm = EditText(this).apply { hint = "KM"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
        
        layout.addView(editGg)
        layout.addView(editPartenza)
        layout.addView(editDestinazione)
        layout.addView(editCausale)
        layout.addView(editKm)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Nuovo Viaggio")
            .setView(layout)
            .setPositiveButton("Aggiungi") { _, _ ->
                try {
                    val g = editGg.text.toString().toInt()
                    val p = editPartenza.text.toString().ifEmpty { "Sestu" }
                    val d = editDestinazione.text.toString().ifEmpty { "Aeroporto" }
                    val c = editCausale.text.toString()
                    val k = editKm.text.toString().toDouble()
                    val v = Viaggio(rimborsoMeseId = 0, giorno = g, causale = c, km = k, partenza = p, destinazione = d)
                    listaViaggi.add(v)
                    aggiornaListaViaggi()
                } catch (e: Exception) {
                    Toast.makeText(this, "Dati non validi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun aggiornaListaViaggi() {
        containerViaggi.removeAllViews()
        listaViaggi.sortBy { it.giorno }
        listaViaggi.forEach { v ->
            val tv = TextView(this).apply {
                text = "Giorno ${v.giorno}: ${v.km}km\n${v.partenza} - ${v.destinazione}\n(${v.causale})"
                setPadding(16, 24, 16, 24)
                setTextColor(ContextCompat.getColor(context, R.color.main_text))
                textSize = 14f
            }
            containerViaggi.addView(tv)
        }
    }

    private fun generaExcel(treeUri: Uri) {
        val auto = selectedAuto ?: return
        val cal = Calendar.getInstance()
        val mese = cal.get(Calendar.MONTH) + 1
        val anno = cal.get(Calendar.YEAR)
        
        thread {
            try {
                val templateName = getString(R.string.template_km_name)
                val inputStream = assets.open(templateName)
                
                val outputName = "RIMBORSO_KM_${mese}_${anno}_${auto.targa}.xlsx"
                val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, treeUri)
                val newFile = pickedDir?.createFile("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputName)
                
                newFile?.uri?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { out ->
                        val manager = KmExcelManager(this)
                        manager.compilaExcelKm(inputStream, out, auto, RimborsoMese(mese = mese, anno = anno, autoId = auto.id), listaViaggi, getString(R.string.user_name))
                    }
                    runOnUiThread { Toast.makeText(this, "Excel generato con successo!", Toast.LENGTH_LONG).show() }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun generaPdf(treeUri: Uri) {
        val auto = selectedAuto ?: return
        val cal = Calendar.getInstance()
        val mese = cal.get(Calendar.MONTH) + 1
        val anno = cal.get(Calendar.YEAR)
        
        thread {
            val manager = KmPdfManager(this)
            val file = manager.generaPdfRimborso(
                auto, 
                RimborsoMese(mese = mese, anno = anno, autoId = auto.id), 
                listaViaggi, 
                getString(R.string.user_name)
            )
            
            runOnUiThread {
                if (file != null) {
                    Toast.makeText(this, "PDF generato in Documents/Schede", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Errore durante la creazione del PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveFolderUri(uri: Uri) { getSharedPreferences("settings", MODE_PRIVATE).edit().putString("folder_uri_km", uri.toString()).apply() }
    private fun loadFolderUri(): Uri? { return getSharedPreferences("settings", MODE_PRIVATE).getString("folder_uri_km", null)?.let { Uri.parse(it) } }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
