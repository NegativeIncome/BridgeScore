package com.bridgescore

import android.app.Application
import com.bridgescore.data.db.BridgeDatabase
import com.bridgescore.data.repository.BridgeRepository

class BridgeScoreApp : Application() {
    val database by lazy { BridgeDatabase.getInstance(this) }
    val repository by lazy {
        BridgeRepository(database.sessionDao(), database.boardResultDao())
    }
}
