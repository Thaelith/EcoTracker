package com.ecotracker.data.local

import androidx.room.*

@Database(entities = [ScannedProduct::class], version = 4, exportSchema = false)
@TypeConverters(EcoTrackerConverters::class)
abstract class EcoTrackerDatabase : RoomDatabase() {

    abstract fun scannedProductDao(): ScannedProductDao

    companion object {
        const val DATABASE_NAME = "eco_tracker_db"
    }
}

class EcoTrackerConverters {
    @TypeConverter
    fun fromStatus(status: EstimationStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): EstimationStatus = try {
        EstimationStatus.valueOf(value)
    } catch (e: Exception) {
        EstimationStatus.UNIDENTIFIED
    }
}
