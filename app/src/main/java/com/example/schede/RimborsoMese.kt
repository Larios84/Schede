package com.example.schede

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "rimborsi_mese",
    foreignKeys = [
        ForeignKey(
            entity = Auto::class,
            parentColumns = ["id"],
            childColumns = ["autoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RimborsoMese(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mese: Int,
    val anno: Int,
    val autoId: Int
) : Serializable
