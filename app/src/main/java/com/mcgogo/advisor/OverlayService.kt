package com.mcgogo.advisor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var bannerText: TextView? = null
    private val slotViews = mutableListOf<TextView>()
    private val handler = Handler(Looper.getMainLooper())
    private var tickCount = 0
    private var isRunning = false
    private val heroes = listOf("Granger","Karrie","Lancelot","Angela","Atlas")
    private val verdicts = listOf("BUY","SAVE","PASS","BUY","SAVE")
    private val bgColors = listOf("#15803D","#A16207","#334155","#15803D","#A16207")
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            tickCount++
            bannerText?.text = "MCGG Advisor Live #$tickCount"
            for (i in 0 until 5) {
                val tv = slotViews.getOrNull(i) ?: continue
                tv.text = heroes[i] + "\n" + verdicts[i]
                tv.setBackgroundColor(Color.parseColor(bgColors[i]))
                tv.setTextColor(Color.WHITE)
            }
            handler.postDelayed(this, 2000)
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        setupOverlay()
        isRunning = true
        handler.post(tickRunnable)
    }
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(tickRunnable)
        try { overlayView?.let { windowManager?.removeView(it) } } catch (e: Exception) {}
    }
    private fun startForegroundNotification() {
        val channelId = "mcgogo_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "MCGG Advisor", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MCGG Advisor")
            .setContentText("Overlay aktif")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, notif)
    }
    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 120 }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F0111827"))
            setPadding(20, 14, 20, 14)
        }
        var ix = 0; var iy = 0; var tx = 0f; var ty = 0f
        val handle = TextView(this).apply {
            text = "MCGG Advisor - Geser di sini"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 13f; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(16, 10, 16, 10)
            setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { ix = params.x; iy = params.y; tx = e.rawX; ty = e.rawY; true }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = ix + (e.rawX - tx).toInt(); params.y = iy + (e.rawY - ty).toInt()
                        try { windowManager?.updateViewLayout(overlayView, params) } catch (e: Exception) {}
                        true
                    }
                    else -> false
                }
            }
        }
        root.addView(handle)
        bannerText = TextView(this).apply {
            text = "Memuat..."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 11f; gravity = Gravity.CENTER; setPadding(0, 6, 0, 6)
        }
        root.addView(bannerText)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 5f }
        slotViews.clear()
        for (i in 0 until 5) {
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(3, 4, 3, 0) }
                text = "Slot " + (i+1).toString() + "\n--"
                textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1E293B")); setPadding(4, 8, 4, 8)
            }
            slotViews.add(tv); row.addView(tv)
        }
        root.addView(row)
        overlayView = root
        windowManager?.addView(overlayView, params)
    }
}
