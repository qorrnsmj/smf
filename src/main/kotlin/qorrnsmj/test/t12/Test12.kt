package qorrnsmj.test.t12

import org.lwjgl.BufferUtils
import org.lwjgl.assimp.*
import org.lwjgl.system.MemoryStack
import qorrnsmj.smf.graphic.`object`.Material
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.use

object Test12 {
    fun loadModel(filePath: String) {
        // Assimpでファイルをインポート
        val scene = Assimp.aiImportFile(
            filePath,
            Assimp.aiProcess_Triangulate or Assimp.aiProcess_FlipUVs
        ) ?: throw IllegalStateException("モデルの読み込みに失敗したわ！")

        // メッシュ情報を取得
        val numMeshes = scene.mNumMeshes()
        println("メッシュ数: $numMeshes")

        val meshArray = scene.mMeshes() ?: throw IllegalStateException("メッシュがないじゃない！")
        val materialMap = mutableMapOf<String, Material>()  // マテリアル名をキーにしたマテリアルのマップ

        // マテリアル情報を取得
        val numMaterials = scene.mNumMaterials()
        println("マテリアル数: $numMaterials")

        val materials = scene.mMaterials() ?: throw IllegalStateException("マテリアルがないみたいね。")
        for (i in 0 until numMaterials) {
            val material = AIMaterial.create(materials[i])

            // マテリアル名を取得
            val name = AIString.calloc()
            Assimp.aiGetMaterialString(material, Assimp.AI_MATKEY_NAME, Assimp.aiTextureType_NONE, 0, name)
            val materialName = name.dataString()
            println("マテリアル $i の名前: $materialName")
            name.free()

            // マテリアルをマップに格納
            //materialMap[materialName] = processMaterial(material)
        }

        // 各メッシュに対するマテリアルを紐づけ
        for (i in 0 until numMeshes) {
            val mesh = AIMesh.create(meshArray[i])
            val meshName = mesh.mName().dataString()

            val materialIndex = mesh.mMaterialIndex()
            val materialName = getMaterialNameFromIndex(scene, materialIndex)
            println("$meshName メッシュの頂点数: ${mesh.mNumVertices()}, material: $materialName ($materialIndex)")

            // 頂点位置を取得
            val vertices = mesh.mVertices()
            for (j in 0 until mesh.mNumVertices()) {
                val vertex = vertices[j]
                //println("頂点 $j: (${vertex.x()}, ${vertex.y()}, ${vertex.z()})")
            }

            // UV座標を取得
            val textureCoords = mesh.mTextureCoords(0)
            if (textureCoords != null) {
                for (j in 0 until mesh.mNumVertices()) {
                    val uv = textureCoords[j]
                    //println("UV $j: (${uv.x()}, ${uv.y()})")
                }
            } else {
                println("UV座標が見つからないわよ！")
            }

            // ここでMeshとMaterialを関連付ける処理を追加
            //val model = Model(mesh, material)
            // modelsリストに追加
            //models.add(model)
        }

        // メモリ解放
        Assimp.aiReleaseImport(scene)
    }

// マテリアルインデックスからマテリアル名を取得する関数
private fun getMaterialNameFromIndex(scene: AIScene, materialIndex: Int): String {
    val materials = scene.mMaterials() ?: throw IllegalStateException("マテリアルがないみたいね。")
    val material = AIMaterial.create(materials[materialIndex])

    val name = AIString.calloc()
    Assimp.aiGetMaterialString(material, Assimp.AI_MATKEY_NAME, Assimp.aiTextureType_NONE, 0, name)
    val materialName = name.dataString()
    name.free()

    return materialName
}


    fun loadModelMaterial(file: String) {
        val fileExtension = file.substringAfterLast('.', "").lowercase()
        val fileBuffer = getResourceAsByteBuffer("assets/model/$file")

        // これはできる
        val scene = Assimp.aiImportFile(
            "src/main/resources/assets/model/$file",
            Assimp.aiProcess_Triangulate or Assimp.aiProcess_FlipUVs
        ) ?: throw IllegalStateException("モデルの読み込みに失敗したわ！")

        /**
         *  This is a straightforward way to decode models from memory buffers, but it doesn't handle model formats that spread their data across multiple files or
         *  even directories. Examples include OBJ or MD3, which outsource parts of their material info into external scripts. If you need full functionality,
         *  provide a custom IOSystem to make Assimp find these files and use the regular {@link #aiImportFileEx ImportFileEx}/{@link #aiImportFileExWithProperties ImportFileExWithProperties} API.</p>
         */
        // これだとできない
//        val scene = Assimp.aiImportFileFromMemory(
//            fileBuffer,
//            Assimp.aiProcess_Triangulate or Assimp.aiProcess_FlipUVs,
//            fileExtension
//        ) ?: throw IllegalStateException("モデルの読み込みに失敗したわ！")

        val numMaterials = scene.mNumMaterials()
        println("マテリアル数: $numMaterials")

        val materials = scene.mMaterials() ?: throw IllegalStateException("マテリアルが見つからないみたいね。")
        for (i in 0 until numMaterials) {
            val material = AIMaterial.create(materials[i])

            println("=== マテリアル $i ===")
            printMaterialInfo(material)
        }

        Assimp.aiReleaseImport(scene)
    }

    private fun printMaterialInfo(material: AIMaterial) {
        val name = getMaterialName(material)
        val ambient = getMaterialColor(material, Assimp.AI_MATKEY_COLOR_AMBIENT)
        val diffuse = getMaterialColor(material, Assimp.AI_MATKEY_COLOR_DIFFUSE)
        val specular = getMaterialColor(material, Assimp.AI_MATKEY_COLOR_SPECULAR)
        val emissive = getMaterialColor(material, Assimp.AI_MATKEY_COLOR_EMISSIVE)
        val shininess = getMaterialFloat(material, Assimp.AI_MATKEY_SHININESS)
        val opacity = getMaterialFloat(material, Assimp.AI_MATKEY_OPACITY)
        val diffuseTexture = getMaterialTexture(material, Assimp.aiTextureType_DIFFUSE) ?: "null_diff.png"
        val specularTexture = getMaterialTexture(material, Assimp.aiTextureType_SPECULAR) ?: "null_spec.png"
        val normalTexture = getMaterialTexture(material, Assimp.aiTextureType_NORMALS) ?: "null_norm.png"

        println("[Name]: $name")
        println("Ambient: $ambient")
        println("Diffuse: $diffuse")
        println("Specular: $specular")
        println("Emissive: $emissive")
        println("Shininess: $shininess")
        println("Opacity: $opacity")
        println("Diffuse Texture: $diffuseTexture")
        println("Specular Texture: $specularTexture")
        println("Normal Texture: $normalTexture")
    }

    private fun getMaterialName(material: AIMaterial): String {
        MemoryStack.stackPush().use { stack ->
            val name = AIString.malloc(stack)
            val found = Assimp.aiGetMaterialString(material, Assimp.AI_MATKEY_NAME, Assimp.aiTextureType_NONE, 0, name)
            return if (found == 0) name.dataString() else "未設定"
        }
    }

    private fun getMaterialColor(material: AIMaterial, key: String): String {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(4)
            val found = Assimp.aiGetMaterialFloatArray(material, key, Assimp.aiTextureType_NONE, 0, buffer, null)
            return if (found == 0) "(${buffer[0]}, ${buffer[1]}, ${buffer[2]}, ${buffer[3]})" else "未設定"
        }
    }

    private fun getMaterialFloat(material: AIMaterial, key: String): Float {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(1)
            val found = Assimp.aiGetMaterialFloatArray(material, key, Assimp.aiTextureType_NONE, 0, buffer, null)
            return if (found == 0) buffer[0] else -1f
        }
    }

    private fun getMaterialTexture(material: AIMaterial, textureType: Int): String? {
        MemoryStack.stackPush().use { stack ->
            val path = AIString.malloc(stack)
            val found = Assimp.aiGetMaterialTexture(material, textureType, 0, path, null as IntArray?, null, null, null, null, null)
            return if (found == 0) path.dataString() else null
        }
    }

    /* Util */

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

    @JvmStatic
    fun main(string: Array<String>) {
        val filePath = "stall.obj"
        //loadModel(filePath)
        println("--------------------")
        loadModelMaterial(filePath)
    }
}
