package com.example.schede

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class Md121Adapter(private val items: List<Md121Row>) : RecyclerView.Adapter<Md121Adapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtUnitName: TextView = view.findViewById(R.id.txt_unit_name)
        val txtAngoloNominale: TextView = view.findViewById(R.id.txt_angolo_nominale)
        val buttons = listOf<Button>(
            view.findViewById(R.id.btn_c_01),
            view.findViewById(R.id.btn_c_02),
            view.findViewById(R.id.btn_c_03),
            view.findViewById(R.id.btn_c_04),
            view.findViewById(R.id.btn_c_05)
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_md121_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtUnitName.text = "Unità ${item.unitName}"
        holder.txtAngoloNominale.text = "(${item.angoloNominale})"

        val states = mutableListOf(item.check01, item.check02, item.check03, item.check04, item.check05)

        holder.buttons.forEachIndexed { index, button ->
            updateButtonStyle(button, states[index])
            button.setOnClickListener {
                states[index] = if (states[index] == "C") "NC" else "C"
                // Aggiorna il modello
                when(index) {
                    0 -> item.check01 = states[index]
                    1 -> item.check02 = states[index]
                    2 -> item.check03 = states[index]
                    3 -> item.check04 = states[index]
                    4 -> item.check05 = states[index]
                }
                updateButtonStyle(button, states[index])
            }
        }
    }

    private fun updateButtonStyle(button: Button, state: String) {
        button.text = state
        if (state == "C") {
            button.setBackgroundColor(Color.parseColor("#4CAF50")) // Verde
            button.setTextColor(Color.WHITE)
        } else {
            button.setBackgroundColor(Color.parseColor("#F44336")) // Rosso
            button.setTextColor(Color.WHITE)
        }
    }

    override fun getItemCount() = items.size
}
