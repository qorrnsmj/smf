package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.math.Vector3f

data class Transform(
    val position: Vector3f = Vector3f(0f, 0f, 0f),
    val rotation: Vector3f = Vector3f(0f, 0f, 0f),
    val scale: Vector3f = Vector3f(1f, 1f, 1f)
)
