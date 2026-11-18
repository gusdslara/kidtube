package com.kidstv.app

import android.app.Application
import com.kidstv.app.data.db.KidsTvDatabase
import com.kidstv.app.data.repository.KidsTvRepository
import com.kidstv.app.data.security.ParentalPrefs
import com.kidstv.app.data.storage.DeviceStorageManager

class KidsTvApplication : Application() {

    lateinit var repository: KidsTvRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = KidsTvDatabase.getInstance(this)
        val storage = DeviceStorageManager(this)
        val prefs = ParentalPrefs(this)
        repository = KidsTvRepository(
            videoDao = db.videoDao(),
            playlistDao = db.playlistDao(),
            historyDao = db.historyDao(),
            storageManager = storage,
            parentalPrefs = prefs
        )
    }
}
