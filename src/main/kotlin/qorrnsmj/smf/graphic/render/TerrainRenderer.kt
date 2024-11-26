package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33C.glGetUniformLocation
import qorrnsmj.smf.graphic.shader.custom.TerrainShader
import qorrnsmj.smf.util.UniformUtils

// TODO: Renderer抽象クラスでもつくる？いらない？
class TerrainRenderer {
    val program = TerrainShader
    val locView = glGetUniformLocation(program.id, "view")
    val locProjection = glGetUniformLocation(program.id, "projection")

    fun start() {
//        program.start()
    }

    fun stop() {
//        program.stop()
    }

    fun render(scene: Scene) {
        UniformUtils.setUniform(locView, scene.camera.getViewMatrix())
        //setLightUniforms(scene.lights)

        //renderEntity(scene.entities)
        //ここに直接render処理書く
    }
}