package qorrnsmj.smf.game.entity.model.component

import qorrnsmj.smf.game.entity.model.EntityLoader
import qorrnsmj.smf.graphic.`object`.TextureBufferObject
import qorrnsmj.smf.math.Vector3f

data class Material(
    val diffuseColor: Vector3f = Vector3f(1.0f, 1.0f, 1.0f),
    val ambientColor: Vector3f = Vector3f(0.0f, 0.0f, 0.0f),
    val specularColor: Vector3f = Vector3f(1.0f, 1.0f, 1.0f),
    val emissiveColor: Vector3f = Vector3f(0.0f, 0.0f, 0.0f),
    val shininess: Float = 32.0f,
    val opacity: Float = 1.0f,
    val diffuseTexture: TextureBufferObject = EntityLoader.loadTexture("null_diff.png"),
    val specularTexture: TextureBufferObject = EntityLoader.loadTexture("null_spec.png"),
    val normalTexture: TextureBufferObject = EntityLoader.loadTexture("null_norm.png"),
)
