package com.hawkeye.movement

import com.hawkeye.movement.interfaces.PhysicalObjectMovementModel
import com.hawkeye.movement.interfaces.RotatableDevice
import com.hawkeye.movement.interfaces.RotatableDeviceControllerBase
import com.hawkeye.movement.interfaces.TrackingSystemControllerBase
import com.hawkeye.movement.utils.AngleMeasure
import com.hawkeye.movement.utils.Degree
import com.hawkeye.movement.utils.Point

open class TrackingSystemController(rotatableDevice: RotatableDevice) :
    TrackingSystemControllerBase {

    protected val ballMovementModel: PhysicalObjectMovementModel = PhysicalObjectMovement(relevanceDeltaTime)
    protected var rotatableDeviceController: RotatableDeviceControllerBase

    private var lastCorrectPositionUpdateTime = 0L
    private enum class SystemStatus {
        Tracking,
        LostObject_TryingLeft,
        LostObject_TryingRight
    }

    private var systemStatus = SystemStatus.Tracking
    private var lastStatusUpdate = 0L

    init {
        rotatableDeviceController = RotatableDeviceController(rotatableDevice, relevanceDeltaTime)
    }

    private fun updateSystemStatusAtTime(absTime_ms: Long, objectLost: Boolean) {

        if (!objectLost) {
            lastStatusUpdate = absTime_ms
            systemStatus = SystemStatus.Tracking
            return
        }
        if (absTime_ms - lastStatusUpdate < statusUpdateTime) {
            return
        }
        lastStatusUpdate = absTime_ms
        systemStatus = if (systemStatus != SystemStatus.LostObject_TryingLeft) {
            SystemStatus.LostObject_TryingLeft
        } else {
            SystemStatus.LostObject_TryingRight
        }
    }

    override fun updateTargetPosition(point: Point?, currentAbsTime_ms: Long) {
        if (point != null) {
            lastCorrectPositionUpdateTime = currentAbsTime_ms
        }
        ballMovementModel.updatePositionAtTime(point, currentAbsTime_ms)
    }

    override fun directDeviceAtObjectAtTime(currentAbsTime_ms: Long, targetAbsTime_ms: Long) {
        val targetPosition =
            ballMovementModel.getApproximatePositionAtTime(targetAbsTime_ms)

        updateSystemStatusAtTime(currentAbsTime_ms, targetPosition == null)

        if (targetPosition == null) {
            if (systemStatus == SystemStatus.LostObject_TryingLeft) {
                rotatableDeviceController.rotateByAngleAtTime(Degree(-10f), Degree(180f / (statusUpdateTime / 1000L)), currentAbsTime_ms)
            } else if (systemStatus == SystemStatus.LostObject_TryingRight) {
                rotatableDeviceController.rotateByAngleAtTime(Degree(10f), Degree(180f / (statusUpdateTime / 1000L)), currentAbsTime_ms)
            }
            return
        }

        rotatableDeviceController.smoothlyDirectDeviceAt(
            targetPosition,
            currentAbsTime_ms,
            targetAbsTime_ms
        )
    }

    override fun getObjectPositionAtTime(absTime_ms: Long): Point? {
        return ballMovementModel.getApproximatePositionAtTime(absTime_ms)
    }

    override fun getCameraDirectionAndFOVAngleAtTime(absTime_ms: Long): Pair<AngleMeasure, AngleMeasure> {
        return Pair(rotatableDeviceController.getDirectionAtTime(absTime_ms), Degree(90.0f))
    }

    companion object {
        private const val relevanceDeltaTime = 1000L

        private const val statusUpdateTime = 10000L
    }
}