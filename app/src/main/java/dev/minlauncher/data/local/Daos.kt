package dev.minlauncher.data.local

import androidx.room.*
import dev.minlauncher.data.model.AppMetadataEntity
import dev.minlauncher.data.model.GestureAppEntity
import dev.minlauncher.data.model.HomeAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppMetadataDao {
    @Query("SELECT * FROM app_metadata")
    fun getAllFlow(): Flow<List<AppMetadataEntity>>
    
    @Query("SELECT * FROM app_metadata")
    suspend fun getAll(): List<AppMetadataEntity>
    
    @Query("SELECT * FROM app_metadata WHERE uniqueId = :id")
    suspend fun getById(id: String): AppMetadataEntity?
    
    @Query("SELECT * FROM app_metadata WHERE isHidden = 1")
    fun getHiddenAppsFlow(): Flow<List<AppMetadataEntity>>
    
    @Upsert
    suspend fun upsert(entity: AppMetadataEntity)
    
    @Delete
    suspend fun delete(entity: AppMetadataEntity)
    
    @Query("DELETE FROM app_metadata WHERE uniqueId = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface HomeAppDao {
    @Query("SELECT * FROM home_apps ORDER BY position ASC")
    fun getAllFlow(): Flow<List<HomeAppEntity>>
    
    @Query("SELECT * FROM home_apps ORDER BY position ASC")
    suspend fun getAll(): List<HomeAppEntity>
    
    @Query("SELECT * FROM home_apps WHERE position = :position")
    suspend fun getByPosition(position: Int): HomeAppEntity?
    
    @Upsert
    suspend fun upsert(entity: HomeAppEntity)
    
    @Query("DELETE FROM home_apps WHERE position = :position")
    suspend fun deleteByPosition(position: Int)
    
    @Query("DELETE FROM home_apps WHERE position > :maxPosition")
    suspend fun deleteAbovePosition(maxPosition: Int)
}

@Dao
interface GestureAppDao {
    @Query("SELECT * FROM gesture_apps")
    fun getAllFlow(): Flow<List<GestureAppEntity>>
    
    @Query("SELECT * FROM gesture_apps WHERE gesture = :gesture")
    suspend fun getByGesture(gesture: String): GestureAppEntity?
    
    @Query("SELECT * FROM gesture_apps WHERE gesture = :gesture")
    fun getByGestureFlow(gesture: String): Flow<GestureAppEntity?>
    
    @Upsert
    suspend fun upsert(entity: GestureAppEntity)
}
