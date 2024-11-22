package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.graphic.MVP

class MasterRenderer : Resizable {
    private val entityRenderer = EntityRenderer()

    init {
        Logger.info("MasterRenderer initializing...")
        
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)

        glEnable(GL_DEPTH_TEST)
        glClearColor(0f, 0f, 0f, 1f)

        Logger.info("MasterRenderer initialized!")
    }

    override fun resize(width: Int, height: Int) {
        val matrix = MVP.getPerspectiveMatrix(width / height.toFloat())
        glViewport(0, 0, width, height)

        entityRenderer.start()
        entityRenderer.setProjection(matrix)
        entityRenderer.stop()
    }

    fun render(scene: Scene) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        entityRenderer.start()
        entityRenderer.setCamera(scene.camera)
        entityRenderer.render(scene.entities)
        entityRenderer.stop()
    }

    fun cleanup() {
        Logger.info("MasterRenderer cleaned up!")
    }
}
