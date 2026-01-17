package qorrnsmj.smf.game.entity

import de.javagl.jgltf.model.MeshPrimitiveModel
import de.javagl.jgltf.model.NodeModel
import de.javagl.jgltf.model.SceneModel
import de.javagl.jgltf.model.io.GltfModelReader
import de.javagl.jgltf.model.v2.MaterialModelV2
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryUtil
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.`object`.Mesh
import qorrnsmj.smf.graphic.`object`.Material
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.graphic.texture.TextureLoader.loadTexture
import qorrnsmj.smf.graphic.texture.TexturePresets
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.smf.util.Cleanable
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer

object EntityLoader : Cleanable {
    private val vaos = mutableListOf<Int>()
    private val vbos = mutableListOf<Int>()
    private val ebos = mutableListOf<Int>()

    /* Loader */

    fun loadModel(file: String): Map<String, Model> {
        try {
            val gltfModel = GltfModelReader()
                .readWithoutReferences(getResourceAsStream("assets/model/$file"))
            val models = hashMapOf<String, Model>()

            gltfModel.sceneModels.forEach { scene: SceneModel ->
                scene.nodeModels.forEach { node: NodeModel ->
                    loadNode(models, node, scene.name)
                    /*node.meshModels.forEach { mesh: MeshModel ->
                        mesh.meshPrimitiveModels.forEach { primitive ->
                            val name = node.name ?: mesh.name ?: "mesh"
                            val mesh = loadMesh(primitive)
                            val material = loadMaterial(primitive)
                            models[name] = Model(name, mesh, material)
                        }
                    }*/
                }
            }

            val faceCount = models.values.sumOf { it.mesh.vertexCount }.div(3)
            Logger.info("\"$file\" loaded ($faceCount faces)")

            return models
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return emptyMap()
    }

    // TEST: Blenderの親子構造のプラクティスを学ぶ
    private fun loadNode(models: HashMap<String, Model>, node: NodeModel, parentName: String?) {
        val nodeName = node.name ?: parentName ?: "node"

        node.meshModels.forEach { meshModel ->
            meshModel.meshPrimitiveModels.forEach { primitive ->
                val name = nodeName
                val mesh = loadMesh(primitive)
                val material = loadMaterial(primitive)
                models[name] = Model(mesh, material)
            }
        }

        node.children.forEach { child ->
            loadNode(models, child, nodeName)
        }
    }

    private fun loadMesh(primitive: MeshPrimitiveModel): Mesh {
        // bind vao
        val vao = glGenVertexArrays()
        glBindVertexArray(vao)
        vaos.add(vao)

        // get buffers
        val positions = primitive.attributes["POSITION"]
            ?.accessorData?.createByteBuffer()?.asFloatBuffer()
        val texCoords0 = primitive.attributes["TEXCOORD_0"]
            ?.accessorData?.createByteBuffer()?.asFloatBuffer()
        val normals = primitive.attributes["NORMAL"]
            ?.accessorData?.createByteBuffer()?.asFloatBuffer()
        var tangents = primitive.attributes["TANGENT"]
            ?.accessorData?.createByteBuffer()?.asFloatBuffer()
        val indices = primitive.indices.accessorData.createByteBuffer()

        // generate default tangents if not present
        val vertexCount = primitive.attributes["POSITION"]!!.accessorData.numElements
        if (tangents == null) {
            tangents = MemoryUtil.memAllocFloat(vertexCount * 4)
            repeat(vertexCount) {
                tangents.put(1.0f).put(0.0f).put(0.0f).put(1.0f)
            }
            tangents.flip()
        }

        // bind objects
        bindVBO(0, 3, positions)
        bindVBO(1, 2, texCoords0)
        bindVBO(2, 3, normals)
        bindVBO(3, 4, tangents)
        bindEBO(indices)

        // enable attributes
        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)
        glEnableVertexAttribArray(2)
        glEnableVertexAttribArray(3)

        // unbind objects
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
        MemoryUtil.memFree(tangents)

        return Mesh(vao, primitive.indices.count, primitive.indices.componentType)
    }

    private fun loadMaterial(primitive: MeshPrimitiveModel): Material {
        val materialModel = primitive.materialModel as? MaterialModelV2
            ?: error("Unsupported material model: ${primitive.materialModel.name}")

        // factors
        val baseColorFactor = materialModel.baseColorFactor.let {
            Vector4f(it[0], it[1], it[2], it[3])
        }
        val emissiveFactor = materialModel.emissiveFactor.let {
            Vector3f(it[0], it[1], it[2])
        }
        val metallicFactor = materialModel.metallicFactor
        val roughnessFactor = materialModel.roughnessFactor

        // textures
        val baseColorTexture = materialModel.baseColorTexture
            ?.let { loadTexture(it, TexturePresets.ENTITY) }
            ?: Textures.DEFAULT_000000
        val metallicRoughnessTexture = materialModel.metallicRoughnessTexture
            ?.let { loadTexture(it, TexturePresets.ENTITY) }
            ?: Textures.DEFAULT_00FF00
        val normalTexture = materialModel.normalTexture
            ?.let { loadTexture(it, TexturePresets.ENTITY) }
            ?: Textures.DEFAULT_8080FF
        val occlusionTexture = materialModel.occlusionTexture
            ?.let { loadTexture(it, TexturePresets.ENTITY) }
            ?: Textures.DEFAULT_FFFFFF
        val emissiveTexture = materialModel.emissiveTexture
            ?.let { loadTexture(it, TexturePresets.ENTITY) }
            ?: Textures.DEFAULT_000000

        // texture params
        val normalScale = materialModel.normalScale
        val occlusionStrength = materialModel.occlusionStrength

        // render states
        val alphaMode = materialModel.alphaMode
        val alphaCutoff = materialModel.alphaCutoff
        val doubleSided = materialModel.isDoubleSided

        Logger.debug("-----------------------")
        Logger.debug("Material: ${materialModel.name}")
        Logger.debug("  Base Color Factor: $baseColorFactor")
        Logger.debug("  Emissive Factor: $emissiveFactor")
        Logger.debug("  Metallic Factor: $metallicFactor")
        Logger.debug("  Roughness Factor: $roughnessFactor")
        Logger.debug("  Base Color Texture: $baseColorTexture")
        Logger.debug("  Metallic-Roughness Texture: $metallicRoughnessTexture")
        Logger.debug("  Normal Texture: $normalTexture")
        Logger.debug("  Occlusion Texture: $occlusionTexture")
        Logger.debug("  Emissive Texture: $emissiveTexture")
        Logger.debug("  Normal Scale: $normalScale")
        Logger.debug("  Occlusion Strength: $occlusionStrength")
        Logger.debug("  Alpha Mode: $alphaMode")
        Logger.debug("  Alpha Cutoff: $alphaCutoff")
        Logger.debug("  Double Sided: $doubleSided")
        Logger.debug("-----------------------")

        return Material(
            baseColorFactor,
            emissiveFactor,
            metallicFactor,
            roughnessFactor,

            baseColorTexture,
            metallicRoughnessTexture,
            normalTexture,
            occlusionTexture,
            emissiveTexture,

            normalScale,
            occlusionStrength,

            alphaMode,
            alphaCutoff,
            doubleSided
        )
    }

    /* Util */

    private fun bindVBO(attribute: Int, size: Int, buffer: FloatBuffer?) {
        if (buffer == null) return

        val vbo = glGenBuffers()
        vbos.add(vbo)

        buffer.rewind()
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
        glVertexAttribPointer(attribute, size, GL_FLOAT, false, 0, 0L)
    }

    private fun bindEBO(buffer: ByteBuffer?) {
        if (buffer == null) return

        val ebo = glGenBuffers()
        ebos.add(ebo)

        buffer.rewind()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
    }

    private fun getResourceAsStream(path: String): InputStream {
        return ClassLoader.getSystemResourceAsStream(path)
            ?: error("Resource not found: $path")
    }

    override fun cleanup() {
        vaos.forEach { glDeleteVertexArrays(it) }
        vbos.forEach { glDeleteBuffers(it) }
        ebos.forEach { glDeleteBuffers(it) }
    }
}
