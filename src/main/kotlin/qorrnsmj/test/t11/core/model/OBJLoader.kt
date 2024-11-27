package qorrnsmj.test.t11.core.model

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.model.component.Mesh
import java.io.File

object OBJLoader {
    fun loadModel(modelFile: String): Mesh {
        val vertices = mutableListOf<Float>()
        val texCoords = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        val vertexMap = mutableMapOf<String, Int>()
        val processedVertices = mutableListOf<Float>()
        val processedTexCoords = mutableListOf<Float>()
        val processedNormals = mutableListOf<Float>()

        File("src/main/resources/test/$modelFile").useLines { lines ->
            lines.forEach { line -> when {
                line.startsWith("v ") -> {
                    val tokens = line.trim().split("\\s+".toRegex()).drop(1).map { it.toFloat() }
                    vertices.addAll(tokens)
                }
                line.startsWith("vt ") -> {
                    val tokens = line.trim().split("\\s+".toRegex()).drop(1).map { it.toFloat() }
                    texCoords.addAll(tokens)
                }
                line.startsWith("vn ") -> {
                    val tokens = line.trim().split("\\s+".toRegex()).drop(1).map { it.toFloat() }
                    normals.addAll(tokens)
                }
                line.startsWith("f ") -> {
                    val tokens = line.trim().split("\\s+".toRegex()).drop(1)
                    val faceVertices = tokens.map { vertexData ->
                        vertexMap.computeIfAbsent(vertexData) {
                            val vertexIndices = vertexData.split("/").map { it.toInt() - 1 }
                            val vertexIndex = vertexIndices[0] * 3
                            val texIndex = vertexIndices[1] * 2
                            val normIndex = vertexIndices[2] * 3

                            processedVertices.add(vertices[vertexIndex])
                            processedVertices.add(vertices[vertexIndex + 1])
                            processedVertices.add(vertices[vertexIndex + 2])

                            processedTexCoords.add(texCoords[texIndex])
                            processedTexCoords.add(texCoords[texIndex + 1])

                            processedNormals.add(normals[normIndex])
                            processedNormals.add(normals[normIndex + 1])
                            processedNormals.add(normals[normIndex + 2])

                            processedVertices.size / 3 - 1
                        }
                    }
                    // TODO: 四角形対応してる？
                    for (i in 1 until faceVertices.size - 1) {
                        indices.add(faceVertices[0])
                        indices.add(faceVertices[i])
                        indices.add(faceVertices[i + 1])
                    }
                }
            }}
        }

        Logger.info("\"$modelFile\" loaded (Faces: ${indices.size / 3})")

        return Loader.loadMesh(
            processedVertices.toFloatArray(),
            processedTexCoords.toFloatArray(),
            processedNormals.toFloatArray(),
            indices.toIntArray()
        )
    }
}
