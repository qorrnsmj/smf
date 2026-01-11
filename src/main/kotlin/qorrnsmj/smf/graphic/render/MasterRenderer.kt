package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.util.Cleanable
import qorrnsmj.smf.util.Resizable

class MasterRenderer : Resizable, Cleanable {
    private val postProcessor = PostProcessor()
    private val skyboxRenderer = SkyboxRenderer()
    private val terrainRenderer = TerrainRenderer()
    private val entityRenderer = EntityRenderer()

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

        skyboxRenderer.start()
        skyboxRenderer.loadCamera(scene.camera)
        skyboxRenderer.renderSkybox(scene.skybox)
        skyboxRenderer.stop()

        terrainRenderer.start()
        terrainRenderer.loadCamera(scene.camera)
        terrainRenderer.loadSkyColor(skyColor)
        terrainRenderer.loadFog(0.007f, 1.5f)
        terrainRenderer.renderTerrains(scene.terrain)
        terrainRenderer.stop()

        entityRenderer.start()
        entityRenderer.loadCamera(scene.camera)
        entityRenderer.loadLights(scene.lights)
        entityRenderer.loadSkyColor(skyColor)
        entityRenderer.loadFog(0.007f, 1.5f)
        entityRenderer.renderEntity(scene.camera, scene.entities)
        entityRenderer.stop()

        if (scene.effects.isNotEmpty()) {
            postProcessor.bindDefaultFrameBuffer()
            postProcessor.applyPostProcess(scene.effects)
        }
    }

    override fun cleanup() {
        postProcessor.cleanup()
        skyboxRenderer.cleanup()
        terrainRenderer.cleanup()
        entityRenderer.cleanup()

        Logger.info("MasterRenderer cleaned up!")
    }

    override fun resize(width: Int, height: Int) {
        glViewport(0, 0, width, height)
        postProcessor.resize(width, height)

        skyboxRenderer.start()
        skyboxRenderer.resize(width, height)
        skyboxRenderer.stop()

        terrainRenderer.start()
        terrainRenderer.resize(width, height)
        terrainRenderer.stop()

        entityRenderer.start()
        entityRenderer.resize(width, height)
        entityRenderer.stop()
    }
}
