package qorrnsmj.smf.game.entity.custom

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL33C.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL33C.GL_FLOAT
import org.lwjgl.opengl.GL33C.GL_STATIC_DRAW
import org.lwjgl.opengl.GL33C.glBindBuffer
import org.lwjgl.opengl.GL33C.glBindVertexArray
import org.lwjgl.opengl.GL33C.glBufferData
import org.lwjgl.opengl.GL33C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL33C.glGenBuffers
import org.lwjgl.opengl.GL33C.glGenVertexArrays
import org.lwjgl.opengl.GL33C.glVertexAttribPointer
import qorrnsmj.smf.graphic.`object`.Material
import qorrnsmj.smf.graphic.`object`.Mesh
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.smf.physics.collision.shape.BoxCollider
import qorrnsmj.smf.physics.component.StaticPhysics

class ShadowTestBlockEntity(
    transform: Transform,
    color: Vector4f = Vector4f(0.78f, 0.72f, 0.62f, 1f),
) : ObjectEntity(
    transform = transform,
    model = createModel(color),
    physicsComponent = StaticPhysics(
        collider = BoxCollider(
            width = transform.scale.x,
            height = transform.scale.y,
            depth = transform.scale.z,
        )
    )
) {
    companion object {
        private fun createModel(color: Vector4f): Model {
            return Model(
                mesh = createCubeMesh(),
                material = Material(
                    baseColorFactor = color,
                    metallicFactor = 0f,
                    roughnessFactor = 0.82f,
                    baseColorTexture = Textures.DEFAULT_FFFFFF,
                    metallicRoughnessTexture = Textures.DEFAULT_00FF00,
                    normalTexture = Textures.DEFAULT_8080FF,
                    occlusionTexture = Textures.DEFAULT_FFFFFF,
                    emissiveTexture = Textures.DEFAULT_000000,
                ),
            )
        }

        private fun createCubeMesh(): Mesh {
            val positions = floatArrayOf(
                -0.5f, -0.5f,  0.5f,  0.5f, -0.5f,  0.5f,  0.5f,  0.5f,  0.5f, -0.5f,  0.5f,  0.5f,
                 0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f,  0.5f, -0.5f,  0.5f,  0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f, -0.5f, -0.5f,  0.5f, -0.5f,  0.5f,  0.5f, -0.5f,  0.5f, -0.5f,
                 0.5f, -0.5f,  0.5f,  0.5f, -0.5f, -0.5f,  0.5f,  0.5f, -0.5f,  0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,  0.5f,  0.5f,  0.5f,  0.5f,  0.5f, -0.5f, -0.5f,  0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f,  0.5f, -0.5f, -0.5f,  0.5f, -0.5f,  0.5f, -0.5f, -0.5f,  0.5f,
            )
            val uvs = FloatArray(24 * 2) { index -> if (index % 2 == 0) (index / 2 % 2).toFloat() else (index / 4 % 2).toFloat() }
            val normals = floatArrayOf(
                 0f,  0f,  1f,  0f,  0f,  1f,  0f,  0f,  1f,  0f,  0f,  1f,
                 0f,  0f, -1f,  0f,  0f, -1f,  0f,  0f, -1f,  0f,  0f, -1f,
                -1f,  0f,  0f, -1f,  0f,  0f, -1f,  0f,  0f, -1f,  0f,  0f,
                 1f,  0f,  0f,  1f,  0f,  0f,  1f,  0f,  0f,  1f,  0f,  0f,
                 0f,  1f,  0f,  0f,  1f,  0f,  0f,  1f,  0f,  0f,  1f,  0f,
                 0f, -1f,  0f,  0f, -1f,  0f,  0f, -1f,  0f,  0f, -1f,  0f,
            )
            val tangents = FloatArray(24 * 4) { index -> if (index % 4 == 0 || index % 4 == 3) 1f else 0f }
            val indices = intArrayOf(
                 0,  1,  2,  0,  2,  3,
                 4,  5,  6,  4,  6,  7,
                 8,  9, 10,  8, 10, 11,
                12, 13, 14, 12, 14, 15,
                16, 17, 18, 16, 18, 19,
                20, 21, 22, 20, 22, 23,
            )

            val vao = glGenVertexArrays()
            glBindVertexArray(vao)
            bindFloatVbo(0, 3, positions)
            bindFloatVbo(1, 2, uvs)
            bindFloatVbo(2, 3, normals)
            bindFloatVbo(3, 4, tangents)
            bindIndexBuffer(indices)
            glEnableVertexAttribArray(0)
            glEnableVertexAttribArray(1)
            glEnableVertexAttribArray(2)
            glEnableVertexAttribArray(3)
            glBindVertexArray(0)
            glBindBuffer(GL_ARRAY_BUFFER, 0)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

            return Mesh(vao = vao, vertexCount = indices.size)
        }

        private fun bindFloatVbo(attribute: Int, size: Int, data: FloatArray) {
            val vbo = glGenBuffers()
            val buffer = BufferUtils.createFloatBuffer(data.size)
            buffer.put(data).flip()
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
            glVertexAttribPointer(attribute, size, GL_FLOAT, false, 0, 0L)
        }

        private fun bindIndexBuffer(data: IntArray) {
            val ebo = glGenBuffers()
            val buffer = BufferUtils.createIntBuffer(data.size)
            buffer.put(data).flip()
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
        }
    }
}