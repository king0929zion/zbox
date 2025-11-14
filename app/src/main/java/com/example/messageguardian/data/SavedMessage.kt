package com.example.messageguardian.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_messages",
    indices = [Index(value = ["packageName"]), Index(value = ["notificationKey"], unique = true)]
)
data class SavedMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val conversation: String?,
    val sender: String?,
    val messageText: String,
    val timestamp: Long,
    val isRevoked: Boolean = false,
    val notificationKey: String,
    val conversationId: String? = null
)
