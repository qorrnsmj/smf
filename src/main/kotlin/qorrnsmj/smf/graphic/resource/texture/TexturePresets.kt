package qorrnsmj.smf.graphic.resource.texture
import qorrnsmj.smf.graphic.scene.sky.Skybox
import qorrnsmj.smf.graphic.scene.terrain.Terrain

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

    val SKYBOX = TextureParams(
        GL_LINEAR,
        GL_LINEAR,
        GL_CLAMP_TO_EDGE,
        GL_CLAMP_TO_EDGE,
        GL_CLAMP_TO_EDGE,
    )

    val FOLIAGE = TextureParams(
        GL_LINEAR,
        GL_LINEAR_MIPMAP_LINEAR,
        GL_CLAMP_TO_EDGE,
        GL_CLAMP_TO_EDGE,
        GL_CLAMP_TO_EDGE,
    )
}

