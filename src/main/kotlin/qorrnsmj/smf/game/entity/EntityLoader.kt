package qorrnsmj.smf.game.entity

import org.lwjgl.BufferUtils
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.AIString
import org.lwjgl.assimp.Assimp
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.model.Material
import qorrnsmj.smf.game.entity.model.Mesh
import qorrnsmj.smf.game.entity.model.Model
import qorrnsmj.smf.game.entity.model.Texture
import qorrnsmj.smf.math.Vector3f
import java.io.InputStream
import java.nio.ByteBuffer

object EntityLoader {
    private val vaos = mutableListOf<Int>()
    private val vbos = mutableListOf<Int>()
    private val ebos = mutableListOf<Int>()
    private val textures = mutableListOf<Texture>()

    /* Model */

    fun loadModel(file: String): Map<String, Model> {
        try {
            // Load scene
            val fileExtension = file.substringAfterLast('.', "").lowercase()
            require(fileExtension != "obj" && fileExtension != "md3") {
                throw IllegalArgumentException("Unsupported model format: $fileExtension")
            }

            val fileBuffer = getResourceAsByteBuffer("assets/model/$file")
            val scene = Assimp.aiImportFileFromMemory(
                fileBuffer,
                Assimp.aiProcess_Triangulate or Assimp.aiProcess_FlipUVs or Assimp.aiProcess_CalcTangentSpace,
                fileExtension
            ) ?: throw IllegalStateException(Assimp.aiGetErrorString())

            // Process models
            val models = hashMapOf<String, Model>()
            processModel(scene, models)

            // Log face count
            val faceCount = models.values.sumOf { it.mesh.vertexCount }.div(3)
            Logger.info("\"$file\" loaded ($faceCount faces)")

            Assimp.aiReleaseImport(scene)
            return models
        } catch (e: Exception) {
            Logger.error(e) { "Failed to load model: \"$file\"" }
        }

        return emptyMap()
    }

    private fun processModel(scene: AIScene, models: MutableMap<String, Model>) {
        val meshes = scene.mMeshes() ?: throw IllegalStateException("Scene has no meshes")
        val materials = scene.mMaterials() ?: throw IllegalStateException("Scene has no materials")

        for (i in 0 until scene.mNumMeshes()) {
            val aiMesh = AIMesh.create(meshes[i])
            val materialIndex = aiMesh.mMaterialIndex()
            val aiMaterial = AIMaterial.create(materials[materialIndex])

            val meshName = aiMesh.mName().dataString()
            val meshInstance = loadMesh(aiMesh)
            val materialInstance = loadMaterial(aiMaterial)
            models[meshName] = Model(meshName, meshInstance, materialInstance)
        }
    }

    /* Mesh */

    private fun loadMesh(mesh: AIMesh): Mesh {
        val positions = FloatArray(mesh.mNumVertices() * 3)
        val texCoords = FloatArray(mesh.mNumVertices() * 2)
        val normals = FloatArray(mesh.mNumVertices() * 3)
        val tangents = FloatArray(mesh.mNumVertices() * 3)
        val indices = IntArray(mesh.mNumFaces() * 3)

        for (i in 0 until mesh.mNumVertices()) {
            val pos = mesh.mVertices().get(i)
            positions[i * 3] = pos.x()
            positions[i * 3 + 1] = pos.y()
            positions[i * 3 + 2] = pos.z()

            val texCoord = mesh.mTextureCoords(0)?.get(i)
            texCoords[i * 2] = texCoord?.x() ?: 0.0f
            texCoords[i * 2 + 1] = texCoord?.y() ?: 0.0f

            val normal = mesh.mNormals()?.get(i)
            normals[i * 3] = normal?.x() ?: 0.0f
            normals[i * 3 + 1] = normal?.y() ?: 0.0f
            normals[i * 3 + 2] = normal?.z() ?: 0.0f

            val tangent = mesh.mTangents()?.get(i)
            tangents[i * 3] = tangent?.x() ?: 0.0f
            tangents[i * 3 + 1] = tangent?.y() ?: 0.0f
            tangents[i * 3 + 2] = tangent?.z() ?: 0.0f
        }

        for (i in 0 until mesh.mNumFaces()) {
            val face = mesh.mFaces().get(i)
            indices[i * 3] = face.mIndices().get(0)
            indices[i * 3 + 1] = face.mIndices().get(1)
            indices[i * 3 + 2] = face.mIndices().get(2)
        }

        return loadMesh(positions, texCoords, normals, tangents, indices)
    }

    private fun loadMesh(positions: FloatArray, texCoords: FloatArray, normals: FloatArray, tangents: FloatArray, indices: IntArray): Mesh {
        val vaoID = glGenVertexArrays()
        vaos.add(vaoID)

        // bind
        glBindVertexArray(vaoID)
        bindVBO(0, 3, positions)
        bindVBO(1, 2, texCoords)
        bindVBO(2, 3, normals)
        bindVBO(3, 3, tangents)
        bindEBO(indices)

        // unbind
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        return Mesh(vaoID, indices.size)
    }

    /* Material */

    private fun loadMaterial(material: AIMaterial): Material {
        val diffuseColor = getMaterialColor(material, Assimp.AI_MATKEY_COLOR_DIFFUSE)
        val ambientColor = getMaterialColor(material, Assimp.AI_MATKEY_COLOR_AMBIENT)
        val specularColor = getMaterialColor(material, Assimp.AI_MATKEY_COLOR_SPECULAR)
        val emissiveColor = getMaterialColor(material, Assimp.AI_MATKEY_COLOR_EMISSIVE)
        val shininess = getMaterialFloat(material, Assimp.AI_MATKEY_SHININESS)
        val opacity = getMaterialFloat(material, Assimp.AI_MATKEY_OPACITY)
        val diffuseTexture = getMaterialTexture(material, Assimp.aiTextureType_DIFFUSE) ?: "null_diff.png"
        val specularTexture = getMaterialTexture(material, Assimp.aiTextureType_SPECULAR) ?: "null_spec.png"
        val normalTexture = getMaterialTexture(material, Assimp.aiTextureType_NORMALS) ?: "null_norm.png"

        return Material(diffuseColor, ambientColor, specularColor, emissiveColor, shininess, opacity,
            loadTexture(diffuseTexture), loadTexture(specularTexture), loadTexture(normalTexture))
    }

    private fun getMaterialName(material: AIMaterial): String {
        MemoryStack.stackPush().use { stack ->
            val buffer = AIString.calloc(stack)
            val result = Assimp.aiGetMaterialString(material, Assimp.AI_MATKEY_NAME, 0, 0, buffer)
            return if (result == Assimp.aiReturn_SUCCESS) buffer.dataString() else ""
        }
    }

    private fun getMaterialColor(material: AIMaterial, key: String): Vector3f {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(4)
            val result = Assimp.aiGetMaterialFloatArray(material, key, Assimp.aiTextureType_NONE, 0, buffer, null)
            return if (result == Assimp.aiReturn_SUCCESS) Vector3f(buffer[0], buffer[1], buffer[2]) else Vector3f()
        }
    }

    private fun getMaterialFloat(material: AIMaterial, key: String): Float {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(1)
            val result = Assimp.aiGetMaterialFloatArray(material, key, Assimp.aiTextureType_NONE, 0, buffer, null)
            return if (result == Assimp.aiReturn_SUCCESS) buffer[0] else -1f
        }
    }

    private fun getMaterialTexture(material: AIMaterial, textureType: Int): String? {
        MemoryStack.stackPush().use { stack ->
            val path = AIString.malloc(stack)
            val result = Assimp.aiGetMaterialTexture(material, textureType, 0, path, null as IntArray?, null, null, null, null, null)
            return if (result == Assimp.aiReturn_SUCCESS) path.dataString() else null
        }
    }

    // TODO: これも同じテクスチャなのに、別のidで取得してる (画像事に生成してTexturesにまとめる？)
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
                val buffer = getResourceAsByteBuffer("assets/texture/entity/$file")
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

    /* Util */

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
