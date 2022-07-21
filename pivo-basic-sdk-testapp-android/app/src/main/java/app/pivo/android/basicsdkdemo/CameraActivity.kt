package app.pivo.android.basicsdkdemo

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import app.pivo.android.basicsdk.PivoSdk
import app.pivo.android.basicsdkdemo.utils.createLogger
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.pow

class CameraActivity : AppCompatActivity() {

    private var mediaStoreOutputOptions: MediaStoreOutputOptions? = null
    private var recorder: Recorder? = null
    private var selector: QualitySelector? = null
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private val backgroundExecutor: ExecutorService by lazy { Executors.newWorkStealingPool() }
    private val labelData: List<String> by lazy { readLabels() }
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private var ortEnv: OrtEnvironment? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    private var recording: Recording? = null



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

        videoCaptureButton.setOnClickListener {
            toggleCameraRecording()
        }
    }

    private val recordingListener = Consumer<VideoRecordEvent> { event ->
        when(event) {
            is VideoRecordEvent.Start -> {
                val msg = "Capture Started"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT)
                    .show()
                // update app internal recording state
                LOG.i(msg)
            }
            is VideoRecordEvent.Finalize -> {
                val msg = if (!event.hasError()) {
                    "Video capture succeeded: ${event.outputResults.outputUri}"
                    // TODO() handle succeeding here
                } else {
                    // update app state when the capture failed.
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
        } else {
            startCapture()
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            LOG.e("Not all permissions granted.")
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

        cameraProviderFuture.addListener(Runnable {
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

                // Preview
                val preview = Preview.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()

                imageCapture = ImageCapture.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                setORTAnalyzer()


                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture, imageAnalysis
                )

                preview.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch (ex: Exception) {
                if (ex.message != null) {
                    LOG.d("Camera setup failed with exception", ex)
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

    private fun getBoxInfo(box: ClassifiedBox) : String {
        val strPosition = "(${round(box.centerX.toDouble(), 2)};${round(box.centerY.toDouble(), 2)})"
        val strConfidence = "conf: ${round((box.confidence * 100).toDouble(), 2)}"
        return "$strPosition $strConfidence"
    }

    // translates x to [-1; 1]
    private fun normalizeBoxCoord(x: Float): Float = x * 2.0F - 1.0F

    private fun handlePivoPod(box_x: Float) {
        // normalizing
        val x = normalizeBoxCoord(box_x)
        val abs_val = abs(x)
        var angle = 0
        var speed = 0

        when {
            abs_val >= 0.6F -> {
                angle = 10
                speed = 10
            }
            abs_val >= 0.2F -> {
                angle = 5
                speed = 20
            }
            else -> {
                PivoSdk.getInstance().stop()
                return
            }
        }
        if (x < 0) {
            PivoSdk.getInstance().turnLeft(angle, speed)
        } else {
            PivoSdk.getInstance().turnRight(angle, speed)
        }
    }

    private fun updateUIAndCameraFOV(result: Result) {
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
            return
        }
        val bestBallX = result.detectedObjects[0].centerX
        LOG.d("Found objects. The best is at %f", bestBallX)
        handlePivoPod(bestBallX)
    }

    // Read MobileNet V2 classification labels
    private fun readLabels(): List<String> =
        resources.openRawResource(R.raw.model_classes).bufferedReader().readLines()

    // Read ort model into a ByteArray, run in background
    private suspend fun readModel(): ByteArray = withContext(Dispatchers.IO) {
        val modelID = R.raw.best2
        resources.openRawResource(modelID).readBytes()
    }

    // Create a new ORT session in background
    private suspend fun createOrtSession(): OrtSession? = withContext(Dispatchers.Default) {
//        appendToLog("Ort env is starting to create")
        ortEnv?.createSession(readModel())
    }

    // Create a new ORT session and then change the ImageAnalysis.Analyzer
    // This part is done in background to avoid blocking the UI
    private fun setORTAnalyzer(){
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
        private val LOG = createLogger<CameraActivity>()
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}