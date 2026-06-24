package qorrnsmj.smf.game.task.cutscene

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.math.Vector3f

data class CameraKeyframe(
    val timeSeconds: Float,
    val position: Vector3f,
    val lookAt: Vector3f,
    val easingToNext: Easing = Easing.SMOOTH_STEP,
    val interpolationToNext: CameraInterpolation = CameraInterpolation.CATMULL_ROM,
) {
    init {
        require(timeSeconds >= 0f) { "Camera keyframe time must be zero or greater" }
    }
}

internal class CameraTrack(private val camera: Camera) {
    private val keyframes = mutableListOf<CameraKeyframe>()

    val endTimeSeconds: Float
        get() = keyframes.maxOfOrNull { it.timeSeconds } ?: 0f

    fun add(keyframe: CameraKeyframe) {
        keyframes.add(keyframe)
        keyframes.sortBy { it.timeSeconds }
    }

    fun apply(timeSeconds: Float) {
        if (keyframes.isEmpty()) return

        val first = keyframes.first()
        val last = keyframes.last()
        when {
            timeSeconds <= first.timeSeconds -> apply(first.position, first.lookAt)
            timeSeconds >= last.timeSeconds -> apply(last.position, last.lookAt)
            else -> {
                val nextIndex = keyframes.indexOfFirst { it.timeSeconds >= timeSeconds }
                val start = keyframes[nextIndex - 1]
                val end = keyframes[nextIndex]
                val segmentDuration = end.timeSeconds - start.timeSeconds
                val progress = if (segmentDuration <= 0f) 1f else {
                    (timeSeconds - start.timeSeconds) / segmentDuration
                }
                val easedProgress = start.easingToNext.apply(progress)
                apply(
                    position = interpolate(nextIndex, easedProgress) { it.position },
                    lookAt = interpolate(nextIndex, easedProgress) { it.lookAt },
                )
            }
        }
    }

    private fun interpolate(
        nextIndex: Int,
        progress: Float,
        value: (CameraKeyframe) -> Vector3f,
    ): Vector3f {
        val start = keyframes[nextIndex - 1]
        val end = keyframes[nextIndex]
        if (start.interpolationToNext == CameraInterpolation.LINEAR) {
            return value(start).lerp(value(end), progress)
        }

        val before = keyframes.getOrElse(nextIndex - 2) { start }
        val after = keyframes.getOrElse(nextIndex + 1) { end }
        return catmullRom(
            value(before),
            value(start),
            value(end),
            value(after),
            progress,
        )
    }

    private fun catmullRom(
        p0: Vector3f,
        p1: Vector3f,
        p2: Vector3f,
        p3: Vector3f,
        t: Float,
    ): Vector3f {
        val t2 = t * t
        val t3 = t2 * t
        return Vector3f(
            catmullRomComponent(p0.x, p1.x, p2.x, p3.x, t, t2, t3),
            catmullRomComponent(p0.y, p1.y, p2.y, p3.y, t, t2, t3),
            catmullRomComponent(p0.z, p1.z, p2.z, p3.z, t, t2, t3),
        )
    }

    private fun catmullRomComponent(
        p0: Float,
        p1: Float,
        p2: Float,
        p3: Float,
        t: Float,
        t2: Float,
        t3: Float,
    ): Float {
        return 0.5f * (
            2f * p1 +
                (-p0 + p2) * t +
                (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
                (-p0 + 3f * p1 - 3f * p2 + p3) * t3
            )
    }

    private fun apply(position: Vector3f, lookAt: Vector3f) {
        camera.position = position.copy()
        val front = lookAt.subtract(position)
        if (front.lengthSquared() > 0.000001f) {
            camera.setFront(front)
        }
    }

    private fun Vector3f.copy(): Vector3f = Vector3f(x, y, z)
}

enum class CameraInterpolation {
    LINEAR,
    CATMULL_ROM,
}
