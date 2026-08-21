package com.leitz.app.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val type: String, // "text", "image", "video", "file", "audio"
    val timestamp: Long,
    val status: String, // "sending", "sent", "delivered", "read"
    val mediaUrl: String? = null,
    val mediaSize: Long? = null,
    val mediaMimeType: String? = null,
    val thumbnailUrl: String? = null
)