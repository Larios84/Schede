package com.example.schede

data class DailyAvlRow(
    val numero: String,
    val descrizione: String,
    var stato: String = "", // C, NC, NA
    var note: String = ""
)
