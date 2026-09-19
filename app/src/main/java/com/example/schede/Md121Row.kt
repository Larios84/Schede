package com.example.schede

data class Md121Row(
    val unitName: String,
    val angoloNominale: String,
    var check01: String = "C", // C = Conforme, NC = Non Conforme
    var check02: String = "C",
    var check03: String = "C",
    var check04: String = "C",
    var check05: String = "C"
)
