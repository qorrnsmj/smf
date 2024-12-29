package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.skybox.SkyboxModels
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.effect.*
import qorrnsmj.smf.graphic.effect.custom.BlurHorizontalEffect
import qorrnsmj.smf.graphic.effect.custom.BlurVerticalEffect
import qorrnsmj.smf.graphic.effect.custom.NoiseEffect
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.impl.Resizable

class MasterRenderer : Resizable {
    private val postProcessor = PostProcessor()
    private val entityRenderer = EntityRenderer()
    private val terrainRenderer = TerrainRenderer()
    private val skyboxRenderer = SkyboxRenderer()

    init {
        Logger.info("MasterRenderer initializing...")
        
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glEnable(GL_DEPTH_TEST)
        glClearColor(0f, 0f, 0f, 1f)

        Logger.info("MasterRenderer initialized!")
    }

    fun render(scene: Scene) {
        // If there are no effects, render directly to the default frame-buffer
        if (scene.effects.isNotEmpty()) postProcessor.bindFrameBuffer()

        // TODO: Implement sky-color handling
        val skyColor = scene.skyColor
        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        entityRenderer.start()
        entityRenderer.loadCamera(scene.camera)
        entityRenderer.loadLights(scene.lights)
        entityRenderer.loadSkyColor(skyColor)
        entityRenderer.renderEntities(scene.entities)
        entityRenderer.stop()

        terrainRenderer.start()
        terrainRenderer.loadCamera(scene.camera)
        terrainRenderer.loadSkyColor(skyColor)
        terrainRenderer.renderTerrains(scene.terrains)
        terrainRenderer.stop()

        skyboxRenderer.start()
        skyboxRenderer.loadCamera(scene.camera)
        skyboxRenderer.render(SkyboxModels.SKY1)

        if (scene.effects.isNotEmpty()) {
            postProcessor.unbindFrameBuffer()
            postProcessor.applyPostProcess(scene.effects)
        }
    }

    fun cleanup() {
        Logger.info("MasterRenderer cleaned up!")

        // TODO
        postProcessor.cleanup()
        //entityRenderer.cleanup()
        //terrainRenderer.cleanup()
        //skyboxRenderer.cleanup()
    }

    override fun resize(width: Int, height: Int) {
        glViewport(0, 0, width, height)
        postProcessor.resize(width, height)

        entityRenderer.start()
        entityRenderer.resize(width, height)
        entityRenderer.stop()

        terrainRenderer.start()
        terrainRenderer.resize(width, height)
        terrainRenderer.stop()

        skyboxRenderer.start()
        skyboxRenderer.resize(width, height)
        skyboxRenderer.stop()
    }
}
