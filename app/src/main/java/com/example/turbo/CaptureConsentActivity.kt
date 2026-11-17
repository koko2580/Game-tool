package com.example.turbo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.NexoraApplication

class CaptureConsentActivity : ComponentActivity() {

    private var mode: String = MODE_SCREENSHOT

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val controller = (application as NexoraApplication).container.mediaCaptureController
        if (mode == MODE_RECORD) {
            controller.startRecordingWithConsent(result.resultCode, result.data)
        } else {
            controller.takeScreenshotWithConsent(result.resultCode, result.data)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SCREENSHOT
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (mgr == null) {
            finish()
            return
        }
        projectionLauncher.launch(mgr.createScreenCaptureIntent())
    }

    companion object {
        const val MODE_SCREENSHOT = "screenshot"
        const val MODE_RECORD = "record"
        private const val EXTRA_MODE = "extra_mode"

        fun buildIntent(context: Context, mode: String): Intent {
            return Intent(context, CaptureConsentActivity::class.java).apply {
                putExtra(EXTRA_MODE, mode)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
