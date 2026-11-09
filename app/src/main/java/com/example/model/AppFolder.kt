package com.example.model

data class AppFolder(
    val id: String,
    val name: String,
    val appPackageNames: List<String>
)

sealed interface LauncherHomeItem {
    data class SingleApp(val app: AppInfo) : LauncherHomeItem
    data class Folder(
        val folder: AppFolder,
        val apps: List<AppInfo>
    ) : LauncherHomeItem
}
