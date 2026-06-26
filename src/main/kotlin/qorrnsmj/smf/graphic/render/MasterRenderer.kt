package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.util.Resizable

class MasterRenderer : SceneRenderer, Resizable {
    val skyboxRenderer = SkyboxRenderer()
    val terrainRenderer = TerrainRenderer()
    val mapRenderer = MapRenderer()
    val entityRenderer = EntityRenderer()
    val shadowRenderer = ShadowRenderer()
    val postProcessor = PostProcessor()
    val debugRenderer = DebugRenderer()
    val cinematicOverlayRenderer = CinematicOverlayRenderer()
    val textRenderer = TextRenderer()
    private var viewportWidth = 1280
    private var viewportHeight = 720

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

    override fun render(scene: Scene) {
        val shadowState = shadowRenderer.render(scene)
        glViewport(0, 0, viewportWidth, viewportHeight)

        // If there are no effects, render directly to the default frame-buffer
        if (scene.effects.isNotEmpty()) postProcessor.bindFrameBuffer()

        val skyColor = scene.skyColor
        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        skyboxRenderer.render(scene)
        terrainRenderer.render(scene, shadowState)
        mapRenderer.render(scene, shadowState)
        entityRenderer.render(scene, shadowState)

        if (scene.effects.isNotEmpty()) {
            postProcessor.bindDefaultFrameBuffer()
            postProcessor.applyPostProcess(scene.effects)
        }

        debugRenderer.render(scene)
        cinematicOverlayRenderer.render(scene)
        textRenderer.render(scene)
    }

    override fun resize(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        glViewport(0, 0, width, height)

        entityRenderer.resize(width, height)
        terrainRenderer.resize(width, height)
        mapRenderer.resize(width, height)
        skyboxRenderer.resize(width, height)
        postProcessor.resize(width, height)
        debugRenderer.resize(width, height)
        textRenderer.resize(width, height)
    }
}
