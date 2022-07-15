package app.pivo.android.basicsdkdemo.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import app.pivo.android.basicsdk.PivoSdk
import app.pivo.android.basicsdk.events.PivoEvent
import app.pivo.android.basicsdk.events.PivoEventBus
import app.pivo.android.basicsdkdemo.R
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_pivo_controller.*


class PivoControllerActivity : AppCompatActivity() {

    private val TAG = "PivoControllerActivity"

    private fun openCameraActivity(){
        startActivity(Intent(this, CameraActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pivo_controller)

        //show pivo version
        version_view.text = PivoSdk.getInstance().pivoVersion

        //rotate continuously to left
        btn_left_con_turn.setOnClickListener { PivoSdk.getInstance().turnLeftContinuously() }

        //rotate to left
        btn_left_turn.setOnClickListener { PivoSdk.getInstance().turnLeft(getAngle()) }

        //rotate continuously to right
        btn_right_con_turn.setOnClickListener { PivoSdk.getInstance().turnRightContinuously() }

        //rotate to right
        btn_right_turn.setOnClickListener { PivoSdk.getInstance().turnRight(getAngle()) }

        //stop rotating the device
        btn_stop.setOnClickListener { PivoSdk.getInstance().stop() }

        // Transition to the camera activity
        buttonToCamera.setOnClickListener { openCameraActivity() }

        //change Pivo name
        btn_change_name.setOnClickListener {
            if (input_pivo_name.text.isNotEmpty()){
                PivoSdk.getInstance().changeName(input_pivo_name.text.toString())
            }
        }

        //get Pivo supported speed list
        val speedList = PivoSdk.getInstance().supportedSpeeds.toMutableList()
        //speed list view
        speed_list_view.adapter= ArrayAdapter<Int>(this, android.R.layout.simple_spinner_item, speedList)
        speed_list_view.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.e(TAG, "onSpeedChange: ${speedList[position]} save: ${save_speed_view.isChecked}")
                PivoSdk.getInstance().setSpeed(speedList[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onStart() {
        super.onStart()
        //subscribe to connection failure event
        PivoEventBus.subscribe(
            PivoEventBus.CONNECTION_FAILURE,this, Consumer {
                if (it is PivoEvent.ConnectionFailure){
                    finish()
                }
            })
        //subscribe pivo remote controller event
        PivoEventBus.subscribe(
            PivoEventBus.REMOTE_CONTROLLER, this, Consumer {
            when(it){
                is PivoEvent.RCCamera->notification_view.text = "CAMERA"
                is PivoEvent.RCMode->notification_view.text = "MODE"
                is PivoEvent.RCStop->notification_view.text = "STOP"
                is PivoEvent.RCRightContinuous->notification_view.text = "RIGHT_CONTINUOUS"
                is PivoEvent.RCLeftContinuous->notification_view.text = "LEFT_CONTINUOUS"
                is PivoEvent.RCLeftPressed->notification_view.text = "LEFT_PRESSED"
                is PivoEvent.RCLeftRelease->notification_view.text = "LEFT_RELEASE"
                is PivoEvent.RCRightPressed->notification_view.text = "RIGHT_PRESSED"
                is PivoEvent.RCRightRelease->notification_view.text = "RIGHT_RELEASE"
                is PivoEvent.RCSpeedUpPressed->notification_view.text = "SPEED_UP_PRESSED: ${it.level}"
                is PivoEvent.RCSpeedDownPressed->notification_view.text = "SPEED_DOWN_PRESSED: ${it.level}"
                is PivoEvent.RCSpeedUpRelease->notification_view.text = "SPEED_UP_RELEASE: ${it.level}"
                is PivoEvent.RCSpeedDownRelease->notification_view.text = "SPEED_DOWN_RELEASE: ${it.level}"
                is PivoEvent.RCSpeed->notification_view.text = "SPEED: ${it.level}"
            }
        })
        //subscribe to name change event
        PivoEventBus.subscribe(
            PivoEventBus.NAME_CHANGED, this, Consumer {
            if (it is PivoEvent.NameChanged){
                notification_view.text = "Name: ${it.name}"
            }
        })
        //subscribe to get pivo notifications
        PivoEventBus.subscribe(
            PivoEventBus.PIVO_NOTIFICATION, this, Consumer {
            if (it is PivoEvent.BatteryChanged){
                notification_view.text = "BatteryLevel: ${it.level}"
            }else{
                notification_view.text = "Notification Received"
            }
        })
    }

    private fun getAngle():Int{
        return try {
            val num = angle_view.text.toString()
            num.toInt()
        }catch (e:NumberFormatException){
            90
        }
    }

    override fun onPause() {
        super.onPause()
        //unregister before stopping the activity
        PivoEventBus.unregister(this)
    }
}
