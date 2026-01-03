package qorrnsmj.smf.game.skybox

import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryUtil
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.model.component.Material
import qorrnsmj.smf.game.model.component.Mesh
import qorrnsmj.smf.graphic.`object`.TextureBufferObject
import qorrnsmj.smf.util.impl.Cleanable
import qorrnsmj.smf.game.model.component.Model
import qorrnsmj.smf.game.texture.TextureLoader
import qorrnsmj.smf.game.texture.TexturePresets

object SkyboxLoader : Cleanable {
    private val vaos = mutableListOf<Int>()
    private val vbos = mutableListOf<Int>()
    private val ebos = mutableListOf<Int>()
    private val textures = mutableListOf<TextureBufferObject>()

    private val SKYBOX_VERTICES = floatArrayOf(
        -1f,  1f, -1f,
        -1f, -1f, -1f,
         1f, -1f, -1f,
         1f,  1f, -1f,
        -1f,  1f,  1f,
        -1f, -1f,  1f,
         1f, -1f,  1f,
         1f,  1f,  1f
    )
    private val SKYBOX_INDICES = intArrayOf(
        0, 1, 3, 3, 1, 2,
        4, 5, 7, 7, 5, 6,
        0, 1, 4, 4, 1, 5,
        3, 2, 7, 7, 2, 6,
        0, 3, 4, 4, 3, 7,
        1, 2, 5, 5, 2, 6
    )

    fun loadSkybox(id: String): Skybox {
        val model = loadModel(id)
        return Skybox(model)
    }

    private fun loadModel(id: String): Model {
        val mesh = loadMesh()
        val texture = TextureLoader.loadCubemapTexture(
            "assets/texture/skybox/$id",
            TexturePresets.SKYBOX
        )
        val material = Material(baseColorTexture = texture)

        val faceCount = mesh.vertexCount.div(3)
        Logger.info("\"${id}\" loaded ($faceCount faces)")

        return Model(mesh, material)
    }

    private fun loadMesh(): Mesh {
        val vao = glGenVertexArrays()
        val vbo = glGenBuffers()
        val ebo = glGenBuffers()
        vaos.add(vao); vbos.add(vbo); ebos.add(ebo)
        glBindVertexArray(vao)

        val vb = MemoryUtil.memAllocFloat(SKYBOX_VERTICES.size).apply {
            this.put(SKYBOX_VERTICES).flip()
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBufferData(GL_ARRAY_BUFFER, this, GL_STATIC_DRAW)
        }

        val ib = MemoryUtil.memAllocInt(SKYBOX_INDICES.size).apply {
            this.put(SKYBOX_INDICES).flip()
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, this, GL_STATIC_DRAW)
        }

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)
        glBindVertexArray(0)

        MemoryUtil.memFree(vb)
        MemoryUtil.memFree(ib)

        return Mesh(vao, SKYBOX_INDICES.size)
    }

    override fun cleanup() {
        vaos.forEach { glDeleteVertexArrays(it) }
        vbos.forEach { glDeleteBuffers(it) }
        ebos.forEach { glDeleteBuffers(it) }
        textures.forEach { it.delete() }
    }
}
