package com.example.schede

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "interventi")
data class Intervento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val data: String,
    val nome: String,
    val reparto: String,
    val turno: String,
    val oreTotali: Double,
    val straordinario: Double,
    val attivitaJson: String
) : Serializable {
    @Ignore var isFromWeb: Boolean = false
}
