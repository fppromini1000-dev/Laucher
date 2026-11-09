package com.example.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.ui.system.CometVolumeView

class VolumeOverlayService : Service() {

    companion object {
        private const val TAG = "VolumeOverlayService"
        const val ACTION_SHOW_TEST = "com.example.service.ACTION_SHOW_TEST"
        const val ACTION_TRIGGER_VOLUME = "com.example.service.ACTION_TRIGGER_VOLUME"
        const val EXTRA_FRACTION = "extra_fraction"

        @Volatile
        var currentInstance: VolumeOverlayService? = null
            private set

        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, VolumeOverlayService::class.java)
                try {
                    context.startService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting VolumeOverlayService", e)
                }
            }
        }

        fun showVolume(context: Context, fraction: Float, userInitiated: Boolean = true) {
            if (VolumeAccessibilityService.instance != null) {
                VolumeAccessibilityService.instance?.showOverlay(fraction)
                return
            }

            val instance = currentInstance
            if (instance != null) {
                instance.displayVolume(fraction, userInitiated)
            } else {
                if (Settings.canDrawOverlays(context)) {
                    val intent = Intent(context, VolumeOverlayService::class.java).apply {
                        action = ACTION_TRIGGER_VOLUME
                        putExtra(EXTRA_FRACTION, fraction)
                    }
                    try {
                        context.startService(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting VolumeOverlayService for volume event", e)
                    }
                }
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var audioManager: AudioManager? = null
    private var volumeView: CometVolumeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isViewAttached = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val volumeContentObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            checkVolumeAndShow(userInitiated = false)
        }
    }

    private val volumeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                checkVolumeAndShow(userInitiated = false)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        currentInstance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        initVolumeOverlay()
        registerListeners()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_TEST -> {
                checkVolumeAndShow(userInitiated = false)
            }
            ACTION_TRIGGER_VOLUME -> {
                val frac = intent.getFloatExtra(EXTRA_FRACTION, -1f)
                if (frac >= 0f) {
                    displayVolume(frac, userInitiated = true)
                } else {
                    checkVolumeAndShow(userInitiated = true)
                }
            }
        }
        return START_STICKY
    }

    private fun initVolumeOverlay() {
        if (!Settings.canDrawOverlays(this) || windowManager == null) return

        val am = audioManager ?: return

        volumeView = CometVolumeView(
            context = this,
            onVolumeChangedByUser = { fraction ->
                try {
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val target = (fraction * max).toInt().coerceIn(0, max)
                    // 0 flag ensures the native Android volume dialog is never invoked
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set volume", e)
                }
            },
            onDismissRequested = {
                // View collapsed into edge, ready for next trigger
            }
        )

        val dm = resources.displayMetrics
        val density = dm.density
        val totalHeight = (192f * density).toInt()
        val expandedWidth = (52f * density).toInt()
        val initialY = (dm.heightPixels * 0.24f).toInt()

        layoutParams = WindowManager.LayoutParams(
            expandedWidth,
            totalHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = initialY
        }

        try {
            volumeView?.visibility = android.view.View.GONE
            windowManager?.addView(volumeView, layoutParams)
            isViewAttached = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add volume overlay to WindowManager", e)
            isViewAttached = false
        }
    }

    private fun registerListeners() {
        try {
            contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeContentObserver
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register content observer", e)
        }

        try {
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                    this,
                    volumeBroadcastReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(volumeBroadcastReceiver, filter)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register volume receiver", e)
        }
    }

    fun displayVolume(fraction: Float, userInitiated: Boolean) {
        if (VolumeAccessibilityService.isRunning()) {
            VolumeAccessibilityService.instance?.showOverlay(fraction)
            return
        }
        if (!Settings.canDrawOverlays(this)) return

        if (!isViewAttached || volumeView == null) {
            initVolumeOverlay()
        }

        volumeView?.post {
            volumeView?.updateVolume(fraction, userInitiated)
            volumeView?.reveal(expandFully = true)
        }
    }

    private fun checkVolumeAndShow(userInitiated: Boolean) {
        if (VolumeAccessibilityService.isRunning()) return
        val am = audioManager ?: return
        if (!Settings.canDrawOverlays(this)) return

        if (!isViewAttached || volumeView == null) {
            initVolumeOverlay()
        }

        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val fraction = if (max > 0) cur.toFloat() / max.toFloat() else 0.5f

        displayVolume(fraction, userInitiated)
    }

    override fun onDestroy() {
        super.onDestroy()
        currentInstance = null
        try {
            contentResolver.unregisterContentObserver(volumeContentObserver)
            unregisterReceiver(volumeBroadcastReceiver)
        } catch (_: Exception) {}

        if (isViewAttached && volumeView != null && windowManager != null) {
            try {
                windowManager?.removeView(volumeView)
            } catch (_: Exception) {}
            isViewAttached = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
