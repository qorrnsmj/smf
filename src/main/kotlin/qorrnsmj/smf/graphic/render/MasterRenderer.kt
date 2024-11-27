package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.Scene
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.graphic.MVP
import qorrnsmj.smf.util.UniformUtils

class MasterRenderer : Resizable {
    private val entityRenderer = EntityRenderer()
    private val terrainRenderer = TerrainRenderer()

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
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        entityRenderer.start()
        entityRenderer.render(scene)
        entityRenderer.stop()

        terrainRenderer.start()
        terrainRenderer.render(scene)
        terrainRenderer.stop()
    }

    override fun resize(width: Int, height: Int) {
        val matrix = MVP.getPerspectiveMatrix(width / height.toFloat())
        glViewport(0, 0, width, height)

        entityRenderer.start()
        UniformUtils.setUniform(entityRenderer.locationProjection, matrix)
        entityRenderer.stop()

        terrainRenderer.start()
        UniformUtils.setUniform(terrainRenderer.locationProjection, matrix)
        terrainRenderer.stop()
    }

    fun cleanup() {
        Logger.info("MasterRenderer cleaned up!")
    }
}
