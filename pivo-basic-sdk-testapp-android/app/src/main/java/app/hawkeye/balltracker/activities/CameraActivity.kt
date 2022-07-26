package app.hawkeye.balltracker.activities

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.hawkeye.balltracker.App
import app.hawkeye.balltracker.R
import app.hawkeye.balltracker.controllers.FootballTrackingSystemController
import app.hawkeye.balltracker.rotating.PivoPodDevice
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.RuntimeUtils
import app.hawkeye.balltracker.utils.createLogger
import app.hawkeye.balltracker.utils.image_processing.ModelResult
import app.hawkeye.balltracker.utils.image_processing.ORTAnalyzer
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.pow


private val LOG = createLogger<CameraActivity>()

class CameraActivity : AppCompatActivity() {

    private var selector: QualitySelector? = null

    private val backgroundExecutor: ExecutorService by lazy { Executors.newWorkStealingPool() }
    private val labelData: List<String> by lazy { readLabels() }
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private var ortEnv: OrtEnvironment? = null
    private var imageAnalysis: ImageAnalysis? = null

    private var movementControllerDevice: FootballTrackingSystemController = FootballTrackingSystemController(
        App.getRotatableDevice())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        // Request Camera permission
        if (allPermissionsGranted()) {
            ortEnv = OrtEnvironment.getEnvironment()
            startCamera()
        } else {
            LOG.e("Permissions were not granted.")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        videoCaptureButton.setOnClickListener { }

        scanPivoButton.setOnClickListener {
            if (RuntimeUtils.isEmulator()) {
                LOG.i("Scan button pressed")
            } else {
                PivoPodDevice.scanForPivoDevices(this, layoutInflater)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        cameraProviderFuture.addListener({
            try {
                selector = QualitySelector
                    .from(
                        Quality.UHD,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                    )

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                setORTAnalyzer()

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )

                preview.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch (ex: Exception) {
                if (ex.message != null) {
                    LOG.e(ex.toString())
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
        ortEnv?.close()
        ProcessCameraProvider.getInstance(this).get().unbindAll()
        LOG.i("OnDestroy called")

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
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

    private fun round(number: Double, decimals: Int): Double {
        val multiplier = 10.0.pow(decimals)
        return kotlin.math.round(number * multiplier) / multiplier
    }

    private fun getBoxInfo(box: ClassifiedBox): String {
        val strPosition =
            "(${round(box.center.x.toDouble(), 2)};${round(box.center.y.toDouble(), 2)})"
        val strConfidence = "conf: ${round((box.confidence * 100).toDouble(), 2)}"
        return "$strPosition $strConfidence"
    }


    private fun updateUIAndCameraFOV(result: ModelResult) {
        runOnUiThread {
            if (result.detectedObjects.isNotEmpty()) {
                detected_item_1.text = labelData[result.detectedObjects[0].classId]
                detected_item_value_1.text = getBoxInfo(result.detectedObjects[0])
            } else {
                detected_item_1.text = "lost"
                detected_item_value_1.text = ""
            }
            if (result.detectedObjects.size > 1) {
                detected_item_2.text = labelData[result.detectedObjects[1].classId]
                detected_item_value_2.text = getBoxInfo(result.detectedObjects[1])
            } else {
                detected_item_2.text = "lost"
                detected_item_value_2.text = ""
            }
            if (result.detectedObjects.size > 2) {
                detected_item_3.text = labelData[result.detectedObjects[2].classId]
                detected_item_value_3.text = getBoxInfo(result.detectedObjects[2])
            } else {
                detected_item_3.text = "lost"
                detected_item_value_3.text = ""
            }
            inference_time_value.text = "${result.processTimeMs}ms"
        }
        if (result.detectedObjects.isEmpty()) {
            LOG.i("No appropriate objects found")
            movementControllerDevice.updateTargetWithClassifiedBox(
                null,
                result.processTimeMs / 1000.0f
            )
            return
        }
        LOG.d("Found objects. The best is at x = %f", result.detectedObjects[0].center.x)
        movementControllerDevice.updateTargetWithClassifiedBox(
            result.detectedObjects[0],
            result.processTimeMs / 1000.0f
        )
    }

    // Read MobileNet V2 classification labels
    private fun readLabels(): List<String> =
        resources.openRawResource(R.raw.model_classes).bufferedReader().readLines()

    // Read ort model into a ByteArray, run in background
    private suspend fun readModel(): ByteArray = withContext(Dispatchers.IO) {
        val modelID = R.raw.yolov5s
        resources.openRawResource(modelID).readBytes()
    }

    // Create a new ORT session in background
    private suspend fun createOrtSession(): OrtSession? = withContext(Dispatchers.Default) {
        ortEnv?.createSession(readModel())
    }

    // Create a new ORT session and then change the ImageAnalysis.Analyzer
    // This part is done in background to avoid blocking the UI
    private fun setORTAnalyzer() {
        scope.launch {
            imageAnalysis?.clearAnalyzer()
            try {
                imageAnalysis?.setAnalyzer(
                    backgroundExecutor,
                    ORTAnalyzer(createOrtSession(), ::updateUIAndCameraFOV)
                )
            } catch (e: Exception) {
                LOG.d("Analyzer setup failed. Using model best2.pt", e)
            }
            if (imageAnalysis != null)
                LOG.i("Analyzer has been successfully set up.")
            else
                LOG.e("Analyzer is null.")
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}