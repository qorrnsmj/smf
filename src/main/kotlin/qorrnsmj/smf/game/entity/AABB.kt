package qorrnsmj.smf.game.entity

import qorrnsmj.smf.math.Vector3f

class AABB(entity: Entity) {
    private val min: Vector3f = entity.position
    private val max: Vector3f = min.add(entity.size)

    fun intersects(other: AABB): Boolean {
        return !(this.max.x < other.min.x || this.max.y < other.min.y || this.max.z < other.min.z ||
                this.min.x > other.max.x || this.min.y > other.max.y || this.min.z > other.max.z)
    }
}
