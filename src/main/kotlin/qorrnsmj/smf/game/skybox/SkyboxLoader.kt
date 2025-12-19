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
import java.io.IOException

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

        val texture = loadCubeMap(
            "${id}_front.png",
            "${id}_back.png",
            "${id}_top.png",
            "${id}_bottom.png",
            "${id}_right.png",
            "${id}_left.png"
        )
        textures.add(texture)

        val mesh = Mesh(vao, indices.size)
        val material = Material(diffuseTexture = texture)

        Logger.info("Loaded skybox model '${id}'")
        return Model(id, mesh, material)
    }

    private fun loadCubeMap(vararg files: String): TextureBufferObject {
        val texture = TextureBufferObject()
        texture.bind()

        // ensure proper row alignment for arbitrary width images
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

        for ((i, file) in files.withIndex()) {
            val resourcePath = "/assets/texture/skybox/$file"
            val stream = SkyboxLoader::class.java.getResourceAsStream(resourcePath)
                ?: throw IOException("Failed to find resource: $resourcePath")
            val bytes = stream.use { it.readAllBytes() }

            val imageBuffer = MemoryUtil.memAlloc(bytes.size)
            try {
                imageBuffer.put(bytes).flip()
                MemoryStack.stackPush().use { stack ->
                    val w = stack.mallocInt(1)
                    val h = stack.mallocInt(1)
                    val comp = stack.mallocInt(1)
                    val img = stbi_load_from_memory(imageBuffer, w, h, comp, 4)
                        ?: throw RuntimeException("Failed to load image $file: ${stbi_failure_reason()}")
                    try {
                        glTexImage2D(
                            GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                            0,
                            GL_RGBA,
                            w.get(0),
                            h.get(0),
                            0,
                            GL_RGBA,
                            GL_UNSIGNED_BYTE,
                            img
                        )
                    } finally {
                        stbi_image_free(img)
                    }
                }
            } finally {
                MemoryUtil.memFree(imageBuffer)
            }
        }

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)

        texture.unbind()
        return texture
    }

    override fun cleanup() {
        vaos.forEach { glDeleteVertexArrays(it) }
        vbos.forEach { glDeleteBuffers(it) }
        ebos.forEach { glDeleteBuffers(it) }
        textures.forEach { it.delete() }
    }
}
