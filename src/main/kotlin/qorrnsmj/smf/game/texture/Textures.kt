package qorrnsmj.smf.game.texture

import qorrnsmj.smf.graphic.`object`.TextureBufferObject

// TODO???: val ZOMBIE = Texture("entity/zombie.png")
// EntityTexture, TerrainTexture, SkyboxTexture
// それか今の方がきれい？
object Textures {
    lateinit var DEFAULT_000000: TextureBufferObject
    lateinit var DEFAULT_00FF00: TextureBufferObject
    lateinit var DEFAULT_8080FF: TextureBufferObject
    lateinit var DEFAULT_FFFFFF: TextureBufferObject

    lateinit var TERRAIN_BLEND_MAP: TextureBufferObject
    lateinit var TERRAIN_GRASS: TextureBufferObject
    lateinit var TERRAIN_FLOWER: TextureBufferObject
    lateinit var TERRAIN_DIRT: TextureBufferObject
    lateinit var TERRAIN_PATH: TextureBufferObject

    //lateinit var SKYBOX_SKY1: TextureBufferObject

    fun load() {
        DEFAULT_000000 = loadEntityTexture("_000000")
        DEFAULT_00FF00 = loadEntityTexture("_00FF00")
        DEFAULT_8080FF = loadEntityTexture("_8080FF")
        DEFAULT_FFFFFF = loadEntityTexture("_FFFFFF")

        TERRAIN_BLEND_MAP = loadTerrainTexture("blendmap")
        TERRAIN_GRASS = loadTerrainTexture("grass")
        TERRAIN_FLOWER = loadTerrainTexture("flower")
        TERRAIN_DIRT = loadTerrainTexture("dirt")
        TERRAIN_PATH = loadTerrainTexture("path")

        //SKYBOX_SKY1 = loadSkyboxTexture("sky1")
    }

    private fun loadEntityTexture(fileName: String): TextureBufferObject {
        return TextureLoader.loadTexture("assets/texture/entity/$fileName.png", TexturePresets.ENTITY)
    }

    private fun loadTerrainTexture(fileName: String): TextureBufferObject {
        return TextureLoader.loadTexture("assets/texture/terrain/$fileName.png", TexturePresets.TERRAIN)
    }

    // TODO
    private fun loadSkyboxTexture(fileId: String): TextureBufferObject {
        return TextureLoader.loadTexture("assets/texture/skybox/$fileId", TexturePresets.SKYBOX)
    }
}
