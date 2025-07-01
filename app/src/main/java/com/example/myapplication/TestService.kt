package com.example.myapplication

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Created by FPL on 09/05/2025.
 */

class TestService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("dungnt99", "onStartCommand zzzz")
        return super.onStartCommand(intent, flags, startId)
    }
}