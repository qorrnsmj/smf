package qorrnsmj.smf.game.task.trigger

import qorrnsmj.smf.math.Vector3f

// TODO: AABB
open class AreaEnterTrigger(
    private val areaCenter: Vector3f,
    private val areaRadius: Float,
    private val playerPosition: () -> Vector3f,
) : Trigger() {

    override fun check(): Boolean {
        if (finished) return false

        val distance = playerPosition().subtract(areaCenter).length()
        if (distance <= areaRadius) {
            return true
        }

        return false
    }
}
