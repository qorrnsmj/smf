package qorrnsmj.smf.game.terrain

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33C.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.`object`.TextureBufferObject
import qorrnsmj.smf.graphic.`object`.VertexArrayObject
import qorrnsmj.smf.math.Vector2f
import qorrnsmj.smf.math.Vector3f
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import qorrnsmj.smf.game.terrain.component.TerrainMaterial
import qorrnsmj.smf.game.terrain.component.TerrainMesh
import qorrnsmj.smf.game.terrain.component.TerrainModel
import qorrnsmj.smf.game.terrain.component.TerrainTextureMode
import qorrnsmj.smf.util.ResourceUtils
import qorrnsmj.smf.util.Cleanable

object TerrainLoader : Cleanable {
    private val vaos = mutableListOf<Int>()
    private val vbos = mutableListOf<Int>()
    private val ebos = mutableListOf<Int>()
    private val textures = mutableListOf<TextureBufferObject>()

    fun loadModel(
        sizeX: Float,
        sizeY: Float,
        vertexCount: Int,
        heightmapFile: String? = null,
        textureMode: TerrainTextureMode,
    ): Terrain {
        val size = Vector2f(sizeX, sizeY)
        val mesh = loadMesh(size, vertexCount, heightmapFile)
        val material = TerrainMaterial(textureMode = textureMode)
        val model = TerrainModel(mesh = mesh, material = material)

        return Terrain(model = model)
    }

    private fun loadMesh(
        size: Vector2f,
        gridResolution: Int,
        heightmapFile: String? = null,
        heightScale: Float = 50f,
        minHeight: Float = 1f,
    ): TerrainMesh {
        val count = gridResolution * gridResolution
        val positions = FloatArray(count * 3)
        val texCoords = FloatArray(count * 2)
        val normals = FloatArray(count * 3)
        val indices = IntArray(6 * (gridResolution - 1) * (gridResolution - 1))
        val heights = Array(gridResolution) { FloatArray(gridResolution) }

        if (heightmapFile != null) {
            val heightmapData = extractHeightmapPixelData(heightmapFile)
            generateHeightmapTerrain(
                gridResolution, size, heightmapData, heightScale, minHeight,
                positions, texCoords, normals, indices,
                heights,
            )
        } else {
            generateFlatTerrain(
                gridResolution, size,
                positions, texCoords, normals, indices,
                heights,
            )
        }

        val mesh = loadMeshInternal(size, positions, texCoords, normals, indices, gridResolution, heights)
        val faceCount = mesh.vertexCount.div(3)
        Logger.info("Terrain loaded ($faceCount faces)")

        return mesh
    }

    private fun generateFlatTerrain(
        gridResolution: Int,
        size: Vector2f,
        positions: FloatArray,
        texCoords: FloatArray,
        normals: FloatArray,
        indices: IntArray,
        heights: Array<FloatArray>,
    ) {
        var vertexPointer = 0
        for (z in 0 until gridResolution) {
            for (x in 0 until gridResolution) {
                positions[vertexPointer * 3] = x.toFloat() / (gridResolution - 1) * size.x
                positions[vertexPointer * 3 + 1] = 0f
                positions[vertexPointer * 3 + 2] = z.toFloat() / (gridResolution - 1) * size.y

                normals[vertexPointer * 3] = 0f
                normals[vertexPointer * 3 + 1] = 1f
                normals[vertexPointer * 3 + 2] = 0f

                texCoords[vertexPointer * 2] = x.toFloat() / (gridResolution - 1)
                texCoords[vertexPointer * 2 + 1] = z.toFloat() / (gridResolution - 1)

                heights[x][z] = 0f
                vertexPointer++
            }
        }

        var pointer = 0
        for (gz in 0 until gridResolution - 1) {
            for (gx in 0 until gridResolution - 1) {
                val topLeft = (gz * gridResolution) + gx
                val topRight = topLeft + 1
                val bottomLeft = ((gz + 1) * gridResolution) + gx
                val bottomRight = bottomLeft + 1

                indices[pointer++] = topLeft
                indices[pointer++] = bottomLeft
                indices[pointer++] = topRight
                indices[pointer++] = topRight
                indices[pointer++] = bottomLeft
                indices[pointer++] = bottomRight
            }
        }
    }

    private fun generateHeightmapTerrain(
        gridResolution: Int,
        size: Vector2f,
        heightmapData: HeightmapData,
        heightScale: Float,
        minHeight: Float,
        positions: FloatArray,
        texCoords: FloatArray,
        normals: FloatArray,
        indices: IntArray,
        heights: Array<FloatArray>,
    ) {
        var vertexPointer = 0
        for (z in 0 until gridResolution) {
            for (x in 0 until gridResolution) {
                val heightValue = sampleHeightmap(x, z, gridResolution, heightmapData, heightScale, minHeight)

                positions[vertexPointer * 3] = x.toFloat() / (gridResolution - 1) * size.x
                positions[vertexPointer * 3 + 1] = heightValue
                positions[vertexPointer * 3 + 2] = z.toFloat() / (gridResolution - 1) * size.y

                texCoords[vertexPointer * 2] = x.toFloat() / (gridResolution - 1)
                texCoords[vertexPointer * 2 + 1] = z.toFloat() / (gridResolution - 1)

                heights[x][z] = heightValue
                vertexPointer++
            }
        }

        var pointer = 0
        for (gz in 0 until gridResolution - 1) {
            for (gx in 0 until gridResolution - 1) {
                val topLeft = (gz * gridResolution) + gx
                val topRight = topLeft + 1
                val bottomLeft = ((gz + 1) * gridResolution) + gx
                val bottomRight = bottomLeft + 1

                indices[pointer++] = topLeft
                indices[pointer++] = bottomLeft
                indices[pointer++] = topRight
                indices[pointer++] = topRight
                indices[pointer++] = bottomLeft
                indices[pointer++] = bottomRight
            }
        }

        calculateNormals(positions, indices, normals)
    }

    private fun sampleHeightmap(
        x: Int,
        z: Int,
        vertexCount: Int,
        heightmapData: HeightmapData,
        heightScale: Float,
        minHeight: Float,
    ): Float {
        val pixelX = (x * heightmapData.width) / vertexCount
        val pixelZ = (z * heightmapData.height) / vertexCount
        val pixelIndex = (pixelZ * heightmapData.width + pixelX) * 4

        if (pixelIndex < heightmapData.pixels.size) {
            return ((heightmapData.pixels[pixelIndex].toInt() and 0xFF).toFloat() / 255f * heightScale) + minHeight
        }

        return 0f
    }

    private fun extractHeightmapPixelData(
        heightmapFile: String,
    ): HeightmapData {
        val imageBuffer = ResourceUtils.getResourceAsDirectBuffer("assets/texture/terrain/$heightmapFile")
        var width = 0
        var height = 0
        var channels = 0
        val pixels: ByteArray

        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val c = stack.mallocInt(1)
            val loadedPixels = STBImage.stbi_load_from_memory(imageBuffer, w, h, c, 4)
                ?: error("Failed to load heightmap image: $heightmapFile")
            width = w.get(0)
            height = h.get(0)
            channels = c.get(0)

            val pixelSize = width * height * 4
            pixels = ByteArray(pixelSize)
            loadedPixels.get(pixels)
            loadedPixels.rewind()

            STBImage.stbi_image_free(loadedPixels)
        }

        return HeightmapData(width, height, channels, pixels)
    }

    private fun calculateNormals(
        positions: FloatArray,
        indices: IntArray,
        normals: FloatArray,
    ) {
        normals.fill(0f)

        for (i in indices.indices step 3) {
            val i0 = indices[i] * 3
            val i1 = indices[i + 1] * 3
            val i2 = indices[i + 2] * 3

            val v0 = Vector3f(positions[i0], positions[i0 + 1], positions[i0 + 2])
            val v1 = Vector3f(positions[i1], positions[i1 + 1], positions[i1 + 2])
            val v2 = Vector3f(positions[i2], positions[i2 + 1], positions[i2 + 2])

            val edge1 = v1.subtract(v0)
            val edge2 = v2.subtract(v0)

            val faceNormal = edge1.cross(edge2).normalize()

            normals[indices[i] * 3] += faceNormal.x
            normals[indices[i] * 3 + 1] += faceNormal.y
            normals[indices[i] * 3 + 2] += faceNormal.z

            normals[indices[i + 1] * 3] += faceNormal.x
            normals[indices[i + 1] * 3 + 1] += faceNormal.y
            normals[indices[i + 1] * 3 + 2] += faceNormal.z

            normals[indices[i + 2] * 3] += faceNormal.x
            normals[indices[i + 2] * 3 + 1] += faceNormal.y
            normals[indices[i + 2] * 3 + 2] += faceNormal.z
        }

        for (i in 0 until normals.size / 3) {
            val normal = Vector3f(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2])
            val length = normal.length()
            if (length > 0f) {
                val normalized = normal.normalize()
                normals[i * 3] = normalized.x
                normals[i * 3 + 1] = normalized.y
                normals[i * 3 + 2] = normalized.z
            }
        }
    }

    private fun loadMeshInternal(
        size: Vector2f,
        positions: FloatArray,
        texCoords: FloatArray,
        normals: FloatArray,
        indices: IntArray,
        gridResolution: Int,
        heights: Array<FloatArray>,
    ): TerrainMesh {
        val vao = VertexArrayObject()
        vaos.add(vao.id)
        vao.bind()

        bindVBO(0, 3, positions)
        bindVBO(1, 2, texCoords)
        bindVBO(2, 3, normals)
        bindEBO(indices)

        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)
        glEnableVertexAttribArray(2)

        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        return TerrainMesh(
            vao = vao.id,
            vertexCount = indices.size,
            size = size,
            gridResolution = gridResolution,
            heights = heights,
        )
    }

    private fun bindVBO(attribIndex: Int, attribSize: Int, data: FloatArray, ) {
        val vboID = glGenBuffers()
        vbos.add(vboID)

        val buffer = BufferUtils.createFloatBuffer(data.size)
        buffer.put(data)
        buffer.flip()

        glBindBuffer(GL_ARRAY_BUFFER, vboID)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
        glVertexAttribPointer(attribIndex, attribSize, GL_FLOAT, false, 0, 0)
    }

    private fun bindEBO(data: IntArray, ) {
        val eboID = glGenBuffers()
        ebos.add(eboID)

        val buffer = BufferUtils.createIntBuffer(data.size)
        buffer.put(data)
        buffer.flip()

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
    }

    override fun cleanup() {
        vaos.forEach { glDeleteVertexArrays(it) }
        vbos.forEach { glDeleteBuffers(it) }
        ebos.forEach { glDeleteBuffers(it) }
        textures.forEach { it.delete() }
    }

    data class HeightmapData(
        val width: Int,
        val height: Int,
        val channels: Int,
        val pixels: ByteArray,
    )
}
