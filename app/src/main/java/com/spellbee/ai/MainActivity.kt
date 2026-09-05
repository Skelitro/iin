package com.spellbee.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private companion object {
        const val OVERLAY_PERMISSION_REQ_CODE = 1234
        const val RECORD_AUDIO_REQ_CODE = 5678
    }

    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildScreen())
        refreshStatus()
    }

    private fun buildScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(22))
            setBackgroundColor(0xFF101315.toInt())
        }

        val scroll = ScrollView(this).apply {
            addView(root)
            clipToPadding = false
        }

        val logo = TextView(this).apply {
            text = "🐝"
            textSize = 40f
            gravity = Gravity.CENTER
        }
        root.addView(logo, lp(match = true, height = dp(62)))

        val title = TextView(this).apply {
            text = "SpellBee AI"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(0xFFF7F4E9.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(title, lp(match = true, height = dp(44)))

        val subtitle = TextView(this).apply {
            text = "Speech spelling assistant • native floating companion"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(0x99F7F4E9.toInt())
        }
        root.addView(subtitle, lp(match = true, height = dp(46)))

        val card = MaterialCardView(this).apply {
            radius = dp(26).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(0xFF171C1E.toInt())
            strokeWidth = dp(1)
            strokeColor = 0x22FFC425
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, dp(10), 0, dp(12))
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(17), dp(17), dp(17))
        }

        statusView = TextView(this).apply {
            textSize = 12f
            setTextColor(0xCCF7F4E9.toInt())
        }
        content.addView(statusView, lp(match = true, height = dp(44)))

        val start = Button(this).apply {
            text = "LAUNCH FLOATING BUBBLE"
            isAllCaps = false
            textSize = 13f
            setTextColor(0xFF111111.toInt())
            setBackgroundColor(0xFFFFC425.toInt())
            setOnClickListener { launchOverlay() }
        }
        content.addView(start, lp(match = true, height = dp(54)))

        val stop = Button(this).apply {
            text = "Stop floating bubble"
            isAllCaps = false
            textSize = 12f
            setTextColor(0xCCF7F4E9.toInt())
            setBackgroundColor(0xFF242A2D.toInt())
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
                refreshStatus()
            }
        }
        content.addView(stop, lp(match = true, height = dp(52), top = 10))

        val note = TextView(this).apply {
            text = "Tip: grant microphone and “display over other apps” access once. The bubble can then be moved over other apps."
            textSize = 11f
            setTextColor(0x88F7F4E9.toInt())
            setPadding(0, dp(13), 0, 0)
        }
        content.addView(note, lp(match = true, height = dp(58)))

        card.addView(content)
        root.addView(card)

        val footer = TextView(this).apply {
            text = "Base phoneme rules run locally with Vosk speech recognition. Ultra AI suggestions are optional."
            textSize = 10.5f
            gravity = Gravity.CENTER
            setTextColor(0x66F7F4E9.toInt())
        }
        root.addView(footer, lp(match = true, height = dp(58), top = 4))

        return scroll
    }

    private fun launchOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQ_CODE
            )
            return
        }

        startOverlayService()
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        ContextCompat.startForegroundService(this, intent)
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    @Deprecated("Android callback API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            launchOverlay()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQ_CODE &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            startOverlayService()
        } else {
            refreshStatus()
        }
    }

    private fun refreshStatus() {
        if (!::statusView.isInitialized) return
        val overlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

        statusView.text = buildString {
            append("Permissions\n")
            append(if (overlay) "✓ Overlay access" else "○ Overlay access needed")
            append("    ")
            append(if (mic) "✓ Microphone" else "○ Microphone needed")
        }
    }

    private fun lp(match: Boolean, height: Int, top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(if (match) -1 else -2, height).apply {
            if (top > 0) setMargins(0, dp(top), 0, 0)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
