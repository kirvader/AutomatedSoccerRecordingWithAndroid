package app.pivo.android.basicsdkdemo

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import app.pivo.android.basicsdk.PivoSdk
import kotlinx.android.synthetic.main.camera_activity.*
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.pow

class CameraActivity : AppCompatActivity(), LifecycleOwner {

    private var mediaStoreOutputOptions: MediaStoreOutputOptions? = null
    private var recorder: Recorder? = null
    private var selector: QualitySelector? = null
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val labelData: List<String> by lazy { readLabels() }
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private var ortEnv: OrtEnvironment? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    private var recording: Recording? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.camera_activity)
        // Request Camera permission
        if (allPermissionsGranted()) {
            ortEnv = OrtEnvironment.getEnvironment()
            startCamera()
        } else {
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

        cameraProviderFuture.addListener(Runnable {
            selector = QualitySelector
                .from(
                    Quality.UHD,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
                )

            recorder = Recorder.Builder()
                .setExecutor(cameraExecutor).setQualitySelector(selector!!)
                .build()

            videoCapture = VideoCapture.withOutput(recorder!!)

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }


            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            setORTAnalyzer()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        backgroundExecutor.shutdown()
        ortEnv?.close()
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
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
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
    private fun normalizeBoxCoord(x: Float): Float {
        return x * 2.0F - 1.0F
    }

    private fun handlePivoPod(box_x: Float) {
         // normalizing
        val x = normalizeBoxCoord(box_x)
        val abs_val = abs(x)
        var angle = 0
        var speed = 0

        if (abs_val >= 0.8F) {
            angle = 6
            speed = 20
        } else if (abs_val >= 0.4F) {
            angle = 3
            speed = 30
        } else {
            PivoSdk.getInstance().stop()
            return
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
            inference_time_value.text = result.processTimeMs.toString() + "ms"
        }
        if (result.detectedObjects.isEmpty()) return
        val bestBallX = result.detectedObjects[0].centerX
        handlePivoPod(bestBallX)
    }

    // Read MobileNet V2 classification labels
    private fun readLabels(): List<String> {
        return resources.openRawResource(R.raw.model_classes).bufferedReader().readLines()
    }

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
    private fun setORTAnalyzer(){
        scope.launch {
            imageAnalysis?.clearAnalyzer()
            imageAnalysis?.setAnalyzer(
                backgroundExecutor,
                ORTAnalyzer(createOrtSession(), ::updateUIAndCameraFOV)
            )
        }
    }

    companion object {
        public const val TAG = "ORTImageClassifier"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}