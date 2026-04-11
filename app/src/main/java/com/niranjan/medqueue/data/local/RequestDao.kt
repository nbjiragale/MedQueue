package com.niranjan.medqueue.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: RequestEntity)

    @Query("SELECT * FROM requests ORDER BY id DESC")
    fun getAll(): Flow<List<RequestEntity>>

    @Query("UPDATE requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: RequestStatus)

    @Query("DELETE FROM requests WHERE id = :id")
    suspend fun delete(id: Int)
}

