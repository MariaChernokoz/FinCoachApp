package com.example.fincoach

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FinCoachApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupFirestoreCache()
    }

    private fun setupFirestoreCache() {
        val db = Firebase.firestore

        // Включаем постоянный кеш — 100 МБ на диске
        // Firestore будет отдавать кешированные данные когда нет интернета
        val cacheSettings = PersistentCacheSettings.newBuilder()
            .setSizeBytes(100 * 1024 * 1024L) // 100 MB
            .build()

        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(cacheSettings)
            .build()

        db.firestoreSettings = settings
    }
}