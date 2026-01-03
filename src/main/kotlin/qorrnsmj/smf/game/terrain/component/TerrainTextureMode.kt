package qorrnsmj.smf.game.terrain.component

import qorrnsmj.smf.graphic.`object`.TextureBufferObject

sealed interface TerrainTextureMode

data class SingleTexture(
    val baseTexture: TextureBufferObject
) : TerrainTextureMode

data class BlendedTexture(
    val blendMap: TextureBufferObject,
    val baseTexture: TextureBufferObject,
    val rTexture: TextureBufferObject,
    val gTexture: TextureBufferObject,
    val bTexture: TextureBufferObject
) : TerrainTextureMode
