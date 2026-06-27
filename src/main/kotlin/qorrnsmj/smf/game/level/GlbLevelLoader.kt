package qorrnsmj.smf.game.level

import de.javagl.jgltf.model.MeshPrimitiveModel
import de.javagl.jgltf.model.NodeModel
import de.javagl.jgltf.model.io.GltfModelReader
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.EntityLoader
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.ObjectEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.BoxCollider
import qorrnsmj.smf.physics.collision.shape.Collider
import qorrnsmj.smf.physics.collision.shape.ConvexHullCollider
import qorrnsmj.smf.physics.collision.shape.Plane
import qorrnsmj.smf.physics.component.StaticPhysics
import qorrnsmj.smf.util.ResourceUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object GlbLevelLoader {
    private const val JSON_CHUNK_TYPE = 0x4E4F534A
    private const val BLENDER_METER_TO_GAME_UNIT = 100f

    fun loadIfPresent(resourcePath: String): GlbLevel? {
        return if (ClassLoader.getSystemResource(resourcePath) == null) {
            Logger.info("GLB level not found, skipped: {}", resourcePath)
            null
        } else {
            load(resourcePath)
        }
    }

    fun load(resourcePath: String): GlbLevel {
        logDebugJsonSummary(resourcePath)
        val metadata = readNodeMetadata(resourcePath)
        val gltfModel = GltfModelReader()
            .readWithoutReferences(ResourceUtils.getResourceAsStream(resourcePath))
        val modelMap = EntityLoader.loadModelFromResource(resourcePath)

        var terrain: GlbTerrain? = null
        val staticMeshes = mutableListOf<ObjectEntity>()
        val entitySpawns = mutableListOf<GlbEntity>()
        val areaTriggers = mutableListOf<GlbTrigger>()

        gltfModel.nodeModels.forEachIndexed { index, node ->
            val meta = metadata.getOrNull(index) ?: GlbNodeMetadata(node.name ?: "node_$index")
            val extras = meta.extras
            val transform = transformFromMatrix(node.computeGlobalTransform(FloatArray(16)))

            when (resolveKind(extras)) {
                NodeKind.TERRAIN -> terrain = buildTerrain(node, meta.name, transform, modelMap)
                NodeKind.STATIC -> staticMeshes.add(buildStaticMesh(node, meta.name, extras, transform, modelMap))
                NodeKind.ENTITY -> entitySpawns.add(buildEntitySpawn(meta.name, extras, transform))
                NodeKind.TRIGGER -> areaTriggers.add(buildAreaTrigger(meta.name, extras, transform))
                NodeKind.UNKNOWN -> Unit
            }
        }
        val dedupedEntitySpawns = deduplicateEntitySpawns(entitySpawns)

        Logger.info(
            "GLB level loaded: {} (terrain={}, staticMeshes={}, entities={}, areaTriggers={})",
            resourcePath,
            terrain != null,
            staticMeshes.size,
            dedupedEntitySpawns.size,
            areaTriggers.size,
        )

        return GlbLevel(
            resourcePath = resourcePath,
            terrain = terrain,
            staticMeshes = staticMeshes,
            entitySpawns = dedupedEntitySpawns,
            areaTriggers = areaTriggers,
        )
    }

    private fun deduplicateEntitySpawns(spawns: List<GlbEntity>): List<GlbEntity> {
        val groups = spawns.groupBy { spawn ->
            EntitySpawnKey(
                type = spawn.type.normalized(),
                position = spawn.transform.position.roundedKey(),
                rotation = spawn.transform.rotation.roundedKey(),
                scale = spawn.transform.scale.roundedKey(),
            )
        }

        groups.values
            .filter { it.size > 1 }
            .forEach { duplicates ->
                val first = duplicates.first()
                Logger.warn(
                    "GLB duplicate entity spawns collapsed: {} x {} ({}) at position={}",
                    duplicates.size,
                    first.name,
                    first.type,
                    first.transform.position,
                )
            }

        return groups.values.map { it.first() }
    }

    private fun buildTerrain(
        node: NodeModel,
        name: String,
        transform: Transform,
        modelMap: Map<String, Model>,
    ): GlbTerrain {
        val geometry = collectWorldGeometry(node)
        val entity = ObjectEntity(
            transform = transform,
            model = modelMap[name] ?: EntityModels.EMPTY,
            physicsComponent = StaticPhysics(),
        )

        Logger.info("GLB terrain loaded from mesh node {}", name)

        return GlbTerrain(
            entity = entity,
            triangles = geometry.triangles.map { triangle ->
                GlbTerrainTriangle(triangle.a, triangle.b, triangle.c)
            },
        )

//        HeightMap terrain loading is intentionally kept out of the GLB level path.
//        val heightmap = extras.string("heightmap") ?: extras.string("heightMap") ?: "heightmap.png"
//        val sizeX = extras.float("sizeX") ?: extras.float("width") ?: abs(transform.scale.x).takeIf { it > 0f } ?: 400f
//        val sizeZ = extras.float("sizeZ") ?: extras.float("depth") ?: abs(transform.scale.z).takeIf { it > 0f } ?: 400f
//        val vertexCount = extras.int("gridResolution") ?: extras.int("vertexCount") ?: 64
//        val heightScale = extras.float("heightScale") ?: 50f
//        val minHeight = extras.float("minHeight") ?: 1f
    }

    private fun buildStaticMesh(
        node: NodeModel,
        name: String,
        extras: Map<String, Any?>,
        transform: Transform,
        modelMap: Map<String, Model>,
    ): ObjectEntity {
        val colliderType = colliderType(extras)
        val geometry = collectWorldGeometry(node)
        val localVertices = geometry.vertices.map { it.subtract(transform.position) }
        val localTriangles = geometry.triangles.map { triangle ->
            Triangle(
                a = triangle.a.subtract(transform.position),
                b = triangle.b.subtract(transform.position),
                c = triangle.c.subtract(transform.position),
            )
        }
        val collider = buildCollider(colliderType, localVertices, localTriangles)
        val model = modelMap[name] ?: EntityModels.EMPTY

        return ObjectEntity(
            transform = transform,
            model = model,
            physicsComponent = StaticPhysics(collider = collider),
        )
    }

    private fun buildEntitySpawn(
        name: String,
        extras: Map<String, Any?>,
        transform: Transform,
    ): GlbEntity {
        val type = extras.string("entityType")
            ?: extras.string("type")
            ?: extras.string("classname")
            ?: if (extras.boolean("spawnpoint") == true) "player" else name

        return GlbEntity(
            name = name,
            type = type,
            spawnPoint = extras.boolean("spawnpoint") ?: false,
            transform = transform,
            properties = extras.stringProperties(),
        )
    }

    private fun buildAreaTrigger(
        name: String,
        extras: Map<String, Any?>,
        transform: Transform,
    ): GlbTrigger {
        return GlbTrigger(
            name = name,
            eventName = extras.string("eventName") ?: extras.string("event") ?: name,
            center = transform.position,
            halfExtents = extras.vector3("halfExtents")
                ?: Vector3f(abs(transform.scale.x), abs(transform.scale.y), abs(transform.scale.z)),
            properties = extras.stringProperties(),
        )
    }

    private fun buildCollider(
        type: GlbColliderType,
        vertices: List<Vector3f>,
        triangles: List<Triangle>,
    ): Collider? {
        if (type == GlbColliderType.NONE || vertices.isEmpty()) return null

        return when (type) {
            GlbColliderType.AABB -> buildBoxCollider(vertices)
            GlbColliderType.CONVEX_HULL -> buildConvexHullCollider(vertices, triangles)
            GlbColliderType.NONE -> null
        }
    }

    private fun buildBoxCollider(vertices: List<Vector3f>): BoxCollider {
        val bounds = bounds(vertices)
        val center = bounds.first.add(bounds.second).scale(0.5f)
        val size = bounds.second.subtract(bounds.first)
        return BoxCollider(
            width = size.x,
            height = size.y,
            depth = size.z,
            offset = center,
        )
    }

    private fun buildConvexHullCollider(vertices: List<Vector3f>, triangles: List<Triangle>): ConvexHullCollider {
        val center = vertices.fold(Vector3f()) { acc, vertex -> acc.add(vertex) }.divide(vertices.size.toFloat())
        val planes = mutableListOf<Plane>()

        for (triangle in triangles) {
            val normal = triangle.b.subtract(triangle.a).cross(triangle.c.subtract(triangle.a))
            if (normal.lengthSquared() <= 0.000001f) continue

            val unitNormal = normal.normalize()
            var plane = Plane(normal = unitNormal, distance = unitNormal.dot(triangle.a))
            if (plane.distanceTo(center) > 0f) {
                plane = Plane(normal = plane.normal.scale(-1f), distance = -plane.distance)
            }

            if (vertices.all { plane.distanceTo(it) <= 0.05f } && planes.none { isSamePlane(it, plane) }) {
                planes.add(plane)
            }
        }

        return ConvexHullCollider(planes, vertices.distinctBy { Triple(it.x, it.y, it.z) })
    }

    private fun collectWorldGeometry(node: NodeModel): MeshGeometry {
        val matrix = node.computeGlobalTransform(FloatArray(16))
        val vertices = mutableListOf<Vector3f>()
        val triangles = mutableListOf<Triangle>()

        node.meshModels.forEach { mesh ->
            mesh.meshPrimitiveModels.forEach { primitive ->
                val positions = readPositions(primitive).map { point -> transformPoint(matrix, point) }
                val baseIndex = vertices.size
                vertices.addAll(positions)

                val indices = readIndices(primitive, positions.size)
                for (i in indices.indices step 3) {
                    if (i + 2 >= indices.size) break
                    triangles.add(
                        Triangle(
                            a = vertices[baseIndex + indices[i]],
                            b = vertices[baseIndex + indices[i + 1]],
                            c = vertices[baseIndex + indices[i + 2]],
                        )
                    )
                }
            }
        }

        return MeshGeometry(vertices, triangles)
    }

    private fun readPositions(primitive: MeshPrimitiveModel): List<Vector3f> {
        val buffer = primitive.attributes["POSITION"]
            ?.accessorData
            ?.createByteBuffer()
            ?.asFloatBuffer()
            ?: return emptyList()
        val result = mutableListOf<Vector3f>()

        while (buffer.remaining() >= 3) {
            result.add(Vector3f(buffer.get(), buffer.get(), buffer.get()))
        }

        return result
    }

    private fun readIndices(primitive: MeshPrimitiveModel, vertexCount: Int): List<Int> {
        val indices = primitive.indices ?: return (0 until vertexCount).toList()
        val buffer = indices.accessorData.createByteBuffer().order(ByteOrder.LITTLE_ENDIAN)
        return when (indices.componentType) {
            5121 -> List(indices.count) { buffer.get().toInt() and 0xFF }
            5123 -> List(indices.count) { buffer.short.toInt() and 0xFFFF }
            5125 -> List(indices.count) { buffer.int }
            else -> (0 until vertexCount).toList()
        }
    }

    private fun transformFromMatrix(matrix: FloatArray): Transform {
        val scaleX = Vector3f(matrix[0], matrix[1], matrix[2]).length()
        val scaleY = Vector3f(matrix[4], matrix[5], matrix[6]).length()
        val scaleZ = Vector3f(matrix[8], matrix[9], matrix[10]).length()
        val rotation = eulerFromMatrix(matrix, scaleX, scaleY, scaleZ)

        return Transform(
            position = Vector3f(matrix[12], matrix[13], matrix[14]).scale(BLENDER_METER_TO_GAME_UNIT),
            rotation = rotation,
            scale = Vector3f(scaleX, scaleY, scaleZ).scale(BLENDER_METER_TO_GAME_UNIT),
        )
    }

    private fun eulerFromMatrix(matrix: FloatArray, scaleX: Float, scaleY: Float, scaleZ: Float): Vector3f {
        val r00 = matrix[0] / scaleX
        val r10 = matrix[1] / scaleX
        val r20 = matrix[2] / scaleX
        val r21 = matrix[6] / scaleY
        val r22 = matrix[10] / scaleZ
        val sy = sqrt(r00 * r00 + r10 * r10)

        val x: Float
        val y: Float
        val z: Float
        if (sy > 0.000001f) {
            x = atan2(r21, r22)
            y = atan2(-r20, sy)
            z = atan2(r10, r00)
        } else {
            x = atan2(-matrix[9] / scaleZ, matrix[5] / scaleY)
            y = atan2(-r20, sy)
            z = 0f
        }

        return Vector3f(
            Math.toDegrees(x.toDouble()).toFloat(),
            Math.toDegrees(y.toDouble()).toFloat(),
            Math.toDegrees(z.toDouble()).toFloat(),
        )
    }

    private fun transformPoint(matrix: FloatArray, point: Vector3f): Vector3f {
        return Vector3f(
            matrix[0] * point.x + matrix[4] * point.y + matrix[8] * point.z + matrix[12],
            matrix[1] * point.x + matrix[5] * point.y + matrix[9] * point.z + matrix[13],
            matrix[2] * point.x + matrix[6] * point.y + matrix[10] * point.z + matrix[14],
        ).scale(BLENDER_METER_TO_GAME_UNIT)
    }

    private fun bounds(vertices: List<Vector3f>): Pair<Vector3f, Vector3f> {
        val first = vertices.first()
        var minX = first.x
        var minY = first.y
        var minZ = first.z
        var maxX = first.x
        var maxY = first.y
        var maxZ = first.z

        vertices.drop(1).forEach { vertex ->
            minX = minOf(minX, vertex.x)
            minY = minOf(minY, vertex.y)
            minZ = minOf(minZ, vertex.z)
            maxX = maxOf(maxX, vertex.x)
            maxY = maxOf(maxY, vertex.y)
            maxZ = maxOf(maxZ, vertex.z)
        }

        return Vector3f(minX, minY, minZ) to Vector3f(maxX, maxY, maxZ)
    }

    private fun isSamePlane(a: Plane, b: Plane): Boolean {
        return a.normal.subtract(b.normal).length() <= 0.001f &&
            abs(a.distance - b.distance) <= 0.05f
    }

    private fun colliderType(extras: Map<String, Any?>): GlbColliderType {
        return when ((extras.string("colliderType") ?: extras.string("collider") ?: "none").normalized()) {
            "aabb", "box", "boxcollider" -> GlbColliderType.AABB
            "convexhull", "convexhullcollider", "convex" -> GlbColliderType.CONVEX_HULL
            else -> GlbColliderType.NONE
        }
    }

    private fun resolveKind(extras: Map<String, Any?>): NodeKind {
        return extras.string("kind")?.let { kindFromString(it) } ?: NodeKind.UNKNOWN
    }

    private fun kindFromString(value: String): NodeKind {
        return when (value.normalized()) {
            "terrain" -> NodeKind.TERRAIN
            "static" -> NodeKind.STATIC
            "entity" -> NodeKind.ENTITY
            "trigger" -> NodeKind.TRIGGER
            else -> NodeKind.UNKNOWN
        }
    }

    private fun readNodeMetadata(resourcePath: String): List<GlbNodeMetadata> {
        val root = readJsonRoot(resourcePath) ?: return emptyList()
        val nodes = root["nodes"] as? List<*> ?: return emptyList()

        return nodes.mapIndexed { index, value ->
            val node = value as? Map<*, *> ?: emptyMap<String, Any?>()
            @Suppress("UNCHECKED_CAST")
            GlbNodeMetadata(
                name = node["name"] as? String ?: "node_$index",
                extras = node["extras"] as? Map<String, Any?> ?: emptyMap(),
            )
        }
    }

    private fun logDebugJsonSummary(resourcePath: String) {
        val root = readJsonRoot(resourcePath) ?: return

        Logger.debug("GLB JSON summary: {}", resourcePath)
        (root["asset"] as? Map<*, *>)?.let { asset ->
            Logger.debug(
                "  asset: version={}, generator={}",
                asset["version"] ?: "",
                asset["generator"] ?: "",
            )
        }

        logNamedArray(root, "scenes")
        logNodeSummaries(root)
        logNamedArray(root, "meshes")
        logNamedArray(root, "materials")
        logNamedArray(root, "images")

        (root["buffers"] as? List<*>)?.forEachIndexed { index, value ->
            val buffer = value as? Map<*, *> ?: return@forEachIndexed
            Logger.debug("  buffer[{}]: byteLength={}", index, buffer["byteLength"] ?: "")
        }
    }

    private fun logNodeSummaries(root: Map<*, *>) {
        val nodes = root["nodes"] as? List<*> ?: return
        Logger.debug("  nodes: {}", nodes.size)

        nodes.forEachIndexed { index, value ->
            val node = value as? Map<*, *> ?: return@forEachIndexed
            Logger.debug(
                "    node[{}]: name={}, mesh={}, kind={}, translation={}, rotation={}, scale={}, extras={}",
                index,
                node["name"] ?: "",
                node["mesh"] ?: "",
                (node["extras"] as? Map<*, *>)?.get("kind") ?: "",
                compactJsonValue(node["translation"]),
                compactJsonValue(node["rotation"]),
                compactJsonValue(node["scale"]),
                compactJsonValue(node["extras"]),
            )
        }
    }

    private fun logNamedArray(root: Map<*, *>, key: String) {
        val values = root[key] as? List<*> ?: return
        Logger.debug("  {}: {}", key, values.size)

        values.forEachIndexed { index, value ->
            val item = value as? Map<*, *> ?: return@forEachIndexed
            Logger.debug("    {}[{}]: name={}", key.dropLast(1), index, item["name"] ?: "")
        }
    }

    private fun readJsonRoot(resourcePath: String): Map<*, *>? {
        val bytes = ResourceUtils.getResourceAsStream(resourcePath).readAllBytes()
        val jsonText = if (resourcePath.endsWith(".glb", ignoreCase = true)) {
            readGlbJson(bytes)
        } else {
            bytes.toString(Charsets.UTF_8)
        }

        return GlbJson.parse(jsonText) as? Map<*, *>
    }

    private fun readGlbJson(bytes: ByteArray): String {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buffer.int
        check(magic == 0x46546C67) { "Not a GLB file" }
        buffer.int
        buffer.int

        while (buffer.remaining() >= 8) {
            val chunkLength = buffer.int
            val chunkType = buffer.int
            val chunk = ByteArray(chunkLength)
            buffer.get(chunk)
            if (chunkType == JSON_CHUNK_TYPE) {
                return chunk.toString(Charsets.UTF_8).trimEnd('\u0000', ' ', '\n', '\r', '\t')
            }
        }

        error("GLB JSON chunk not found")
    }

    private fun compactJsonValue(value: Any?): String {
        return when (value) {
            null -> ""
            is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { (key, item) ->
                "$key=${compactJsonValue(item)}"
            }
            is List<*> -> value.joinToString(prefix = "[", postfix = "]") { compactJsonValue(it) }
            else -> value.toString()
        }
    }

    private fun Map<String, Any?>.string(key: String): String? = this[key] as? String

    private fun Map<String, Any?>.float(key: String): Float? = when (val value = this[key]) {
        is Number -> value.toFloat()
        is String -> value.toFloatOrNull()
        else -> null
    }

    private fun Map<String, Any?>.int(key: String): Int? = when (val value = this[key]) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }

    private fun Map<String, Any?>.boolean(key: String): Boolean? = when (val value = this[key]) {
        is Boolean -> value
        is String -> value.equals("true", ignoreCase = true)
        else -> null
    }

    private fun Map<String, Any?>.vector3(key: String): Vector3f? {
        val value = this[key] as? List<*> ?: return null
        if (value.size < 3) return null
        return Vector3f(
            (value[0] as? Number)?.toFloat() ?: return null,
            (value[1] as? Number)?.toFloat() ?: return null,
            (value[2] as? Number)?.toFloat() ?: return null,
        )
    }

    private fun Map<String, Any?>.stringProperties(): Map<String, String> {
        return mapValues { (_, value) -> value.toString() }
    }

    private fun String.normalized(): String =
        lowercase().filter { it.isLetterOrDigit() }

    private data class GlbNodeMetadata(
        val name: String,
        val extras: Map<String, Any?> = emptyMap(),
    )

    private data class EntitySpawnKey(
        val type: String,
        val position: Triple<Int, Int, Int>,
        val rotation: Triple<Int, Int, Int>,
        val scale: Triple<Int, Int, Int>,
    )

    private data class MeshGeometry(
        val vertices: List<Vector3f>,
        val triangles: List<Triangle>,
    )

    private data class Triangle(
        val a: Vector3f,
        val b: Vector3f,
        val c: Vector3f,
    )

    private enum class NodeKind {
        TERRAIN,
        STATIC,
        ENTITY,
        TRIGGER,
        UNKNOWN,
    }

    private fun Vector3f.roundedKey(): Triple<Int, Int, Int> =
        Triple(
            (x * 1000f).toInt(),
            (y * 1000f).toInt(),
            (z * 1000f).toInt(),
        )
}
