package com.example.schede

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class UrccAdapter(val items: List<UrccRow>) : RecyclerView.Adapter<UrccAdapter.ViewHolder>() {

    private val valoriAmpere = arrayOf("2.8", "3.4", "4.1", "5.2", "6.6")

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCircuito: TextView = view.findViewById(R.id.tv_circuito)
        val etCarico: EditText = view.findViewById(R.id.et_carico)
        val etAmpere: EditText = view.findViewById(R.id.et_ampere)
        val etVolt: EditText = view.findViewById(R.id.et_volt)
        val etIsolamento: EditText = view.findViewById(R.id.et_isolamento)
        val tvUnitaIso: TextView = view.findViewById(R.id.tv_unita_iso)

        var caricoWatcher: TextWatcher? = null
        var ampereWatcher: TextWatcher? = null
        var voltWatcher: TextWatcher? = null
        var isolamentoWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_urcc_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvCircuito.text = item.circuito
        
        // Rimuovo i vecchi listener per evitare sovrascritture durante il riciclo
        holder.caricoWatcher?.let { holder.etCarico.removeTextChangedListener(it) }
        holder.ampereWatcher?.let { holder.etAmpere.removeTextChangedListener(it) }
        holder.voltWatcher?.let { holder.etVolt.removeTextChangedListener(it) }
        holder.isolamentoWatcher?.let { holder.etIsolamento.removeTextChangedListener(it) }

        holder.etCarico.setText(item.carico)
        holder.etAmpere.setText(item.ampere)
        holder.etVolt.setText(item.volt)
        holder.etIsolamento.setText(item.isolamento)
        holder.tvUnitaIso.text = if (item.unitaIsolamento == "M ohm") "" else item.unitaIsolamento

        // Listener per eliminare "/" al click
        val clearBarraListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus && v is EditText && v.text.toString() == "/") {
                v.setText("")
            }
        }
        holder.etCarico.onFocusChangeListener = clearBarraListener
        holder.etVolt.onFocusChangeListener = clearBarraListener
        holder.etIsolamento.onFocusChangeListener = clearBarraListener

        // Creo i nuovi watcher che puntano all'oggetto riga corrente
        holder.caricoWatcher = createWatcher { item.carico = it }
        holder.ampereWatcher = createWatcher { item.ampere = it }
        holder.voltWatcher = createWatcher { item.volt = it }
        holder.isolamentoWatcher = createWatcher { item.isolamento = it }

        holder.etCarico.addTextChangedListener(holder.caricoWatcher)
        holder.etAmpere.addTextChangedListener(holder.ampereWatcher)
        holder.etVolt.addTextChangedListener(holder.voltWatcher)
        holder.etIsolamento.addTextChangedListener(holder.isolamentoWatcher)

        val unitaDisponibili = arrayOf("M ohm", "K ohm", "ohm")
        holder.tvUnitaIso.setOnClickListener {
            AlertDialog.Builder(it.context)
                .setTitle("Seleziona Unità")
                .setItems(unitaDisponibili) { _, which ->
                    val scelta = unitaDisponibili[which]
                    item.unitaIsolamento = scelta
                    holder.tvUnitaIso.text = if (scelta == "M ohm") "" else scelta
                }
                .show()
        }

        holder.etAmpere.isFocusable = false
        holder.etAmpere.setOnClickListener {
            if (item.ampere == "/") {
                item.ampere = ""
                holder.etAmpere.setText("")
            }
            AlertDialog.Builder(it.context)
                .setTitle("Seleziona Ampere (Gruppo)")
                .setItems(valoriAmpere) { _, which ->
                    val scelto = valoriAmpere[which]
                    val gruppoCorrente = trovaGruppo(item.circuito)
                    
                    if (gruppoCorrente.isEmpty()) {
                        item.ampere = scelto
                        holder.etAmpere.setText(scelto)
                    } else {
                        // Aggiorna tutti i circuiti dello stesso gruppo
                        items.forEachIndexed { index, row ->
                            if (trovaGruppo(row.circuito) == gruppoCorrente) {
                                row.ampere = scelto
                                notifyItemChanged(index)
                            }
                        }
                    }
                }
                .show()
        }

        holder.etAmpere.setOnLongClickListener {
            AlertDialog.Builder(it.context)
                .setTitle("Seleziona Ampere (Singola casella)")
                .setItems(valoriAmpere) { _, which ->
                    val scelto = valoriAmpere[which]
                    item.ampere = scelto
                    holder.etAmpere.setText(scelto)
                }
                .show()
            true
        }
    }

    private fun trovaGruppo(nome: String): String {
        return when {
            nome.startsWith("A") && !nome.startsWith("AP") -> "A"
            nome.startsWith("AP") -> "AP"
            nome.startsWith("BP") -> "BP"
            nome.startsWith("SP") -> "SP"
            nome.startsWith("PAPI") -> "PAPI"
            nome.startsWith("SB") -> "SB"
            nome.startsWith("LD") -> "LD"
            nome.startsWith("VR") -> "VR"
            nome.startsWith("TA") || nome.startsWith("TAMID") -> "TA"
            nome.startsWith("RGL") -> "RGL"
            else -> ""
        }
    }

    private fun createWatcher(onChanged: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { onChanged(s.toString()) }
    }

    override fun getItemCount() = items.size
}
