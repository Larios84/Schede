package com.example.schede

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest

class AttivazioneActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attivazione)

        val inputId = findViewById<EditText>(R.id.input_id_collega)
        val inputChiave = findViewById<EditText>(R.id.input_chiave_segreta)
        val btnGenera = findViewById<Button>(R.id.btn_genera_codice)
        val resultLayout = findViewById<LinearLayout>(R.id.result_layout)
        val textCodice = findViewById<TextView>(R.id.text_codice_generato)
        val btnCopia = findViewById<Button>(R.id.btn_copia_codice)
        val btnIndietro = findViewById<Button>(R.id.btn_indietro)

        btnGenera.setOnClickListener {
            val id = inputId.text.toString().trim()
            val chiave = inputChiave.text.toString().trim()

            if (id.isEmpty()) {
                Toast.makeText(this, "Inserisci l'ID!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (chiave.isEmpty()) {
                Toast.makeText(this, "Inserisci la chiave segreta!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Calcola il codice usando la chiave inserita al momento
            val codice = calcolaCodice(id, chiave)
            textCodice.text = codice
            resultLayout.visibility = View.VISIBLE
        }

        btnCopia.setOnClickListener {
            val codice = textCodice.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Codice Attivazione", codice)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Codice copiato!", Toast.LENGTH_SHORT).show()
        }

        btnIndietro.setOnClickListener {
            finish()
        }
    }

    private fun calcolaCodice(deviceId: String, secretKey: String): String {
        val input = deviceId + secretKey
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(8).uppercase()
    }
}
