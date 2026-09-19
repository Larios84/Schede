package com.example.schede

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SchedeAvlActivity : AppCompatActivity() {

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
            text = "Moduli AVL"
            textSize = 24f
            setTextColor(textColor)
            setPadding(0, 0, 0, 50)
            gravity = Gravity.CENTER
        })

        // Lista schede per AVL
        val schede = listOf("URCC settimanale")

        schede.forEach { nomeScheda ->
            val btn = Button(this).apply {
                text = nomeScheda
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 10, 0, 10)
                }
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(accentColor)
                setOnClickListener {
                    if (nomeScheda == "URCC settimanale") {
                        val intent = Intent(this@SchedeAvlActivity, UrccActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@SchedeAvlActivity, "Apertura $nomeScheda...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            mainLayout.addView(btn)
        }

        setContentView(mainLayout)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Schede AVL"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
