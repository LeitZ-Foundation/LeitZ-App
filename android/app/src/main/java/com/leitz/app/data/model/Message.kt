package com.leitz.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val type: String,
    val timestamp: Long,
    val status: String,
    val mediaUrl: String? = null,
    val mediaSize: Long? = null,
    val mediaMimeType: String? = null,
    val thumbnailUrl: String? = null
)