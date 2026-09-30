package com.mcgogo.advisor

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val OVERLAY_PERMISSION_REQ_CODE = 1001
    private val SCREEN_CAPTURE_REQ_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStart.setOnClickListener {
            if (checkOverlayPermission()) {
                requestScreenCapture()
            } else {
                requestOverlayPermission()
            }
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Overlay dimatikan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        }
    }

    private fun requestScreenCapture() {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQ_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (checkOverlayPermission()) {
                requestScreenCapture()
            } else {
                Toast.makeText(this, "Izin Tampilkan di Atas Layar dibutuhkan!", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == SCREEN_CAPTURE_REQ_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                startOverlayService(resultCode, data)
            } else {
                Toast.makeText(this, "Izin rekam layar ditolak!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startOverlayService(resultCode: Int, data: Intent) {
        try {
            val intent = Intent(this, OverlayService::class.java).apply {
                putExtra("RESULT_CODE", resultCode)
                putExtra("DATA_INTENT", data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "MCGG Overlay & Scanner Aktif!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
        }
    }
}
