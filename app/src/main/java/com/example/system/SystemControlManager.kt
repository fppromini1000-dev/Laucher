package com.example.system

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SystemControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // --- Estados reactivos ---
    var isTorchOn by mutableStateOf(false)
        private set

    var isDndEnabled by mutableStateOf(false)
        private set

    var isAutoRotateOn by mutableStateOf(true)
        private set

    var brightnessFloat by mutableFloatStateOf(0.7f) // 0.0f a 1.0f
        private set

    var musicVolumeFloat by mutableFloatStateOf(0.6f) // 0.0f a 1.0f
        private set

    var isWifiEnabled by mutableStateOf(true)
        private set

    var isBluetoothEnabled by mutableStateOf(true)
        private set

    // Torch camera id
    private var torchCameraId: String? = null

    init {
        initTorch()
        refreshSettingsState()
    }

    private fun initTorch() {
        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash) {
                    torchCameraId = id
                    break
                }
            }

            cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == torchCameraId) {
                        isTorchOn = enabled
                    }
                }
            }, null)
        } catch (_: Exception) {}
    }

    fun refreshSettingsState() {
        // Brillo
        try {
            val curBrightnessInt = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                160
            )
            brightnessFloat = (curBrightnessInt / 255f).coerceIn(0.05f, 1.0f)
        } catch (_: Exception) {}

        // Volumen
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            musicVolumeFloat = if (maxVol > 0) (curVol.toFloat() / maxVol).coerceIn(0f, 1f) else 0.5f
        } catch (_: Exception) {}

        // Rotación
        try {
            val rotate = Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                1
            )
            isAutoRotateOn = rotate == 1
        } catch (_: Exception) {}

        // No molestar
        try {
            isDndEnabled = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } catch (_: Exception) {}
    }

    // --- Comprobación de Permisos ---

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    fun hasNotificationPolicyAccess(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    // --- Solicitudes de Permisos y Ajustes de Android ---

    fun openOverlaySettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun openWriteSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun openSecuritySettings() {
        try {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun openNotificationPolicySettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        return com.example.service.VolumeAccessibilityService.isAccessibilityEnabled(context)
    }

    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    // --- Acciones de Hardware & Control del Sistema ---

    fun toggleTorch(): Boolean {
        val target = !isTorchOn
        val camId = torchCameraId ?: return false
        try {
            cameraManager.setTorchMode(camId, target)
            isTorchOn = target
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun setBrightness(value: Float, activity: Activity? = null) {
        val clamped = value.coerceIn(0.05f, 1.0f)
        brightnessFloat = clamped

        // 1. Aplicar en la ventana activa de inmediato
        activity?.let {
            val lp = it.window.attributes
            lp.screenBrightness = clamped
            it.window.attributes = lp
        }

        // 2. Persistir en el sistema si se tiene el permiso WRITE_SETTINGS
        if (canWriteSettings()) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                val intVal = (clamped * 255).toInt().coerceIn(10, 255)
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    intVal
                )
            } catch (_: Exception) {}
        }
    }

    fun setMusicVolume(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        musicVolumeFloat = clamped
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = (clamped * maxVol).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        } catch (_: Exception) {}
    }

    fun toggleAutoRotate(): Boolean {
        if (!canWriteSettings()) {
            openWriteSettings()
            return false
        }
        val target = if (isAutoRotateOn) 0 else 1
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                target
            )
            isAutoRotateOn = target == 1
            true
        } catch (e: Exception) {
            false
        }
    }

    fun toggleDnd(): Boolean {
        if (!hasNotificationPolicyAccess()) {
            openNotificationPolicySettings()
            return false
        }
        return try {
            val target = if (isDndEnabled) {
                NotificationManager.INTERRUPTION_FILTER_ALL
            } else {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            }
            notificationManager.setInterruptionFilter(target)
            isDndEnabled = target != NotificationManager.INTERRUPTION_FILTER_ALL
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openWifiSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.startActivity(Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    fun openBluetoothSettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    fun openCamera() {
        try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }
}
