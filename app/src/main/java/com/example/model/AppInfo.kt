package com.example.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val activityName: String = "",
    val label: String,
    val category: AppCategory = AppCategory.OTHER,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val customOrder: Int = 0,
    val launchCount: Int = 0,
    val lastUsedTimestamp: Long = 0L,
    val isSimulated: Boolean = false,
    val iconDrawable: Drawable? = null
)
