package com.mcgogo.advisor

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val tv = findViewById<TextView>(R.id.tvStatus)
        if (canDrawOverlays()) {
            tv.text = "Izin overlay: OK\nTap Mulai Overlay untuk aktifkan"
        } else {
            tv.text = "Izin overlay BELUM diberikan\nTap Minta Izin dulu, lalu kembali ke sini"
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnPermission).setOnClickListener {
            if (!canDrawOverlays()) {
                requestOverlayPermission()
            } else {
                Toast.makeText(this, "Izin sudah OK! Tap Mulai Overlay", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!canDrawOverlays()) {
                requestOverlayPermission()
                return@setOnClickListener
            }
            val intent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Overlay aktif! Buka game MCGG sekarang", Toast.LENGTH_LONG).show()
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Overlay dimatikan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(this)
        else true
    }

    private fun requestOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        Toast.makeText(this, "Aktifkan 'Tampilkan di atas aplikasi lain' lalu kembali", Toast.LENGTH_LONG).show()
    }
}
