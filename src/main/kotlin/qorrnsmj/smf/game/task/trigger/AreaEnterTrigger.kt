package qorrnsmj.smf.game.task.trigger

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.data.AABB

open class AreaEnterTrigger(
    areaCenter: Vector3f,
    areaHalfExtents: Vector3f,
    private val aabb: () -> AABB,
    private val containmentMode: ContainmentMode = ContainmentMode.TOUCHING,
) : Trigger() {

    private val areaAabb: AABB = AABB(
        min = areaCenter.subtract(areaHalfExtents),
        max = areaCenter.add(areaHalfExtents)
    )

    override fun check(): Boolean {
        if (finished) return false
        val targetAabb = aabb()
        return when (containmentMode) {
            ContainmentMode.TOUCHING -> areaAabb.intersects(targetAabb)
            ContainmentMode.CENTER_INSIDE -> areaAabb.contains(targetAabb.center())
            ContainmentMode.FULLY_INSIDE -> areaAabb.contains(targetAabb.min) && areaAabb.contains(targetAabb.max)
        }
    }

    private fun AABB.center(): Vector3f =
        min.add(max).scale(0.5f)
}
