package com.example.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// Entities
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderName: String,
    val encryptedText: String,
    val isFromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val fileId: Int? = null
)

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val fileSize: String,
    val encryptedContent: String, // base64 representation
    val timestamp: Long = System.currentTimeMillis(),
    val isIncoming: Boolean,
    val senderName: String
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callerName: String,
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isIncoming: Boolean,
    val callType: String // "VIDEO" or "AUDIO"
)

// DAO
@Dao
interface AppDao {
    // Messages
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    // Files
    @Query("SELECT * FROM files ORDER BY timestamp DESC")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Query("DELETE FROM files WHERE id = :fileId")
    suspend fun deleteFile(fileId: Int)

    // Call Logs
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity)
}

// Database
@Database(
    entities = [MessageEntity::class, FileEntity::class, CallLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
