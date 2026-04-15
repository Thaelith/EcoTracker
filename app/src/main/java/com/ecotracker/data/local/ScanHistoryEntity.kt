package com.ecotracker.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_history",
    foreignKeys = [
        ForeignKey(
            entity = CachedProductEntity::class,
            parentColumns = ["barcode"],
            childColumns = ["barcode"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["barcode"])]
)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val scannedAt: Long = System.currentTimeMillis()
)
