package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.example.ui.system.CometVolumeView

/**
 * Servicio de accesibilidad para interceptar las teclas físicas de volumen (Subir/Bajar)
 * en TODAS las aplicaciones (TikTok, YouTube, Instagram, Netflix, Navegador, etc.).
 *
 * Al consumir el evento de tecla (retornando true) y ajustar el volumen con flag 0,
 * se evita por completo que Android muestre su diálogo de volumen predeterminado,
 * reemplazándolo al instante por la barra flotante CometOS de Firi Launcher directamente
 * mediante una ventana TYPE_ACCESSIBILITY_OVERLAY.
 */
class VolumeAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "VolumeAccessService"

        @Volatile
        var instance: VolumeAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun isAccessibilityEnabled(context: Context): Boolean {
            if (isRunning()) return true
            try {
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                if (am != null) {
                    val enabledList = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                    if (enabledList.any { 
                        it.resolveInfo?.serviceInfo?.packageName == context.packageName ||
                        it.id.contains(context.packageName)
                    }) {
                        return true
                    }
                }

                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false

                return enabledServices.contains(context.packageName) &&
                        (enabledServices.contains("VolumeAccessibilityService") || 
                         enabledServices.contains(VolumeAccessibilityService::class.java.simpleName))
            } catch (_: Exception) {
                return isRunning()
            }
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val fallback = android.content.Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
        }

        fun showVolume(context: Context, fraction: Float) {
            val inst = instance
            if (inst != null) {
                inst.showOverlay(fraction)
            } else {
                VolumeOverlayService.showVolume(context, fraction, userInitiated = true)
            }
        }
    }

    private var audioManager: AudioManager? = null
    private var windowManager: WindowManager? = null
    private var volumeView: CometVolumeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isOverlayAttached = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        Log.d(TAG, "VolumeAccessibilityService connected successfully")

        // El archivo XML volume_accessibility_service_config.xml provee
        // canRequestFilterKeyEvents="true" y flagRequestFilterKeyEvents.
        // Solo añadimos flags si no estuvieran presentes para evitar sobreescribir capacidades XML.
        try {
            val currentInfo = serviceInfo
            if (currentInfo != null) {
                val neededFlags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                if ((currentInfo.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) == 0) {
                    currentInfo.flags = currentInfo.flags or neededFlags
                    serviceInfo = currentInfo
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to verify or update serviceInfo flags", e)
        }

        mainHandler.post {
            ensureOverlayAttached()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No procesamos eventos de accesibilidad regulares
    }

    override fun onInterrupt() {
        Log.w(TAG, "VolumeAccessibilityService interrupted")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || 
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || 
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {

            val isUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP

            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                    handleHardwareMuteKey()
                } else {
                    handleHardwareVolumeKey(isUp)
                }
            }

            // CONSUMIR EL EVENTO (return true) en ACTION_DOWN, ACTION_UP y repetidos.
            // Esto es crucial: al retornar true, Android NO pasa la pulsación al AudioService del sistema
            // ni a la app de primer plano (TikTok, YouTube, etc.), por lo que la barra del sistema NUNCA aparece.
            return true
        }

        return super.onKeyEvent(event)
    }

    private fun handleHardwareVolumeKey(isUp: Boolean) {
        val am = audioManager ?: (getSystemService(Context.AUDIO_SERVICE) as? AudioManager) ?: return

        try {
            val direction = if (isUp) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER

            // Flag 0 asegura que el sistema NO muestre su diálogo de volumen predeterminado
            val targetStream = if (am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION) {
                AudioManager.STREAM_VOICE_CALL
            } else {
                AudioManager.STREAM_MUSIC
            }

            am.adjustStreamVolume(targetStream, direction, 0)

            val cur = am.getStreamVolume(targetStream)
            val max = am.getStreamMaxVolume(targetStream)
            val fraction = if (max > 0) cur.toFloat() / max.toFloat() else 0.5f

            // Mostrar inmediatamente la barra personalizada CometOS de Firi en la pantalla
            showOverlay(fraction)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling hardware volume key in accessibility service", e)
        }
    }

    private fun handleHardwareMuteKey() {
        val am = audioManager ?: (getSystemService(Context.AUDIO_SERVICE) as? AudioManager) ?: return
        try {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0)
            val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val fraction = if (max > 0) cur.toFloat() / max.toFloat() else 0f
            showOverlay(fraction)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling hardware mute key", e)
        }
    }

    fun showOverlay(fraction: Float) {
        mainHandler.post {
            try {
                ensureOverlayAttached()
                setWindowTouchable(true)
                volumeView?.updateVolume(fraction, userInitiated = true)
                volumeView?.reveal(expandFully = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing accessibility volume overlay", e)
            }
        }
    }

    private fun setWindowTouchable(touchable: Boolean) {
        val lp = layoutParams ?: return
        val wm = windowManager ?: return
        val v = volumeView ?: return
        val oldFlags = lp.flags
        val newFlags = if (touchable) {
            oldFlags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            oldFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        if (oldFlags != newFlags) {
            lp.flags = newFlags
            try {
                wm.updateViewLayout(v, lp)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update layoutParams flags", e)
            }
        }
    }

    private fun ensureOverlayAttached() {
        if (isOverlayAttached && volumeView != null) return

        val wm = windowManager ?: (getSystemService(Context.WINDOW_SERVICE) as? WindowManager) ?: return
        val am = audioManager ?: (getSystemService(Context.AUDIO_SERVICE) as? AudioManager)

        volumeView = CometVolumeView(
            context = this,
            onVolumeChangedByUser = { fraction ->
                try {
                    val max = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                    val target = (fraction * max).toInt().coerceIn(0, max)
                    // Flag 0 asegura que no salte el diálogo nativo de Android al deslizar con el dedo
                    am?.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set volume from overlay", e)
                }
            },
            onDismissRequested = {
                // Hacer la ventana transparente e insensible a toques cuando se oculta
                setWindowTouchable(false)
            }
        )

        val dm = resources.displayMetrics
        val density = dm.density
        val totalHeight = (192f * density).toInt()
        val expandedWidth = (52f * density).toInt()

        layoutParams = WindowManager.LayoutParams(
            expandedWidth,
            totalHeight,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // Inicialmente no touchable porque está GONE
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = (dm.heightPixels * 0.24f).toInt()
        }

        try {
            volumeView?.visibility = View.GONE
            wm.addView(volumeView, layoutParams)
            isOverlayAttached = true
            windowManager = wm
            Log.d(TAG, "Successfully attached TYPE_ACCESSIBILITY_OVERLAY to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching TYPE_ACCESSIBILITY_OVERLAY", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (isOverlayAttached && volumeView != null && windowManager != null) {
            try {
                windowManager?.removeView(volumeView)
            } catch (_: Exception) {}
            isOverlayAttached = false
            volumeView = null
        }
        Log.d(TAG, "VolumeAccessibilityService destroyed")
    }
}
