package com.example.schede

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class GeminiResponse(
    val reparto: String? = null,
    val turno: String? = null,
    val data: String? = null,
    val attivita: List<GeminiActivity>? = null
)

data class GeminiActivity(
    val descrizione: String? = null,
    val ore: Any? = null
)

class GeminiAssistant(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analizzaTesto(input: String): GeminiResponse? = withContext(Dispatchers.IO) {
        try {
            val listUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            val listResponse = client.newCall(Request.Builder().url(listUrl).get().build()).execute()
            val listBody = listResponse.body?.string() ?: ""
            val modelsRoot = JSONObject(listBody)
            val modelsArray = modelsRoot.getJSONArray("models")
            var selectedModel = "models/gemini-1.5-flash"
            for (i in 0 until modelsArray.length()) {
                val m = modelsArray.getJSONObject(i).getString("name")
                if ((m.contains("flash") || m.contains("pro")) && !m.contains("vision")) {
                    selectedModel = m; break
                }
            }

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
            val oggi = sdf.format(Date())

            val prompt = """
                Analizza questo testo e restituisci SOLO un JSON.
                Testo: "$input"
                Data di oggi: $oggi
                I reparti: AVL, BHS, IMP.TECNOLOGICI, IMP.ELETTRICI, IMP.SPECIALI, MAN.GEN, STP VERDE.
                I turni: 5.48, 6, 6.48, 7, 7.48, 13, 14, 14.48, 22.
                Usa formato GG/MM/AAAA per la data. "ieri" = data di ieri rispetto a $oggi.
                Struttura JSON: { "reparto": "STRING", "turno": "STRING", "data": "STRING", "attivita": [ { "descrizione": "STRING", "ore": "STRING" } ] }
                Importante: Se non trovi un dato metti null. Restituisci SOLO il JSON senza ```json.
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                put("generationConfig", JSONObject().put("response_mime_type", "application/json"))
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/$selectedModel:generateContent?key=$apiKey"
            val response = client.newCall(Request.Builder().url(url).post(requestBodyJson.toString().toRequestBody("application/json".toMediaType())).build()).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            
            val root = JSONObject(responseBody)
            var resultText = root.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim()

            if (resultText.contains("```")) {
                resultText = resultText.substringAfter("{").substringBeforeLast("}")
                resultText = "{$resultText}"
            }

            Gson().fromJson(resultText, GeminiResponse::class.java)
        } catch (e: Exception) {
            Log.e("GeminiAssistant", "Errore: ${e.message}")
            null
        }
    }
}
