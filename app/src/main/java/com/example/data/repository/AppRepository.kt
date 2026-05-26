package com.example.data.repository

import com.example.data.database.AppDao
import com.example.data.database.MessageEntity
import com.example.data.database.FileEntity
import com.example.data.database.CallLogEntity
import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allMessages: Flow<List<MessageEntity>> = appDao.getAllMessages()
    val allFiles: Flow<List<FileEntity>> = appDao.getAllFiles()
    val allCallLogs: Flow<List<CallLogEntity>> = appDao.getAllCallLogs()

    suspend fun insertMessage(message: MessageEntity) = appDao.insertMessage(message)
    suspend fun clearMessages() = appDao.clearAllMessages()

    suspend fun insertFile(file: FileEntity) = appDao.insertFile(file)
    suspend fun deleteFile(id: Int) = appDao.deleteFile(id)

    suspend fun insertCallLog(callLog: CallLogEntity) = appDao.insertCallLog(callLog)
}
