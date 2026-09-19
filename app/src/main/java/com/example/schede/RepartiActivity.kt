package com.example.schede

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class RepartiActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backgroundColor = ContextCompat.getColor(this, R.color.main_background)
        val textColor = ContextCompat.getColor(this, R.color.main_text)
        val accentColor = ContextCompat.getColor(this, R.color.purple_500)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(50, 50, 50, 50)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        mainLayout.addView(TextView(this).apply {
            text = "Seleziona Reparto"
            textSize = 24f
            setTextColor(textColor)
            setPadding(0, 0, 0, 50)
            gravity = Gravity.CENTER
        })

        // Per ora aggiungiamo i reparti e la gestione KM
        val opzioni = listOf("AVL", "RIMBORSO KM")

        opzioni.forEach { opzione ->
            val btn = Button(this).apply {
                text = opzione
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 10, 0, 10)
                }
                setTextColor(Color.WHITE)
                setBackgroundColor(accentColor)
                setOnClickListener {
                    when (opzione) {
                        "AVL" -> {
                            val intent = Intent(this@RepartiActivity, SchedeAvlActivity::class.java)
                            startActivity(intent)
                        }
                        "RIMBORSO KM" -> {
                            val intent = Intent(this@RepartiActivity, KmActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
            }
            mainLayout.addView(btn)
        }

        setContentView(mainLayout)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Reparti"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
