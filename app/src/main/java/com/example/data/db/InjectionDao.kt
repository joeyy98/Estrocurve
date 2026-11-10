package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.InjectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InjectionDao {
    @Query("SELECT * FROM injections ORDER BY timestamp DESC")
    fun getAllInjectionsFlow(): Flow<List<InjectionEntity>>

    @Query("SELECT * FROM injections ORDER BY timestamp ASC")
    suspend fun getAllInjectionsAscending(): List<InjectionEntity>

    @Query("SELECT * FROM injections ORDER BY timestamp DESC LIMIT 1")
    fun getLatestInjectionFlow(): Flow<InjectionEntity?>

    @Query("SELECT * FROM injections ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestInjection(): InjectionEntity?

    @Query("SELECT * FROM injections WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): InjectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(injection: InjectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(injections: List<InjectionEntity>)

    @Update
    suspend fun update(injection: InjectionEntity)

    @Delete
    suspend fun delete(injection: InjectionEntity)

    @Query("DELETE FROM injections")
    suspend fun clearAll()
}
