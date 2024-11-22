package qorrnsmj.smf.game.entity.model

import org.lwjgl.BufferUtils
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.AIVector3D
import org.lwjgl.assimp.Assimp
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

    fun loadMesh(file: String): Mesh {
        try {
            // Loads model data
            val byteArray = getResourceAsStream("assets/model/$file").readAllBytes()
            val byteBuffer = ByteBuffer.allocateDirect(byteArray.size).apply {
                put(byteArray)
                flip()
            }

            // Loads model
            val scene = Assimp.aiImportFileFromMemory(
                byteBuffer,
                Assimp.aiProcess_Triangulate or Assimp.aiProcess_FlipUVs,
                "obj"
            ) ?: throw RuntimeException("Failed to load model: \"$file\" (${Assimp.aiGetErrorString()})")
            val mesh = AIMesh.create(scene.mMeshes()!!.get(0))

            // Extracts model data
            val positions = FloatArray(mesh.mNumVertices() * 3)
            val texCoords = FloatArray(mesh.mNumVertices() * 2)
            val normals = FloatArray(mesh.mNumVertices() * 3)
            val indices = IntArray(mesh.mNumFaces() * 3)
            for (i in 0 until mesh.mNumVertices()) {
                val pos = mesh.mVertices().get(i)
                positions[i * 3] = pos.x()
                positions[i * 3 + 1] = pos.y()
                positions[i * 3 + 2] = pos.z()

                val texCoord = mesh.mTextureCoords(0)!!.get(i)
                texCoords[i * 2] = texCoord.x()
                texCoords[i * 2 + 1] = texCoord.y()

                val normal = mesh.mNormals()!!.get(i)
                normals[i * 3] = normal.x()
                normals[i * 3 + 1] = normal.y()
                normals[i * 3 + 2] = normal.z()
            }

            for (i in 0 until mesh.mNumFaces()) {
                val face = mesh.mFaces().get(i)
                indices[i * 3] = face.mIndices().get(0)
                indices[i * 3 + 1] = face.mIndices().get(1)
                indices[i * 3 + 2] = face.mIndices().get(2)
            }

            return loadMesh(file, positions, texCoords, normals, indices)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Mesh(0, 0)
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
        try {
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

                STBImage.stbi_set_flip_vertically_on_load(false)
                val imageByteBuffer = STBImage.stbi_load_from_memory(byteBuffer, width, height, channels, 0)
                checkNotNull(imageByteBuffer) {
                    throw RuntimeException("Failed to load image: \"$file\" (${STBImage.stbi_failure_reason()})")
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Texture(0)
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
