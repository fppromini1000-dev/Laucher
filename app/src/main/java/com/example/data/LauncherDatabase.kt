package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppEntity::class,
        UsageHabitEntity::class,
        UserSettingsEntity::class,
        LauncherWidgetEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appCustomizationDao(): AppCustomizationDao
    abstract fun usageHabitDao(): UsageHabitDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun widgetDao(): WidgetDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        fun getDatabase(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "firi_launcher.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
