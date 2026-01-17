package qorrnsmj.smf.graphic.terrain.component

import org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT
import qorrnsmj.smf.math.Vector2f

data class TerrainMesh(
    val vao: Int = 0,
    val vertexCount: Int = 0,
    val vertexType: Int = GL_UNSIGNED_INT,
    val size: Vector2f,
    val gridResolution: Int,
    val heights: Array<FloatArray>,
)
