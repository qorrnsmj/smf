package qorrnsmj.smf.graphic.debug

import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f

sealed interface DebugPrimitive {
    val color: Vector4f
}

data class DebugBox(
    val center: Vector3f,
    val size: Vector3f,
    val rotation: Vector3f,
    override val color: Vector4f,
) : DebugPrimitive

data class DebugSphere(
    val center: Vector3f,
    val radius: Float,
    override val color: Vector4f,
) : DebugPrimitive

data class DebugCapsule(
    val feetPosition: Vector3f,
    val radius: Float,
    val height: Float,
    override val color: Vector4f,
) : DebugPrimitive

data class DebugLine(
    val from: Vector3f,
    val to: Vector3f,
    override val color: Vector4f,
) : DebugPrimitive
