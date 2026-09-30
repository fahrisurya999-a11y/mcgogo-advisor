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

    private var wm: WindowManager? = null
    private var root: View? = null
    private var banner: TextView? = null
    private val slots = mutableListOf<TextView>()
    private val h = Handler(Looper.getMainLooper())
    private var tick = 0
    private var alive = false

    private val heroes = listOf("Granger","Karrie","Lancelot","Angela","Atlas")
    private val verdicts = listOf("BUY","SAVE","PASS","BUY","SAVE")
    private val colors = listOf("#15803D","#A16207","#334155","#15803D","#A16207")

    private val runner = object : Runnable {
        override fun run() {
            if (!alive) return
            tick++
            banner?.text = "MCGG Advisor #$tick"
            for (i in 0 until 5) {
                val tv = slots.getOrNull(i) ?: continue
                tv.text = heroes[i] + "\n" + verdicts[i]
                tv.setBackgroundColor(Color.parseColor(colors[i]))
                tv.setTextColor(Color.WHITE)
            }
            h.postDelayed(this, 2000)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notif()
        buildOverlay()
        alive = true
        h.postDelayed(runner, 500)
    }

    override fun onDestroy() {
        alive = false
        h.removeCallbacks(runner)
        try { root?.let { wm?.removeView(it) } } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun notif() {
        val ch = "mcgg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val c = NotificationChannel(ch, "MCGG", NotificationManager.IMPORTANCE_MIN)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(c)
        }
        val n: Notification = NotificationCompat.Builder(this, ch)
            .setContentTitle("MCGG Advisor")
            .setContentText("Overlay aktif")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(1, n)
    }

    private fun buildOverlay() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).also { it.gravity = Gravity.TOP or Gravity.START; it.x = 24; it.y = 200 }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E8111827"))
            setPadding(18, 12, 18, 12)
        }

        var ix = 0; var iy = 0; var tx = 0f; var ty = 0f
        val handle = TextView(this).apply {
            text = "■ MCGG Advisor"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 12f; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(14, 10, 14, 10)
            setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { ix = lp.x; iy = lp.y; tx = e.rawX; ty = e.rawY; true }
                    MotionEvent.ACTION_MOVE -> {
                        lp.x = ix + (e.rawX - tx).toInt()
                        lp.y = iy + (e.rawY - ty).toInt()
                        try { wm?.updateViewLayout(root, lp) } catch (_: Exception) {}
                        true
                    }
                    else -> false
                }
            }
        }
        container.addView(handle)

        banner = TextView(this).apply {
            text = "Memuat..."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 10f; gravity = Gravity.CENTER; setPadding(0, 4, 0, 4)
        }
        container.addView(banner)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 5f }
        slots.clear()
        for (i in 0 until 5) {
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { setMargins(2, 4, 2, 0) }
                text = "S${i+1}\n--"; textSize = 9f; gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(3, 6, 3, 6)
            }
            slots.add(tv); row.addView(tv)
        }
        container.addView(row)
        root = container
        wm?.addView(root, lp)
    }
}
