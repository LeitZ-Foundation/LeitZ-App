package com.leitz.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val publicKey: String? = null,
    val status: String? = null,
    val lastSeen: Long? = null
)