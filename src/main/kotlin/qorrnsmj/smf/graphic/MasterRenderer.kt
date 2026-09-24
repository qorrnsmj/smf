package qorrnsmj.smf.graphic

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.debug.DebugRenderer
import qorrnsmj.smf.graphic.effect.PostProcessor
import qorrnsmj.smf.graphic.billboard.BillboardRenderer
import qorrnsmj.smf.graphic.entity.ModelRenderer
import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.graphic.shadow.ShadowRenderer
import qorrnsmj.smf.graphic.skydome.SkydomeRenderer
import qorrnsmj.smf.graphic.skybox.SkyboxRenderer
import qorrnsmj.smf.graphic.terrain.TerrainRenderer
import qorrnsmj.smf.graphic.text.TextRenderer
import qorrnsmj.smf.util.Resizable

class MasterRenderer : SceneRenderer, Resizable {
    val shadowRenderer = ShadowRenderer()
    val skyboxRenderer = SkyboxRenderer()
    val skydomeRenderer = SkydomeRenderer()
    val terrainRenderer = TerrainRenderer(shadowRenderer)
    val modelRenderer = ModelRenderer(shadowRenderer)
    val billboardRenderer = BillboardRenderer()
    val postProcessor = PostProcessor()
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

    override fun render(scene: Scene) {
        // Shadow maps must be rendered before the main scene because terrain/entity shaders sample them
        shadowRenderer.render(scene)

        // If there are no effects, render directly to the default frame-buffer
        if (scene.sceneEffects.isNotEmpty()) {
            postProcessor.bindFrameBuffer()
        }

        if (scene.environment.skydome?.enabled == true) {
            skydomeRenderer.render(scene)
        } else {
            skyboxRenderer.render(scene)
        }
        terrainRenderer.render(scene)
        modelRenderer.render(scene)
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
        billboardRenderer.resize(safeWidth, safeHeight)
        terrainRenderer.resize(safeWidth, safeHeight)
        skyboxRenderer.resize(safeWidth, safeHeight)
        skydomeRenderer.resize(safeWidth, safeHeight)
        debugRenderer.resize(safeWidth, safeHeight)
        textRenderer.resize(safeWidth, safeHeight)
        postProcessor.resize(safeWidth, safeHeight)
    }
}
