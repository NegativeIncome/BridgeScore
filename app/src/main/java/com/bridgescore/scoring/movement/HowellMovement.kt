package com.bridgescore.scoring.movement

/**
 * Howell movement tables for 3, 4, and 5 tables.
 *
 * Each entry = RoundInfo(round, table, nsPair, ewPair, boards)
 * "boards" is a set of board numbers played at that table in that round.
 *
 * These are the standard "Alternate Howell" movements commonly used in club duplicate.
 * Source: Standard ACBl/EBU movement cards.
 */
data class RoundInfo(
    val round: Int,
    val table: Int,
    val nsPair: Int,
    val ewPair: Int,
    val boards: List<Int>   // board numbers for this round/table
)

/**
 * For a given movement, returns the list of RoundInfo entries for a specific pair number.
 * This tells the pair: each round, who they play and what boards.
 */
data class PairSchedule(
    val round: Int,
    val table: Int,
    val opponentPair: Int,
    val boards: List<Int>,
    val sitNS: Boolean      // true = this pair sits NS this round
)

object HowellMovement {

    private val threeTablePairs = arrayOf(
        // Table 1: NS=6, EW cycles 1..5
        arrayOf(6 to 1, 6 to 2, 6 to 3, 6 to 4, 6 to 5),
        // Table 2
        arrayOf(3 to 4, 4 to 5, 5 to 1, 1 to 2, 2 to 3),
        // Table 3
        arrayOf(5 to 2, 1 to 3, 2 to 4, 3 to 5, 4 to 1)
    )

    private fun threeTable(bpr: Int): List<RoundInfo> = buildList {
        for (round in 1..5) {
            for (table in 1..3) {
                val (ns, ew) = threeTablePairs[table - 1][round - 1]
                val boards = if (round == 5) {
                    val start = 4 * bpr + 1
                    (start until start + bpr).toList()
                } else {
                    val setIndex = ((round - 1) + (table - 1)) % 4
                    val start = setIndex * bpr + 1
                    (start until start + bpr).toList()
                }
                add(RoundInfo(round, table, ns, ew, boards))
            }
        }
    }

    // -------------------------------------------------------------------------
    // 4-Table Howell: 8 pairs, 7 rounds, bpr boards per round
    // Pair 8 is stationary NS at Table 1.
    // Source: Baron Barclay Howell Movement Guide Card (4 tables, 8 pairs).
    // NS/EW pair assignments are fixed; only board numbers vary with bpr.
    // -------------------------------------------------------------------------
    private fun setBoards(setIndex: Int, bpr: Int): List<Int> {
        val start = (setIndex - 1) * bpr + 1
        return (start until start + bpr).toList()
    }

    private fun fourTable(bpr: Int): List<RoundInfo> {
        val nsPairs = intArrayOf(8, 6, 7, 4, 8, 7, 1, 5, 8, 1, 2, 6, 8, 2, 3, 7, 8, 3, 4, 1, 8, 4, 5, 2, 8, 5, 6, 3)
        val ewPairs = intArrayOf(1, 3, 2, 5, 2, 4, 3, 6, 3, 5, 4, 7, 4, 6, 5, 1, 5, 7, 6, 2, 6, 1, 7, 3, 7, 2, 1, 4)
        return buildList {
            for (round in 1..7) {
                val t1si = round
                val t2si = ((round - 1 + 3) % 7) + 1
                val t3si = ((round - 1 + 5) % 7) + 1
                val t4si = ((round - 1 + 6) % 7) + 1
                val base = (round - 1) * 4
                add(RoundInfo(round, 1, nsPairs[base], ewPairs[base], setBoards(t1si, bpr)))
                add(RoundInfo(round, 2, nsPairs[base + 1], ewPairs[base + 1], setBoards(t2si, bpr)))
                add(RoundInfo(round, 3, nsPairs[base + 2], ewPairs[base + 2], setBoards(t3si, bpr)))
                add(RoundInfo(round, 4, nsPairs[base + 3], ewPairs[base + 3], setBoards(t4si, bpr)))
            }
        }
    }

    // -------------------------------------------------------------------------
    // 5-Table Howell: 9 pairs, 9 rounds, 2 boards per round (18 boards total)
    // Pair 1 sits out each round (relay/bye stand) in a 9-pair game.
    // For a full 10-pair Howell: all play every round.
    // This uses the 9-pair (one sit-out) version.
    // -------------------------------------------------------------------------
    private val fiveTable: List<RoundInfo> = listOf(
        // Round 1
        RoundInfo(1, 1, 2, 3, listOf(1, 2)),
        RoundInfo(1, 2, 4, 5, listOf(3, 4)),
        RoundInfo(1, 3, 6, 7, listOf(5, 6)),
        RoundInfo(1, 4, 8, 9, listOf(7, 8)),
        RoundInfo(1, 5, 0, 1, listOf(0, 0)),    // pair 1 sits out (table 5 = bye)
        // Round 2
        RoundInfo(2, 1, 3, 4, listOf(9, 10)),
        RoundInfo(2, 2, 5, 6, listOf(11, 12)),
        RoundInfo(2, 3, 7, 8, listOf(13, 14)),
        RoundInfo(2, 4, 9, 2, listOf(1, 2)),
        RoundInfo(2, 5, 0, 1, listOf(0, 0)),
        // Round 3
        RoundInfo(3, 1, 4, 5, listOf(15, 16)),
        RoundInfo(3, 2, 6, 7, listOf(9, 10)),
        RoundInfo(3, 3, 8, 9, listOf(11, 12)),
        RoundInfo(3, 4, 2, 3, listOf(13, 14)),
        RoundInfo(3, 5, 0, 1, listOf(0, 0)),
        // Round 4
        RoundInfo(4, 1, 5, 6, listOf(17, 18)),
        RoundInfo(4, 2, 7, 8, listOf(15, 16)),
        RoundInfo(4, 3, 9, 2, listOf(3, 4)),
        RoundInfo(4, 4, 3, 4, listOf(5, 6)),
        RoundInfo(4, 5, 0, 1, listOf(0, 0)),
        // Round 5
        RoundInfo(5, 1, 6, 7, listOf(7, 8)),
        RoundInfo(5, 2, 8, 9, listOf(17, 18)),
        RoundInfo(5, 3, 2, 3, listOf(15, 16)),
        RoundInfo(5, 4, 4, 5, listOf(9, 10)),
        RoundInfo(5, 5, 0, 1, listOf(0, 0)),
        // Round 6
        RoundInfo(6, 1, 7, 8, listOf(11, 12)),
        RoundInfo(6, 2, 9, 2, listOf(7, 8)),
        RoundInfo(6, 3, 3, 4, listOf(17, 18)),
        RoundInfo(6, 4, 5, 6, listOf(13, 14)),
        RoundInfo(6, 5, 0, 1, listOf(0, 0)),
        // Round 7
        RoundInfo(7, 1, 8, 9, listOf(13, 14)),
        RoundInfo(7, 2, 2, 3, listOf(11, 12)),
        RoundInfo(7, 3, 4, 5, listOf(7, 8)),
        RoundInfo(7, 4, 6, 7, listOf(17, 18)),
        RoundInfo(7, 5, 0, 1, listOf(0, 0)),
        // Round 8
        RoundInfo(8, 1, 9, 2, listOf(5, 6)),
        RoundInfo(8, 2, 3, 4, listOf(3, 4)),
        RoundInfo(8, 3, 5, 6, listOf(1, 2)),
        RoundInfo(8, 4, 7, 8, listOf(15, 16)),
        RoundInfo(8, 5, 0, 1, listOf(0, 0)),
        // Round 9
        RoundInfo(9, 1, 2, 9, listOf(17, 18)),
        RoundInfo(9, 2, 4, 3, listOf(5, 6)),
        RoundInfo(9, 3, 6, 5, listOf(3, 4)),
        RoundInfo(9, 4, 8, 7, listOf(9, 10)),
        RoundInfo(9, 5, 0, 1, listOf(0, 0))
    )

    fun defaultBoardsPerRound(tables: Int): Int = when (tables) {
        3 -> 3
        4 -> 4
        else -> 2
    }

    fun boardCountForMovement(tables: Int, bpr: Int): Int = when (tables) {
        3 -> 5 * bpr
        4 -> 7 * bpr
        5 -> 9 * bpr
        else -> 0
    }

    fun getMovement(tables: Int, bpr: Int = defaultBoardsPerRound(tables)): List<RoundInfo> = when (tables) {
        3 -> threeTable(bpr)
        4 -> fourTable(bpr)
        5 -> fiveTable
        else -> emptyList()
    }

    /** Returns the schedule for a specific pair across all rounds. */
    fun getScheduleForPair(tables: Int, pairNumber: Int, bpr: Int = defaultBoardsPerRound(tables)): List<PairSchedule> {
        val movement = getMovement(tables, bpr)
        val schedule = mutableListOf<PairSchedule>()
        for (round in movement) {
            when (pairNumber) {
                round.nsPair -> schedule.add(
                    PairSchedule(round.round, round.table, round.ewPair, round.boards, sitNS = true)
                )
                round.ewPair -> schedule.add(
                    PairSchedule(round.round, round.table, round.nsPair, round.boards, sitNS = false)
                )
            }
        }
        return schedule.sortedBy { it.round }
    }

    /** Returns next table to go to after the current round. */
    fun nextTable(tables: Int, pairNumber: Int, currentRound: Int, bpr: Int = defaultBoardsPerRound(tables)): Int? {
        val schedule = getScheduleForPair(tables, pairNumber, bpr)
        return schedule.firstOrNull { it.round == currentRound + 1 }?.table
    }
}
