package com.leitz.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leitz.app.data.model.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<Conversation>)

    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): Conversation?

    @Query("UPDATE conversations SET lastMessage = :lastMessage, lastMessageTimestamp = :timestamp WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, lastMessage: String, timestamp: Long)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}