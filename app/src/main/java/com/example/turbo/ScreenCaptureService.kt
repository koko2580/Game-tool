package com.example.turbo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NexoraApplication
import com.example.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var imageReader: ImageReader? = null
    private var activeVideoPfd: ParcelFileDescriptor? = null
    private var activeVideoUri: Uri? = null
    private var isScreenshotDone = false

    private lateinit var captureThread: HandlerThread
    private lateinit var captureHandler: Handler

    private val controller: MediaCaptureController by lazy {
        (application as NexoraApplication).container.mediaCaptureController
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            releaseResources(notifyError = null)
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        captureThread = HandlerThread("NexoraCaptureThread").apply { start() }
        captureHandler = Handler(captureThread.looper)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                finalizeRecordingAndStop()
                return START_NOT_STICKY
            }
            ACTION_START_RECORD, ACTION_SCREENSHOT -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val consentData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_CONSENT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_CONSENT_DATA)
                }

                if (consentData == null || resultCode == -1) {
                    controller.updateRecording(false, error = "Invalid screen capture consent.")
                    stopSelf()
                    return START_NOT_STICKY
                }

                val isRecord = intent.action == ACTION_START_RECORD
                startForegroundCompat(isRecord)

                val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                val projection = mgr?.getMediaProjection(resultCode, consentData)
                if (projection == null) {
                    controller.updateRecording(false, error = "MediaProjection unavailable.")
                    stopSelf()
                    return START_NOT_STICKY
                }

                mediaProjection = projection
                projection.registerCallback(projectionCallback, captureHandler)

                if (isRecord) {
                    beginScreenRecording(projection)
                } else {
                    beginScreenshotCapture(projection)
                }
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(isRecording: Boolean) {
        val notification = buildCaptureNotification(isRecording)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun beginScreenshotCapture(projection: MediaProjection) {
        controller.updateScreenshotState(capturing = true)
        isScreenshotDone = false
        val (width, height, dpi) = readDisplayMetrics()

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        reader.setOnImageAvailableListener({ r ->
            if (isScreenshotDone) return@setOnImageAvailableListener
            val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            isScreenshotDone = true
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val rawBitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                rawBitmap.copyPixelsFromBuffer(buffer)
                val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, width, height)
                if (rawBitmap !== cropped) rawBitmap.recycle()

                val savedName = saveBitmapToGalleryOrAppDir(cropped)
                cropped.recycle()
                controller.updateScreenshotState(
                    capturing = false,
                    savedMessage = "Screenshot saved: $savedName"
                )
            } catch (t: Throwable) {
                controller.updateScreenshotState(
                    capturing = false,
                    error = t.localizedMessage ?: "Screenshot capture failed"
                )
            } finally {
                image.close()
                releaseResources(notifyError = null)
                stopSelf()
            }
        }, captureHandler)

        runCatching {
            virtualDisplay = projection.createVirtualDisplay(
                "NexoraScreenshot",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                captureHandler
            )
        }.onFailure { err ->
            controller.updateScreenshotState(false, error = err.localizedMessage ?: "VirtualDisplay failed")
            releaseResources(notifyError = null)
            stopSelf()
        }
    }

    private fun beginScreenRecording(projection: MediaProjection) {
        val (rawWidth, rawHeight, dpi) = readDisplayMetrics()
        // Scale down safely if above 1080p for broad hardware encoder compatibility, keeping even dimensions
        val scale = if (maxOf(rawWidth, rawHeight) > 1920) {
            1920f / maxOf(rawWidth, rawHeight).toFloat()
        } else 1f
        val width = ((rawWidth * scale).toInt() / 2) * 2
        val height = ((rawHeight * scale).toInt() / 2) * 2

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder = recorder

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "NEXORA_REC_$timestamp.mp4"

        val configured = runCatching {
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            recorder.setVideoSize(width.coerceAtLeast(320), height.coerceAtLeast(320))
            recorder.setVideoEncodingBitRate(6_000_000)
            recorder.setVideoFrameRate(30)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/NEXORA_X")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                activeVideoUri = uri
                val pfd = uri?.let { contentResolver.openFileDescriptor(it, "w") }
                activeVideoPfd = pfd
                if (pfd != null) {
                    recorder.setOutputFile(pfd.fileDescriptor)
                } else {
                    val fallbackFile = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName)
                    recorder.setOutputFile(fallbackFile.absolutePath)
                }
            } else {
                val dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
                val outFile = File(dir, fileName)
                recorder.setOutputFile(outFile.absolutePath)
            }

            recorder.prepare()
            virtualDisplay = projection.createVirtualDisplay(
                "NexoraScreenRecord",
                width.coerceAtLeast(320),
                height.coerceAtLeast(320),
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null,
                captureHandler
            )
            recorder.start()
            true
        }

        configured.onSuccess {
            controller.updateRecording(recording = true)
        }.onFailure { err ->
            controller.updateRecording(
                recording = false,
                error = err.localizedMessage ?: "Unable to start MediaRecorder."
            )
            releaseResources(notifyError = null)
            stopSelf()
        }
    }

    private fun finalizeRecordingAndStop() {
        val wasRecording = controller.captureState.value.isRecordingScreen
        runCatching { mediaRecorder?.stop() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activeVideoUri?.let { uri ->
                val doneValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                runCatching { contentResolver.update(uri, doneValues, null, null) }
            }
        }
        releaseResources(notifyError = null)
        if (wasRecording) {
            controller.updateRecording(
                recording = false,
                savedMessage = "Screen recording saved to Movies/NEXORA_X"
            )
        } else {
            controller.updateRecording(recording = false)
        }
        stopSelf()
    }

    private fun releaseResources(notifyError: String?) {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { mediaRecorder?.reset() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        runCatching { activeVideoPfd?.close() }
        activeVideoPfd = null
        activeVideoUri = null
        runCatching { mediaProjection?.unregisterCallback(projectionCallback) }
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        if (controller.captureState.value.isRecordingScreen) {
            controller.updateRecording(recording = false, error = notifyError)
        }
    }

    private fun saveBitmapToGalleryOrAppDir(bitmap: Bitmap): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "NEXORA_SHOT_$timestamp.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NEXORA_X")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                contentResolver.update(uri, done, null, null)
                return "Pictures/NEXORA_X/$fileName"
            }
        }

        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: filesDir
        val outFile = File(dir, fileName)
        FileOutputStream(outFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return outFile.name
    }

    private fun readDisplayMetrics(): Triple<Int, Int, Int> {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dpi = resources.displayMetrics.densityDpi.coerceAtLeast(160)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            Triple(bounds.width().coerceAtLeast(360), bounds.height().coerceAtLeast(360), dpi)
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay?.getRealMetrics(dm)
            Triple(dm.widthPixels.coerceAtLeast(360), dm.heightPixels.coerceAtLeast(360), dpi)
        }
    }

    private fun ensureNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Game Turbo Capture",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Displays active screen recording or screenshot status"
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildCaptureNotification(isRecording: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            buildStopIntent(this),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isRecording) "NEXORA X Screen Recording" else "NEXORA X Screenshot")
            .setContentText(if (isRecording) "Recording screen via MediaProjection" else "Capturing display frame")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .apply {
                if (isRecording) {
                    addAction(0, "Stop Recording", stopIntent)
                }
            }
            .build()
    }

    override fun onDestroy() {
        releaseResources(notifyError = null)
        captureThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "nexora_capture_channel"
        private const val NOTIFICATION_ID = 4102
        private const val ACTION_START_RECORD = "com.example.turbo.ACTION_START_RECORD"
        private const val ACTION_SCREENSHOT = "com.example.turbo.ACTION_SCREENSHOT"
        private const val ACTION_STOP = "com.example.turbo.ACTION_STOP_CAPTURE"
        private const val EXTRA_RESULT_CODE = "extra_result_code"
        private const val EXTRA_CONSENT_DATA = "extra_consent_data"

        fun buildStartRecordIntent(context: Context, resultCode: Int, data: Intent): Intent {
            return Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_START_RECORD
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_CONSENT_DATA, data)
            }
        }

        fun buildScreenshotIntent(context: Context, resultCode: Int, data: Intent): Intent {
            return Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_SCREENSHOT
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_CONSENT_DATA, data)
            }
        }

        fun buildStopIntent(context: Context): Intent {
            return Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
