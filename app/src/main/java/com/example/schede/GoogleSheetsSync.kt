package com.example.schede

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class GoogleSheetsSync {

    // URL fornito dopo la pubblicazione dello Script di Google
    private val scriptUrl = "https://script.google.com/macros/s/AKfycbwa5cZnejXKZ_cV0CtyyXLiAUlIdiD9_U2NeSdmXH6K7WCBt8mkBLslY6flH1SXlbz5hA/exec"

    private val client = OkHttpClient()

    // Helper per gestire turno come stringa o numero
    private fun getTurnoAsString(obj: JSONObject): String {
        return try {
            when (val turno = obj.get("turno")) {
                is String -> turno
                is Number -> turno.toString()
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun sendIntervento(intervento: Intervento, onResult: ((Boolean, String?) -> Unit)? = null) {
        val json = JSONObject().apply {
            put("data", intervento.data)
            put("nome", intervento.nome)
            put("reparto", intervento.reparto)
            put("turno", intervento.turno)
            put("oreTotali", intervento.oreTotali)
            put("straordinario", intervento.straordinario)
            put("attivita", intervento.attivitaJson)
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(scriptUrl)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Sync", "Errore invio dati: ${e.message}")
                onResult?.invoke(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Log.d("Sync", "Dati inviati con successo: $responseBody")
                    onResult?.invoke(true, null)
                } else {
                    Log.e("Sync", "Errore server: ${response.code}")
                    onResult?.invoke(false, "Errore server: ${response.code}")
                }
            }
        })
    }

    fun readInterventi(onResult: (List<Intervento>?, String?) -> Unit) {
        val url = "$scriptUrl?action=read&t=${System.currentTimeMillis()}"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Sync", "Errore lettura dati: ${e.message}")
                onResult(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val jsonArray = JSONArray(responseBody)
                        val interventi = mutableListOf<Intervento>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val intervento = Intervento(
                                data = obj.optString("data", ""),
                                nome = obj.optString("nome", ""),
                                reparto = obj.optString("reparto", ""),
                                turno = getTurnoAsString(obj),
                                oreTotali = obj.optDouble("oreTotali", 0.0),
                                straordinario = obj.optDouble("straordinario", 0.0),
                                attivitaJson = obj.optString("attivitaJson", obj.optString("attivita", "[]"))
                            )
                            interventi.add(intervento)
                        }

                        Log.d("Sync", "Letti ${interventi.size} interventi")
                        onResult(interventi, null)
                    } else {
                        Log.e("Sync", "Errore server: ${response.code}")
                        onResult(null, "Errore server: ${response.code}")
                    }
                } catch (e: Exception) {
                    Log.e("Sync", "Errore parsing JSON: ${e.message}")
                    onResult(null, e.message)
                }
            }
        })
    }
}
