package qorrnsmj.smf.game.task.trigger

import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.math.Vector3f

class TeleportTrigger(
    areaCenter: Vector3f,
    areaHalfExtents: Vector3f,
    private val player: Player,
    private val destinationFeet: Vector3f,
    private val facing: TeleportFacing = TeleportFacing.Preserve,
    private val look: TeleportLook = TeleportLook.SameAsFacing,
    containmentMode: ContainmentMode = ContainmentMode.TOUCHING,
    private val onTeleport: () -> Unit = {},
) : AreaEnterTrigger(
    areaCenter = areaCenter,
    areaHalfExtents = areaHalfExtents,
    aabb = { player.getAabb() },
    containmentMode = containmentMode,
) {
    override fun fire() {
        player.setFeetPosition(destinationFeet)

        val facingFront = resolveFacingFront()
        val lookFront = resolveLookFront(facingFront)

        when {
            lookFront != null -> player.camera.setFront(lookFront)
            facingFront != null -> player.camera.setFront(facingFront)
        }

        onTeleport()
    }

    private fun resolveFacingFront(): Vector3f? {
        return when (facing) {
            TeleportFacing.Preserve -> null
            is TeleportFacing.Direction -> horizontal(facing.front)
            is TeleportFacing.LookAt -> horizontal(facing.target.subtract(destinationFeet))
        }
    }

    private fun resolveLookFront(facingFront: Vector3f?): Vector3f? {
        return when (look) {
            TeleportLook.Preserve -> null
            TeleportLook.SameAsFacing -> facingFront
            is TeleportLook.Direction -> look.front
            is TeleportLook.LookAt -> look.target.subtract(player.camera.position)
        }
    }

    private fun horizontal(front: Vector3f): Vector3f? {
        val horizontal = Vector3f(front.x, 0f, front.z)
        return if (horizontal.lengthSquared() > 0f) horizontal else null
    }
}
