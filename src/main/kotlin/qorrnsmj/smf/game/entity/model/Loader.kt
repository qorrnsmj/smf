package qorrnsmj.smf.game.entity.model

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.model.component.Mesh
import qorrnsmj.smf.game.entity.model.component.Texture
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Path

object Loader {
    private val vaos = mutableListOf<Int>()
    private val vbos = mutableListOf<Int>()
    private val ebos = mutableListOf<Int>()
    private val textures = mutableListOf<Texture>()

    // TODO: OBJLoader.load()じゃなくて、このメソッドにfileだけ渡してロードできるようにする。
    fun loadMesh(file: String) {
    }

    fun loadMesh(file: String, positions: FloatArray, texCoords: FloatArray, normals: FloatArray, indices: IntArray): Mesh {
        val vaoID = glGenVertexArrays()
        vaos.add(vaoID)

        // bind
        glBindVertexArray(vaoID)
        bindVBO(0, 3, positions)
        bindVBO(1, 2, texCoords)
        bindVBO(2, 3, normals)
        bindEBO(indices)

        // unbind
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        Logger.info("\"$file\" loaded (${indices.size / 3} faces)")
        return Mesh(vaoID, indices.size)
    }

    fun loadTexture(file: String): Texture {
        val texture = Texture(glGenTextures())
        texture.bind()

        // Sets texture parameters
        glGenerateMipmap(GL_TEXTURE_2D)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, -0.4f)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        // Loads image data
        val byteArray = getResourceAsStream("assets/texture/$file").readAllBytes()
        val byteBuffer = ByteBuffer.allocateDirect(byteArray.size).apply {
            put(byteArray)
            flip()
        }

        // Uploads image data
        MemoryStack.stackPush().use { stack ->
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)
            val channels = stack.mallocInt(1)

            STBImage.stbi_set_flip_vertically_on_load(true)
            val imageByteBuffer = STBImage.stbi_load_from_memory(byteBuffer, width, height, channels, 0)
            checkNotNull(imageByteBuffer) {
                Logger.error("Failed to load image: \"$file\" (${STBImage.stbi_failure_reason()})")
            }

            val w = width.get()
            val h = height.get()
            val format = when (val c = channels.get()) {
                3 -> GL_RGB
                4 -> GL_RGBA
                else -> throw IllegalArgumentException("Unsupported number of channels: $c")
            }
            glTexImage2D(GL_TEXTURE_2D, 0, format, w, h, 0, format, GL_UNSIGNED_BYTE, imageByteBuffer)
            STBImage.stbi_image_free(imageByteBuffer)
            Logger.info("\"$file\" loaded (${w}x${h})")
        }

        texture.unbind()
        return texture
    }
    
    fun cleanup() {
        vaos.forEach { glDeleteVertexArrays(it) }
        vbos.forEach { glDeleteBuffers(it) }
        ebos.forEach { glDeleteBuffers(it) }
        textures.forEach { it.delete() }
    }

    /* Shader Object */

    private fun bindVBO(attribIndex: Int, attribSize: Int, data: FloatArray) {
        val vboID = glGenBuffers()
        vbos.add(vboID)

        val buffer = BufferUtils.createFloatBuffer(data.size)
        buffer.put(data)
        buffer.flip()

        glBindBuffer(GL_ARRAY_BUFFER, vboID)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
        glVertexAttribPointer(attribIndex, attribSize, GL_FLOAT, false, 0, 0)
    }

    private fun bindEBO(data: IntArray) {
        val eboID = glGenBuffers()
        ebos.add(eboID)

        val buffer = BufferUtils.createIntBuffer(data.size)
        buffer.put(data)
        buffer.flip()

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
    }

    /* Util */

    private fun getResourceAsStream(path: String): InputStream {
        return ClassLoader.getSystemResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
    }
}
