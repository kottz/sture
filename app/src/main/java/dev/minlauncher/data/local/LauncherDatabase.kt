package dev.minlauncher.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.minlauncher.data.model.AppMetadataEntity
import dev.minlauncher.data.model.GestureAppEntity
import dev.minlauncher.data.model.HomeAppEntity

@Database(
    entities = [
        AppMetadataEntity::class,
        HomeAppEntity::class,
        GestureAppEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun homeAppDao(): HomeAppDao
    abstract fun gestureAppDao(): GestureAppDao
    
    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null
        
        fun getInstance(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "launcher_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
