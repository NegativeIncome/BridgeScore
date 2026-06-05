package com.bridgescore.data.db

import androidx.room.*
import com.bridgescore.data.model.BoardResult
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(board: BoardResult): Long

    @Update
    suspend fun update(board: BoardResult)

    @Query("SELECT * FROM board_results WHERE sessionId = :sessionId ORDER BY boardNumber ASC")
    fun getBoardsForSession(sessionId: Long): Flow<List<BoardResult>>

    @Query("SELECT * FROM board_results WHERE sessionId = :sessionId ORDER BY boardNumber ASC")
    suspend fun getBoardsForSessionOnce(sessionId: Long): List<BoardResult>

    @Query("SELECT * FROM board_results WHERE sessionId = :sessionId AND boardNumber = :boardNumber LIMIT 1")
    suspend fun getBoard(sessionId: Long, boardNumber: Int): BoardResult?

    @Query("DELETE FROM board_results WHERE sessionId = :sessionId AND boardNumber = :boardNumber")
    suspend fun deleteBoard(sessionId: Long, boardNumber: Int)
}
