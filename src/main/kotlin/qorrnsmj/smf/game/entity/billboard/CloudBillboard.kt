package qorrnsmj.smf.game.entity.billboard

import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.graphic.billboard.Billboard
import qorrnsmj.smf.graphic.billboard.BillboardAlphaSource
import qorrnsmj.smf.graphic.billboard.BillboardFacing
import qorrnsmj.smf.graphic.resource.buffer.TextureBufferObject
import qorrnsmj.smf.math.Vector2f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.smf.physics.component.KinematicPhysics

class CloudBillboard(
    position: Vector3f,
    size: Vector2f,
    texture: TextureBufferObject,
    tint: Vector4f = Vector4f(1f, 1f, 1f, 0.32f),
    velocity: Vector3f = Vector3f(),
) : BillboardEntity(
    transform = Transform(position = position),
    billboard = Billboard(
        texture = texture,
        size = size,
        tint = tint,
        facing = BillboardFacing.HORIZONTAL,
        doubleSided = true,
        alphaSource = BillboardAlphaSource.RED,
        edgeFade = 0.42f,
    ),
    physicsComponent = KinematicPhysics(velocity = velocity),
) {
    fun wrapWithin(minX: Float, maxX: Float, minZ: Float, maxZ: Float) {
        val position = localTransform.position
        val wrappedX = wrapCoordinate(position.x, minX, maxX)
        val wrappedZ = wrapCoordinate(position.z, minZ, maxZ)
        if (wrappedX == position.x && wrappedZ == position.z) return

        localTransform = localTransform.copy(
            position = Vector3f(wrappedX, position.y, wrappedZ),
        )
    }

    private fun wrapCoordinate(value: Float, minimum: Float, maximum: Float): Float {
        require(maximum > minimum) { "Cloud wrap maximum must be greater than minimum." }
        if (value in minimum..maximum) return value

        val range = maximum - minimum
        var offset = (value - minimum) % range
        if (offset < 0f) offset += range
        return minimum + offset
    }
}
