package qorrnsmj.smf.graphic.renderer

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.renderer.DebugRenderer
import qorrnsmj.smf.graphic.effect.PostProcessor
import qorrnsmj.smf.graphic.render.RenderSettings
import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.graphic.scene.shadow.ShadowSettings
import qorrnsmj.smf.graphic.text.GameDebugHud
import qorrnsmj.smf.util.Resizable

class MasterRenderer(
    shadowSettings: ShadowSettings = ShadowSettings(),
    val settings: RenderSettings = RenderSettings(),
) : SceneRenderer, Resizable {
    val shadowRenderer = ShadowRenderer(shadowSettings, settings)
    val skyboxRenderer = SkyboxRenderer(settings)
    val skydomeRenderer = SkydomeRenderer(settings)
    val terrainRenderer = TerrainRenderer(shadowRenderer, settings)
    val modelRenderer = ModelRenderer(shadowRenderer, settings)
    val grassRenderer = GrassRenderer(shadowRenderer, settings)
    val billboardRenderer = BillboardRenderer(settings)
    val postProcessor = PostProcessor()
    val debugRenderer = DebugRenderer()
    val textRenderer = TextRenderer()
    val debugHud = GameDebugHud()

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
        // Shadow maps must be rendered before the main scene because terrain/entity shaders sample them
        shadowRenderer.render(scene)

        // If there are no effects, render directly to the default frame-buffer
        if (scene.sceneEffects.isNotEmpty()) {
            postProcessor.bindFrameBuffer()
        }

        if (scene.environment.sky.skydome?.enabled == true) {
            skydomeRenderer.render(scene)
        } else {
            skyboxRenderer.render(scene)
        }

        terrainRenderer.render(scene)
        modelRenderer.render(scene)
        grassRenderer.render(scene)
        billboardRenderer.render(scene)

        if (scene.sceneEffects.isNotEmpty()) {
            postProcessor.bindDefaultFrameBuffer()
            postProcessor.applyPostProcess(scene.sceneEffects)
        }

        debugRenderer.render(scene)
        textRenderer.render(scene)
    }

    override fun resize(width: Int, height: Int) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        glViewport(0, 0, safeWidth, safeHeight)

        modelRenderer.resize(safeWidth, safeHeight)
        grassRenderer.resize(safeWidth, safeHeight)
        billboardRenderer.resize(safeWidth, safeHeight)
        terrainRenderer.resize(safeWidth, safeHeight)
        skyboxRenderer.resize(safeWidth, safeHeight)
        skydomeRenderer.resize(safeWidth, safeHeight)
        debugRenderer.resize(safeWidth, safeHeight)
        textRenderer.resize(safeWidth, safeHeight)
        postProcessor.resize(safeWidth, safeHeight)
    }
}
