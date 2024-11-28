package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.Scene
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.graphic.MVP
import qorrnsmj.smf.math.Vector3f
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
        glClearColor(0.5f, 0.5f, 0.5f, 1f) // TODO: Sky color

        Logger.info("MasterRenderer initialized!")
    }

    fun render(scene: Scene) {
        val skyColor = Vector3f(0.5f, 0.5f, 0.5f)
        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // TODO: renderEntity()にするのとloadCameraとかはMasterRendererで実装する
        entityRenderer.start()
        entityRenderer.loadCamera(scene.camera)
        entityRenderer.loadLights(scene.lights)
        entityRenderer.loadSkyColor(skyColor)
        entityRenderer.renderEntity(scene.entities)
        entityRenderer.stop()

        terrainRenderer.start()
        terrainRenderer.loadCamera(scene.camera)
        terrainRenderer.loadSkyColor(skyColor)
        terrainRenderer.renderTerrain(scene.terrains)
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
