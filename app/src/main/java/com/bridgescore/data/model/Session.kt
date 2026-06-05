package com.bridgescore.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MovementType { HOWELL, MITCHELL }

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val partner: String,
    val pairNumber: Int,
    val movementType: MovementType,
    val numberOfTables: Int,
    val boardCount: Int = 28,
    val boardsPerRound: Int = 4
)
