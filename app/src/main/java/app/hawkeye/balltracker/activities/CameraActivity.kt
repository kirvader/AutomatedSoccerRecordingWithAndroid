package app.hawkeye.balltracker.activities

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import app.hawkeye.balltracker.App
import app.hawkeye.balltracker.R
import app.hawkeye.balltracker.controllers.FootballTrackingSystemController
import app.hawkeye.balltracker.rotating.PivoPodDevice
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.RuntimeUtils
import app.hawkeye.balltracker.utils.createLogger
import app.hawkeye.balltracker.utils.image_processing.GoogleMLkitAnalyzer
import app.hawkeye.balltracker.utils.image_processing.ORTAnalyzer
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


private val LOG = createLogger<CameraActivity>()

class CameraActivity : AppCompatActivity() {


    private var imageAnalysis: ImageAnalysis? = null

    // object detectors
    private var ortEnv: OrtEnvironment? = null
    private var objectDetector: ObjectDetector? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var selector: QualitySelector? = null
    private var mediaStoreOutputOptions: MediaStoreOutputOptions? = null

    private var recorder: Recorder? = null

    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private val labelData: List<String> by lazy { readLabels() }

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

    private var movementControllerDevice: FootballTrackingSystemController =
        FootballTrackingSystemController(
            App.getRotatableDevice()
        )

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

        videoCaptureButton.setOnClickListener { toggleCameraRecording() }

        scanPivoButton.setOnClickListener {
            if (RuntimeUtils.isEmulator()) {
                LOG.i("Scan button pressed")
            } else {
                PivoPodDevice.scanForPivoDevices(this, layoutInflater)
            }
        }
    }

    private val recordingListener = Consumer<VideoRecordEvent> { event ->
        when(event) {
            is VideoRecordEvent.Start -> {
                val msg = "Capture Started"

                Toast.makeText(this, msg, Toast.LENGTH_SHORT)
                    .show()
                LOG.i(msg)
            }
            is VideoRecordEvent.Finalize -> {
                val msg = if (!event.hasError()) {
                    "Video capture succeeded: ${event.outputResults.outputUri}"
                } else {
                    recording?.close()
                    recording = null
                    "Video capture ends with error: ${event.error}"
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT)
                    .show()
                LOG.i(msg)
            }
        }
    }
    private fun toggleCameraRecording() {
        if (recording != null) {
            stopCapturing()
            videoCaptureButton.setText(R.string.start_recording)
        } else {
            startCapture()
            videoCaptureButton.setText(R.string.stop_recording)
        }
    }

    private fun startCapture() {
        val name = "CameraX-recording-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }

        mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(this.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            LOG.i("Not all permissions granted.")
            return
        }
        recording = videoCapture?.output
            ?.prepareRecording(this, mediaStoreOutputOptions!!)
            ?.withAudioEnabled()
            ?.start(ContextCompat.getMainExecutor(this), recordingListener)!!
    }

    private fun stopCapturing() {
        recording?.stop()
        recording = null
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

                recorder = Recorder.Builder()
                    .setExecutor(cameraExecutor).setQualitySelector(selector!!)
                    .build()

                videoCapture = VideoCapture.withOutput(recorder!!)

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val objectDetectorOptions = ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableClassification()
                    .build()

                objectDetector = ObjectDetection.getClient(objectDetectorOptions)

                setORTAnalyzer()

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture, imageAnalysis
                )

                preview.setSurfaceProvider(cameraPreview.surfaceProvider)
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

    private fun updateUI(result: List<ClassifiedBox>) {
        runOnUiThread {
            if (result.isNotEmpty()) {
                detected_item_1.text = labelData[result[0].classId]
                detected_item_value_1.text = result[0].getStrInfo()
            } else {
                detected_item_1.text = "lost"
                detected_item_value_1.text = ""
            }
//            inference_time_value.text = "${result.processTimeMs}ms"
            detectedObjectsSurface.updateCurrentDetectedObject(
                if (result.isNotEmpty()) {
                    result[0].toRect(
                        detectedObjectsSurface.measuredWidth,
                        detectedObjectsSurface.measuredHeight
                    )
                } else {
                    null
                }
            )
        }
    }

    private fun updateCameraFOV(result: List<ClassifiedBox>) {
        if (result.isEmpty()) {
            LOG.i("No appropriate objects found")
            movementControllerDevice.updateTargetWithClassifiedBox(
                null,
                0.0f
//                result.processTimeMs / 1000.0f
            )
            return
        }
        LOG.d("Found objects. The best is at x = %f", result[0].center.x)
        movementControllerDevice.updateTargetWithClassifiedBox(
            result[0],
            0.0f
//            result.processTimeMs / 1000.0f
        )
    }

    private fun setGoogleMLkitAnalyzer() {
        scope.launch {
            imageAnalysis?.clearAnalyzer()
            try {
                imageAnalysis?.setAnalyzer(
                    backgroundExecutor,
                    GoogleMLkitAnalyzer(objectDetector, detectedObjectsSurface.measuredWidth,
                        detectedObjectsSurface.measuredHeight, ::updateUI, ::updateCameraFOV)
                )
            } catch (ex: Exception) {
                LOG.e("Analyzer setup failed. Using google ml kit analyzer", ex)
            }
            if (imageAnalysis != null)
                LOG.i("Analyzer has been successfully set up.")
            else
                LOG.e("Analyzer is null.")
        }
    }

    // Create a new ORT session and then change the ImageAnalysis.Analyzer
    // This part is done in background to avoid blocking the UI
    private fun setORTAnalyzer() {
        scope.launch {
            imageAnalysis?.clearAnalyzer()
            try {
                imageAnalysis?.setAnalyzer(
                    backgroundExecutor,
                    ORTAnalyzer(createOrtSession(), ::updateUI, ::updateCameraFOV)
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