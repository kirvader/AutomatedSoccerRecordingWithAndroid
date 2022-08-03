package app.hawkeye.balltracker.activities

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import app.hawkeye.balltracker.utils.*
import app.hawkeye.balltracker.utils.image_processors.*
import app.hawkeye.balltracker.utils.image_processors.GoogleMLkitImageProcessor
import app.hawkeye.balltracker.utils.image_processors.ORTImageProcessor
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


private val LOG = createLogger<CameraActivity>()

class CameraActivity : AppCompatActivity() {

    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var preview: Preview? = null

    private var imageAnalysis: ImageAnalysis? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var selector: QualitySelector? = null
    private var mediaStoreOutputOptions: MediaStoreOutputOptions? = null

    private var recorder: Recorder? = null

    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private var objectDetectorImageAnalyzer: ObjectDetectorImageAnalyzer? = null


    private var movementControllerDevice: FootballTrackingSystemController =
        FootballTrackingSystemController(
            App.getRotatableDevice()
        )

    private fun readModel(): ByteArray {
        val modelID = R.raw.yolov5s
        return resources.openRawResource(modelID).readBytes()
    }

    private fun createOrtSession(): OrtSession? = OrtEnvironment.getEnvironment()?.createSession(readModel())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)


        if (allPermissionsGranted()) {
            setupImageAnalyzer()

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

                    objectDetectorImageAnalyzer?.setCurrentImageProcessor(imageProcessorsChoice)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    LOG.i("Model is not changed.")
                }
            }
    }

    private fun setupImageAnalyzer() {

        val objectDetectorOptions = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()
        val objectDetector = ObjectDetection.getClient(objectDetectorOptions)


        objectDetectorImageAnalyzer = ObjectDetectorImageAnalyzer(
            mapOf(
                ImageProcessorsChoice.None to ImageProcessor.Default,
                ImageProcessorsChoice.GoogleML to GoogleMLkitImageProcessor(
                    detectedObjectsSurface.measuredWidth,
                    detectedObjectsSurface.measuredHeight,
                    objectDetector
                ),
                ImageProcessorsChoice.ORT_YOLO_V5 to ORTImageProcessor(createOrtSession())
            ),
            ::updateUI,
            ::updateCameraFOV
        )


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

                preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()

                imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                setAnalyzer()

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture, imageAnalysis
                )

                preview?.setSurfaceProvider(cameraPreview.surfaceProvider)
            } catch (ex: Exception) {
                if (ex.message != null) {
                    LOG.e(ex.toString())
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setAnalyzer() {
        scope.launch {
            imageAnalysis?.clearAnalyzer()
            try {

                objectDetectorImageAnalyzer?.let {
                    imageAnalysis?.setAnalyzer(
                        backgroundExecutor,
                        it
                    )
                }
            } catch (e: Exception) {
                LOG.d("Analyzer setup failed. Using model best2.pt", e)
            }
            if (imageAnalysis != null)
                LOG.i("Analyzer has been successfully set up.")
            else
                LOG.e("Analyzer is null.")
        }
    }

    private val recordingListener = Consumer<VideoRecordEvent> { event ->
        when (event) {
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

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
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
            )
            return
        }
        LOG.d("Found objects. The best is at x = %f", result[0].center.x)
        movementControllerDevice.updateTargetWithClassifiedBox(
            result[0],
            0.0f
        )
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}