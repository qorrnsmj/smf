package qorrnsmj.smf.game.terrain

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.model.Material
import qorrnsmj.smf.game.entity.model.Mesh
import qorrnsmj.smf.game.entity.model.Model
import qorrnsmj.smf.game.entity.model.Texture
import qorrnsmj.smf.game.terrain.Terrain.Companion.SIZE
import qorrnsmj.smf.game.terrain.Terrain.Companion.VERTEX_COUNT
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.use

object TerrainLoader {
    private val vaos = mutableListOf<Int>()
    private val vbos = mutableListOf<Int>()
    private val ebos = mutableListOf<Int>()
    private val textures = mutableListOf<Texture>()

    // TODO: EntityLoaderみたいに綺麗にまとめる
    fun loadModel(terrain: Terrain): Model {
        val count = VERTEX_COUNT * VERTEX_COUNT
        val positions = FloatArray(count * 3)
        val texCoords = FloatArray(count * 2)
        val normals = FloatArray(count * 3)
        val indices = IntArray(6 * (VERTEX_COUNT - 1) * (VERTEX_COUNT - 1))
        var vertexPointer = 0

        // 頂点座標、法線、テクスチャ座標の計算
        for (i in 0 until VERTEX_COUNT) {
            for (j in 0 until VERTEX_COUNT) {
                positions[vertexPointer * 3] = j.toFloat() / (VERTEX_COUNT - 1) * SIZE
                positions[vertexPointer * 3 + 1] = 0f
                positions[vertexPointer * 3 + 2] = i.toFloat() / (VERTEX_COUNT - 1) * SIZE

                texCoords[vertexPointer * 2] = j.toFloat() / (VERTEX_COUNT - 1)
                texCoords[vertexPointer * 2 + 1] = i.toFloat() / (VERTEX_COUNT - 1)

                normals[vertexPointer * 3] = 0f
                normals[vertexPointer * 3 + 1] = 1f
                normals[vertexPointer * 3 + 2] = 0f

                vertexPointer++
            }
        }

        // インデックス配列の計算
        var pointer = 0
        for (gz in 0 until VERTEX_COUNT - 1) {
            for (gx in 0 until VERTEX_COUNT - 1) {
                val topLeft = (gz * VERTEX_COUNT) + gx
                val topRight = topLeft + 1
                val bottomLeft = ((gz + 1) * VERTEX_COUNT) + gx
                val bottomRight = bottomLeft + 1

                indices[pointer++] = topLeft
                indices[pointer++] = bottomLeft
                indices[pointer++] = topRight
                indices[pointer++] = topRight
                indices[pointer++] = bottomLeft
                indices[pointer++] = bottomRight
            }
        }

        val mesh = loadMesh(positions, texCoords, normals, indices)
        val material = Material(diffuseTexture = loadTexture("grass.png"), specularTexture = Texture(), normalTexture = Texture())
        val model = Model("terrain", mesh, material)
        Logger.info("Terrain loaded (${mesh.vertexCount / 3} faces)")

        // TODO: Terrainとモデルの関係どうにかする
        terrain.model = model

        return model
    }

    private fun loadMesh(positions: FloatArray, texCoords: FloatArray, normals: FloatArray, indices: IntArray): Mesh {
        val vaoID = glGenVertexArrays()
        vaos.add(vaoID)

        // bind
        glBindVertexArray(vaoID)
        bindVBO(0, 3, positions)
        bindVBO(1, 2, texCoords)
        //bindVBO(2, 3, normals)
        bindEBO(indices)

        // unbind
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        return Mesh(vaoID, indices.size)
    }
    
    private fun loadTexture(file: String): Texture {
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

            // Uploads image data
            MemoryStack.stackPush().use { stack ->
                val width = stack.mallocInt(1)
                val height = stack.mallocInt(1)
                val channels = stack.mallocInt(1)

                STBImage.stbi_set_flip_vertically_on_load(false)
                val buffer = getResourceAsByteBuffer("assets/texture/terrain/$file")
                val imageByteBuffer = STBImage.stbi_load_from_memory(buffer, width, height, channels, 0)
                checkNotNull(imageByteBuffer) {
                    throw IllegalStateException("Failed to load image: \"$file\" (${STBImage.stbi_failure_reason()})")
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
            }

            textures.add(texture)
            texture.unbind()
            return texture
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Texture()
    }

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
    
    private fun getResourceAsByteBuffer(path: String): ByteBuffer {
        val bytes = getResourceAsStream(path).readAllBytes()
        val buffer = BufferUtils.createByteBuffer(bytes.size).apply {
            put(bytes)
            flip()
        }

        return buffer
    }

    private fun getResourceAsStream(path: String): InputStream {
        return ClassLoader.getSystemResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
    }

    fun cleanup() {
        vaos.forEach { glDeleteVertexArrays(it) }
        vbos.forEach { glDeleteBuffers(it) }
        ebos.forEach { glDeleteBuffers(it) }
        textures.forEach { it.delete() }
    }
}
