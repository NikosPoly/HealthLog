package com.example.healthlog.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(symptom: SymptomEntry): Long

    @Update
    suspend fun update(symptom: SymptomEntry)

    @Delete
    suspend fun delete(symptom: SymptomEntry)

    @Query("DELETE FROM symptoms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM symptoms WHERE id = :id LIMIT 1")
    fun getSymptomById(id: Long): Flow<SymptomEntry?>

    // All symptoms within [startMillis, endMillis), newest first.
    @Query(
        "SELECT * FROM symptoms WHERE timestamp >= :startMillis AND timestamp < :endMillis " +
            "ORDER BY timestamp DESC, id DESC"
    )
    fun getSymptomsInRange(startMillis: Long, endMillis: Long): Flow<List<SymptomEntry>>

    @Query("SELECT * FROM symptoms ORDER BY timestamp DESC, id DESC")
    fun getAllSymptoms(): Flow<List<SymptomEntry>>
}
