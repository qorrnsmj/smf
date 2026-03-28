package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.render.debug.DebugRenderer
import qorrnsmj.smf.graphic.text.TextRenderer
import qorrnsmj.smf.util.Cleanable
import qorrnsmj.smf.util.Resizable

class MasterRenderer : Resizable, Cleanable {
    val postProcessor = PostProcessor()
    val skyboxRenderer = SkyboxRenderer()
    val terrainRenderer = TerrainRenderer()
    val entityRenderer = EntityRenderer()
    val debugRenderer = DebugRenderer()
    val textRenderer = TextRenderer()

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

        val skyColor = scene.skyColor
        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        skyboxRenderer.start()
        skyboxRenderer.loadCamera(scene.camera)
        skyboxRenderer.loadSkyColor(skyColor)
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

        // Render debug visualizations after post-processing, before text
        debugRenderer.render(scene.entities, scene.camera.getViewMatrix())

        if (scene.textElements.isNotEmpty()) {
            textRenderer.start()
            textRenderer.renderText(scene.textElements)
            textRenderer.stop()
        }
    }

    override fun cleanup() {
        entityRenderer.cleanup()
        terrainRenderer.cleanup()
        skyboxRenderer.cleanup()
        postProcessor.cleanup()
        debugRenderer.cleanup()
        textRenderer.cleanup()

        Logger.info("MasterRenderer cleaned up!")
    }

    override fun resize(width: Int, height: Int) {
        glViewport(0, 0, width, height)

        entityRenderer.start()
        entityRenderer.resize(width, height)
        entityRenderer.stop()

        terrainRenderer.start()
        terrainRenderer.resize(width, height)
        terrainRenderer.stop()

        skyboxRenderer.start()
        skyboxRenderer.resize(width, height)
        skyboxRenderer.stop()

        postProcessor.resize(width, height)
        debugRenderer.resize(width, height)
        textRenderer.resize(width, height)
    }
}
