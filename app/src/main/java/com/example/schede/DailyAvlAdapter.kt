package com.example.schede

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DailyAvlAdapter(private val items: List<DailyAvlRow>) : RecyclerView.Adapter<DailyAvlAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDescrizione: TextView = view.findViewById(R.id.tv_descrizione_attivita)
        val rgStato: RadioGroup = view.findViewById(R.id.rg_stato)
        val rbC: RadioButton = view.findViewById(R.id.rb_c)
        val rbNc: RadioButton = view.findViewById(R.id.rb_nc)
        val rbNa: RadioButton = view.findViewById(R.id.rb_na)
        val etNote: EditText = view.findViewById(R.id.et_note_riga)
        
        var noteWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_avl_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvDescrizione.text = "${item.numero}. ${item.descrizione}"
        
        // Reset listeners
        holder.rgStato.setOnCheckedChangeListener(null)
        holder.noteWatcher?.let { holder.etNote.removeTextChangedListener(it) }

        // Set values
        when (item.stato) {
            "C" -> holder.rbC.isChecked = true
            "NC" -> holder.rbNc.isChecked = true
            "NA" -> holder.rbNa.isChecked = true
            else -> holder.rgStato.clearCheck()
        }
        holder.etNote.setText(item.note)

        // Listeners
        holder.rgStato.setOnCheckedChangeListener { _, checkedId ->
            item.stato = when (checkedId) {
                R.id.rb_c -> "C"
                R.id.rb_nc -> "NC"
                R.id.rb_na -> "NA"
                else -> ""
            }
        }

        holder.noteWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                item.note = s.toString()
            }
        }
        holder.etNote.addTextChangedListener(holder.noteWatcher)
    }

    override fun getItemCount() = items.size
}
