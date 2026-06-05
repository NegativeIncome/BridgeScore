package com.bridgescore.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Suit { CLUBS, DIAMONDS, HEARTS, SPADES, NOTRUMP }
enum class Doubled { NONE, DOUBLED, REDOUBLED }
enum class Vulnerability { NONE, NS, EW, BOTH }

@Entity(
    tableName = "board_results",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("sessionId"),
        Index(value = ["sessionId", "boardNumber"], unique = true)
    ]
)
data class BoardResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val boardNumber: Int,
    val opponentPairNumber: Int = 0,   // from movement table, 0 if unknown
    val declarer: String = "N",        // N, S, E, W
    val level: Int = 0,                // 1-7, 0 = not played / passed out
    val suit: Suit = Suit.NOTRUMP,
    val doubled: Doubled = Doubled.NONE,
    val tricksMade: Int = 0,           // actual tricks won by declarer (0-13)
    val score: Int = 0,                // computed duplicate score, positive = NS plus
    val passed: Boolean = false,       // true = passed out (0 score)
    val notPlayed: Boolean = false     // true = board not played (half-table situation)
)
