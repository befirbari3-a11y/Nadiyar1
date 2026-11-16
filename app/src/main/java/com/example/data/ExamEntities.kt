package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_snapshots")
data class ExamSnapshotEntity(
    @PrimaryKey val id: Int = 1,
    val jsonData: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "header_presets")
data class HeaderPresetEntity(
    @PrimaryKey val id: Int = 1,
    val jsonData: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "certificate_snapshots")
data class CertificateSnapshotEntity(
    @PrimaryKey val id: Int = 1,
    val jsonData: String,
    val updatedAt: Long = System.currentTimeMillis()
)
