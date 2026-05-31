package qorrnsmj.smf.game.task.trigger

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.data.AABB

open class AreaEnterTrigger(
    areaCenter: Vector3f,
    areaHalfExtents: Vector3f,
    private val aabb: () -> AABB,
) : Trigger() {

    private val areaAabb: AABB = AABB(
        min = areaCenter.subtract(areaHalfExtents),
        max = areaCenter.add(areaHalfExtents)
    )

    override fun check(): Boolean {
        if (finished) return false
        return areaAabb.intersects(aabb())
    }
}
