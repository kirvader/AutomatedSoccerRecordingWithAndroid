package app.hawkeye.balltracker.activities


import android.Manifest
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
import app.hawkeye.balltracker.rotatable.PivoPodDevice
import app.hawkeye.balltracker.utils.*
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
            ::updateUIOnStartRecording,
            ::updateUIOnStopRecording,
            ::getPreviewSurfaceProvider,
            ::updateUIWhenImageAnalyzerFinished,
            ::updateUIAreaOfDetectionWithNewArea
        )

        if (allPermissionsGranted()) {
            cameraManager.startCamera()
        } else {
            LOG.e("Permissions were not granted.")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        videoCaptureButton.setOnClickListener {
            cameraManager.toggleCameraRecording()
        }

        scanPivoButton.setOnClickListener {
            if (RuntimeUtils.isEmulator()) {
                LOG.i("Scan button pressed")
            } else {
                PivoPodDevice.scanForPivoDevices(this, layoutInflater)
            }
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.available_object_detectors,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            imageAnalyzerChoiceSpinner.adapter = adapter
        }

        imageAnalyzerChoiceSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    val imageProcessorsChoice = ImageProcessorsChoice.getByValue(p2) ?: return
                    LOG.i("Clicked on Model number $imageProcessorsChoice")

                    cameraManager.setImageProcessor(imageProcessorsChoice)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    LOG.i("Model is not changed.")
                }
            }

        updateUIAreaOfDetectionWithNewArea(listOf())
    }

    private fun updateUIOnStartRecording() {
        videoCaptureButton.setText(R.string.stop_recording)
    }

    private fun updateUIOnStopRecording() {
        videoCaptureButton.setText(R.string.start_recording)
    }

    private fun updateUIAreaOfDetectionWithNewArea(newArea: List<AdaptiveRect>) {
        areaOfDetectionSurface.updateAreaOfDetection(newArea)
    }

    private fun updateUIWhenImageAnalyzerFinished(rect: AdaptiveRect?, newBenchmarksInfo: String) {
        runOnUiThread {
            detectedObjectsSurface.updateCurrentDetectedObject(
                rect?.toRect(
                    detectedObjectsSurface.measuredWidth,
                    detectedObjectsSurface.measuredHeight
                )
            )
            inference_time_info.text = newBenchmarksInfo
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