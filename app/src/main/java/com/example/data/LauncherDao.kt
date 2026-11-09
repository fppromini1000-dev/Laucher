package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCustomizationDao {
    @Query("SELECT * FROM app_customizations")
    fun getAllCustomizations(): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomization(app: AppEntity)

    @Query("SELECT * FROM app_customizations WHERE packageName = :packageName LIMIT 1")
    suspend fun getCustomization(packageName: String): AppEntity?
}

@Dao
interface UsageHabitDao {
    @Query("SELECT * FROM usage_habits ORDER BY launchCount DESC")
    fun getAllHabits(): Flow<List<UsageHabitEntity>>

    @Query("SELECT * FROM usage_habits WHERE hourOfDay BETWEEN :startHour AND :endHour ORDER BY launchCount DESC LIMIT :limit")
    suspend fun getTopAppsForHourRange(startHour: Int, endHour: Int, limit: Int = 6): List<UsageHabitEntity>

    @Query("SELECT * FROM usage_habits WHERE hourOfDay = :hour ORDER BY launchCount DESC LIMIT :limit")
    suspend fun getTopAppsForHour(hour: Int, limit: Int = 6): List<UsageHabitEntity>

    @Query("SELECT * FROM usage_habits WHERE packageName = :packageName AND hourOfDay = :hourOfDay AND dayOfWeek = :dayOfWeek LIMIT 1")
    suspend fun findHabit(packageName: String, hourOfDay: Int, dayOfWeek: Int): UsageHabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHabit(habit: UsageHabitEntity)

    @Query("SELECT SUM(launchCount) FROM usage_habits")
    fun getTotalLaunchesFlow(): Flow<Int?>

    @Query("DELETE FROM usage_habits")
    suspend fun clearAllHabits()
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettingsEntity)
}

@Dao
interface WidgetDao {
    @Query("SELECT * FROM launcher_widgets ORDER BY orderIndex ASC")
    fun getAllWidgetsFlow(): Flow<List<LauncherWidgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWidget(widget: LauncherWidgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWidgets(widgets: List<LauncherWidgetEntity>)

    @Query("DELETE FROM launcher_widgets WHERE id = :id")
    suspend fun deleteWidget(id: String)
}
