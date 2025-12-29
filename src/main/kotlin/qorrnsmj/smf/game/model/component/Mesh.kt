package qorrnsmj.smf.game.model.component

import org.lwjgl.opengl.GL33C.GL_UNSIGNED_INT

// TODO:
//  vaoIdじゃなくてvaoもたせる?
//  いやVAOクラス消す？
data class Mesh(
    val vao: Int = 0,
    val vertexCount: Int = 0,
    val vertexType: Int = GL_UNSIGNED_INT
)
