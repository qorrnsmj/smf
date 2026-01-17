package qorrnsmj.smf.game.terrain

import qorrnsmj.smf.game.terrain.component.BlendedTexture
import qorrnsmj.smf.game.texture.Textures

object Terrains {
    lateinit var DEFAULT: Terrain
    lateinit var FLAT: Terrain
    lateinit var PLANE: Terrain

    fun load() {
        //DEFAULT = TerrainLoader.loadModel() // TODO

        FLAT = TerrainLoader.loadModel(
            sizeX = 400f,
            sizeY = 400f,
            vertexCount = 64,
            textureMode = BlendedTexture(
                blendMap = Textures.TERRAIN_BLEND_MAP,
                baseTexture = Textures.TERRAIN_GRASS,
                rTexture = Textures.TERRAIN_DIRT,
                gTexture = Textures.TERRAIN_FLOWER,
                bTexture = Textures.TERRAIN_PATH,
            )
        )

        DEFAULT = FLAT

        PLANE = TerrainLoader.loadModel(
            sizeX = 400f,
            sizeY = 400f,
            vertexCount = 64,
            heightmapFile = "heightmap.png",
            textureMode = BlendedTexture(
                blendMap = Textures.TERRAIN_BLEND_MAP,
                baseTexture = Textures.TERRAIN_GRASS,
                rTexture = Textures.TERRAIN_DIRT,
                gTexture = Textures.TERRAIN_FLOWER,
                bTexture = Textures.TERRAIN_PATH,
            )
        )
    }
}
