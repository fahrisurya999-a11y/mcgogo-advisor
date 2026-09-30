package com.mcgogo.advisor

import android.content.Intent
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
        if (isAccessibilityEnabled()) {
            tv.text = "Accessibility Service: AKTIF\nOverlay sudah berjalan!\nBuka game MCGG sekarang."
        } else {
            tv.text = "Accessibility Service BELUM aktif\nTap tombol di bawah untuk aktifkan"
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnPermission).setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this,
                "Cari 'MCGG Advisor Overlay' dan aktifkan",
                Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!isAccessibilityEnabled()) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Aktifkan dulu di Accessibility Settings", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Overlay sudah aktif! Buka game MCGG", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Cari MCGG Advisor dan matikan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = packageName + "/" + OverlayService::class.java.canonicalName
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(service)
    }
}
