package app.pivo.android.basicsdkdemo

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.pivo.android.basicsdk.PivoSdk
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.pow

class CameraActivity : AppCompatActivity() {
    private val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val labelData: List<String> by lazy { readLabels() }
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private lateinit var ortEnv: OrtEnvironment
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysis: ImageAnalysis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        // Request Camera permission
        if (allPermissionsGranted()) {
            ortEnv = OrtEnvironment.getEnvironment()
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            setORTAnalyzer()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
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

        if (abs_val >= 0.6F) {
            angle = 10
            speed = 10
        } else if (abs_val >= 0.2F) {
            angle = 5
            speed = 20
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
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}