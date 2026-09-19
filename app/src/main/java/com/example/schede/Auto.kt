package com.example.schede

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "auto")
data class Auto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val modello: String,
    val targa: String,
    val annoImmatricolazione: String = "",
    val combustibile: String = ""
) : Serializable
