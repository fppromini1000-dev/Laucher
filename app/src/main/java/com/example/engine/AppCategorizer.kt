package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.example.model.AppCategory
import com.example.model.AppInfo

object AppCategorizer {

    fun categorize(packageName: String, label: String, appInfo: ApplicationInfo?): AppCategory {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = label.lowercase()

        // 1. Check system ApplicationInfo category (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo != null) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO -> return AppCategory.ENTERTAINMENT
                ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.CREATIVITY
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_NEWS,
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_MAPS -> return AppCategory.UTILITIES
            }
        }

        // 2. Keyword heuristic matching against known apps
        for (category in AppCategory.entries) {
            for (kw in category.keywords) {
                if (lowerPkg.contains(kw) || lowerLabel.contains(kw)) {
                    return category
                }
            }
        }

        // 3. Fallback defaults based on common prefixes
        if (lowerPkg.contains("android.intent") || lowerPkg.contains("system") || lowerPkg.contains("service")) {
            return AppCategory.UTILITIES
        }

        return AppCategory.OTHER
    }

    fun loadInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        val myPackage = context.packageName
        val apps = mutableListOf<AppInfo>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == myPackage) continue // Skip our own launcher

            val label = resolveInfo.loadLabel(pm).toString()
            val activityName = resolveInfo.activityInfo.name
            val icon = resolveInfo.loadIcon(pm)
            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (e: Exception) {
                null
            }

            val category = categorize(pkg, label, appInfo)
            apps.add(
                AppInfo(
                    packageName = pkg,
                    activityName = activityName,
                    label = label,
                    category = category,
                    iconDrawable = icon,
                    isSimulated = false
                )
            )
        }

        // Siempre incluir Firi AI como aplicación nativa integrada
        apps.add(
            0,
            AppInfo(
                packageName = "com.firi.ai",
                activityName = "com.example.MainActivity",
                label = "Firi AI",
                category = AppCategory.PRODUCTIVITY,
                iconDrawable = null,
                isSimulated = false,
                isPinned = true
            )
        )

        // If very few apps exist (like clean emulator), enrich with popular mock companion apps
        // so the user experiences the full automatic categorization immediately
        if (apps.size < 6) {
            val sampleApps = getSampleApps()
            for (sample in sampleApps) {
                if (apps.none { it.packageName == sample.packageName }) {
                    apps.add(sample)
                }
            }
        }

        return apps.sortedWith(compareBy({ it.category.ordinal }, { it.label.lowercase() }))
    }

    fun getSampleApps(): List<AppInfo> {
        return listOf(
            AppInfo(
                packageName = "com.firi.ai",
                activityName = "com.example.MainActivity",
                label = "Firi AI",
                category = AppCategory.PRODUCTIVITY,
                iconDrawable = null,
                isSimulated = false,
                isPinned = true
            ),
            AppInfo(
                packageName = "com.google.android.gm",
                label = "Gmail",
                category = AppCategory.PRODUCTIVITY,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.google.android.calendar",
                label = "Google Calendar",
                category = AppCategory.PRODUCTIVITY,
                isSimulated = true
            ),
            AppInfo(
                packageName = "notion.id",
                label = "Notion Workspace",
                category = AppCategory.PRODUCTIVITY,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.whatsapp",
                label = "WhatsApp",
                category = AppCategory.SOCIAL,
                isSimulated = true
            ),
            AppInfo(
                packageName = "org.telegram.messenger",
                label = "Telegram",
                category = AppCategory.SOCIAL,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.instagram.android",
                label = "Instagram",
                category = AppCategory.SOCIAL,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.spotify.music",
                label = "Spotify",
                category = AppCategory.ENTERTAINMENT,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.google.android.youtube",
                label = "YouTube",
                category = AppCategory.ENTERTAINMENT,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.netflix.mediaclient",
                label = "Netflix",
                category = AppCategory.ENTERTAINMENT,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.google.android.apps.photos",
                label = "Google Fotos",
                category = AppCategory.CREATIVITY,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.canva.editor",
                label = "Canva Design",
                category = AppCategory.CREATIVITY,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.android.chrome",
                label = "Chrome",
                category = AppCategory.UTILITIES,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.google.android.calculator",
                label = "Calculadora",
                category = AppCategory.UTILITIES,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.google.android.deskclock",
                label = "Reloj y Alarmas",
                category = AppCategory.UTILITIES,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.paypal.android.p2pmobile",
                label = "PayPal",
                category = AppCategory.FINANCE,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.google.android.apps.walletnfcrel",
                label = "Google Wallet",
                category = AppCategory.FINANCE,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.strava",
                label = "Strava Running",
                category = AppCategory.HEALTH,
                isSimulated = true
            ),
            AppInfo(
                packageName = "com.headspace.meditation",
                label = "Headspace",
                category = AppCategory.HEALTH,
                isSimulated = true
            )
        )
    }
}
