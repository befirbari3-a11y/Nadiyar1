package com.example.data

import com.example.model.CertificateState
import com.example.model.ExamDocumentState
import com.example.model.ExamLanguage
import com.example.model.HeaderState
import com.example.model.LocalizationPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExamRepository(private val dao: ExamDao) {

    suspend fun loadExamDocument(): ExamDocumentState = withContext(Dispatchers.IO) {
        val entity = dao.getExamSnapshot()
        if (entity != null) {
            runCatching { ExamJsonCodec.decodeDocument(entity.jsonData) }.getOrNull()
                ?: LocalizationPresets.defaultDocumentState()
        } else {
            LocalizationPresets.defaultDocumentState()
        }
    }

    suspend fun saveExamDocument(doc: ExamDocumentState) = withContext(Dispatchers.IO) {
        dao.upsertExamSnapshot(
            ExamSnapshotEntity(
                id = 1,
                jsonData = ExamJsonCodec.encodeDocument(doc),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearExamDocument() = withContext(Dispatchers.IO) {
        dao.clearExamSnapshot()
    }

    suspend fun saveHeaderPreset(header: HeaderState) = withContext(Dispatchers.IO) {
        dao.upsertHeaderPreset(
            HeaderPresetEntity(
                id = 1,
                jsonData = ExamJsonCodec.encodeHeader(header),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun loadHeaderPreset(fallbackLang: ExamLanguage): HeaderState? = withContext(Dispatchers.IO) {
        val entity = dao.getHeaderPreset() ?: return@withContext null
        runCatching { ExamJsonCodec.decodeHeader(entity.jsonData, fallbackLang) }.getOrNull()
    }

    suspend fun loadCertificate(): CertificateState = withContext(Dispatchers.IO) {
        val entity = dao.getCertificateSnapshot()
        if (entity != null) {
            runCatching { ExamJsonCodec.decodeCertificate(entity.jsonData) }.getOrNull()
                ?: LocalizationPresets.certificateTemplates().first().state
        } else {
            LocalizationPresets.certificateTemplates().first().state
        }
    }

    suspend fun saveCertificate(cert: CertificateState) = withContext(Dispatchers.IO) {
        dao.upsertCertificateSnapshot(
            CertificateSnapshotEntity(
                id = 1,
                jsonData = ExamJsonCodec.encodeCertificate(cert),
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
