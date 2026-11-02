package qorrnsmj.smf.graphic.scene.terrain

import qorrnsmj.smf.graphic.resource.texture.Textures
import qorrnsmj.smf.graphic.scene.terrain.component.BlendedTexture

object Terrains {
    lateinit var DEFAULT: Terrain
    lateinit var FLAT: Terrain
    lateinit var PLANE: Terrain
    lateinit var SUNWASH_MEADOW: Terrain
    lateinit var MOONVEIL_MEADOW: Terrain

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

        SUNWASH_MEADOW = MeadowTerrainGenerator.create(seed = 2_048, texture = Textures.TERRAIN_GRASS)
        MOONVEIL_MEADOW = MeadowTerrainGenerator.create(seed = 4_096, texture = Textures.TERRAIN_GRASS)
    }
}
