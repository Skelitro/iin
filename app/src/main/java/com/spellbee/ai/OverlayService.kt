package com.spellbee.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

class OverlayService : Service(), RecognitionListener {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var speechService: SpeechService? = null
    private var isUltraMode = true
    private var selectedLanguage = "English"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithMicType()
        setupFloatingView()
        initVosk()
    }

    private fun startForegroundWithMicType() {
        val channelId = "spellbee_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SpellBee AI microphone",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps SpellBee AI available while listening."
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("SpellBee AI is listening")
            .setContentText("Floating spelling assistant is active")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(1, notification)
        }
    }

    private fun setupFloatingView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_overlay, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 18
            y = 120
        }

        val bubble = floatingView.findViewById<View>(R.id.bubble_root)
        val modeBtn = floatingView.findViewById<Button>(R.id.toggle_mode_btn)
        val modeLabel = floatingView.findViewById<TextView>(R.id.mode_label)
        val langSpinner = floatingView.findViewById<Spinner>(R.id.language_spinner)

        val languages = arrayOf("English", "Spanish", "French", "German")
        langSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages
        )
        langSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedLanguage = languages[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        modeLabel.text = "ULTRA AI"
        modeBtn.text = "Ultra AI: On"
        modeBtn.setOnClickListener {
            isUltraMode = !isUltraMode
            modeLabel.text = if (isUltraMode) "ULTRA AI" else "NORMAL"
            modeBtn.text = if (isUltraMode) "Ultra AI: On" else "Ultra AI: Off"
        }

        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var moved = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        moved = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (kotlin.math.abs(dx) > 4 || kotlin.math.abs(dy) > 4) moved = true
                        params.x = (initialX + dx).toInt()
                        params.y = (initialY + dy).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        return moved
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)
    }

    private fun initVosk() {
        StorageService.unpack(
            this,
            "model-en-us",
            "model",
            { model: Model ->
                try {
                    speechService = SpeechService(
                        Recognizer(model, 16000.0f),
                        16000.0f
                    )
                    speechService?.startListening(this)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { exception ->
                exception.printStackTrace()
                floatingView.findViewById<TextView>(R.id.transcription_text)?.text =
                    "Model unavailable"
            }
        )
    }

    override fun onResult(hypothesis: String) {
        updateUI(parseHypothesis(hypothesis))
    }

    override fun onPartialResult(hypothesis: String) {
        updateUI(parseHypothesis(hypothesis))
    }

    private fun updateUI(text: String) {
        if (!::floatingView.isInitialized) return

        val textView = floatingView.findViewById<TextView>(R.id.transcription_text)
        val suggestionsView = floatingView.findViewById<TextView>(R.id.suggestions_text)
        val commandView = floatingView.findViewById<TextView>(R.id.command_text)

        textView.text = if (text.isBlank()) "Listening…" else text

        val command = PhonemeAI.detectCommands(text)
        if (command != null) {
            commandView.visibility = View.VISIBLE
            commandView.text = command
        } else {
            commandView.visibility = View.GONE
        }

        if (isUltraMode && text.isNotBlank()) {
            val suggestions = PhonemeAI.generateSuggestions(text, selectedLanguage)
            suggestionsView.text = suggestions
                .take(5)
                .joinToString("  •  ")
            suggestionsView.visibility = if (suggestions.isEmpty()) View.GONE else View.VISIBLE
        } else {
            suggestionsView.visibility = View.GONE
        }
    }

    private fun parseHypothesis(hypothesis: String): String {
        return try {
            JSONObject(hypothesis).optString("text", "")
        } catch (_: Exception) {
            hypothesis.substringAfter("text", "").trim(' ', ':', '"', '{', '}')
        }
    }

    override fun onDestroy() {
        speechService?.stop()
        speechService?.shutdown()
        if (::floatingView.isInitialized) {
            try {
                windowManager.removeView(floatingView)
            } catch (_: Exception) {
            }
        }
        super.onDestroy()
    }

    override fun onError(error: Exception?) {
        error?.printStackTrace()
    }

    override fun onTimeout() = Unit
    override fun onFinalResult(hypothesis: String?) = Unit
}
