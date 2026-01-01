package qorrnsmj.smf.game.terrain

import qorrnsmj.smf.graphic.`object`.TextureBufferObject
import qorrnsmj.smf.game.texture.Textures

/**
 * Terrain用のマテリアル
 * blendMapと4つのテクスチャ（grass, flower, dirt, path）を使用してマルチテクスチャリングを行う
 */
data class TerrainMaterial(
    val blendMap: TextureBufferObject,
    val grassTexture: TextureBufferObject,
    val flowerTexture: TextureBufferObject,
    val dirtTexture: TextureBufferObject,
    val pathTexture: TextureBufferObject
) {
    companion object {
        fun createDefault(): TerrainMaterial {
            return TerrainMaterial(
                blendMap = Textures.TERRAIN_BLEND_MAP,
                grassTexture = Textures.TERRAIN_GRASS,
                flowerTexture = Textures.TERRAIN_FLOWER,
                dirtTexture = Textures.TERRAIN_DIRT,
                pathTexture = Textures.TERRAIN_PATH
            )
        }
    }
}

