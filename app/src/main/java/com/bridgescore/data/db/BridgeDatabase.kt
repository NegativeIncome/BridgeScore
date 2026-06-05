package com.bridgescore.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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

@Database(entities = [Session::class, BoardResult::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class BridgeDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun boardResultDao(): BoardResultDao

    companion object {
        @Volatile private var INSTANCE: BridgeDatabase? = null

        fun getInstance(context: Context): BridgeDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BridgeDatabase::class.java,
                    "bridge_score.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
