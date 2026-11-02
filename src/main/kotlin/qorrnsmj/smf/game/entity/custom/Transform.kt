package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.math.Quaternion
import qorrnsmj.smf.math.Vector3f

data class Transform(
    val position: Vector3f = Vector3f(0f, 0f, 0f),
    val rotation: Quaternion = Quaternion.identity(),
    val scale: Vector3f = Vector3f(1f, 1f, 1f)
)
