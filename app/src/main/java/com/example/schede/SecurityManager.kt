package com.example.schede

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object SecurityManager {
    // Questa è la tua "chiave segreta". Non comunicarla a nessuno.
    // Se la cambi, tutti i vecchi codici di attivazione smetteranno di funzionare.
    private const val SECRET_KEY = "Luca Garau"

    // Ottiene l'ID univoco del dispositivo
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "0000"
    }

    // Algoritmo per generare il codice di attivazione
    fun generateActivationCode(deviceId: String): String {
        val input = deviceId + SECRET_KEY
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        // Prendiamo i primi 8 caratteri dell'hash MD5 in maiuscolo
        return bytes.joinToString("") { "%02x".format(it) }.take(8).uppercase()
    }

    // Verifica se l'app è già stata attivata su questo dispositivo
    fun isActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        val savedCode = prefs.getString("activation_code", null) ?: return false
        val expectedCode = generateActivationCode(getDeviceId(context))
        return savedCode == expectedCode
    }

    // Salva il codice di attivazione nelle impostazioni dell'app
    fun saveActivationCode(context: Context, code: String) {
        val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("activation_code", code).apply()
    }
}
