package qorrnsmj.smf.game.task.sequence

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.math.Vector3f

class CameraMovementSequence(
    private val camera: Camera,
    private val startPosition: Vector3f,
    private val targetPosition: Vector3f,
    private val duration: Float,
) : Sequence() {

    init {
        camera.position = Vector3f(startPosition.x, startPosition.y, startPosition.z)
    }

    override fun update(delta: Float) {
        super.update(delta)

        if (elapsedTime >= duration) {
            camera.position = Vector3f(targetPosition.x, targetPosition.y, targetPosition.z)
            finished = true
            return
        }

        val progress = elapsedTime / duration
        val currentPos = Vector3f(
            startPosition.x + (targetPosition.x - startPosition.x) * progress,
            startPosition.y + (targetPosition.y - startPosition.y) * progress,
            startPosition.z + (targetPosition.z - startPosition.z) * progress,
        )
        camera.position = currentPos
    }
}
