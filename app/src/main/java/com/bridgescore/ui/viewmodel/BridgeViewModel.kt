package com.bridgescore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bridgescore.data.model.*
import com.bridgescore.data.model.BoardResult
import com.bridgescore.data.model.Suit
import com.bridgescore.data.model.Doubled
import com.bridgescore.data.repository.BridgeRepository
import com.bridgescore.scoring.computeScore
import com.bridgescore.scoring.movement.HowellMovement
import com.bridgescore.scoring.movement.MitchellMovement
import com.bridgescore.scoring.movement.PairSchedule
import com.bridgescore.scoring.vulnerabilityForBoard
import com.bridgescore.scoring.isNSVulnerable
import com.bridgescore.scoring.isEWVulnerable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BoardEntryState(
    val boardNumber: Int = 1,
    val level: Int = 1,
    val suit: Suit = Suit.NOTRUMP,
    val doubled: Doubled = Doubled.NONE,
    val declarer: String = "N",         // N, S, E, W
    val tricksMade: Int = 7,            // default: bid made exactly
    val passed: Boolean = false,
    val notPlayed: Boolean = false,
    val computedScore: Int = 0,
    val vulnerability: Vulnerability = Vulnerability.NONE,
    val opponentPair: Int = 0,
    val nextTable: Int? = null
)

data class SessionUiState(
    val sessionId: Long = 0L,
    val date: String = LocalDate.now().toString(),
    val partner: String = "",
    val pairNumber: Int = 1,
    val movementType: MovementType = MovementType.HOWELL,
    val numberOfTables: Int = 4,
    val boardCount: Int = 28,
    val boardsPerRound: Int = 4,
    val boards: List<BoardResult> = emptyList(),
    val currentBoardNumber: Int = 1,
    val entryState: BoardEntryState = BoardEntryState(),
    val pairSchedule: List<PairSchedule> = emptyList(),
    val playOrder: List<Int> = emptyList()
)

class BridgeViewModel(private val repo: BridgeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllSessions().collect { _sessions.value = it }
        }
    }

    fun startNewSession(
        partner: String,
        pairNumber: Int,
        movementType: MovementType,
        tables: Int,
        boardsPerRound: Int = HowellMovement.defaultBoardsPerRound(tables),
        boardCount: Int = if (movementType == MovementType.HOWELL)
            HowellMovement.boardCountForMovement(tables, boardsPerRound) else 0
    ) {
        viewModelScope.launch {
            val effectiveBoardCount = when (movementType) {
                MovementType.HOWELL -> HowellMovement.boardCountForMovement(tables, boardsPerRound)
                MovementType.MITCHELL -> MitchellMovement.boardCountForMovement(tables, boardsPerRound)
            }
            val session = Session(
                date = LocalDate.now().toString(),
                partner = partner,
                pairNumber = pairNumber,
                movementType = movementType,
                numberOfTables = tables,
                boardCount = effectiveBoardCount,
                boardsPerRound = boardsPerRound
            )
            val id = repo.saveSession(session)
            val schedule = when (movementType) {
                MovementType.HOWELL -> HowellMovement.getScheduleForPair(tables, pairNumber, boardsPerRound)
                MovementType.MITCHELL -> MitchellMovement.getScheduleForPair(tables, boardsPerRound, pairNumber)
            }

            val playOrder = computePlayOrder(schedule, effectiveBoardCount)
            val firstBoard = playOrder.firstOrNull() ?: 1
            val entry = buildEntryState(firstBoard, id, pairNumber, movementType, tables, boardsPerRound, schedule)
            _uiState.value = SessionUiState(
                sessionId = id,
                date = session.date,
                partner = partner,
                pairNumber = pairNumber,
                movementType = movementType,
                numberOfTables = tables,
                boardCount = effectiveBoardCount,
                boardsPerRound = boardsPerRound,
                currentBoardNumber = firstBoard,
                entryState = entry,
                pairSchedule = schedule,
                playOrder = playOrder
            )
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = repo.getSession(sessionId) ?: return@launch
            val boards = repo.getBoardsOnce(sessionId)
            val bpr = session.boardsPerRound
            val schedule = when (session.movementType) {
                MovementType.HOWELL -> HowellMovement.getScheduleForPair(session.numberOfTables, session.pairNumber, bpr)
                MovementType.MITCHELL -> MitchellMovement.getScheduleForPair(session.numberOfTables, bpr, session.pairNumber)
            }
            val playOrder = computePlayOrder(schedule, session.boardCount)
            val playedBoardNumbers = boards.map { it.boardNumber }.toSet()
            val current = playOrder.firstOrNull { it !in playedBoardNumbers } ?: playOrder.lastOrNull() ?: 1
            val entry = buildEntryState(current, sessionId, session.pairNumber,
                session.movementType, session.numberOfTables, bpr, schedule)
            _uiState.value = SessionUiState(
                sessionId = sessionId,
                date = session.date,
                partner = session.partner,
                pairNumber = session.pairNumber,
                movementType = session.movementType,
                numberOfTables = session.numberOfTables,
                boardCount = session.boardCount,
                boardsPerRound = bpr,
                boards = boards,
                currentBoardNumber = current,
                entryState = entry,
                pairSchedule = schedule,
                playOrder = playOrder
            )
        }
    }

    fun navigateToBoard(boardNumber: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            val existing = repo.getBoard(state.sessionId, boardNumber)
            val entry = if (existing != null) {
                val vuln = vulnerabilityForBoard(boardNumber)
                val schedule = state.pairSchedule.firstOrNull {
                    it.boards.contains(boardNumber)
                }
                BoardEntryState(
                    boardNumber = boardNumber,
                    level = existing.level,
                    suit = existing.suit,
                    doubled = existing.doubled,
                    declarer = existing.declarer,
                    tricksMade = existing.tricksMade,
                    passed = existing.passed,
                    notPlayed = existing.notPlayed,
                    computedScore = existing.score,
                    vulnerability = vuln,
                    opponentPair = existing.opponentPairNumber,
                    nextTable = schedule?.let {
                        HowellMovement.nextTable(state.numberOfTables, state.pairNumber,
                            state.pairSchedule.firstOrNull { s -> s.boards.contains(boardNumber) }?.round ?: 0,
                            state.boardsPerRound)
                    }
                )
            } else {
                buildEntryState(boardNumber, state.sessionId, state.pairNumber,
                    state.movementType, state.numberOfTables, state.boardsPerRound, state.pairSchedule)
            }
            _uiState.update { it.copy(currentBoardNumber = boardNumber, entryState = entry) }
        }
    }

    fun updateLevel(level: Int) {
        _uiState.update { s ->
            s.copy(entryState = s.entryState.copy(
                level = level,
                tricksMade = level + 6,
                computedScore = recalc(s.entryState.copy(level = level, tricksMade = level + 6))
            ))
        }
    }

    fun updateSuit(suit: Suit) {
        _uiState.update { s ->
            s.copy(entryState = s.entryState.copy(
                suit = suit,
                computedScore = recalc(s.entryState.copy(suit = suit))
            ))
        }
    }

    fun updateDoubled(doubled: Doubled) {
        _uiState.update { s ->
            s.copy(entryState = s.entryState.copy(
                doubled = doubled,
                computedScore = recalc(s.entryState.copy(doubled = doubled))
            ))
        }
    }

    fun updateDeclarer(declarer: String) {
        _uiState.update { s ->
            s.copy(entryState = s.entryState.copy(
                declarer = declarer,
                computedScore = recalc(s.entryState.copy(declarer = declarer))
            ))
        }
    }

    fun updateResult(tricksMade: Int) {
        _uiState.update { s ->
            s.copy(entryState = s.entryState.copy(
                tricksMade = tricksMade,
                computedScore = recalc(s.entryState.copy(tricksMade = tricksMade))
            ))
        }
    }

    fun togglePassed() {
        _uiState.update { s ->
            val passed = !s.entryState.passed
            s.copy(entryState = s.entryState.copy(
                passed = passed,
                notPlayed = if (passed) false else s.entryState.notPlayed,
                computedScore = 0
            ))
        }
    }

    fun toggleNotPlayed() {
        _uiState.update { s ->
            val notPlayed = !s.entryState.notPlayed
            s.copy(entryState = s.entryState.copy(
                notPlayed = notPlayed,
                passed = if (notPlayed) false else s.entryState.passed,
                computedScore = 0
            ))
        }
    }

    fun saveCurrentBoard() {
        val state = _uiState.value
        val entry = state.entryState
        viewModelScope.launch {
            val board = BoardResult(
                sessionId = state.sessionId,
                boardNumber = entry.boardNumber,
                opponentPairNumber = entry.opponentPair,
                declarer = entry.declarer,
                level = entry.level,
                suit = entry.suit,
                doubled = entry.doubled,
                tricksMade = entry.tricksMade,
                score = entry.computedScore,
                passed = entry.passed,
                notPlayed = entry.notPlayed
            )
            repo.saveBoard(board)
            val updatedBoards = repo.getBoardsOnce(state.sessionId)
            _uiState.update { it.copy(boards = updatedBoards) }
        }
    }

    fun saveAndNextBoard() {
        saveCurrentBoard()
        val state = _uiState.value
        val idx = state.playOrder.indexOf(state.currentBoardNumber)
        val nextBoard = if (idx >= 0 && idx + 1 < state.playOrder.size) state.playOrder[idx + 1] else null
        if (nextBoard != null) {
            navigateToBoard(nextBoard)
        }
    }

    fun navigatePrevBoard() {
        val state = _uiState.value
        val idx = state.playOrder.indexOf(state.currentBoardNumber)
        val prevBoard = when {
            idx > 0 -> state.playOrder[idx - 1]
            state.currentBoardNumber > 1 -> state.currentBoardNumber - 1
            else -> null
        }
        if (prevBoard != null) navigateToBoard(prevBoard)
    }

    fun hasPrevBoard(): Boolean {
        val state = _uiState.value
        val idx = state.playOrder.indexOf(state.currentBoardNumber)
        return idx > 0 || state.currentBoardNumber > 1
    }

    fun hasNextBoard(): Boolean {
        val state = _uiState.value
        val idx = state.playOrder.indexOf(state.currentBoardNumber)
        return idx >= 0 && idx + 1 < state.playOrder.size
    }

    private fun recalc(entry: BoardEntryState): Int {
        if (entry.passed || entry.notPlayed || entry.level == 0) return 0
        val vuln = entry.vulnerability
        val declarerIsNS = entry.declarer == "N" || entry.declarer == "S"
        val vulnerable = if (declarerIsNS) vuln.isNSVulnerable() else vuln.isEWVulnerable()
        val raw = computeScore(entry.level, entry.suit, entry.doubled, entry.tricksMade, vulnerable)
        // Score is from NS perspective: positive = NS plus
        return if (declarerIsNS) raw else -raw
    }

    private fun buildEntryState(
        boardNumber: Int,
        sessionId: Long,
        pairNumber: Int,
        movementType: MovementType,
        tables: Int,
        bpr: Int,
        schedule: List<PairSchedule>
    ): BoardEntryState {
        val vuln = vulnerabilityForBoard(boardNumber)
        val roundEntry = schedule.firstOrNull { it.boards.contains(boardNumber) }
        val opponentPair = roundEntry?.opponentPair ?: 0
        val nextTbl = roundEntry?.let {
            HowellMovement.nextTable(tables, pairNumber, it.round, bpr)
        }
        return BoardEntryState(
            boardNumber = boardNumber,
            level = 1,
            suit = Suit.NOTRUMP,
            doubled = Doubled.NONE,
            declarer = "N",
            tricksMade = 7,
            passed = false,
            notPlayed = false,
            vulnerability = vuln,
            opponentPair = opponentPair,
            nextTable = nextTbl
        ).let { e -> e.copy(computedScore = recalc(e)) }
    }

    private fun computePlayOrder(schedule: List<PairSchedule>, boardCount: Int): List<Int> =
        if (schedule.isNotEmpty())
            schedule.sortedBy { it.round }.flatMap { it.boards }.filter { it > 0 }
        else
            (1..boardCount).toList()

    suspend fun buildExportCsv(sessionIds: List<Long>): String {
        val sb = StringBuilder()
        sb.append("date,board,contract,score\n")
        for (sid in sessionIds) {
            val session = repo.getSession(sid) ?: continue
            val boards = repo.getBoardsOnce(sid).sortedBy { it.boardNumber }
            for (board in boards) {
                sb.append("${session.date},${board.boardNumber},${contractString(board)},${board.score}\n")
            }
        }
        return sb.toString()
    }

    private fun contractString(board: BoardResult): String {
        if (board.notPlayed) return "NP"
        if (board.passed) return "PASS"
        val suitStr = when (board.suit) {
            Suit.CLUBS    -> "C"
            Suit.DIAMONDS -> "D"
            Suit.HEARTS   -> "H"
            Suit.SPADES   -> "S"
            Suit.NOTRUMP  -> "NT"
        }
        val doubledStr = when (board.doubled) {
            Doubled.NONE      -> ""
            Doubled.DOUBLED   -> "X"
            Doubled.REDOUBLED -> "XX"
        }
        return "${board.level}${suitStr}${doubledStr} ${board.declarer}"
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch { repo.deleteSession(session) }
    }

    class Factory(private val repo: BridgeRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BridgeViewModel(repo) as T
        }
    }
}
