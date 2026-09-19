package com.example.schede

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "viaggi",
    foreignKeys = [
        ForeignKey(
            entity = RimborsoMese::class,
            parentColumns = ["id"],
            childColumns = ["rimborsoMeseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Viaggio(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rimborsoMeseId: Int,
    val giorno: Int,
    val causale: String,
    val km: Double,
    val partenza: String = "Sestu",
    val destinazione: String = "Aeroporto"
) : Serializable
