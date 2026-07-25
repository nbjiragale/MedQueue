package com.niranjan.medqueue.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: RequestEntity): Long

    /**
     * Newest first. Ordered by createdAt rather than id so that edited or
     * re-imported rows still sort by when the customer actually asked;
     * id is the tie-breaker for rows saved in the same millisecond.
     */
    @Query("SELECT * FROM requests ORDER BY createdAt DESC, id DESC")
    fun getAll(): Flow<List<RequestEntity>>

    @Query("UPDATE requests SET customerName = :customerName, phoneNumber = :phoneNumber, medicineName = :medicineName WHERE id = :id")
    suspend fun updateFields(id: Int, customerName: String, phoneNumber: String, medicineName: String)

    @Query("UPDATE requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: RequestStatus)

    @Query("UPDATE requests SET isEmergency = :isEmergency WHERE id = :id")
    suspend fun updateEmergency(id: Int, isEmergency: Boolean)

    @Query("DELETE FROM requests WHERE id = :id")
    suspend fun delete(id: Int)
}

