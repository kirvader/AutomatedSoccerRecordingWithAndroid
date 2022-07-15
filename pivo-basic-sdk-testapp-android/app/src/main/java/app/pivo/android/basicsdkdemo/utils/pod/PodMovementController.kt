package app.pivo.android.basicsdkdemo.utils.pod

import android.util.Log
import app.pivo.android.basicsdk.PivoSdk
import kotlin.math.abs


data class Point(
    val x: Float,
    val y: Float
)

data class ClassifiedBox(
    val center: Point,
    val width: Float,
    val height: Float,
    val classId: Int,
    val confidence: Float
)

data class XPositionWithTime( // x coord and time needed to arrive in it from last pos
    val pos: Float,
    val time: Float
)

class MovementController {
    companion object {
        private val lastPosAndTimes: MutableList<XPositionWithTime> = mutableListOf(
            XPositionWithTime(0.5f, 1.0f),
            XPositionWithTime(0.5f, 1.0f),
            XPositionWithTime(0.5f, 1.0f)
        )
        private var velocityList: MutableList<Float> =
            mutableListOf(0.0f, 0.0f) // approximate velocity on last consecutive segments
        private var acceleration: Float = 0.0f
        private var approximateInferenceTime: Float = 1.0f // in seconds
        private const val eps: Float = 0.00001F
    }

    fun updateObjectCurrentPosition(
        detectedObjectPositionWithTime: XPositionWithTime,
        lastSegmentCameraSpeed: Float
    ) {
        lastPosAndTimes.add(detectedObjectPositionWithTime)
        lastPosAndTimes.removeFirst()
        updatePhysicParameters(lastSegmentCameraSpeed)
    }

    private fun updatePhysicParameters(lastSegmentCameraSpeed: Float) {
        if (lastPosAndTimes[2].time > eps) {
            velocityList.add((lastPosAndTimes[2].pos - lastPosAndTimes[1].pos) / lastPosAndTimes[2].time + lastSegmentCameraSpeed)
        } else {
            velocityList.add(0.0f)
        }
        velocityList.removeFirst()

        acceleration = if (lastPosAndTimes[2].time + lastPosAndTimes[1].time > eps) {
            2 * (velocityList[1] - velocityList[0]) / (lastPosAndTimes[1].time + lastPosAndTimes[2].time)
        } else {
            0.0f
        }
        approximateInferenceTime =
            lastPosAndTimes.fold(0.0f) { acc, e -> acc + e.time } / lastPosAndTimes.size
        Log.e("Physics params", "approx time = $approximateInferenceTime")
        Log.e("Physics params", "current velocity = ${velocityList.last()}")
        Log.e("Physics params", "current accel = $acceleration")
        Log.e("Physics params", "current pos = ${lastPosAndTimes.last().pos}")
    }

    fun predictAbsoluteDeltaXPos(): Float {
        Log.e(
            "Prediction",
            "Next pos is ${(lastPosAndTimes[2].pos - 0.5f) + velocityList.last() * approximateInferenceTime + acceleration * approximateInferenceTime * approximateInferenceTime / 2}"
        )
        // Pod velocity is in seconds per round.


        return lastPosAndTimes[2].pos - 0.5f + velocityList.last() * approximateInferenceTime + acceleration * approximateInferenceTime * approximateInferenceTime / 2
    }
}

class PodController {
    companion object {
        private val movementController: MovementController = MovementController()
        private lateinit var possibleSpeeds: FloatArray
        private var currentCameraSpeed: Float = 1000.0f // Positive if turning right, negative if turning left
        private var approxInferenceTime: Float = 0.3f
    }

    init {
        val possibleSpeedsList: MutableList<Float> =
            PivoSdk.getInstance().supportedSpeeds.filter { it >= 20 && it <= 100 }
                .map { it.toFloat() }.toMutableList()
        val speedsListLastIndex = possibleSpeedsList.lastIndex
        for (i in 0..speedsListLastIndex) {
            possibleSpeedsList.add(-possibleSpeedsList[i])
        }
        possibleSpeedsList.add(1000.0f)
        possibleSpeedsList.reverse()
        possibleSpeeds = possibleSpeedsList.toFloatArray()

        PivoSdk.getInstance().setSpeed(currentCameraSpeed.toInt())
    }

    fun updateObjectCurrentPosition(detectedObject: ClassifiedBox, inferenceTime: Float) {
        movementController.updateObjectCurrentPosition(
            XPositionWithTime(
                detectedObject.center.x,
                inferenceTime
            ), getScreenPartVelocity(currentCameraSpeed)
        )
        updateApproxTime(inferenceTime)
        movePivoPod(inferenceTime)
    }

    fun updateObjectCurrentPosition(detectedObjectPosition: Point, inferenceTime: Float) {
        movementController.updateObjectCurrentPosition(
            XPositionWithTime(
                detectedObjectPosition.x,
                inferenceTime
            ), getScreenPartVelocity(currentCameraSpeed)
        )
        updateApproxTime(inferenceTime)
        movePivoPod(inferenceTime)
    }

    private fun updateApproxTime(nextInferenceTime: Float) {
        approxInferenceTime = (approxInferenceTime + nextInferenceTime) / 2
    }

    private fun getScreenPartWalkedByCameraWithSpeedForTime(speed: Float, time: Float): Float {
        val gradPerSecond: Float = if (abs(speed) >= 500.0f) {
            0.0f
        } else {
            360 / speed
        }
        val partOfScreenPerSecond: Float = gradPerSecond / 90
        return partOfScreenPerSecond * time
    }
    private fun getScreenPartVelocity(secondsPerRound: Float): Float {
        return getScreenPartWalkedByCameraWithSpeedForTime(secondsPerRound, 1.0f)
    }

    private fun getElemWithMinFuncValue(floatArr: FloatArray, func: (Float) -> Float): Float {
        var appropriateElem = floatArr.first()
        var elemFuncValue = func(appropriateElem)

        for (elem in floatArr) {
            if (elem == appropriateElem) continue

            val curElemFuncValue = func(elem)
            if (curElemFuncValue < elemFuncValue) {
                appropriateElem = elem
                elemFuncValue = curElemFuncValue
            }
        }

        return appropriateElem
    }

    private fun movePivoPod(lastFrameTime: Float) {
        var xCoordForNextBallPos =
            movementController.predictAbsoluteDeltaXPos()


        val screenPartWalkedByCameraInLastFrame = getScreenPartWalkedByCameraWithSpeedForTime(
            currentCameraSpeed, lastFrameTime)
        xCoordForNextBallPos -= screenPartWalkedByCameraInLastFrame

        val theMostAppropriateCameraSpeed = getElemWithMinFuncValue(possibleSpeeds) { elem ->
            abs(xCoordForNextBallPos - getScreenPartWalkedByCameraWithSpeedForTime(
                elem,
                approxInferenceTime
            ))
        }
        PivoSdk.getInstance().setSpeed(theMostAppropriateCameraSpeed.toInt())
        currentCameraSpeed = theMostAppropriateCameraSpeed

        xCoordForNextBallPos *= 90
        Log.e("PivoPod movement", "angle = $xCoordForNextBallPos");
        Log.e("PivoPod movement", "int angle = ${abs(xCoordForNextBallPos).toInt()}")
        if (xCoordForNextBallPos > 0) {
            PivoSdk.getInstance().turnRight(abs(xCoordForNextBallPos).toInt())
        } else if (xCoordForNextBallPos < 0) {
            PivoSdk.getInstance().turnLeft(abs(xCoordForNextBallPos).toInt())
        } else {
            PivoSdk.getInstance().stop()
        }

    }
}