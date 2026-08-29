package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SavedCamera
import com.example.data.model.SecurityEvent
import com.example.data.model.SnapshotRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CctvDao {
    @Query("SELECT * FROM saved_cameras ORDER BY lastConnected DESC")
    fun getAllSavedCameras(): Flow<List<SavedCamera>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCamera(camera: SavedCamera)

    @Delete
    suspend fun deleteCamera(camera: SavedCamera)

    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<SnapshotRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotRecord): Long

    @Delete
    suspend fun deleteSnapshot(snapshot: SnapshotRecord)

    @Query("SELECT * FROM security_events ORDER BY timestamp DESC LIMIT 50")
    fun getRecentSecurityEvents(): Flow<List<SecurityEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityEvent(event: SecurityEvent): Long

    @Query("DELETE FROM security_events")
    suspend fun clearSecurityEvents()
}
