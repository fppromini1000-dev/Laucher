package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

class ScreenStateReceiver(
    private val onScreenOn: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        Log.d("ScreenStateReceiver", "Action received: $action")
        if (Intent.ACTION_SCREEN_ON == action) {
            onScreenOn()
        }
    }

    companion object {
        fun register(context: Context, onScreenOn: () -> Unit): ScreenStateReceiver {
            val receiver = ScreenStateReceiver(onScreenOn)
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            context.registerReceiver(receiver, filter)
            return receiver
        }

        fun unregister(context: Context, receiver: ScreenStateReceiver?) {
            if (receiver != null) {
                try {
                    context.unregisterReceiver(receiver)
                } catch (e: Exception) {
                    Log.e("ScreenStateReceiver", "Error unregistering receiver", e)
                }
            }
        }
    }
}
