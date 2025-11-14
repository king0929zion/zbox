package com.example.messageguardian.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SavedMessageRepository(private val dao: SavedMessageDao) {
    val savedMessages: Flow<List<SavedMessage>> = dao.observeMessages()

    suspend fun persist(message: SavedMessage) = withContext(Dispatchers.IO) {
        dao.insert(message)
    }

    suspend fun markRevoked(notificationKey: String) = withContext(Dispatchers.IO) {
        dao.markRevoked(notificationKey)
    }

    suspend fun pruneOlderThan(days: Int) = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        dao.deleteOlderThan(threshold)
    }

    companion object {
        fun from(context: android.content.Context): SavedMessageRepository {
            return SavedMessageRepository(AppDatabase.getInstance(context).savedMessageDao())
        }
    }
}
