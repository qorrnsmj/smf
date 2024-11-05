package qorrnsmj.smf.game.entity

import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f

abstract class Entity(
    val id: String,
    val modelId: String
) {
    var modelMatrix = Matrix4f()
    var position = Vector3f()
    var rotation = Vector4f()
    var size = Vector3f()
    var scale = 1.0f

    fun updateModelMatrix() {
//        modelMatrix.setIdentity()
//        modelMatrix.translate(position)
//        modelMatrix.rotate(rotation)
//        modelMatrix.scale(scale)
//        modelMatrix.translationRotateScale(position, rotation, scale);
    }
}
