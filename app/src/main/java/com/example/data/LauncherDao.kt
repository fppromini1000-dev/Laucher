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

    @Query("SELECT * FROM app_customizations WHERE isPinned = 1")
    suspend fun getPinnedCustomizations(): List<AppEntity>
}

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val totalLaunches: Int
)

@Dao
interface UsageHabitDao {
    @Query("SELECT * FROM usage_habits ORDER BY launchCount DESC")
    fun getAllHabits(): Flow<List<UsageHabitEntity>>

    @Query("SELECT * FROM usage_habits WHERE hourOfDay BETWEEN :startHour AND :endHour ORDER BY launchCount DESC LIMIT :limit")
    suspend fun getTopAppsForHourRange(startHour: Int, endHour: Int, limit: Int = 6): List<UsageHabitEntity>

    @Query("SELECT * FROM usage_habits WHERE hourOfDay = :hour ORDER BY launchCount DESC LIMIT :limit")
    suspend fun getTopAppsForHour(hour: Int, limit: Int = 6): List<UsageHabitEntity>

    @Query("SELECT packageName, appName, SUM(launchCount) as totalLaunches FROM usage_habits GROUP BY packageName ORDER BY totalLaunches DESC LIMIT :limit")
    suspend fun getMostUsedAppsOverall(limit: Int = 6): List<AppUsageStat>

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

    @Query("SELECT * FROM launcher_widgets ORDER BY orderIndex ASC")
    suspend fun getAllWidgetsSnapshot(): List<LauncherWidgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWidget(widget: LauncherWidgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWidgets(widgets: List<LauncherWidgetEntity>)

    @Query("DELETE FROM launcher_widgets WHERE id = :id")
    suspend fun deleteWidget(id: String)
}

@Dao
interface FiriSqlDao {
    @Query("SELECT * FROM firi_sql_memory ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int = 6): List<FiriMemoryEntity>

    @Query("SELECT * FROM firi_sql_memory WHERE userQuery LIKE '%' || :term || '%' OR keywords LIKE '%' || :term || '%' OR summaryResponse LIKE '%' || :term || '%' ORDER BY hitCount DESC, timestamp DESC LIMIT :limit")
    suspend fun searchMemories(term: String, limit: Int = 5): List<FiriMemoryEntity>

    @Query("SELECT * FROM firi_sql_memory WHERE LOWER(userQuery) = LOWER(:query) LIMIT 1")
    suspend fun findExactMemory(query: String): FiriMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMemory(memory: FiriMemoryEntity)

    @Query("SELECT COUNT(*) FROM firi_sql_memory")
    suspend fun getMemoryCount(): Int

    @Query("SELECT COUNT(*) FROM firi_sql_memory")
    fun getMemoryCountFlow(): Flow<Int>

    @Query("SELECT * FROM firi_saved_places ORDER BY lastSearchedAt DESC LIMIT :limit")
    suspend fun getRecentPlaces(limit: Int = 10): List<FiriSavedPlaceEntity>

    @Query("SELECT * FROM firi_saved_places ORDER BY lastSearchedAt DESC")
    fun getSavedPlacesFlow(): Flow<List<FiriSavedPlaceEntity>>

    @Query("SELECT * FROM firi_saved_places WHERE title LIKE '%' || :term || '%' OR address LIKE '%' || :term || '%' OR category LIKE '%' || :term || '%' ORDER BY searchCount DESC, lastSearchedAt DESC LIMIT :limit")
    suspend fun searchSavedPlaces(term: String, limit: Int = 6): List<FiriSavedPlaceEntity>

    @Query("SELECT * FROM firi_saved_places WHERE placeKey = :key LIMIT 1")
    suspend fun findPlaceByKey(key: String): FiriSavedPlaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedPlace(place: FiriSavedPlaceEntity)

    @Query("SELECT COUNT(*) FROM firi_saved_places")
    suspend fun getSavedPlacesCount(): Int

    @Query("SELECT COUNT(*) FROM firi_saved_places")
    fun getSavedPlacesCountFlow(): Flow<Int>

    @Query("DELETE FROM firi_sql_memory")
    suspend fun clearMemories()

    @Query("DELETE FROM firi_saved_places")
    suspend fun clearSavedPlaces()
}
