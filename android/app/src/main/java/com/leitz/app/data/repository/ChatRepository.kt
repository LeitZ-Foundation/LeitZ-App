package com.leitz.app.data.repository

import com.leitz.app.data.local.dao.ConversationDao
import com.leitz.app.data.local.dao.MessageDao
import com.leitz.app.data.model.Conversation
import com.leitz.app.data.model.Message
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesForConversation(conversationId)

    fun getAllConversations(): Flow<List<Conversation>> =
        conversationDao.getAllConversations()

    suspend fun insertMessage(message: Message) =
        messageDao.insertMessage(message)

    suspend fun insertMessages(messages: List<Message>) =
        messageDao.insertMessages(messages)

    suspend fun insertConversation(conversation: Conversation) =
        conversationDao.insertConversation(conversation)

    suspend fun insertConversations(conversations: List<Conversation>) =
        conversationDao.insertConversations(conversations)

    suspend fun updateLastMessage(
        conversationId: String,
        lastMessage: String,
        timestamp: Long
    ) = conversationDao.updateLastMessage(conversationId, lastMessage, timestamp)

    suspend fun getConversationById(conversationId: String): Conversation? =
        conversationDao.getConversationById(conversationId)

    suspend fun getMessageById(messageId: String): Message? =
        messageDao.getMessageById(messageId)

    suspend fun deleteMessagesForConversation(conversationId: String) =
        messageDao.deleteMessagesForConversation(conversationId)

    suspend fun deleteConversation(conversationId: String) =
        conversationDao.deleteConversation(conversationId)
}