package com.ecotracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScannedProduct::class], version = 3, exportSchema = false)
abstract class EcoTrackerDatabase : RoomDatabase() {

    abstract fun scannedProductDao(): ScannedProductDao

    companion object {
        const val DATABASE_NAME = "eco_tracker_db"
    }
}
