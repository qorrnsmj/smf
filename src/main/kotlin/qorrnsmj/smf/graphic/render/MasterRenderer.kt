package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.ViewportShadingMode
import qorrnsmj.smf.util.Resizable

class MasterRenderer : SceneRenderer, Resizable {
    val skyboxRenderer = SkyboxRenderer()
    val terrainRenderer = TerrainRenderer()
    val entityRenderer = EntityRenderer()
    val postProcessor = PostProcessor()
    val debugRenderer = DebugRenderer()
    val cinematicOverlayRenderer = CinematicOverlayRenderer()
    val textRenderer = TextRenderer()
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var postProcessorWidth = 0
    private var postProcessorHeight = 0

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
        // If there are no effects, render directly to the default frame-buffer
        if (scene.effects.isNotEmpty()) {
            resizePostProcessorIfNeeded()
            postProcessor.bindFrameBuffer()
        }

        val showRenderedSky = scene.viewportShadingMode == ViewportShadingMode.RENDERED && scene.skyVisible
        val skyColor = if (showRenderedSky) scene.skyColor else EDITOR_SKY_HIDDEN_COLOR
        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        if (showRenderedSky) skyboxRenderer.render(scene)
        if (scene.cullingEnabled) {
            glEnable(GL_CULL_FACE)
            glCullFace(GL_BACK)
        } else {
            glDisable(GL_CULL_FACE)
        }
        if (scene.viewportShadingMode == ViewportShadingMode.WIRE) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
            glLineWidth(1.0f)
        }
        terrainRenderer.render(scene)
        entityRenderer.render(scene)
        if (scene.viewportShadingMode == ViewportShadingMode.WIRE) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        }

        if (scene.effects.isNotEmpty()) {
            postProcessor.bindDefaultFrameBuffer()
            postProcessor.applyPostProcess(scene.effects)
        }

        debugRenderer.render(scene)
        cinematicOverlayRenderer.render(scene)
        textRenderer.render(scene)
    }

    override fun resize(width: Int, height: Int) {
        resizeForViewport(width, height)
        resizePostProcessorIfNeeded()
    }

    fun resizeForViewport(width: Int, height: Int) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        if (viewportWidth == safeWidth && viewportHeight == safeHeight) {
            glViewport(0, 0, safeWidth, safeHeight)
            return
        }

        viewportWidth = safeWidth
        viewportHeight = safeHeight
        glViewport(0, 0, safeWidth, safeHeight)

        entityRenderer.resize(safeWidth, safeHeight)
        terrainRenderer.resize(safeWidth, safeHeight)
        skyboxRenderer.resize(safeWidth, safeHeight)
        debugRenderer.resize(safeWidth, safeHeight)
        textRenderer.resize(safeWidth, safeHeight)
    }

    private fun resizePostProcessorIfNeeded() {
        if (postProcessorWidth == viewportWidth && postProcessorHeight == viewportHeight) return
        postProcessorWidth = viewportWidth.coerceAtLeast(1)
        postProcessorHeight = viewportHeight.coerceAtLeast(1)
        postProcessor.resize(postProcessorWidth, postProcessorHeight)
    }

    private companion object {
        val EDITOR_SKY_HIDDEN_COLOR = qorrnsmj.smf.math.Vector3f(0.08f, 0.09f, 0.1f)
    }
}
