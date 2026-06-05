package com.bridgescore.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bridgescore.data.model.BoardResult
import com.bridgescore.data.model.Doubled
import com.bridgescore.data.model.MovementType
import com.bridgescore.data.model.Session
import com.bridgescore.data.model.Suit

class Converters {
    @TypeConverter fun fromSuit(v: Suit) = v.name
    @TypeConverter fun toSuit(v: String) = Suit.valueOf(v)
    @TypeConverter fun fromDoubled(v: Doubled) = v.name
    @TypeConverter fun toDoubled(v: String) = Doubled.valueOf(v)
    @TypeConverter fun fromMovement(v: MovementType) = v.name
    @TypeConverter fun toMovement(v: String) = MovementType.valueOf(v)
}

@Database(entities = [Session::class, BoardResult::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class BridgeDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun boardResultDao(): BoardResultDao

    companion object {
        @Volatile private var INSTANCE: BridgeDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Keep only the most-recently-inserted row for each (sessionId, boardNumber)
                database.execSQL(
                    "DELETE FROM board_results WHERE id NOT IN " +
                    "(SELECT MAX(id) FROM board_results GROUP BY sessionId, boardNumber)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX index_board_results_sessionId_boardNumber " +
                    "ON board_results(sessionId, boardNumber)"
                )
            }
        }

        fun getInstance(context: Context): BridgeDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BridgeDatabase::class.java,
                    "bridge_score.db"
                ).addMigrations(MIGRATION_3_4).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
