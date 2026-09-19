package com.example.schede

data class UrccRow(
    val circuito: String,
    var carico: String = "",
    var ampere: String = "",
    var volt: String = "",
    var isolamento: String = "",
    var unitaIsolamento: String = "M ohm"
)
