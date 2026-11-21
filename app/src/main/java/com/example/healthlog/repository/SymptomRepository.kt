package com.example.healthlog.repository

import com.example.healthlog.database.SymptomDao
import com.example.healthlog.database.SymptomEntry
import kotlinx.coroutines.flow.Flow

class SymptomRepository(
    private val dao: SymptomDao
) {
    fun getAllSymptoms(): Flow<List<SymptomEntry>> =
        dao.getAllSymptoms()

    fun getSymptomById(id: Long): Flow<SymptomEntry?> =
        dao.getSymptomById(id)

    // Live list of symptoms in [startMillis, endMillis), newest first.
    fun getSymptomsInRange(startMillis: Long, endMillis: Long): Flow<List<SymptomEntry>> =
        dao.getSymptomsInRange(startMillis, endMillis)

    suspend fun save(symptom: SymptomEntry): Long =
        dao.insert(symptom)

    suspend fun update(symptom: SymptomEntry) {
        dao.update(symptom)
    }

    suspend fun delete(symptom: SymptomEntry) {
        dao.delete(symptom)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }
}
