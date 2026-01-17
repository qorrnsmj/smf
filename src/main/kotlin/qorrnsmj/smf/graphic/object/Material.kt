package qorrnsmj.smf.graphic.`object`

import de.javagl.jgltf.model.v2.MaterialModelV2.AlphaMode
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f

data class Material(
    // factors
    val baseColorFactor: Vector4f = Vector4f(1f, 1f, 1f, 1f),
    val emissiveFactor: Vector3f = Vector3f(0f, 0f, 0f),
    val metallicFactor: Float = 0f,
    val roughnessFactor: Float = 1f,

    // textures
    val baseColorTexture: TextureBufferObject,
    val metallicRoughnessTexture: TextureBufferObject,
    val normalTexture: TextureBufferObject,
    val occlusionTexture: TextureBufferObject,
    val emissiveTexture: TextureBufferObject,

    // texture params
    val normalScale: Float = 1f,
    val occlusionStrength: Float = 1f,

    // TODO
    // render states
    val alphaMode: AlphaMode = AlphaMode.OPAQUE,
    val alphaCutoff: Float = 0.5f,
    val doubleSided: Boolean = false,
)

