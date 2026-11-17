package com.example.turbo

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NexoraApplication
import com.example.R
import com.example.data.system.DndToggleOutcome
import com.example.domain.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class GamingSidebarService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var windowManager: WindowManager? = null
    private var rootContainer: LinearLayout? = null
    private var expandedPanel: LinearLayout? = null
    private var handleButton: TextView? = null
    private var timerTextView: TextView? = null
    private var metricsTextView: TextView? = null
    private var dndButton: TextView? = null
    private var recordButton: TextView? = null
    private var brightnessBar: SeekBar? = null
    private var brightnessHeader: TextView? = null
    private var isExpanded = false

    private val container by lazy { (application as NexoraApplication).container }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForegroundCompat()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        if (Settings.canDrawOverlays(this)) {
            attachOverlay()
            container.mediaCaptureController.setSidebarRunning(true)
            startLiveTelemetryUpdates()
        } else {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachOverlay() {
        val wm = windowManager ?: return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(180)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EE090B12"))
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.parseColor("#AA8257FF"))
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val handle = TextView(this).apply {
            text = "NX"
            setTextColor(Color.parseColor("#20D9E8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#268257FF"))
                cornerRadius = dp(12).toFloat()
            }
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    runCatching { wm.updateViewLayout(root, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        toggleExpanded(root)
                    }
                    true
                }
                else -> false
            }
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(10), dp(4), dp(6), dp(4))
            layoutParams = LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val title = TextView(this).apply {
            text = "NEXORA X SIDEBAR"
            setTextColor(Color.parseColor("#F5F5FA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        val timerTv = TextView(this).apply {
            text = "Session: Ready"
            setTextColor(Color.parseColor("#20D9E8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(4), 0, dp(2))
        }
        val metricsTv = TextView(this).apply {
            text = "Reading telemetry..."
            setTextColor(Color.parseColor("#9299AB"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, 0, 0, dp(8))
        }

        val brightLabel = TextView(this).apply {
            text = "Brightness"
            setTextColor(Color.parseColor("#F5F5FA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        val seekBar = SeekBar(this).apply {
            max = 255
            progress = 128
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        serviceScope.launch {
                            val ok = container.telemetryRepository.setSystemBrightness(progress.coerceAtLeast(10))
                            if (!ok) {
                                container.systemSettingsNavigator.launchManageWriteSettings()
                            }
                        }
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        val dndBtn = createPillButton("DND: Check") {
            when (container.dndController.toggleDnd()) {
                is DndToggleOutcome.PermissionRequired -> container.dndController.openPolicyAccessSettings()
                else -> refreshQuickStatus()
            }
        }
        val shotBtn = createPillButton("Take Screenshot") {
            container.mediaCaptureController.launchConsentFromOverlay(CaptureConsentActivity.MODE_SCREENSHOT)
        }
        val recBtn = createPillButton("Record Screen") {
            if (container.mediaCaptureController.captureState.value.isRecordingScreen) {
                container.mediaCaptureController.stopScreenRecording()
            } else {
                container.mediaCaptureController.launchConsentFromOverlay(CaptureConsentActivity.MODE_RECORD)
            }
        }
        val settingsBtn = createPillButton("System Display Settings") {
            val shortcut = container.systemSettingsNavigator
                .getAvailableShortcuts(false)
                .firstOrNull { it.id == "display_settings" }
            if (shortcut != null) {
                container.systemSettingsNavigator.launchShortcut(shortcut)
            }
        }
        val closeBtn = createPillButton("Close Sidebar", bgHex = "#2EEFABAB", textHex = "#FCA5A5") {
            stopSelf()
        }

        panel.addView(title)
        panel.addView(timerTv)
        panel.addView(metricsTv)
        panel.addView(brightLabel)
        panel.addView(seekBar)
        panel.addView(dndBtn)
        panel.addView(shotBtn)
        panel.addView(recBtn)
        panel.addView(settingsBtn)
        panel.addView(closeBtn)

        root.addView(handle)
        root.addView(panel)

        rootContainer = root
        expandedPanel = panel
        handleButton = handle
        timerTextView = timerTv
        metricsTextView = metricsTv
        dndButton = dndBtn
        recordButton = recBtn
        brightnessBar = seekBar
        brightnessHeader = brightLabel

        runCatching { wm.addView(root, params) }
    }

    private fun toggleExpanded(root: ViewGroup) {
        isExpanded = !isExpanded
        TransitionManager.beginDelayedTransition(root)
        expandedPanel?.visibility = if (isExpanded) View.VISIBLE else View.GONE
        refreshQuickStatus()
    }

    private fun startLiveTelemetryUpdates() {
        serviceScope.launch {
            while (isActive) {
                refreshQuickStatus()
                delay(1500L)
            }
        }
    }

    private fun refreshQuickStatus() {
        serviceScope.launch {
            val prefs = container.userPreferencesRepository.preferencesFlow.first()
            val memory = container.deviceTelemetryManager.readMemoryTelemetry()
            val battery = container.deviceTelemetryManager.readBatteryTelemetry()
            val thermal = container.deviceTelemetryManager.readThermalTelemetry()
            val display = container.deviceTelemetryManager.readDisplayTelemetry()
            val dnd = container.dndController.readStatus()
            val capture = container.mediaCaptureController.captureState.value

            val session = prefs.activeSession
            timerTextView?.text = if (session != null) {
                "${session.appLabel}: ${FormatUtils.formatDurationTimer(session.durationSeconds())}"
            } else {
                "Session: Idle (${prefs.turboProfile.title})"
            }

            metricsTextView?.text = "BAT ${battery.levelPercent}% • RAM ${FormatUtils.formatMemoryBytes(memory.availableBytes)} • ${thermal.level.label}"

            brightnessHeader?.text = if (display.canWriteSystemSettings) {
                "Brightness (${display.brightnessValue ?: "--"}/255)"
            } else {
                "Brightness (Tap bar to grant permission)"
            }
            display.brightnessValue?.let { brightnessBar?.progress = it }

            dndButton?.text = when {
                !dnd.hasPolicyAccess -> "Grant DND Access"
                dnd.isDndEnabled -> "DND: Active (Tap to disable)"
                else -> "DND: Off (Tap to enable)"
            }

            recordButton?.text = if (capture.isRecordingScreen) {
                "Stop Screen Recording"
            } else {
                "Record Screen (MediaProjection)"
            }
        }
    }

    private fun createPillButton(
        label: String,
        bgHex: String = "#1B2033",
        textHex: String = "#F5F5FA",
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(Color.parseColor(textHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = dp(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Floating Gaming Sidebar",
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NEXORA X Gaming Sidebar")
            .setContentText("Floating edge overlay active")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        container.mediaCaptureController.setSidebarRunning(false)
        serviceScope.cancel()
        rootContainer?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        rootContainer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.example.turbo.ACTION_STOP_SIDEBAR"
        private const val CHANNEL_ID = "nexora_sidebar_channel"
        private const val NOTIFICATION_ID = 4101
    }
}
