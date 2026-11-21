package com.example.healthlog.repository

import com.example.healthlog.database.SymptomDao
import com.example.healthlog.database.SymptomEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SymptomRepositoryTest {

    private class FakeSymptomDao : SymptomDao {
        private val items = mutableListOf<SymptomEntry>()
        private val itemsFlow = MutableStateFlow<List<SymptomEntry>>(emptyList())
        private var nextId = 1L

        private fun notifyChanges() {
            itemsFlow.value = items.sortedWith(compareByDescending<SymptomEntry> { it.timestamp }.thenByDescending { it.id })
        }

        override suspend fun insert(symptom: SymptomEntry): Long {
            val id = if (symptom.id == 0L) nextId++ else symptom.id
            val entry = symptom.copy(id = id)
            items.removeAll { it.id == id }
            items.add(entry)
            notifyChanges()
            return id
        }

        override suspend fun update(symptom: SymptomEntry) {
            items.removeAll { it.id == symptom.id }
            items.add(symptom)
            notifyChanges()
        }

        override suspend fun delete(symptom: SymptomEntry) {
            items.removeAll { it.id == symptom.id }
            notifyChanges()
        }

        override suspend fun deleteById(id: Long) {
            items.removeAll { it.id == id }
            notifyChanges()
        }

        override fun getSymptomById(id: Long): Flow<SymptomEntry?> =
            itemsFlow.map { list -> list.find { it.id == id } }

        override fun getSymptomsInRange(startMillis: Long, endMillis: Long): Flow<List<SymptomEntry>> =
            itemsFlow.map { list -> list.filter { it.timestamp >= startMillis && it.timestamp < endMillis } }

        override fun getAllSymptoms(): Flow<List<SymptomEntry>> = itemsFlow
    }

    private val repository = SymptomRepository(FakeSymptomDao())

    @Test
    fun saveAndQueryById_returnsSavedSymptom() = runBlocking {
        val id = repository.save(
            SymptomEntry(
                symptomType = "HEADACHE",
                severity = 3,
                timestamp = 1_000L,
                notes = "After lunch"
            )
        )

        val fetched = repository.getSymptomById(id).first()
        assertEquals("HEADACHE", fetched?.symptomType)
        assertEquals(3, fetched?.severity)
        assertEquals("After lunch", fetched?.notes)
    }

    @Test
    fun updateAndDelete_modifiesStoredSymptoms() = runBlocking {
        val id = repository.save(
            SymptomEntry(
                symptomType = "CUSTOM",
                customName = "Neck stiffness",
                severity = 2,
                timestamp = 2_000L
            )
        )

        val saved = repository.getSymptomById(id).first()!!
        repository.update(saved.copy(severity = 4, notes = "Worse in evening"))

        val updated = repository.getSymptomById(id).first()
        assertEquals(4, updated?.severity)
        assertEquals("Worse in evening", updated?.notes)

        repository.deleteById(id)
        assertNull(repository.getSymptomById(id).first())
    }

    @Test
    fun getSymptomsInRange_filtersHalfOpenInterval() = runBlocking {
        repository.save(SymptomEntry(symptomType = "HEADACHE", severity = 2, timestamp = 999L))
        repository.save(SymptomEntry(symptomType = "DIZZINESS", severity = 3, timestamp = 1_000L))
        repository.save(SymptomEntry(symptomType = "FATIGUE", severity = 4, timestamp = 1_500L))
        repository.save(SymptomEntry(symptomType = "NAUSEA", severity = 1, timestamp = 2_000L))

        val inRange = repository.getSymptomsInRange(1_000L, 2_000L).first()
        assertEquals(listOf("FATIGUE", "DIZZINESS"), inRange.map { it.symptomType })
    }
}
