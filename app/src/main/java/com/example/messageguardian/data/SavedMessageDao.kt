package com.example.messageguardian.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMessageDao {
    @Query("SELECT * FROM saved_messages ORDER BY timestamp DESC")
    fun observeMessages(): Flow<List<SavedMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: SavedMessage)

    @Query("UPDATE saved_messages SET isRevoked = 1 WHERE notificationKey = :notificationKey")
    suspend fun markRevoked(notificationKey: String)

    @Query("DELETE FROM saved_messages WHERE timestamp < :oldestAllowed")
    suspend fun deleteOlderThan(oldestAllowed: Long)
}
