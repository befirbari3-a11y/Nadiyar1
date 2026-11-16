package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exam_snapshots WHERE id = 1 LIMIT 1")
    fun observeExamSnapshot(): Flow<ExamSnapshotEntity?>

    @Query("SELECT * FROM exam_snapshots WHERE id = 1 LIMIT 1")
    suspend fun getExamSnapshot(): ExamSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExamSnapshot(entity: ExamSnapshotEntity)

    @Query("DELETE FROM exam_snapshots")
    suspend fun clearExamSnapshot()

    @Query("SELECT * FROM header_presets WHERE id = 1 LIMIT 1")
    suspend fun getHeaderPreset(): HeaderPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHeaderPreset(entity: HeaderPresetEntity)

    @Query("SELECT * FROM certificate_snapshots WHERE id = 1 LIMIT 1")
    fun observeCertificateSnapshot(): Flow<CertificateSnapshotEntity?>

    @Query("SELECT * FROM certificate_snapshots WHERE id = 1 LIMIT 1")
    suspend fun getCertificateSnapshot(): CertificateSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCertificateSnapshot(entity: CertificateSnapshotEntity)
}
