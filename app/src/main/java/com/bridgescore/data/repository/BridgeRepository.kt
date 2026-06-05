package com.bridgescore.data.repository

import com.bridgescore.data.db.BoardResultDao
import com.bridgescore.data.db.SessionDao
import com.bridgescore.data.model.BoardResult
import com.bridgescore.data.model.Session
import kotlinx.coroutines.flow.Flow

class BridgeRepository(
    private val sessionDao: SessionDao,
    private val boardDao: BoardResultDao
) {
    fun getAllSessions(): Flow<List<Session>> = sessionDao.getAllSessions()

    suspend fun saveSession(session: Session): Long = sessionDao.insert(session)

    suspend fun updateSession(session: Session) = sessionDao.update(session)

    fun getBoardsForSession(sessionId: Long): Flow<List<BoardResult>> =
        boardDao.getBoardsForSession(sessionId)

    suspend fun getBoardsOnce(sessionId: Long): List<BoardResult> =
        boardDao.getBoardsForSessionOnce(sessionId)

    suspend fun saveBoard(board: BoardResult): Long = boardDao.insert(board)

    suspend fun getBoard(sessionId: Long, boardNumber: Int): BoardResult? =
        boardDao.getBoard(sessionId, boardNumber)

    suspend fun getSession(id: Long): Session? = sessionDao.getById(id)

    suspend fun deleteSession(session: Session) = sessionDao.delete(session)
}
