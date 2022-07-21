package app.pivo.android.basicsdkdemo.movementController

import app.pivo.android.basicsdkdemo.movementController.utils.Point
import app.pivo.android.basicsdkdemo.movementController.utils.convertRadianToGrad
import kotlin.math.min


open class DeviceRotatingControllerBase {
    private var lastUpdatedDirection: Float = 0.0f
    private var currentPODRotationSpeed: Float = 0.0f
    private var lastUpdatedRotationLeftover: Float = 0.0f
    private var lastUpdateTime = System.currentTimeMillis()

    private lateinit var rotateDevice: RotateDeviceInterface

    fun initializeRotateDevice(rotateDeviceImplementation: RotateDeviceInterface) {
        rotateDevice = rotateDeviceImplementation
    }

    fun getLastDirection() = lastUpdatedDirection

    /**
     * Updated inner state of controller. Such parameters as current time, current rotation left to perform, current rotation.
     */
    private fun updateCurrentState() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000.0f
        lastUpdateTime = currentTime

        val podTraveledRotation = min(currentPODRotationSpeed * deltaTime, lastUpdatedRotationLeftover)
        lastUpdatedRotationLeftover -= podTraveledRotation
        lastUpdatedDirection += podTraveledRotation
    }

    /**
     * @param targetPosition position of tracked object in 3D coordinate system where (0, 0, 0) is device position
     */
    private fun rotatePodToSeeTargetPosition(targetPosition: Point, averageSegmentTime: Float) {
        val deltaGradAngle = convertRadianToGrad(targetPosition.getAngle()) - lastUpdatedDirection
        lastUpdatedRotationLeftover = deltaGradAngle

        if (averageSegmentTime == 0.0f)
            return // TODO throw something or notice user about it

        if (!this::rotateDevice.isInitialized)
        {
            // TODO throw something or notice user about it
            return
        }
        val speedOfEvenMovement = deltaGradAngle / averageSegmentTime
        val availableAppropriateSpeed = rotateDevice.getTheMostAppropriateSpeedFromAvailable(speedOfEvenMovement)
        rotateDevice.rotateBy(availableAppropriateSpeed, lastUpdatedRotationLeftover)

        // To make sure we are storing an exact same speed as device using in real life
        currentPODRotationSpeed = rotateDevice.getGradPerSecSpeedFromAvailable(speedOfEvenMovement)
    }

    /**
     * 1. Updating inner state of device
     * 2. Setting up speed and angle to turn device to see target position(3D with zero at device position) at the center
     */
    fun updateAndMovePodToTargetPosition(targetPosition: Point, averageSegmentTime: Float) {
        updateCurrentState()
        rotatePodToSeeTargetPosition(targetPosition, averageSegmentTime)
    }


    /**
     * 1. Updating inner state of device
     * 2. Setting up an exact  speed and angle
     */
    fun updateAndMovePodBy(speed: Float, orientedAngle: Float) { // if we will want to control it manually
        updateCurrentState()
        if (!this::rotateDevice.isInitialized)
        {
            // TODO throw something or notice user about it
            return
        }
        val availableAppropriateDeviceSpeed = rotateDevice.getGradPerSecSpeedFromAvailable(speed)
        rotateDevice.rotateBy(availableAppropriateDeviceSpeed, orientedAngle)

        lastUpdatedRotationLeftover = orientedAngle
        currentPODRotationSpeed = rotateDevice.getGradPerSecSpeedFromAvailable(availableAppropriateDeviceSpeed)
    }
}