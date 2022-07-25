package app.pivo.android.basicsdkdemo.utils

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.pivo.android.basicsdk.PivoSdk
import app.pivo.android.basicsdk.events.PivoEvent
import app.pivo.android.basicsdk.events.PivoEventBus
import app.pivo.android.basicsdkdemo.R
import app.pivo.android.basicsdkdemo.activities.CameraActivity
import com.example.movementcontrollingmodule.movementController.DeviceToObjectControllerBase
import com.example.movementcontrollingmodule.movementController.utils.Point
import com.example.movementcontrollingmodule.movementController.utils.PolarPoint
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import io.reactivex.functions.Consumer


class DeviceToObjectController : DeviceToObjectControllerBase() {

    fun updateTargetWithClassifiedBox(box: ClassifiedBox?, timeFromLastSegmentUpdate: Float) {
        if (box == null)
        {
            updateTargetPosition(null, timeFromLastSegmentUpdate)
            return
        }
        val deltaAngle = box.center.x * 90.0f - 45.0f  // relative to lastDirection

        val ballPlaneWidth = ballDiam * 1.0f / box.width
        val distance = ballPlaneWidthToDistance * ballPlaneWidth

        val ballPlaneHeight = ballDiam * 1.0f / box.height
        val height = box.center.y * ballPlaneHeight

        updateTargetPosition(Point(PolarPoint(distance, deviceRotatingController.getLastDirection() + deltaAngle, height)), timeFromLastSegmentUpdate)
    }

    companion object {
        private const val ballDiam = 0.23f // ball diameter in meters
        private const val ballPlaneWidthToDistance = 16.0f / 17.0f // it is probably should be some trigonometrical formula but we haven't got zoom control for now, sooooo...

        fun scanForDevices(context: Context, layoutInflater: LayoutInflater) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Pivo scan results")
            val dialogLayout = layoutInflater.inflate(R.layout.pivo_scan_results, null)
            val scanResults = dialogLayout.findViewById<RecyclerView>(R.id.scan_results)

            //initialize device scan adapter
            val pivoScanResultsAdapter = ScanResultsAdapter()
            pivoScanResultsAdapter.setOnAdapterItemClickListener(object :
                ScanResultsAdapter.OnAdapterItemClickListener {
                override fun onAdapterViewClick(view: View?) {
                    val scanResult = pivoScanResultsAdapter.getItemAtPosition(
                        scanResults.getChildAdapterPosition(view!!)
                    )
                    if (scanResult != null) {
                        PivoSdk.getInstance().connectTo(scanResult)
                    }
                }
            })

            PivoEventBus.subscribe(
                PivoEventBus.CONNECTION_COMPLETED, context, Consumer {
                    if (it is PivoEvent.ConnectionComplete) {
                        Log.e(CameraActivity.TAG, "CONNECTION_COMPLETED")
                    }
                })
            //subscribe to get scan device
            PivoEventBus.subscribe(
                PivoEventBus.SCAN_DEVICE, context, Consumer {
                    if (it is PivoEvent.Scanning) {

                        Log.e(CameraActivity.TAG, "Result for scanning is updated")
                        pivoScanResultsAdapter.addScanResult(it.device)
                    }
                })

            scanResults.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = pivoScanResultsAdapter
            }

            checkPermission(context)

            builder.setView(dialogLayout)
            builder.show()
        }

        //check permissions if they're granted start scanning, otherwise ask to user to grant permissions
        private fun checkPermission(context: Context) {// alternative Permission library Dexter
            Permissions.check(context,
                permissionList, null, null,
                object : PermissionHandler() {
                    override fun onGranted() {
                        PivoSdk.getInstance().scan()
                    }
                })
        }

        //permissions which are required for bluetooth
        private var permissionList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        } else {
            TODO("VERSION.SDK_INT < S")
        }
    }
}