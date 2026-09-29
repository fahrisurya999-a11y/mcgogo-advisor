package com.mcgogo.advisor

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var bannerText: TextView? = null
    private val slotViews = mutableListOf<TextView>()

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var db: MetaDB? = null
    private var advisor: Advisor? = null
    private var matcher: PureImageMatcher? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        setupOverlayView()
        loadMetaAndAssets()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val dataIntent = intent?.getParcelableExtra<Intent>("DATA_INTENT")

        if (resultCode == Activity.RESULT_OK && dataIntent != null) {
            startScreenCapture(resultCode, dataIntent)
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "mcgogo_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MCGG Advisor Running",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MCGG Advisor Active")
            .setContentText("Overlay scanner sedang memantau rekomendasi toko...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(101, notification)
    }

    private fun setupOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val paramsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            paramsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 50
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EE111827"))
            setPadding(32, 24, 32, 24)
        }

        bannerText = TextView(this).apply {
            text = "⚡ MCGG Advisor: Menunggu Toko Terbuka..."
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        container.addView(bannerText)

        val slotsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 5f
            setPadding(0, 12, 0, 0)
        }

        slotViews.clear()
        for (i in 1..5) {
            val slotText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(4, 0, 4, 0)
                }
                text = "Slot $i\n--"
                setTextColor(Color.WHITE)
                textSize = 11f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#334455"))
                setPadding(4, 8, 4, 8)
            }
            slotViews.add(slotText)
            slotsRow.addView(slotText)
        }
        container.addView(slotsRow)

        overlayView = container
        windowManager?.addView(overlayView, params)
    }

    private fun loadMetaAndAssets() {
        scope.launch {
            try {
                val heroesJson = assets.open("data/heroes.json").bufferedReader().use { it.readText() }
                val traitsJson = assets.open("data/traits.json").bufferedReader().use { it.readText() }
                val loadedDb = MetaDB.load(heroesJson, traitsJson)
                db = loadedDb
                advisor = Advisor(loadedDb)

                val bitmaps = mutableMapOf<String, Bitmap>()
                for (hid in loadedDb.heroes.keys) {
                    try {
                        assets.open("heroes/$hid.png").use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) bitmaps[hid] = bmp
                        }
                    } catch (_: Exception) {}
                }
                matcher = PureImageMatcher(loadedDb, bitmaps)

                withContext(Dispatchers.Main) {
                    bannerText?.text = "⚡ Scanner Siap (${bitmaps.size} Hero Dimuat) — Memantau Layar..."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    bannerText?.text = "Error load data: ${e.message}"
                }
            }
        }
    }

    private fun startScreenCapture(resultCode: Int, data: Intent) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, data)

        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "MCGGScanDisplay",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        scanJob?.cancel()
        scanJob = scope.launch {
            while (isActive) {
                delay(1500)
                processLatestScreen()
            }
        }
    }

    private suspend fun processLatestScreen() {
        val reader = imageReader ?: return
        val currentMatcher = matcher ?: return
        val currentAdvisor = advisor ?: return

        var bitmap: Bitmap? = null
        try {
            val image = reader.acquireLatestImage() ?: return
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            val scannedCards = currentMatcher.scanShop(bitmap)
            val identifiedIds = scannedCards.mapNotNull { it.heroId }

            if (identifiedIds.isNotEmpty()) {
                val rec = currentAdvisor.recommend(emptyList(), identifiedIds)
                withContext(Dispatchers.Main) {
                    updateOverlayUI(scannedCards, rec)
                }
            }
        } catch (_: Exception) {
        } finally {
            bitmap?.recycle()
        }
    }

    private fun updateOverlayUI(cards: List<ScannedCard>, rec: Recommendation) {
        bannerText?.text = "⚡ Target: ${rec.targetComp}"

        for (i in 0 until 5) {
            val tv = slotViews.getOrNull(i) ?: continue
            val card = cards.getOrNull(i)
            if (card != null && card.heroId != null) {
                val advice = rec.advices.firstOrNull { it.hero.id == card.heroId }
                val verdict = advice?.verdict ?: "PASS"
                tv.text = "${card.heroName}\n$verdict"

                when (verdict) {
                    "BUY" -> {
                        tv.setBackgroundColor(Color.parseColor("#15803D"))
                        tv.setTextColor(Color.WHITE)
                    }
                    "SAVE" -> {
                        tv.setBackgroundColor(Color.parseColor("#A16207"))
                        tv.setTextColor(Color.WHITE)
                    }
                    else -> {
                        tv.setBackgroundColor(Color.parseColor("#334155"))
                        tv.setTextColor(Color.parseColor("#94A3B8"))
                    }
                }
            } else {
                tv.text = "Slot ${i + 1}\n--"
                tv.setBackgroundColor(Color.parseColor("#1E293B"))
                tv.setTextColor(Color.parseColor("#64748B"))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        overlayView?.let { windowManager?.removeView(it) }
    }
}
