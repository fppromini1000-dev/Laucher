package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_customizations")
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val categoryId: String,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val customOrder: Int = 0
)
