package app.hawkeye.balltracker.activities


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.hawkeye.balltracker.*
import app.hawkeye.balltracker.R
import app.hawkeye.balltracker.controllers.UIController
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.rotatable.PivoPodDevice
import app.hawkeye.balltracker.utils.*
import com.hawkeye.movement.interfaces.TrackingSystemControllerBase
import kotlinx.android.synthetic.main.activity_camera.*


private val LOG = createLogger<CameraActivity>()

class CameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        cameraManager = CameraManager(
            this,
            this,
        )

        if (allPermissionsGranted()) {
            cameraManager.startCamera()
        } else {
            LOG.e("Permissions were not granted.")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        recording_button.setOnClickListener {
            cameraManager.toggleCameraRecording()
        }

        settings_button.setOnClickListener{
            val intent = Intent(this@CameraActivity, SettingsActivity::class.java)
            startActivity(intent)
        }

        updateUIAreaOfDetectionWithNewArea(listOf())

        UIController.updateUIOnStartRecording = ::updateUIOnStartRecording
        UIController.updateUIOnStopRecording = ::updateUIOnStopRecording
        UIController.updateUIAreaOfDetectionWithNewArea = ::updateUIAreaOfDetectionWithNewArea
        UIController.updateUIWhenImageAnalyzerFinished = ::updateUIWhenImageAnalyzerFinished
        UIController.getPreviewSurfaceProvider = ::getPreviewSurfaceProvider
    }

    private fun updateUIOnStartRecording() {
        recording_button.setBackgroundResource(R.drawable.ic_record_in_progress)
    }

    private fun updateUIOnStopRecording() {
        recording_button.setBackgroundResource(R.drawable.ic_ready_to_record)
    }

    private fun updateUIAreaOfDetectionWithNewArea(newArea: List<AdaptiveScreenRect>) {
        areaOfDetectionSurface.updateAreaOfDetection(newArea)
    }

    private fun updateUIWhenImageAnalyzerFinished(rect: AdaptiveScreenRect?, newBenchmarksInfo: String) {
        runOnUiThread {
            detectedObjectsSurface.updateCurrentDetectedObject(
                rect?.toScreenRect(
                    detectedObjectsSurface.measuredWidth,
                    detectedObjectsSurface.measuredHeight
                )?.toRect()
            )
            inference_time_info.text = newBenchmarksInfo

            LOG.i("rect size ${rect?.size}")
            trackingSystemStateView.updateLocatorState()
        }
    }

    private fun getPreviewSurfaceProvider() = cameraPreview.surfaceProvider

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.destroy()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraManager.startCamera()
                LOG.i("Camera started")
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
                finish()
            }

        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }
}