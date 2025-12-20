package qorrnsmj.smf.game.skybox

import org.lwjgl.opengl.GL33C.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.model.component.Material
import qorrnsmj.smf.game.entity.model.component.Mesh
import qorrnsmj.smf.graphic.`object`.TextureBufferObject
import qorrnsmj.smf.util.impl.Cleanable
import qorrnsmj.smf.game.entity.model.component.Model
import qorrnsmj.smf.game.terrain.TerrainLoader
import qorrnsmj.smf.util.ResourceUtils.getResourceAsByteBuffer

object SkyboxLoader : Cleanable {
    private val vaos = mutableListOf<Int>()
    private val vbos = mutableListOf<Int>()
    private val ebos = mutableListOf<Int>()
    private val textures = mutableListOf<TextureBufferObject>()

    fun loadModel(id: String): Model {
        val vertices = floatArrayOf(
            -1f,  1f, -1f,
            -1f, -1f, -1f,
             1f, -1f, -1f,
             1f,  1f, -1f,
            -1f,  1f,  1f,
            -1f, -1f,  1f,
             1f, -1f,  1f,
             1f,  1f,  1f
        )
        val indices = intArrayOf(
            0, 1, 3, 3, 1, 2,
            4, 5, 7, 7, 5, 6,
            0, 1, 4, 4, 1, 5,
            3, 2, 7, 7, 2, 6,
            0, 3, 4, 4, 3, 7,
            1, 2, 5, 5, 2, 6
        )

        val vao = glGenVertexArrays()
        val vbo = glGenBuffers()
        val ebo = glGenBuffers()
        vaos.add(vao); vbos.add(vbo); ebos.add(ebo)

        glBindVertexArray(vao)
        val vertexBuffer = MemoryUtil.memAllocFloat(vertices.size)
        try {
            vertexBuffer.put(vertices).flip()
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW)
        } finally {
            MemoryUtil.memFree(vertexBuffer)
        }

        val indexBuffer = MemoryUtil.memAllocInt(indices.size)
        try {
            indexBuffer.put(indices).flip()
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW)
        } finally {
            MemoryUtil.memFree(indexBuffer)
        }

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)

        val texture = loadTexture(
            "${id}_front.png",
            "${id}_back.png",
            "${id}_top.png",
            "${id}_bottom.png",
            "${id}_right.png",
            "${id}_left.png"
        )
        val mesh = Mesh(vao, indices.size)
        val material = Material(diffuseTexture = texture)

        val faceCount = mesh.vertexCount.div(3)
        Logger.info("\"${id}\" loaded ($faceCount faces)")

        return Model(id, mesh, material)
    }

    private fun loadTexture(vararg files: String): TextureBufferObject {
        // Create TBO
        val texture = TextureBufferObject().apply {
            this.bind()
            textures.add(this)
        }

        // Load image
        for ((i, file) in files.withIndex()) {
            MemoryStack.stackPush().use { stack ->
                val width = stack.mallocInt(1)
                val height = stack.mallocInt(1)
                val channel = stack.mallocInt(1)
                val buffer = getResourceAsByteBuffer("assets/texture/skybox/$file")
                val imageByteBuffer = stbi_load_from_memory(buffer, width, height, channel, 4)
                    ?: throw RuntimeException("Failed to load image $file: ${stbi_failure_reason()}")

                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGBA,
                     width.get(), height.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, imageByteBuffer)
                stbi_image_free(imageByteBuffer)
            }
        }

        // Set parameter
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)

        return texture
    }

    override fun cleanup() {
        vaos.forEach { glDeleteVertexArrays(it) }
        vbos.forEach { glDeleteBuffers(it) }
        ebos.forEach { glDeleteBuffers(it) }
        textures.forEach { it.delete() }
    }
}
