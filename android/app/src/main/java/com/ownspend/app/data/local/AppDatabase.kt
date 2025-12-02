package com.ownspend.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Entity representing a pending event to sync.
 */
@Entity(tableName = "pending_events")
data class PendingEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "source_type")
    val sourceType: String,  // SMS, NOTIFICATION
    
    @ColumnInfo(name = "source_sender")
    val sourceSender: String,
    
    @ColumnInfo(name = "source_package")
    val sourcePackage: String? = null,
    
    @ColumnInfo(name = "raw_text")
    val rawText: String,
    
    @ColumnInfo(name = "received_at")
    val receivedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING",  // PENDING, SYNCING, SYNCED, FAILED
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)

/**
 * DAO for pending events.
 */
@Dao
interface PendingEventDao {
    @Query("SELECT * FROM pending_events WHERE sync_status = 'PENDING' OR sync_status = 'FAILED' ORDER BY received_at ASC")
    suspend fun getPendingEvents(): List<PendingEvent>
    
    @Query("SELECT * FROM pending_events ORDER BY received_at DESC")
    fun getAllEventsFlow(): Flow<List<PendingEvent>>
    
    @Query("SELECT COUNT(*) FROM pending_events WHERE sync_status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM pending_events WHERE sync_status = 'SYNCED'")
    fun getSyncedCountFlow(): Flow<Int>
    
    @Insert
    suspend fun insert(event: PendingEvent): Long
    
    @Update
    suspend fun update(event: PendingEvent)
    
    @Delete
    suspend fun delete(event: PendingEvent)
    
    @Query("DELETE FROM pending_events WHERE sync_status = 'SYNCED' AND synced_at < :olderThan")
    suspend fun deleteOldSyncedEvents(olderThan: Long)
    
    @Query("UPDATE pending_events SET sync_status = 'SYNCING' WHERE id = :eventId")
    suspend fun markSyncing(eventId: Long)
    
    @Query("UPDATE pending_events SET sync_status = 'SYNCED', synced_at = :syncedAt WHERE id = :eventId")
    suspend fun markSynced(eventId: Long, syncedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE pending_events SET sync_status = 'FAILED', retry_count = retry_count + 1, error_message = :error WHERE id = :eventId")
    suspend fun markFailed(eventId: Long, error: String)
}

/**
 * Room database for the app.
 */
@Database(entities = [PendingEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingEventDao(): PendingEventDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ownspend_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
