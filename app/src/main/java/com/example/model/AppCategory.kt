package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppCategory(
    val id: String,
    val labelEs: String,
    val labelEn: String,
    val icon: ImageVector,
    val keywords: List<String>
) {
    PRODUCTIVITY(
        id = "productivity",
        labelEs = "Productividad",
        labelEn = "Productivity",
        icon = Icons.Default.Work,
        keywords = listOf(
            "mail", "correo", "gmail", "outlook", "notes", "notas", "calendar", "calendario",
            "docs", "sheets", "slides", "word", "excel", "powerpoint", "drive", "notion",
            "trello", "todo", "task", "keep", "slack", "teams", "zoom", "meet", "pdf", "office"
        )
    ),
    SOCIAL(
        id = "social",
        labelEs = "Social",
        labelEn = "Social",
        icon = Icons.AutoMirrored.Filled.Chat,
        keywords = listOf(
            "whatsapp", "telegram", "instagram", "facebook", "twitter", "x", "tiktok",
            "messenger", "snapchat", "discord", "signal", "reddit", "threads", "chat",
            "wechat", "linkedin", "social"
        )
    ),
    ENTERTAINMENT(
        id = "entertainment",
        labelEs = "Entretenimiento",
        labelEn = "Entertainment",
        icon = Icons.Default.Headphones,
        keywords = listOf(
            "youtube", "netflix", "spotify", "music", "musica", "disney", "prime",
            "hbo", "max", "twitch", "podcast", "radio", "video", "player", "stream",
            "crunchyroll", "deezer", "sound"
        )
    ),
    UTILITIES(
        id = "utilities",
        labelEs = "Utilidades",
        labelEn = "Utilities",
        icon = Icons.Default.Build,
        keywords = listOf(
            "calculator", "calculadora", "clock", "reloj", "alarm", "alarma", "settings",
            "ajustes", "config", "files", "archivos", "camera", "camara", "gallery",
            "galeria", "browser", "chrome", "firefox", "edge", "weather", "clima", "map",
            "maps", "compass", "flashlight", "linterna", "cleaner", "scan"
        )
    ),
    FINANCE(
        id = "finance",
        labelEs = "Finanzas",
        labelEn = "Finance",
        icon = Icons.Default.Payments,
        keywords = listOf(
            "bank", "banco", "pay", "pago", "wallet", "billetera", "crypto", "bitcoin",
            "paypal", "revolut", "bbva", "santander", "caixa", "mercado", "money",
            "dinero", "finance", "finanzas", "tarjeta", "card", "stocks", "invest"
        )
    ),
    HEALTH(
        id = "health",
        labelEs = "Salud y Fitness",
        labelEn = "Health & Fitness",
        icon = Icons.Default.Favorite,
        keywords = listOf(
            "fit", "health", "salud", "run", "correr", "gym", "gimnasio", "workout",
            "meditation", "meditacion", "sleep", "sueno", "water", "agua", "step", "pasos",
            "strava", "yoga", "calm", "headspace", "diet", "dieta"
        )
    ),
    CREATIVITY(
        id = "creativity",
        labelEs = "Creatividad",
        labelEn = "Creativity",
        icon = Icons.Default.Palette,
        keywords = listOf(
            "canva", "photo", "foto", "editor", "paint", "dibujo", "art", "arte",
            "lightroom", "photoshop", "snapseed", "vsco", "capcut", "illustrator",
            "design", "diseno", "audio", "recorder", "studio"
        )
    ),
    GAMES(
        id = "games",
        labelEs = "Juegos",
        labelEn = "Games",
        icon = Icons.Default.Games,
        keywords = listOf(
            "game", "juego", "play", "puzzle", "arcade", "rpg", "chess", "ajedrez",
            "clash", "candy", "subway", "roblox", "minecraft", "pokemon", "action"
        )
    ),
    OTHER(
        id = "other",
        labelEs = "Otros",
        labelEn = "Other",
        icon = Icons.Default.Category,
        keywords = emptyList()
    );

    fun getDisplayName(isSpanish: Boolean): String {
        return if (isSpanish) labelEs else labelEn
    }

    companion object {
        fun fromId(id: String): AppCategory {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }
    }
}
