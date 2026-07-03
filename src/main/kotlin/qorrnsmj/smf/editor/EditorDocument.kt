package qorrnsmj.smf.editor

import com.fasterxml.jackson.databind.ObjectMapper
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.EntityLoader
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.ObjectEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.graphic.skybox.SkyboxLoader
import qorrnsmj.smf.math.Quaternion
import qorrnsmj.smf.math.Vector3f
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.collections.get
import kotlin.collections.iterator

internal class EditorDocument(private val context: EditorContext) {
    private val objectMapper = ObjectMapper()

    fun openWorkspace(path: String, loadLevel: Boolean = true) {
        if (path.isBlank()) {
            Logger.warn("Workspace path is empty")
            return
        }

        val root = Paths.get(path).toAbsolutePath().normalize()
        Files.createDirectories(root.resolve("level"))
        Files.createDirectories(root.resolve("model"))
        Files.createDirectories(root.resolve("texture").resolve("terrain").resolve("map"))

        context.workspaceRoot.set(root.toString())
        setProjectName(context.projectName.get())
        refreshTerrainMapPaths()
        context.assetModelRoot.set(root.resolve("model").toString())
        context.assetBrowserPath.set(context.assetModelRoot.get())
        context.modelCache.clear()
        refreshAssets()
        if (loadLevel) loadMap(recordUndo = false)
    }

    fun setProjectName(name: String) {
        val safeName = sanitizeProjectName(name)
        context.projectName.set(safeName)
        if (safeName.isBlank()) {
            context.mapPath.set("")
            return
        }
        val workspaceRoot = workspaceRootPathOrNull() ?: return
        context.mapPath.set(workspaceRoot.resolve("level").resolve("$safeName.json").toString())
        refreshTerrainMapPaths()
    }

    fun setLevelPath(path: String) {
        val levelPath = Paths.get(path).toAbsolutePath().normalize()
        context.mapPath.set(levelPath.toString())
        syncProjectNameFromLevelPath(levelPath)
        syncWorkspaceFromLevelPath(levelPath)
        refreshTerrainMapPaths()
        refreshAssets()
    }

    fun setSkyboxPath(path: String) {
        val normalized = normalizeSkyboxPath(path)
        context.skyboxPath.set(displaySkyboxPath(normalized))
        applySkyboxPath()
    }

    fun setNightMode(enabled: Boolean) {
        setTimeOfDay(if (enabled) EditorTimeOfDay.NIGHT else EditorTimeOfDay.NOON)
    }

    fun setTimeOfDay(timeOfDay: EditorTimeOfDay) {
        setViewportTimeOfDay(0, timeOfDay)
    }

    fun setViewportTimeOfDay(viewportIndex: Int, timeOfDay: EditorTimeOfDay) {
        context.setViewportTimeOfDay(viewportIndex, timeOfDay)
        applyTimeOfDay()
    }

    fun refreshAssets() {
        if (context.assetModelRoot.get().isBlank()) {
            context.availableGlbs.clear()
            context.availableAssetDirectories.clear()
            context.selectedAssetIndex = 0
            return
        }

        val root = assetRootPath()
        var current = assetBrowserPath()
        if (!Files.isDirectory(current)) current = root
        if (!current.startsWith(root)) {
            current = root
        }
        context.assetBrowserPath.set(current.toString())

        context.availableGlbs.clear()
        context.availableAssetDirectories.clear()
        context.availableAssetDirectories.addAll(scanAssetDirectories(assetBrowserPath()))
        context.availableGlbs.addAll(scanGlbs(assetRootPath(), assetBrowserPath()))
        if (context.selectedAssetIndex !in context.availableGlbs.indices) context.selectedAssetIndex = 0
    }

    fun setAssetRoot(path: String) {
        if (path.isBlank()) return
        val root = Paths.get(path).toAbsolutePath().normalize()
        context.assetModelRoot.set(root.toString())
        context.assetBrowserPath.set(root.toString())
        refreshAssets()
    }

    fun setAssetBrowserPath(path: String) {
        if (path.isBlank()) return
        if (context.assetModelRoot.get().isBlank()) {
            setAssetRoot(path)
            return
        }
        val requested = Paths.get(path).toAbsolutePath().normalize()
        val root = assetRootPath()
        context.assetBrowserPath.set(if (requested.startsWith(root)) requested.toString() else root.toString())
        refreshAssets()
    }

    fun enterAssetDirectory(name: String) {
        val next = assetBrowserPath().resolve(name).normalize()
        if (!Files.isDirectory(next)) return
        context.assetBrowserPath.set(next.toString())
        refreshAssets()
    }

    fun leaveAssetDirectory() {
        val current = assetBrowserPath()
        val parent = current.parent ?: return
        if (!parent.startsWith(assetRootPath())) return
        context.assetBrowserPath.set(parent.toString())
        refreshAssets()
    }

    fun addSelectedAsset(position: Vector3f) {
        val asset = context.availableGlbs.getOrNull(context.selectedAssetIndex) ?: return
        addAsset(asset, position)
    }

    fun addAsset(resourcePath: String, position: Vector3f) {
        val modelPath = normalizeModelPath(resourcePath)
        registerExternalGlb(modelPath)
        pushUndoSnapshot()
        addObject(
            name = uniqueStaticObjectName(defaultStaticObjectName(modelPath)),
            id = nextStaticObjectId(modelPath),
            resourcePath = modelPath,
            transform = Transform(position = position, scale = Vector3f(1f, 1f, 1f)),
            folder = "",
        )
        context.selectedIndex = context.placedObjects.lastIndex
        selectOnly(context.selectedIndex)
        context.selectedCollisionIndex = -1
    }

    fun addSpawnPoint(position: Vector3f) {
        pushUndoSnapshot()
        context.eventAreas.add(
            EditorEventArea(
                name = uniqueEventAreaName("Spawnpoint"),
                id = uniqueEventAreaId(SPAWNPOINT_ID),
                kind = EditorEventAreaKind.SPAWN_POINT,
                position = placePointOnGround(position),
                size = Vector3f(PLAYER_CAPSULE_RADIUS * 2f, PLAYER_CAPSULE_HEIGHT, PLAYER_CAPSULE_RADIUS * 2f),
            )
        )
        selectEventArea(context.eventAreas.lastIndex)
    }

    fun addEventArea(position: Vector3f) {
        pushUndoSnapshot()
        val size = Vector3f(3f, 2f, 3f)
        context.eventAreas.add(
            EditorEventArea(
                name = uniqueEventAreaName("EventArea"),
                id = uniqueEventAreaId("area_trigger"),
                kind = EditorEventAreaKind.AREA_TRIGGER,
                position = placeBoxOnGround(position, size),
                size = size,
            )
        )
        selectEventArea(context.eventAreas.lastIndex)
    }

    fun selectObject(index: Int) {
        context.selectedIndex = index
        context.selectedSpawnPointIndex = -1
        context.selectedEventAreaIndex = -1
    }

    fun selectSpawnPoint(eventAreaIndex: Int, index: Int) {
        context.selectedIndex = -1
        context.selectedIndices.clear()
        context.selectedCollisionIndex = -1
        context.selectedSpawnPointIndex = index
        context.selectedEventAreaIndex = eventAreaIndex
    }

    fun selectEventArea(index: Int) {
        context.selectedIndex = -1
        context.selectedIndices.clear()
        context.selectedCollisionIndex = -1
        context.selectedSpawnPointIndex = -1
        context.selectedEventAreaIndex = index
    }

    fun deleteSelected() {
        when {
            context.selectedEventAreaIndex in context.eventAreas.indices -> deleteEventArea(context.selectedEventAreaIndex)
            else -> deleteObjects(selectedObjectIndices())
        }
    }

    fun deleteEventArea(index: Int) {
        if (index !in context.eventAreas.indices) return
        pushUndoSnapshot()
        context.eventAreas.removeAt(index)
        context.selectedEventAreaIndex = -1
        context.selectedSpawnPointIndex = -1
    }

    fun snapSelectedMarkerToGround() {
        when {
            context.selectedEventAreaIndex in context.eventAreas.indices -> {
                pushUndoSnapshot()
                val area = context.eventAreas[context.selectedEventAreaIndex]
                area.position = if (area.isSpawnpoint()) {
                    placePointOnGround(area.position)
                } else {
                    placeBoxOnGround(area.position, area.size)
                }
            }
        }
    }

    fun registerExternalGlb(path: String) {
        if (!path.endsWith(".glb", ignoreCase = true)) return
        val modelPath = normalizeModelPath(path)
        if (modelPath !in context.availableGlbs) {
            context.availableGlbs.add(modelPath)
            context.availableGlbs.sort()
        }
        context.selectedAssetIndex = context.availableGlbs.indexOf(modelPath)
    }

    fun duplicateSelected() {
        val selected = context.selectedObject() ?: return
        pushUndoSnapshot()
        addObject(
            name = uniqueStaticObjectName("${selected.name} Copy"),
            id = nextStaticObjectId(selected.resourcePath),
            resourcePath = selected.resourcePath,
            folder = selected.folder,
            transform = selected.root.localTransform.copy(
                position = selected.root.localTransform.position.add(Vector3f(1f, 0f, 1f))
            ),
            collisions = selected.collisions.map { it.deepCopy() }.toMutableList(),
        )
        context.selectedIndex = context.placedObjects.lastIndex
        selectOnly(context.selectedIndex)
    }

    fun deleteObjects(indices: Collection<Int>) {
        val targets = indices.filter { it in context.placedObjects.indices }.distinct().sortedDescending()
        if (targets.isEmpty()) return
        pushUndoSnapshot()

        for (index in targets) {
            context.scene.entities.remove(context.placedObjects[index].root)
            context.placedObjects.removeAt(index)
        }
        context.selectedIndex = context.placedObjects.indices.lastOrNull() ?: -1
        selectOnly(context.selectedIndex)
        context.selectedCollisionIndex = -1
    }

    fun addObject(
        name: String,
        id: String,
        resourcePath: String,
        transform: Transform,
        folder: String = "",
        collisions: MutableList<EditorCollisionState> = mutableListOf(),
    ) {
        val root = ObjectEntity(transform = transform, model = EntityModels.EMPTY)
        val models = context.modelCache.getOrPut(resourcePath) { loadModels(resourcePath) }

        for ((name, model) in models) {
            if (name.startsWith("collision_", ignoreCase = true)) continue
            root.addChild(ObjectEntity(transform = Transform(), model = model))
        }

        context.scene.entities.add(root)
        context.placedObjects.add(EditorPlacedObject(name, id, resourcePath, root, folder, collisions))
    }

    fun pushUndoSnapshot() {
        context.undoStack.add(captureSnapshot())
        context.redoStack.clear()
    }

    fun undo() {
        if (context.undoStack.isEmpty()) return

        context.redoStack.add(captureSnapshot())
        restoreSnapshot(context.undoStack.removeAt(context.undoStack.lastIndex))
    }

    fun redo() {
        if (context.redoStack.isEmpty()) return

        context.undoStack.add(captureSnapshot())
        restoreSnapshot(context.redoStack.removeAt(context.redoStack.lastIndex))
    }

    fun saveMap() {
        context.projectName.set(sanitizeProjectName(context.projectName.get()))
        if (context.mapPath.get().isBlank() && context.projectName.get().isNotBlank() && context.workspaceRoot.get().isNotBlank()) {
            setProjectName(context.projectName.get())
        }
        if (context.mapPath.get().isBlank()) {
            Logger.warn("Level path is empty")
            return
        }
        val path = Paths.get(context.mapPath.get())
        Files.createDirectories(path.parent ?: Path.of("."))
        ensureUniqueStaticObjectNames()

        val objects = context.placedObjects.map { placed ->
            val transform = placed.root.localTransform
            linkedMapOf(
                "name" to placed.name,
                "id" to placed.id,
                "model" to placed.resourcePath,
                "folder" to placed.folder,
                "pos" to transform.position.toList(),
                "rot" to transform.rotation.toEulerDegrees().toList(),
                "scale" to transform.scale.toList(),
                "collisions" to placed.collisions.map { collision ->
                    val item = linkedMapOf<String, Any>(
                        "name" to collision.name,
                        "shape" to collision.shape.jsonName,
                        "pos" to collision.position.toList(),
                    )
                    if (collision.shape == EditorCollisionShape.BOX) {
                        item["rot"] = collision.rotation.toList()
                        item["size"] = collision.size.toList()
                    } else {
                        item["radius"] = collision.radius
                    }
                    item
                },
            )
        }

        Files.writeString(path, formatMapJson(objects))
        Logger.info("Editor map saved: {}", path)
    }

    @Suppress("UNCHECKED_CAST")
    fun loadMap(recordUndo: Boolean = true) {
        if (context.mapPath.get().isBlank()) {
            Logger.warn("Level path is empty")
            return
        }
        val path = Paths.get(context.mapPath.get())
        if (!Files.isRegularFile(path)) {
            Logger.warn("Editor map not found: {}", path)
            return
        }

        syncProjectNameFromLevelPath(path)
        syncWorkspaceFromLevelPath(path)
        refreshAssets()
        if (recordUndo) pushUndoSnapshot()
        context.scene.entities.removeAll(context.placedObjects.map { it.root }.toSet())
        context.placedObjects.clear()
        context.eventAreas.clear()
        context.hierarchyFolders.clear()
        context.eventAreaFolders.clear()
        context.pathFolders.clear()
        context.selectedIndex = -1
        context.selectedSpawnPointIndex = -1
        context.selectedEventAreaIndex = -1
        context.selectedIndices.clear()
        context.hierarchyRangeAnchorIndex = -1
        context.selectedCollisionIndex = -1

        val root = objectMapper.readValue(path.toFile(), Map::class.java)
        val environment = root["environment"] as? Map<String, Any?>
        if (environment != null) {
            readEnvironmentSettings(environment)
        }
        val terrain = root["terrain"] as? Map<String, Any?>
        if (terrain != null) {
            readTerrainSettings(terrain)
        }
        val folders = root["folders"] as? List<Map<String, Any?>> ?: emptyList()
        for (folder in folders) {
            val name = folder["name"] as? String ?: continue
            context.hierarchyFolders.add(EditorHierarchyFolder(uniqueFolderName(name)))
        }
        val event = root["event"] as? Map<String, Any?>
        val eventAreaFolders = (event?.get("folders") as? List<Map<String, Any?>>)
            ?: root["event_area_folders"] as? List<Map<String, Any?>>
            ?: emptyList()
        for (folder in eventAreaFolders) {
            val name = folder["name"] as? String ?: continue
            context.eventAreaFolders.add(EditorHierarchyFolder(uniqueEventAreaFolderName(name)))
        }
        val paths = root["paths"] as? Map<String, Any?>
        val pathFolders = (paths?.get("folders") as? List<Map<String, Any?>>)
            ?: root["path_folders"] as? List<Map<String, Any?>>
            ?: emptyList()
        for (folder in pathFolders) {
            val name = folder["name"] as? String ?: continue
            context.pathFolders.add(EditorHierarchyFolder(uniquePathFolderName(name)))
        }
        val objects = (root["static_objects"] as? List<Map<String, Any?>>)
            ?: (root["objects"] as? List<Map<String, Any?>>)
            ?: emptyList()
        for (item in objects) {
            val model = normalizeModelPath(item["model"] as? String ?: continue)
            val transform = Transform(
                position = vectorFromJson(item["pos"], Vector3f()),
                rotation = Quaternion.fromEulerDegrees(vectorFromJson(item["rot"], Vector3f())),
                scale = vectorFromJson(item["scale"], Vector3f(1f, 1f, 1f)),
            )
            addObject(
                name = uniqueStaticObjectName(item["name"] as? String ?: defaultStaticObjectName(model)),
                id = item["id"] as? String ?: nextStaticObjectId(model),
                resourcePath = model,
                folder = item["folder"] as? String ?: "",
                transform = transform,
                collisions = collisionsFromJson(item["collisions"]),
            )
        }
        val eventAreas = (event?.get("area_triggers") as? List<Map<String, Any?>>)
            ?: (event?.get("areas") as? List<Map<String, Any?>>)
            ?: (root["event_areas"] as? List<Map<String, Any?>>)
            ?: (root["area_triggers"] as? List<Map<String, Any?>>)
            ?: emptyList()
        for (item in eventAreas) {
            val area = EditorEventArea(
                name = uniqueEventAreaName(item["name"] as? String ?: "EventArea"),
                id = uniqueEventAreaId(item["id"] as? String ?: item["event"] as? String ?: item["eventName"] as? String ?: item["event_name"] as? String ?: AREA_TRIGGER_ID),
                kind = EditorEventAreaKind.AREA_TRIGGER,
                folder = item["folder"] as? String ?: "",
                position = vectorFromJson(item["pos"] ?: item["center"], Vector3f(0f, 0.5f, 0f)),
                rotation = vectorFromJson(item["rot"], Vector3f()),
                size = vectorFromJson(item["size"], Vector3f(2f, 1f, 2f)),
            )
            context.eventAreas.add(area)
            context.eventAreas.addAll(spawnPointAreasFromJson(item["spawnpoints"] ?: item["spawn_points"]))
        }
        val legacySpawnPoints = (event?.get("spawn_points") as? List<Map<String, Any?>>)
            ?: (root["spawnpoints"] as? List<Map<String, Any?>>)
            ?: (root["spawn_points"] as? List<Map<String, Any?>>)
            ?: emptyList()
        context.eventAreas.addAll(spawnPointAreasFromJson(legacySpawnPoints))

        context.selectedIndex = context.placedObjects.indices.firstOrNull() ?: -1
        selectOnly(context.selectedIndex)
        Logger.info("Editor map loaded: {}", path)
    }

    fun renameObject(index: Int, requestedName: String) {
        val placed = context.placedObjects.getOrNull(index) ?: return
        placed.name = uniqueStaticObjectName(requestedName, ignoreIndex = index)
    }

    fun addHierarchyFolder(): Int {
        val name = uniqueFolderName("Folder")
        context.hierarchyFolders.add(EditorHierarchyFolder(name))
        return context.hierarchyFolders.lastIndex
    }

    fun addEventAreaFolder(): Int {
        val name = uniqueEventAreaFolderName("Folder")
        context.eventAreaFolders.add(EditorHierarchyFolder(name))
        return context.eventAreaFolders.lastIndex
    }

    fun addPathFolder(): Int {
        val name = uniquePathFolderName("Folder")
        context.pathFolders.add(EditorHierarchyFolder(name))
        return context.pathFolders.lastIndex
    }

    fun renameHierarchyFolder(index: Int, requestedName: String) {
        val folder = context.hierarchyFolders.getOrNull(index) ?: return
        val oldName = folder.name
        val newName = uniqueFolderName(requestedName, ignoreIndex = index)
        folder.name = newName
        context.placedObjects.filter { it.folder == oldName }.forEach { it.folder = newName }
    }

    fun deleteHierarchyFolder(index: Int) {
        val folder = context.hierarchyFolders.getOrNull(index) ?: return
        context.placedObjects.filter { it.folder == folder.name }.forEach { it.folder = "" }
        context.hierarchyFolders.removeAt(index)
    }

    fun renameEventAreaFolder(index: Int, requestedName: String) {
        val folder = context.eventAreaFolders.getOrNull(index) ?: return
        val oldName = folder.name
        val newName = uniqueEventAreaFolderName(requestedName, ignoreIndex = index)
        folder.name = newName
        context.eventAreas.filter { it.folder == oldName }.forEach { it.folder = newName }
    }

    fun deleteEventAreaFolder(index: Int) {
        val folder = context.eventAreaFolders.getOrNull(index) ?: return
        context.eventAreas.filter { it.folder == folder.name }.forEach { it.folder = "" }
        context.eventAreaFolders.removeAt(index)
    }

    fun renamePathFolder(index: Int, requestedName: String) {
        val folder = context.pathFolders.getOrNull(index) ?: return
        folder.name = uniquePathFolderName(requestedName, ignoreIndex = index)
    }

    fun deletePathFolder(index: Int) {
        if (index !in context.pathFolders.indices) return
        context.pathFolders.removeAt(index)
    }

    fun moveObjectToFolder(objectIndex: Int, folder: String) {
        val placed = context.placedObjects.getOrNull(objectIndex) ?: return
        placed.folder = folder
    }

    fun moveObjectsToFolder(indices: Collection<Int>, folder: String) {
        val targets = indices.filter { it in context.placedObjects.indices }.distinct()
        if (targets.isEmpty()) return
        pushUndoSnapshot()
        targets.forEach { context.placedObjects[it].folder = folder }
    }

    fun moveEventAreasToFolder(indices: Collection<Int>, folder: String) {
        val targets = indices.filter { it in context.eventAreas.indices }.distinct()
        if (targets.isEmpty()) return
        pushUndoSnapshot()
        targets.forEach { context.eventAreas[it].folder = folder }
    }

    fun loadModelsForPreview(modelPath: String): Map<String, qorrnsmj.smf.graphic.`object`.Model> {
        return context.modelCache.getOrPut(modelPath) { loadModels(modelPath) }
    }

    fun terrainGridAt(worldPosition: Vector3f): Pair<Int, Int>? {
        val mapSize = context.terrainMapSize.coerceAtLeast(1f)
        val normalizedX = (worldPosition.x + mapSize * 0.5f) / mapSize
        val normalizedY = (worldPosition.z + mapSize * 0.5f) / mapSize
        val x = floor(normalizedX * context.terrain.width).toInt()
        val y = floor(normalizedY * context.terrain.height).toInt()
        if (x !in 0 until context.terrain.width || y !in 0 until context.terrain.height) return null
        return x to y
    }

    fun setTerrainMapSize(size: Float) {
        context.terrainMapSize = size.coerceIn(16f, 8192f)
        val resolution = recommendedHeightmapResolution(context.terrainMapSize)
        context.terrain.resize(resolution)
        context.splatmaps.resize(resolution)
        context.terrainPreview?.setMapSize(context.terrainMapSize)
        context.terrainPreview?.update()
    }

    fun applyTerrainBrush(gridX: Int, gridY: Int, delta: Float, newStroke: Boolean) {
        if (gridX !in 0 until context.terrain.width || gridY !in 0 until context.terrain.height) return
        val radius = max(1f, context.terrainBrushRadius)
        val radiusInt = radius.toInt().coerceAtLeast(1)
        val heightStrength = (context.terrainBrushStrength * delta * TERRAIN_BRUSH_CM_PER_SECOND).coerceAtLeast(0f)
        val blendStrength = (context.terrainBrushStrength * delta).coerceIn(0f, 1f)
        val falloff = max(0.01f, context.terrainBrushFalloff)
        if (newStroke) {
            pushUndoSnapshot()
            context.terrainFlattenHeight = context.terrain.get(gridX, gridY)
        }
        if (context.terrainBrushMode == EditorTerrainBrushMode.PAINT) {
            applySplatBrush(gridX, gridY, radius, radiusInt, blendStrength, falloff)
            return
        }

        val terrain = context.terrain
        val original = if (context.terrainBrushMode == EditorTerrainBrushMode.SMOOTH) terrain.heights.copyOf() else null
        val minX = max(0, gridX - radiusInt)
        val maxX = min(terrain.width - 1, gridX + radiusInt)
        val minY = max(0, gridY - radiusInt)
        val maxY = min(terrain.height - 1, gridY + radiusInt)

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dx = x - gridX
                val dy = y - gridY
                val distance = kotlin.math.sqrt((dx * dx + dy * dy).toFloat())
                if (distance > radius) continue

                val weight = (1f - distance / radius).coerceIn(0f, 1f).pow(falloff)
                val amount = heightStrength * weight
                val blendAmount = (blendStrength * weight * 3.5f).coerceIn(0f, 1f)
                val current = terrain.get(x, y)
                val next = when (context.terrainBrushMode) {
                    EditorTerrainBrushMode.RAISE -> current + amount
                    EditorTerrainBrushMode.LOWER -> current - amount
                    EditorTerrainBrushMode.SMOOTH -> current + (smoothHeight(original!!, x, y, radius) - current) * blendAmount
                    EditorTerrainBrushMode.FLATTEN -> current + (context.terrainFlattenHeight - current) * blendAmount
                    EditorTerrainBrushMode.NOISE -> current + terrainNoise(x, y) * amount
                    EditorTerrainBrushMode.PAINT -> current
                }
                terrain.set(x, y, next)
            }
        }
        context.terrainPreview?.update()
    }

    fun exportTerrainHeightmap() {
        if (context.workspaceRoot.get().isBlank() || levelFileStem().isBlank()) {
            Logger.warn("Workspace or project name is empty")
            return
        }
        val terrain = context.terrain
        val path = terrainHeightmapOutputDir().resolve("${levelFileStem()}_height.png")
        Files.createDirectories(path.parent)
        context.terrainHeightmapPath.set(path.toString())

        val image = BufferedImage(terrain.width, terrain.height, BufferedImage.TYPE_USHORT_GRAY)
        val raster = image.raster
        for (y in 0 until terrain.height) {
            for (x in 0 until terrain.width) {
                val sample = (terrain.get(x, y).roundToInt() + SIGNED_HEIGHT_ZERO_SAMPLE).coerceIn(0, 65535)
                raster.setSample(x, y, 0, sample)
            }
        }
        ImageIO.write(image, "png", path.toFile())
        Logger.info("Terrain heightmap exported: {}", path)
    }

    fun importTerrainHeightmap(path: String) {
        val file = workspaceResourcePath(path)
        if (!Files.isRegularFile(file)) {
            Logger.warn("Terrain heightmap not found: {}", file)
            return
        }
        context.terrainHeightmapPath.set(file.toString())
        pushUndoSnapshot()
        loadTerrainHeightmap(file.toString())
    }

    fun importTerrainSplatmap(path: String) {
        val file = workspaceResourcePath(path)
        if (!Files.isRegularFile(file)) {
            Logger.warn("Terrain splatmap not found: {}", file)
            return
        }
        context.terrainSplatmapPath.set(file.toString())
        pushUndoSnapshot()
        loadTerrainSplatmaps(listOf(file.toString()))
    }

    fun exportTerrainSplatmaps() {
        if (context.workspaceRoot.get().isBlank() || levelFileStem().isBlank()) {
            Logger.warn("Workspace or project name is empty")
            return
        }
        val outputDir = terrainSplatmapOutputDir()
        Files.createDirectories(outputDir)

        for ((mapIndex, data) in context.splatmaps.mapsForExport()) {
            val path = outputDir.resolve("${levelFileStem()}_splat$mapIndex.png")
            if (mapIndex == 0) context.terrainSplatmapPath.set(path.toString())
            val image = BufferedImage(context.splatmaps.width, context.splatmaps.height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until context.splatmaps.height) {
                for (x in 0 until context.splatmaps.width) {
                    val pixel = (y * context.splatmaps.width + x) * 4
                    val r = (data[pixel] * 255f).toInt().coerceIn(0, 255)
                    val g = (data[pixel + 1] * 255f).toInt().coerceIn(0, 255)
                    val b = (data[pixel + 2] * 255f).toInt().coerceIn(0, 255)
                    val a = (data[pixel + 3] * 255f).toInt().coerceIn(0, 255)
                    image.setRGB(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
                }
            }
            ImageIO.write(image, "png", path.toFile())
            Logger.info("Terrain splatmap exported: {}", path)
        }
    }

    fun refreshTerrainMapPaths() {
        if (context.workspaceRoot.get().isBlank() || levelFileStem().isBlank()) return
        context.terrainHeightmapPath.set(
            terrainHeightmapOutputDir().resolve("${levelFileStem()}_height.png").toString(),
        )
        context.terrainSplatmapPath.set(
            terrainSplatmapOutputDir().resolve("${levelFileStem()}_splat0.png").toString(),
        )
    }

    private fun captureSnapshot(): EditorSnapshot {
        return EditorSnapshot(
            objects = context.placedObjects.map { placed ->
                EditorObjectState(
                    name = placed.name,
                    id = placed.id,
                    resourcePath = placed.resourcePath,
                    folder = placed.folder,
                    transform = placed.root.localTransform.deepCopy(),
                    collisions = placed.collisions.map { it.deepCopy() },
                )
            },
            eventAreas = context.eventAreas.map { area ->
                EditorEventAreaState(
                    name = area.name,
                    id = area.id,
                    kind = area.kind,
                    folder = area.folder,
                    position = Vector3f(area.position.x, area.position.y, area.position.z),
                    rotation = Vector3f(area.rotation.x, area.rotation.y, area.rotation.z),
                    size = Vector3f(area.size.x, area.size.y, area.size.z),
                    spawnPoints = area.spawnPoints.map { spawn ->
                        EditorSpawnPointState(
                            name = spawn.name,
                            id = spawn.id,
                            type = spawn.type,
                            position = Vector3f(spawn.position.x, spawn.position.y, spawn.position.z),
                            rotation = Vector3f(spawn.rotation.x, spawn.rotation.y, spawn.rotation.z),
                        )
                    },
                )
            },
            folders = context.hierarchyFolders.map { folder ->
                EditorHierarchyFolderState(folder.name, folder.expanded)
            },
            eventAreaFolders = context.eventAreaFolders.map { folder ->
                EditorHierarchyFolderState(folder.name, folder.expanded)
            },
            pathFolders = context.pathFolders.map { folder ->
                EditorHierarchyFolderState(folder.name, folder.expanded)
            },
            terrain = EditorTerrainState(
                width = context.terrain.width,
                height = context.terrain.height,
                heights = context.terrain.heights.copyOf(),
                mapSize = context.terrainMapSize,
            ),
            splatmaps = EditorSplatmapState(
                width = context.splatmaps.width,
                height = context.splatmaps.height,
                maxTextureIndex = context.splatmaps.maxTextureIndex,
                maps = context.splatmaps.snapshotMaps(),
            ),
        )
    }

    private fun restoreSnapshot(snapshot: EditorSnapshot) {
        val selectedBefore = context.selectedIndices.filter { it in snapshot.objects.indices }.toSet()
        val selectedIndexBefore = context.selectedIndex
        context.scene.entities.removeAll(context.placedObjects.map { it.root }.toSet())
        context.placedObjects.clear()
        context.eventAreas.clear()
        context.hierarchyFolders.clear()
        context.eventAreaFolders.clear()
        context.pathFolders.clear()

        snapshot.folders.forEach { folder ->
            context.hierarchyFolders.add(EditorHierarchyFolder(folder.name, folder.expanded))
        }
        snapshot.eventAreaFolders.forEach { folder ->
            context.eventAreaFolders.add(EditorHierarchyFolder(folder.name, folder.expanded))
        }
        snapshot.pathFolders.forEach { folder ->
            context.pathFolders.add(EditorHierarchyFolder(folder.name, folder.expanded))
        }

        snapshot.objects.forEach { state ->
            addObject(
                name = state.name,
                id = state.id,
                resourcePath = state.resourcePath,
                folder = state.folder,
                transform = state.transform.deepCopy(),
                collisions = state.collisions.map { it.deepCopy() }.toMutableList(),
            )
        }
        snapshot.eventAreas.forEach { state ->
            context.eventAreas.add(
                EditorEventArea(
                    name = state.name,
                    id = state.id,
                    kind = state.kind,
                    folder = state.folder,
                    position = Vector3f(state.position.x, state.position.y, state.position.z),
                    rotation = Vector3f(state.rotation.x, state.rotation.y, state.rotation.z),
                    size = Vector3f(state.size.x, state.size.y, state.size.z),
                    spawnPoints = state.spawnPoints.map { spawn ->
                        EditorSpawnPoint(
                            name = spawn.name,
                            id = spawn.id,
                            type = spawn.type,
                            position = Vector3f(spawn.position.x, spawn.position.y, spawn.position.z),
                            rotation = Vector3f(spawn.rotation.x, spawn.rotation.y, spawn.rotation.z),
                        )
                    }.toMutableList(),
                )
            )
        }

        context.selectedIndex = when {
            context.placedObjects.isEmpty() -> -1
            selectedIndexBefore in context.placedObjects.indices -> selectedIndexBefore
            selectedBefore.isNotEmpty() -> selectedBefore.first()
            else -> context.placedObjects.lastIndex
        }
        context.selectedIndices.clear()
        context.selectedIndices.addAll(selectedBefore.filter { it in context.placedObjects.indices })
        if (context.selectedIndices.isEmpty() && context.selectedIndex in context.placedObjects.indices) {
            context.selectedIndices.add(context.selectedIndex)
        }
        context.hierarchyRangeAnchorIndex = context.selectedIndex
        context.terrainMapSize = snapshot.terrain.mapSize
        context.terrain.replace(snapshot.terrain.width, snapshot.terrain.height, snapshot.terrain.heights)
        context.splatmaps.replace(
            snapshot.splatmaps.width,
            snapshot.splatmaps.height,
            snapshot.splatmaps.maxTextureIndex,
            snapshot.splatmaps.maps,
        )
        context.terrainPreview?.setMapSize(context.terrainMapSize)
        context.terrainPreview?.update()
        context.propertyEditInProgress = false
        context.gizmoEditInProgress = false
    }

    private fun smoothHeight(original: FloatArray, x: Int, y: Int, brushRadius: Float): Float {
        var total = 0f
        var count = 0
        val sampleRadius = max(2, (brushRadius * 0.18f).toInt())
        val sampleStep = max(1, sampleRadius / 4)
        for (sampleY in max(0, y - sampleRadius)..min(context.terrain.height - 1, y + sampleRadius) step sampleStep) {
            for (sampleX in max(0, x - sampleRadius)..min(context.terrain.width - 1, x + sampleRadius) step sampleStep) {
                total += original[sampleY * context.terrain.width + sampleX]
                count++
            }
        }
        return if (count == 0) context.terrain.get(x, y) else total / count
    }

    private fun applySplatBrush(gridX: Int, gridY: Int, radius: Float, radiusInt: Int, strength: Float, falloff: Float) {
        val minX = max(0, gridX - radiusInt)
        val maxX = min(context.splatmaps.width - 1, gridX + radiusInt)
        val minY = max(0, gridY - radiusInt)
        val maxY = min(context.splatmaps.height - 1, gridY + radiusInt)
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dx = x - gridX
                val dy = y - gridY
                val distance = kotlin.math.sqrt((dx * dx + dy * dy).toFloat())
                if (distance > radius) continue
                val weight = (1f - distance / radius).coerceIn(0f, 1f).pow(falloff)
                context.splatmaps.paint(x, y, context.terrainPaintTextureIndex, strength * weight)
            }
        }
    }

    private fun readTerrainSettings(terrain: Map<String, Any?>) {
        val mapSize = when (val value = terrain["map_size"]) {
            is Number -> value.toFloat()
            is List<*> -> (value.firstOrNull() as? Number)?.toFloat()
            else -> null
        }
        if (mapSize != null) setTerrainMapSize(mapSize)

        val heightmap = terrain["heightmap"] as? String
        if (!heightmap.isNullOrBlank()) {
            context.terrainHeightmapPath.set(displayResourcePath(heightmap))
            loadTerrainHeightmap(heightmap)
        }

        val splatmaps = terrain["splatmaps"] as? List<*>
        if (splatmaps != null) {
            val paths = splatmaps.mapNotNull { it as? String }
            paths.firstOrNull()?.let { context.terrainSplatmapPath.set(displayResourcePath(it)) }
            loadTerrainSplatmaps(paths)
        }
    }

    private fun readEnvironmentSettings(environment: Map<String, Any?>) {
        val skybox = environment["skybox"] as? String
        if (!skybox.isNullOrBlank()) {
            setSkyboxPath(skybox)
        }

        val timeOfDay = environment["time_of_day"] as? String
        context.timeOfDay = EditorTimeOfDay.fromJson(timeOfDay)
        context.secondaryTimeOfDay = context.timeOfDay
        context.nightMode = context.timeOfDay == EditorTimeOfDay.NIGHT
        applyTimeOfDay()
    }

    private fun applySkyboxPath() {
        val path = normalizeSkyboxPath(context.skyboxPath.get())
        if (path.isBlank()) return

        val resourcePrefix = if (path.startsWith("assets/")) path else "assets/$path"
        try {
            context.scene.skybox = SkyboxLoader.loadSkyboxResource(resourcePrefix)
            Logger.info("Editor skybox loaded: {}", resourcePrefix)
        } catch (error: Throwable) {
            Logger.warn(error, "Editor skybox could not be loaded: {}", resourcePrefix)
        }
    }

    private fun applyTimeOfDay() {
        context.scene.skyColor = context.timeOfDay.skyColor
    }

    private fun loadTerrainHeightmap(path: String): Boolean {
        val file = workspaceResourcePath(path)
        if (!Files.isRegularFile(file)) {
            Logger.warn("Terrain heightmap not found: {}", file)
            return false
        }

        val image = ImageIO.read(file.toFile()) ?: run {
            Logger.warn("Terrain heightmap could not be read: {}", file)
            return false
        }
        val resolution = min(image.width, image.height).coerceAtLeast(3)
        if (image.width != image.height) {
            Logger.warn("Terrain heightmap is not square: {} ({} x {})", file, image.width, image.height)
        }

        context.terrain.resize(resolution)
        context.splatmaps.resize(resolution)

        val raster = image.raster
        val sampleBits = raster.sampleModel.getSampleSize(0).coerceAtLeast(1)
        val unsignedMax = if (sampleBits >= 16) 65535f else ((1 shl sampleBits) - 1).toFloat().coerceAtLeast(1f)
        for (y in 0 until resolution) {
            for (x in 0 until resolution) {
                val sourceX = ((x.toFloat() / (resolution - 1)) * (image.width - 1)).roundToInt().coerceIn(0, image.width - 1)
                val sourceY = ((y.toFloat() / (resolution - 1)) * (image.height - 1)).roundToInt().coerceIn(0, image.height - 1)
                val sample = raster.getSample(sourceX, sourceY, 0).toFloat()
                val heightCm = (sample / unsignedMax) * 65535f - SIGNED_HEIGHT_ZERO_SAMPLE
                context.terrain.set(x, y, heightCm)
            }
        }

        context.terrainPreview?.update()
        Logger.info("Terrain heightmap loaded: {}", file)
        return true
    }

    private fun loadTerrainSplatmaps(paths: List<String>) {
        context.splatmaps.clear()
        context.splatmaps.resize(context.terrain.width)

        for ((mapIndex, path) in paths.withIndex()) {
            val file = workspaceResourcePath(path)
            if (!Files.isRegularFile(file)) {
                Logger.warn("Terrain splatmap not found: {}", file)
                continue
            }

            val image = ImageIO.read(file.toFile())
            if (image == null) {
                Logger.warn("Terrain splatmap could not be read: {}", file)
                continue
            }
            val data = FloatArray(context.splatmaps.width * context.splatmaps.height * 4)
            for (y in 0 until context.splatmaps.height) {
                for (x in 0 until context.splatmaps.width) {
                    val sourceX = ((x.toFloat() / (context.splatmaps.width - 1)) * (image.width - 1)).roundToInt().coerceIn(0, image.width - 1)
                    val sourceY = ((y.toFloat() / (context.splatmaps.height - 1)) * (image.height - 1)).roundToInt().coerceIn(0, image.height - 1)
                    val argb = image.getRGB(sourceX, sourceY)
                    val pixel = (y * context.splatmaps.width + x) * 4
                    data[pixel] = ((argb ushr 16) and 0xFF) / 255f
                    data[pixel + 1] = ((argb ushr 8) and 0xFF) / 255f
                    data[pixel + 2] = (argb and 0xFF) / 255f
                    data[pixel + 3] = ((argb ushr 24) and 0xFF) / 255f
                }
            }
            context.splatmaps.setMap(mapIndex, data)
            Logger.info("Terrain splatmap loaded: {}", file)
        }
    }

    private fun recommendedHeightmapResolution(mapSizeMeters: Float): Int {
        val targetCells = kotlin.math.ceil(mapSizeMeters * HEIGHTMAP_CELLS_PER_METER).toInt().coerceAtLeast(512)
        var cells = 1
        while (cells < targetCells && cells < HEIGHTMAP_MAX_CELLS) {
            cells = cells shl 1
        }
        return cells.coerceAtMost(HEIGHTMAP_MAX_CELLS) + 1
    }

    private fun terrainNoise(x: Int, y: Int): Float {
        val value = sin((x * 12.9898f + y * 78.233f) * 43758.5453f)
        return (value - floor(value)) * 2f - 1f
    }

    private fun selectedObjectIndices(): Set<Int> {
        val selected = context.selectedIndices.filter { it in context.placedObjects.indices }.toSet()
        return selected.ifEmpty { if (context.selectedIndex in context.placedObjects.indices) setOf(context.selectedIndex) else emptySet() }
    }

    private fun selectedSpawnPoint(): EditorSpawnPoint? {
        return context.eventAreas.getOrNull(context.selectedEventAreaIndex)
            ?.spawnPoints
            ?.getOrNull(context.selectedSpawnPointIndex)
    }

    private fun selectOnly(index: Int) {
        context.selectedSpawnPointIndex = -1
        context.selectedEventAreaIndex = -1
        context.selectedIndices.clear()
        if (index in context.placedObjects.indices) {
            context.selectedIndices.add(index)
            context.hierarchyRangeAnchorIndex = index
        } else {
            context.hierarchyRangeAnchorIndex = -1
        }
    }

    private fun scanAssetDirectories(current: Path): List<String> {
        if (!Files.isDirectory(current)) return emptyList()

        return Files.list(current).use { stream ->
            stream
                .filter { Files.isDirectory(it) }
                .map { it.fileName.toString() }
                .sorted()
                .toList()
        }
    }

    private fun scanGlbs(root: Path, current: Path): List<String> {
        if (!Files.isDirectory(current)) return emptyList()

        return Files.list(current).use { stream ->
            stream
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".glb", ignoreCase = true) }
                .map { "model/${root.relativize(it).toString().replace('\\', '/')}" }
                .sorted()
                .toList()
        }
    }

    private fun loadModels(modelPath: String): Map<String, qorrnsmj.smf.graphic.`object`.Model> {
        val directPath = Paths.get(modelPath)
        if (Files.isRegularFile(directPath)) return EntityLoader.loadModelFromFile(directPath)

        val workspacePath = workspaceAssetPath(modelPath)
        if (workspacePath != null && Files.isRegularFile(workspacePath)) return EntityLoader.loadModelFromFile(workspacePath)

        val resourcePath = when {
            modelPath.startsWith("assets/") -> modelPath
            modelPath.startsWith("model/") -> "assets/$modelPath"
            else -> "assets/model/$modelPath"
        }
        return EntityLoader.loadModelFromResource(resourcePath)
    }

    private fun normalizeModelPath(path: String): String {
        val portablePath = path.replace('\\', '/')
        if (portablePath.startsWith("assets/model/")) {
            return portablePath.removePrefix("assets/")
        }
        if (portablePath.startsWith("model/")) {
            return portablePath
        }

        val rawPath = Paths.get(path)
        val workspaceRoot = workspaceRootPathOrNull()
        val modelRoot = assetRootPathOrNull()
        val absolutePath = rawPath.toAbsolutePath().normalize()
        return when {
            workspaceRoot != null && absolutePath.startsWith(workspaceRoot) -> {
                workspaceRoot.relativize(absolutePath).toString().replace('\\', '/')
            }
            modelRoot != null && absolutePath.startsWith(modelRoot) -> {
                "model/${modelRoot.relativize(absolutePath).toString().replace('\\', '/')}"
            }
            portablePath.endsWith(".glb", ignoreCase = true) -> {
                "model/$portablePath"
            }
            else -> portablePath
        }
    }

    private fun normalizeSkyboxPath(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isBlank()) return ""

        val rawPath = Paths.get(trimmed)
        val workspaceRoot = workspaceRootPathOrNull()
        val portablePath = if (rawPath.isAbsolute && workspaceRoot != null) {
            val absolutePath = rawPath.toAbsolutePath().normalize()
            if (absolutePath.startsWith(workspaceRoot)) {
                workspaceRoot.relativize(absolutePath).toString().replace('\\', '/')
            } else {
                trimmed.replace('\\', '/')
            }
        } else {
            trimmed.replace('\\', '/')
        }

        var normalized = portablePath.removePrefix("assets/")
        if (normalized.endsWith(".png", ignoreCase = true)) {
            normalized = normalized.substringBeforeLast(".")
            for (suffix in SKYBOX_FACE_SUFFIXES) {
                if (normalized.endsWith(suffix, ignoreCase = true)) {
                    normalized = normalized.dropLast(suffix.length)
                    break
                }
            }
        }
        if (!normalized.contains("/")) {
            normalized = "texture/skybox/$normalized"
        }
        return normalized
    }

    private fun displaySkyboxPath(normalizedPath: String): String {
        if (normalizedPath.isBlank()) return ""

        val portablePath = normalizedPath.removePrefix("assets/").replace('\\', '/')
        val rawPath = Paths.get(portablePath)
        if (rawPath.isAbsolute) return rawPath.normalize().toString()

        val workspaceRoot = workspaceRootPathOrNull()
        return if (workspaceRoot != null) {
            workspaceRoot.resolve(portablePath).normalize().toString()
        } else {
            rawPath.toAbsolutePath().normalize().toString()
        }
    }

    private fun workspaceAssetPath(assetPath: String): Path? {
        val workspaceRoot = workspaceRootPathOrNull() ?: return null
        val portablePath = assetPath.replace('\\', '/')
        val relativePath = when {
            portablePath.startsWith("assets/") -> portablePath.removePrefix("assets/")
            portablePath.startsWith("model/") -> portablePath
            else -> "model/$portablePath"
        }
        return workspaceRoot.resolve(relativePath).normalize()
    }

    private fun workspaceResourcePath(resourcePath: String): Path {
        val rawPath = Paths.get(resourcePath)
        if (rawPath.isAbsolute) return rawPath.normalize()

        val portablePath = resourcePath.replace('\\', '/')
        val relativePath = if (portablePath.startsWith("assets/")) {
            portablePath.removePrefix("assets/")
        } else {
            portablePath
        }
        val workspaceRoot = workspaceRootPathOrNull()
        return if (workspaceRoot != null) {
            workspaceRoot.resolve(relativePath).normalize()
        } else {
            Paths.get(relativePath).toAbsolutePath().normalize()
        }
    }

    private fun displayResourcePath(resourcePath: String): String = workspaceResourcePath(resourcePath).toString()

    private fun normalizeWorkspaceResourcePath(path: String, fallback: String): String {
        val trimmed = path.trim()
        if (trimmed.isBlank()) return fallback

        val portablePath = trimmed.replace('\\', '/')
        if (!Paths.get(trimmed).isAbsolute) {
            return portablePath.removePrefix("assets/")
        }

        val workspaceRoot = workspaceRootPathOrNull()
        val absolutePath = Paths.get(trimmed).toAbsolutePath().normalize()
        return if (workspaceRoot != null && absolutePath.startsWith(workspaceRoot)) {
            workspaceRoot.relativize(absolutePath).toString().replace('\\', '/')
        } else {
            portablePath.removePrefix("assets/")
        }
    }

    private fun workspaceRootPath(): Path = Paths.get(context.workspaceRoot.get()).toAbsolutePath().normalize()

    private fun workspaceRootPathOrNull(): Path? {
        if (context.workspaceRoot.get().isBlank()) return null
        return workspaceRootPath()
    }

    private fun syncProjectNameFromLevelPath(levelPath: Path) {
        context.projectName.set(sanitizeProjectName(levelPath.fileName.toString().substringBeforeLast('.', levelPath.fileName.toString())))
    }

    private fun syncWorkspaceFromLevelPath(levelPath: Path) {
        val levelDir = levelPath.parent ?: return
        val workspaceRoot = if (levelDir.fileName?.toString().equals("level", ignoreCase = true)) {
            levelDir.parent ?: return
        } else {
            levelDir
        }
        context.workspaceRoot.set(workspaceRoot.toString())
        context.assetModelRoot.set(workspaceRoot.resolve("model").toString())
        if (context.assetBrowserPath.get().isBlank() || !Paths.get(context.assetBrowserPath.get()).toAbsolutePath().normalize().startsWith(workspaceRoot)) {
            context.assetBrowserPath.set(context.assetModelRoot.get())
        }
    }

    private fun terrainHeightmapOutputDir(): Path = workspaceRootPath().resolve("texture").resolve("terrain").resolve("map")

    private fun terrainSplatmapOutputDir(): Path = workspaceRootPath().resolve("texture").resolve("terrain").resolve("map")

    private fun levelFileStem(): String {
        return sanitizeProjectName(context.projectName.get())
    }

    private fun sanitizeProjectName(name: String): String {
        return name.trim().replace(Regex("[^A-Za-z0-9_-]"), "_")
    }

    private fun assetRootPath(): Path = Paths.get(context.assetModelRoot.get()).toAbsolutePath().normalize()

    private fun assetRootPathOrNull(): Path? {
        if (context.assetModelRoot.get().isBlank()) return null
        return assetRootPath()
    }

    private fun assetBrowserPath(): Path = Paths.get(context.assetBrowserPath.get()).toAbsolutePath().normalize()

    private fun formatMapJson(objects: List<Map<String, Any>>): String {
        val builder = StringBuilder()
        builder.append("{\n")
        builder.append("    \"editor_version\": \"").append(EDITOR_VERSION).append("\",\n")
        builder.append("    \"environment\": ").append(formatEnvironment()).append(",\n")
        builder.append("    \"terrain\": ").append(formatTerrain()).append(",\n")
        builder.append("    \"folders\": ").append(formatFolders()).append(",\n")
        builder.append("    \"event\": ").append(formatEvent()).append(",\n")
        builder.append("    \"paths\": ").append(formatPaths()).append(",\n")
        builder.append("    \"static_objects\": [")
        if (objects.isNotEmpty()) builder.append("\n")

        objects.forEachIndexed { index, item ->
            builder.append("        {\n")
            builder.append("            \"name\": \"").append(escapeJson(item["name"].toString())).append("\",\n")
            builder.append("            \"id\": \"").append(escapeJson(item["id"].toString())).append("\",\n")
            builder.append("            \"model\": \"").append(escapeJson(item["model"].toString())).append("\",\n")
            builder.append("            \"folder\": \"").append(escapeJson(item["folder"].toString())).append("\",\n")
            builder.append("            \"pos\": ").append(formatFloatArray(item["pos"] as List<*>)).append(",\n")
            builder.append("            \"rot\": ").append(formatFloatArray(item["rot"] as List<*>)).append(",\n")
            builder.append("            \"scale\": ").append(formatFloatArray(item["scale"] as List<*>)).append(",\n")
            builder.append("            \"collisions\": ").append(formatCollisions(item["collisions"] as List<*>)).append("\n")
            builder.append("        }")
            if (index != objects.lastIndex) builder.append(",")
            builder.append("\n")
        }

        builder.append("    ]\n")
        builder.append("}\n")
        return builder.toString()
    }

    private fun formatEnvironment(): String {
        val timeOfDay = context.timeOfDay.jsonName
        return "{\n" +
            "        \"skybox\": \"${escapeJson(normalizeSkyboxPath(context.skyboxPath.get()))}\",\n" +
            "        \"time_of_day\": \"$timeOfDay\"\n" +
            "    }"
    }

    private fun formatTerrain(): String {
        val levelName = levelFileStem()
        val defaultHeightmap = "texture/terrain/map/${levelName}_height.png"
        val defaultSplatmap = "texture/terrain/map/${levelName}_splat0.png"
        val heightmap = normalizeWorkspaceResourcePath(context.terrainHeightmapPath.get(), defaultHeightmap)
        val splatmaps = (0..(context.splatmaps.maxTextureIndex / 4)).joinToString(
            prefix = "[",
            postfix = "]",
        ) { index ->
            val fallback = "texture/terrain/map/${levelName}_splat$index.png"
            val path = if (index == 0) {
                normalizeWorkspaceResourcePath(context.terrainSplatmapPath.get(), defaultSplatmap)
            } else {
                fallback
            }
            "\"${escapeJson(path)}\""
        }
        return "{\n" +
            "        \"map_size\": ${formatFloat(context.terrainMapSize)},\n" +
            "        \"heightmap_resolution\": ${context.terrain.width},\n" +
            "        \"height_format\": \"png_16bit_grayscale_signed_cm\",\n" +
            "        \"min_height_cm\": ${EditorTerrainData.SIGNED_MIN_CM.toInt()},\n" +
            "        \"max_height_cm\": ${EditorTerrainData.SIGNED_MAX_CM.toInt()},\n" +
            "        \"heightmap\": \"${escapeJson(heightmap)}\",\n" +
            "        \"splatmaps\": $splatmaps\n" +
            "    }"
    }

    private fun formatFolders(): String {
        if (context.hierarchyFolders.isEmpty()) return "[]"
        return context.hierarchyFolders.joinToString(prefix = "[", postfix = "]") { folder ->
            "{\"name\":\"${escapeJson(folder.name)}\"}"
        }
    }

    private fun formatEvent(): String {
        return "{\n" +
            "        \"folders\": ${formatEventAreaFolders()},\n" +
            "        \"spawn_points\": ${formatEventAreas(EditorEventAreaKind.SPAWN_POINT)},\n" +
            "        \"area_triggers\": ${formatEventAreas(EditorEventAreaKind.AREA_TRIGGER)}\n" +
            "    }"
    }

    private fun formatPaths(): String {
        return "{\n" +
            "        \"folders\": ${formatPathFolders()}\n" +
            "    }"
    }

    private fun formatEventAreaFolders(): String {
        if (context.eventAreaFolders.isEmpty()) return "[]"
        return context.eventAreaFolders.joinToString(prefix = "[", postfix = "]") { folder ->
            "{\"name\":\"${escapeJson(folder.name)}\"}"
        }
    }

    private fun formatPathFolders(): String {
        if (context.pathFolders.isEmpty()) return "[]"
        return context.pathFolders.joinToString(prefix = "[", postfix = "]") { folder ->
            "{\"name\":\"${escapeJson(folder.name)}\"}"
        }
    }

    private fun formatEventAreas(kind: EditorEventAreaKind): String {
        val areas = context.eventAreas.filter { it.kind == kind }
        if (areas.isEmpty()) return "[]"
        return areas.joinToString(prefix = "[\n", postfix = "        ]", separator = ",\n") { area ->
            "        {\n" +
                "            \"name\": \"${escapeJson(area.name)}\",\n" +
                "            \"id\": \"${escapeJson(area.id)}\",\n" +
                "            \"folder\": \"${escapeJson(area.folder)}\",\n" +
                "            \"pos\": ${formatFloatArray(area.position.toList())},\n" +
                "            \"rot\": ${formatFloatArray(area.rotation.toList())},\n" +
                "            \"size\": ${formatFloatArray(area.size.toList())}\n" +
                "        }"
        }
    }

    private fun formatCollisions(collisions: List<*>): String {
        if (collisions.isEmpty()) return "[]"

        val builder = StringBuilder()
        builder.append("[\n")
        collisions.forEachIndexed { index, collision ->
            val item = collision as Map<*, *>
            builder.append("                {\n")
            builder.append("                    \"name\": \"").append(escapeJson(item["name"].toString())).append("\",\n")
            builder.append("                    \"shape\": \"").append(escapeJson(item["shape"].toString())).append("\",\n")
            builder.append("                    \"pos\": ").append(formatFloatArray(item["pos"] as List<*>))
            if (item["shape"] == EditorCollisionShape.BOX.jsonName) {
                builder.append(",\n")
                builder.append("                    \"rot\": ").append(formatFloatArray(item["rot"] as List<*>)).append(",\n")
                builder.append("                    \"size\": ").append(formatFloatArray(item["size"] as List<*>)).append("\n")
            } else {
                builder.append(",\n")
                builder.append("                    \"radius\": ").append(formatFloat((item["radius"] as? Number)?.toFloat() ?: 1f)).append("\n")
            }
            builder.append("                }")
            if (index != collisions.lastIndex) builder.append(",")
            builder.append("\n")
        }
        builder.append("            ]")
        return builder.toString()
    }

    private fun formatFloatArray(values: List<*>): String {
        return values.joinToString(prefix = "[", postfix = "]", separator = ", ") { value ->
            val number = value as? Number ?: 0f
            formatFloat(number.toFloat())
        }
    }

    private fun formatFloat(value: Float): String {
        return String.format(Locale.US, "%.6f", value).trimEnd('0').let {
            if (it.endsWith(".")) it.dropLast(1) else it
        }
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    }

    private fun vectorFromJson(value: Any?, default: Vector3f): Vector3f {
        val list = value as? List<*> ?: return default
        return Vector3f(
            (list.getOrNull(0) as? Number)?.toFloat() ?: default.x,
            (list.getOrNull(1) as? Number)?.toFloat() ?: default.y,
            (list.getOrNull(2) as? Number)?.toFloat() ?: default.z,
        )
    }

    private fun Vector3f.toList(): List<Float> = listOf(x, y, z)

    private fun collisionsFromJson(value: Any?): MutableList<EditorCollisionState> {
        val list = value as? List<*> ?: return mutableListOf()
        return list.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            EditorCollisionState(
                name = map["name"] as? String ?: "Collision ${list.indexOf(item) + 1}",
                shape = EditorCollisionShape.fromJson(map["shape"] as? String),
                position = vectorFromJson(map["pos"], Vector3f()),
                rotation = vectorFromJson(map["rot"], Vector3f()),
                size = vectorFromJson(map["size"], Vector3f(1f, 1f, 1f)),
                radius = (map["radius"] as? Number)?.toFloat() ?: 1f,
            )
        }.toMutableList()
    }

    private fun spawnPointsFromJson(value: Any?, eventArea: EditorEventArea): MutableList<EditorSpawnPoint> {
        val list = value as? List<*> ?: return mutableListOf()
        val result = mutableListOf<EditorSpawnPoint>()
        for (item in list) {
            val map = item as? Map<*, *> ?: continue
            result.add(
                EditorSpawnPoint(
                    name = uniqueName(
                        map["name"] as? String ?: "Spawnpoint",
                        "Spawnpoint",
                        eventArea.spawnPoints.map { it.name }.toSet() + result.map { it.name }.toSet(),
                    ),
                    id = map["id"] as? String ?: SPAWNPOINT_ID,
                    type = map["type"] as? String ?: "player",
                    position = vectorFromJson(map["pos"], Vector3f()),
                    rotation = vectorFromJson(map["rot"], Vector3f()),
                )
            )
        }
        return result
    }

    private fun spawnPointAreasFromJson(value: Any?): List<EditorEventArea> {
        val list = value as? List<*> ?: return emptyList()
        val result = mutableListOf<EditorEventArea>()
        for (item in list) {
            val map = item as? Map<*, *> ?: continue
            result.add(
                EditorEventArea(
                    name = uniqueEventAreaName(map["name"] as? String ?: "Spawnpoint"),
                    id = uniqueEventAreaId(map["id"] as? String ?: map["event"] as? String ?: map["eventName"] as? String ?: map["event_name"] as? String ?: SPAWNPOINT_ID),
                    kind = EditorEventAreaKind.SPAWN_POINT,
                    folder = map["folder"] as? String ?: "",
                    position = vectorFromJson(map["pos"], Vector3f()),
                    rotation = vectorFromJson(map["rot"], Vector3f()),
                    size = Vector3f(PLAYER_CAPSULE_RADIUS * 2f, PLAYER_CAPSULE_HEIGHT, PLAYER_CAPSULE_RADIUS * 2f),
                )
            )
        }
        return result
    }

    private fun nextStaticObjectId(resourcePath: String): String {
        return defaultStaticObjectName(resourcePath)
    }

    private fun ensureUniqueStaticObjectNames() {
        context.placedObjects.forEachIndexed { index, placed ->
            placed.name = uniqueStaticObjectName(placed.name, ignoreIndex = index)
        }
    }

    private fun uniqueStaticObjectName(requestedName: String, ignoreIndex: Int = -1): String {
        val base = requestedName.trim().ifBlank { "StaticObject" }
        val used = context.placedObjects
            .mapIndexedNotNull { index, placed -> if (index == ignoreIndex) null else placed.name }
            .toSet()
        if (base !in used) return base

        var suffix = 2
        while (true) {
            val candidate = "${base}_$suffix"
            if (candidate !in used) return candidate
            suffix++
        }
    }

    private fun uniqueFolderName(requestedName: String, ignoreIndex: Int = -1): String {
        val base = requestedName.trim().ifBlank { "Folder" }
        val used = context.hierarchyFolders
            .mapIndexedNotNull { index, folder -> if (index == ignoreIndex) null else folder.name }
            .toSet()
        if (base !in used) return base

        var suffix = 2
        while (true) {
            val candidate = "${base}_$suffix"
            if (candidate !in used) return candidate
            suffix++
        }
    }

    private fun uniqueEventAreaFolderName(requestedName: String, ignoreIndex: Int = -1): String {
        val base = requestedName.trim().ifBlank { "Folder" }
        val used = context.eventAreaFolders
            .mapIndexedNotNull { index, folder -> if (index == ignoreIndex) null else folder.name }
            .toSet()
        if (base !in used) return base

        var suffix = 2
        while (true) {
            val candidate = "${base}_$suffix"
            if (candidate !in used) return candidate
            suffix++
        }
    }

    private fun uniquePathFolderName(requestedName: String, ignoreIndex: Int = -1): String {
        val base = requestedName.trim().ifBlank { "Folder" }
        val used = context.pathFolders
            .mapIndexedNotNull { index, folder -> if (index == ignoreIndex) null else folder.name }
            .toSet()
        if (base !in used) return base

        var suffix = 2
        while (true) {
            val candidate = "${base}_$suffix"
            if (candidate !in used) return candidate
            suffix++
        }
    }

    private fun uniqueSpawnPointName(eventArea: EditorEventArea, requestedName: String, ignoreIndex: Int = -1): String {
        return uniqueName(
            requestedName,
            "Spawnpoint",
            eventArea.spawnPoints.mapIndexedNotNull { index, spawn -> if (index == ignoreIndex) null else spawn.name }.toSet(),
        )
    }

    private fun uniqueEventAreaName(requestedName: String, ignoreIndex: Int = -1): String {
        return uniqueName(
            requestedName,
            "EventArea",
            context.eventAreas.mapIndexedNotNull { index, area -> if (index == ignoreIndex) null else area.name }.toSet(),
        )
    }

    private fun uniqueEventAreaId(requestedName: String, ignoreIndex: Int = -1): String {
        return uniqueName(
            requestedName,
            "event_area",
            context.eventAreas.mapIndexedNotNull { index, area -> if (index == ignoreIndex) null else area.id }.toSet(),
        )
    }

    private fun uniqueName(requestedName: String, fallback: String, used: Set<String>): String {
        val base = requestedName.trim().ifBlank { fallback }
        if (base !in used) return base

        var suffix = 2
        while (true) {
            val candidate = "${base}_$suffix"
            if (candidate !in used) return candidate
            suffix++
        }
    }

    private fun placePointOnGround(position: Vector3f): Vector3f {
        return Vector3f(position.x, groundHeight(position.x, position.z), position.z)
    }

    private fun placeBoxOnGround(position: Vector3f, size: Vector3f): Vector3f {
        return Vector3f(position.x, groundHeight(position.x, position.z) + max(0f, size.y) * 0.5f, position.z)
    }

    private fun groundHeight(worldX: Float, worldZ: Float): Float {
        return context.scene.terrainHeightProvider?.getHeight(worldX, worldZ)
            ?: context.terrainPreview?.terrain?.getHeight(worldX, worldZ)
            ?: 0f
    }

    private fun defaultStaticObjectName(resourcePath: String): String {
        return resourcePath
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .substringBeforeLast('.')
            .ifBlank { "StaticObject" }
    }

    private companion object {
        const val EDITOR_VERSION = "1.0"
        const val AREA_TRIGGER_ID = "area_trigger"
        const val SPAWNPOINT_ID = "spawn_point"
        const val SIGNED_HEIGHT_ZERO_SAMPLE = 32768
        const val TERRAIN_BRUSH_CM_PER_SECOND = 1200f
        const val HEIGHTMAP_CELLS_PER_METER = 2f
        const val HEIGHTMAP_MAX_CELLS = 2048
        const val PLAYER_CAPSULE_RADIUS = 0.22f
        const val PLAYER_CAPSULE_HEIGHT = 1.7f
        val DAY_SKY_COLOR = Vector3f(0.46f, 0.58f, 0.68f)
        val NIGHT_SKY_COLOR = Vector3f(0.025f, 0.03f, 0.055f)
        val SKYBOX_FACE_SUFFIXES = listOf("_front", "_back", "_top", "_bottom", "_right", "_left")
    }
}
