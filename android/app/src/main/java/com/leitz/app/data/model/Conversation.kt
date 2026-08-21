package com.leitz.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey
    val id: String,
    val name: String,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val isGroup: Boolean = false,
    val participantIds: String = "",
    val avatarUrl: String? = null,
    val unreadCount: Int = 0
)