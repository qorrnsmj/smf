package qorrnsmj.smf.game.texture

import org.lwjgl.opengl.GL33C.GL_LINEAR
import org.lwjgl.opengl.GL33C.GL_LINEAR_MIPMAP_LINEAR
import org.lwjgl.opengl.GL33C.GL_REPEAT
import org.lwjgl.opengl.GL33C.GL_CLAMP_TO_EDGE

object TexturePresets {
    val ENTITY = TextureParams(
        GL_LINEAR,
        GL_LINEAR_MIPMAP_LINEAR,
        GL_REPEAT,
        GL_REPEAT,
        GL_REPEAT,
    )

    val TERRAIN = TextureParams(
        GL_LINEAR,
        GL_LINEAR_MIPMAP_LINEAR,
        GL_REPEAT,
        GL_REPEAT,
        GL_REPEAT,
    )

    val TERRAIN_BLENDMAP = TextureParams(
        GL_LINEAR,
        GL_LINEAR_MIPMAP_LINEAR,
        GL_CLAMP_TO_EDGE,
        GL_CLAMP_TO_EDGE,
        GL_CLAMP_TO_EDGE,
    )

    val SKYBOX = TextureParams(
        GL_LINEAR,
        GL_LINEAR,
        GL_CLAMP_TO_EDGE,
        GL_CLAMP_TO_EDGE,
        GL_CLAMP_TO_EDGE,
    )
}
