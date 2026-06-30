package qorrnsmj.smf.game.task.trigger

import qorrnsmj.smf.math.Vector3f

sealed class TeleportFacing {
    object Preserve : TeleportFacing()
    data class Direction(val front: Vector3f) : TeleportFacing()
    data class LookAt(val target: Vector3f) : TeleportFacing()
}

sealed class TeleportLook {
    object Preserve : TeleportLook()
    object SameAsFacing : TeleportLook()
    data class Direction(val front: Vector3f) : TeleportLook()
    data class LookAt(val target: Vector3f) : TeleportLook()
}
