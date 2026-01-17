package qorrnsmj.smf.graphic.texture

import qorrnsmj.smf.graphic.`object`.TextureBufferObject

// TODO???: val ZOMBIE = Texture("entity/zombie.png")
// EntityTexture, TerrainTexture, SkyboxTexture
// それか今の方がきれい？
object Textures {
    lateinit var DEFAULT_000000: TextureBufferObject
    lateinit var DEFAULT_00FF00: TextureBufferObject
    lateinit var DEFAULT_8080FF: TextureBufferObject
    lateinit var DEFAULT_FFFFFF: TextureBufferObject

    lateinit var TERRAIN_HEIGHT_MAP: TextureBufferObject
    lateinit var TERRAIN_BLEND_MAP: TextureBufferObject
    lateinit var TERRAIN_GRASS: TextureBufferObject
    lateinit var TERRAIN_FLOWER: TextureBufferObject
    lateinit var TERRAIN_DIRT: TextureBufferObject
    lateinit var TERRAIN_PATH: TextureBufferObject

    //lateinit var SKYBOX_SKY1: TextureBufferObject

    fun load() {
        DEFAULT_000000 = loadEntityTexture("_000000.png")
        DEFAULT_00FF00 = loadEntityTexture("_00FF00.png")
        DEFAULT_8080FF = loadEntityTexture("_8080FF.png")
        DEFAULT_FFFFFF = loadEntityTexture("_FFFFFF.png")

        TERRAIN_HEIGHT_MAP = loadTerrainTexture("heightmap.png")
        TERRAIN_BLEND_MAP = loadTerrainTexture("blendmap.png")
        TERRAIN_GRASS = loadTerrainTexture("grass.png")
        TERRAIN_FLOWER = loadTerrainTexture("flower.png")
        TERRAIN_DIRT = loadTerrainTexture("dirt.png")
        TERRAIN_PATH = loadTerrainTexture("path.png")

        //SKYBOX_SKY1 = loadSkyboxTexture("sky1.png")
    }

    private fun loadEntityTexture(fileName: String): TextureBufferObject {
        return TextureLoader.loadTexture("assets/texture/entity/$fileName", TexturePresets.ENTITY)
    }

    private fun loadTerrainTexture(fileName: String): TextureBufferObject {
        return TextureLoader.loadTexture("assets/texture/terrain/$fileName", TexturePresets.TERRAIN)
    }

    // TODO: skybox一つのテクスチャにする
    private fun loadSkyboxTexture(fileId: String): TextureBufferObject {
        return TextureLoader.loadTexture("assets/texture/skybox/$fileId", TexturePresets.SKYBOX)
    }
}

