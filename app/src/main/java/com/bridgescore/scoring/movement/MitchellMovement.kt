package com.bridgescore.scoring.movement

import com.bridgescore.scoring.movement.PairSchedule
import com.bridgescore.scoring.movement.RoundInfo

/**
 * Mitchell movement: NS pairs stay fixed, EW pairs and boards move.
 * Standard movement for even number of tables.
 * EW pairs move up one table each round; boards move down one table each round.
 */
object MitchellMovement {

    /**
     * Generates movement for [tables] tables, [boardsPerRound] boards per round.
     * NS pairs are numbered 1..tables, EW pairs are numbered tables+1..2*tables.
     */
    fun getMovement(tables: Int, boardsPerRound: Int): List<RoundInfo> {
        val rounds = tables   // full Mitchell = one round per table
        val result = mutableListOf<RoundInfo>()
        for (round in 1..rounds) {
            for (table in 1..tables) {
                val nsPair = table
                // EW pair starts at table+tables, moves up each round
                val ewPairOffset = ((table + round - 2) % tables)
                val ewPair = ewPairOffset + tables + 1
                // Boards move down each round
                val boardSetStart = ((table - round).mod(tables)) * boardsPerRound + 1
                val boards = (boardSetStart until boardSetStart + boardsPerRound).toList()
                result.add(RoundInfo(round, table, nsPair, ewPair, boards))
            }
        }
        return result
    }

    fun boardCountForMovement(tables: Int, boardsPerRound: Int): Int = tables * boardsPerRound

    fun getScheduleForPair(tables: Int, boardsPerRound: Int, pairNumber: Int): List<PairSchedule> {
        val movement = getMovement(tables, boardsPerRound)
        return movement
            .filter { it.nsPair == pairNumber || it.ewPair == pairNumber }
            .sortedBy { it.round }
            .map { round ->
                if (round.nsPair == pairNumber)
                    PairSchedule(round.round, round.table, round.ewPair, round.boards, sitNS = true)
                else
                    PairSchedule(round.round, round.table, round.nsPair, round.boards, sitNS = false)
            }
    }
}
