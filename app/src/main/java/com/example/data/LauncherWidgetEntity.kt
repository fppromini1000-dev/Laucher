package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launcher_widgets")
data class LauncherWidgetEntity(
    @PrimaryKey
    val id: String,
    val typeId: String,
    val orderIndex: Int,
    val isEnabled: Boolean = true,
    val customData: String = ""
)
