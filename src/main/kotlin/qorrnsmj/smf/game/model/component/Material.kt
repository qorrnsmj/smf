package qorrnsmj.smf.game.model.component

import de.javagl.jgltf.model.v2.MaterialModelV2.AlphaMode
import qorrnsmj.smf.game.texture.Textures
import qorrnsmj.smf.graphic.`object`.TextureBufferObject
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f

data class Material(
    // factors
    val baseColorFactor: Vector4f = Vector4f(1f, 1f, 1f, 1f),
    val emissiveFactor: Vector3f = Vector3f(0f, 0f, 0f),
    val metallicFactor: Float = 0f,
    val roughnessFactor: Float = 1f,

    // textures
    val baseColorTexture: TextureBufferObject = Textures.DEFAULT_000000,
    val metallicRoughnessTexture: TextureBufferObject = Textures.DEFAULT_00FF00,
    val normalTexture: TextureBufferObject = Textures.DEFAULT_8080FF,
    val occlusionTexture: TextureBufferObject = Textures.DEFAULT_FFFFFF,
    val emissiveTexture: TextureBufferObject = Textures.DEFAULT_000000,

    // texture params
    val normalScale: Float = 1f,
    val occlusionStrength: Float = 1f,

    // TODO
    // render states
    val alphaMode: AlphaMode = AlphaMode.OPAQUE,
    val alphaCutoff: Float = 0.5f,
    val doubleSided: Boolean = false,
)
