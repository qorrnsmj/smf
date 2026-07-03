package qorrnsmj.smf.editor

import imgui.extension.imguizmo.flag.Operation
import imgui.type.ImString
import org.lwjgl.glfw.GLFW.GLFW_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL
import org.lwjgl.glfw.GLFW.glfwSetInputMode
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.custom.ObjectEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.ViewportShadingMode
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.math.Quaternion
import qorrnsmj.smf.math.Vector3f
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.max

internal class EditorContext {
    val modelCache = mutableMapOf<String, Map<String, Model>>()
    val placedObjects = mutableListOf<EditorPlacedObject>()
    val eventAreas = mutableListOf<EditorEventArea>()
    val availableGlbs = mutableListOf<String>()
    val terrain = EditorTerrainData()
    val splatmaps = EditorSplatmapData()
    var terrainPreview: EditorTerrainPreview? = null
    val undoStack = mutableListOf<EditorSnapshot>()
    val redoStack = mutableListOf<EditorSnapshot>()
    val hierarchyFolders = mutableListOf<EditorHierarchyFolder>()
    val eventAreaFolders = mutableListOf<EditorHierarchyFolder>()
    val pathFolders = mutableListOf<EditorHierarchyFolder>()
    val scene = Scene()
    val secondaryCamera = Camera()
    val cameraController = EditorCameraController(scene.camera)
    val secondaryCameraController = EditorCameraController(secondaryCamera)
    val workspaceRoot = ImString("", 1024)
    val projectName = ImString("", 128)
    val mapPath = ImString("", 1024)
    val skyboxPath = ImString("", 1024)
    val terrainHeightmapPath = ImString("", 1024)
    val terrainSplatmapPath = ImString("", 1024)
    val assetModelRoot = ImString("", 1024)
    val assetBrowserPath = ImString("", 1024)
    val availableAssetDirectories = mutableListOf<String>()
    var errorPopupOpen = false
    var errorPopupTitle = "Error"
    var errorPopupMessage = ""

    var selectedIndex = -1
    var selectedSpawnPointIndex = -1
    var selectedEventAreaIndex = -1
    val selectedIndices = mutableSetOf<Int>()
    var hierarchyRangeAnchorIndex = -1
    var selectedCollisionIndex = -1
    var renamingCollisionIndex = -1
    var renamingCollisionFocusPending = false
    val renamingCollisionName = ImString("", 128)
    var renamingFolderIndex = -1
    var renamingFolderCategory = EditorFolderCategory.OBJECT
    var renamingFolderFocusPending = false
    val renamingFolderName = ImString("", 128)
    var selectedAssetIndex = 0
    var editMode = EditorEditMode.OBJECT
    var gizmoOperation = Operation.TRANSLATE
    var terrainBrushEnabled = false
    var terrainMeshViewEnabled = false
    var terrainGrayViewEnabled = true
    var viewportShadingMode = ViewportShadingMode.SOLID
    var secondaryViewportShadingMode = ViewportShadingMode.RENDERED
    var cullingEnabled = true
    var skyVisible = true
    var nightMode = false
    var timeOfDay = EditorTimeOfDay.NOON
    var secondaryTimeOfDay = EditorTimeOfDay.NOON
    var terrainBrushMode = EditorTerrainBrushMode.RAISE
    var terrainMapSize = 256f
    var terrainBrushRadius = 24f
    var terrainBrushStrength = 0.35f
    var terrainBrushFalloff = 1.5f
    var terrainPaintTextureIndex = 0
    var terrainBrushWasDown = false
    var terrainFlattenHeight = 0f
    var leftMouseWasDown = false
    var lastWantCaptureMouse = false
    var lastWantCaptureKeyboard = false
    var lastGizmoWantsMouse = false
    var undoShortcutWasDown = false
    var redoShortcutWasDown = false
    var deleteShortcutWasDown = false
    var propertyEditInProgress = false
    var gizmoEditInProgress = false
    var viewportX = 0f
    var viewportY = 0f
    var viewportWidth = 1f
    var viewportHeight = 1f
    private val viewportBounds = Array(2) { FloatArray(4) }
    var activeViewportIndex = 0
    var leftPanelWidth = 430f
    var rightPanelWidth = 430f
    var viewportSplitRatio = 0.5f
    var panelResizeInProgress = false
    var windowFontScale = 1f
    var editorFontScale = 1.12f
    var viewportMouseLookActive = false
    var showLeftPanel = true
    var showViewportPanel = true
    var showSecondaryViewport = true
    var showRightPanel = true
    var showMapsTab = true
    var showAssetsTab = true
    var showLevelTab = true
    var leftPanelExpanded = true
    var viewportExpanded = true
    var secondaryViewportExpanded = true
    var rightPanelExpanded = true
    var mapsTabExpanded = true
    var assetsTabExpanded = true
    var levelTabExpanded = true
    var terrainTabExpanded = true
    var bottomPanelTab = EditorBottomPanelTab.ASSETS
    var objectTabExpanded = true
    var collisionsTabExpanded = true
    var mapsTabHeight = 92f
    var assetsTabHeight = 240f
    var levelTabHeight = 240f
    var terrainTabHeight = 340f
    var objectTabHeight = 240f
    var collisionsTabHeight = 260f
    private var cursorDisabled = false

    fun effectiveFontScale(): Float {
        return windowFontScale * editorFontScale
    }

    fun selectedObject(): EditorPlacedObject? = placedObjects.getOrNull(selectedIndex)

    fun selectedCollision(): EditorCollisionState? = selectedObject()?.collisions?.getOrNull(selectedCollisionIndex)

    fun activeCamera(): Camera = if (activeViewportIndex == 1) secondaryCamera else scene.camera

    fun activeCameraController(): EditorCameraController = if (activeViewportIndex == 1) secondaryCameraController else cameraController

    fun viewportCamera(index: Int): Camera = if (index == 1) secondaryCamera else scene.camera

    fun viewportShadingMode(index: Int): ViewportShadingMode = if (index == 1) secondaryViewportShadingMode else viewportShadingMode

    fun viewportTimeOfDay(index: Int): EditorTimeOfDay = if (index == 1) secondaryTimeOfDay else timeOfDay

    fun setViewportShadingMode(index: Int, mode: ViewportShadingMode) {
        if (index == 1) {
            secondaryViewportShadingMode = mode
        } else {
            viewportShadingMode = mode
        }
    }

    fun setViewportTimeOfDay(index: Int, timeOfDay: EditorTimeOfDay) {
        if (index == 1) {
            secondaryTimeOfDay = timeOfDay
        } else {
            this.timeOfDay = timeOfDay
            nightMode = timeOfDay == EditorTimeOfDay.NIGHT
        }
    }

    fun setActiveViewport(index: Int) {
        activeViewportIndex = index.coerceIn(0, 1)
        val bounds = viewportBounds[activeViewportIndex]
        viewportX = bounds[0]
        viewportY = bounds[1]
        viewportWidth = bounds[2].coerceAtLeast(1f)
        viewportHeight = bounds[3].coerceAtLeast(1f)
    }

    fun setViewportBounds(index: Int, x: Float, y: Float, width: Float, height: Float) {
        if (index !in 0..1) return
        viewportBounds[index][0] = x
        viewportBounds[index][1] = y
        viewportBounds[index][2] = width
        viewportBounds[index][3] = height
        if (activeViewportIndex == index) setActiveViewport(index)
    }

    fun viewportIndexAt(x: Float, y: Float): Int {
        for (index in viewportBounds.indices) {
            val bounds = viewportBounds[index]
            if (bounds[2] <= 0f || bounds[3] <= 0f) continue
            if (x >= bounds[0] &&
                y >= bounds[1] &&
                x <= bounds[0] + bounds[2] &&
                y <= bounds[1] + bounds[3]
            ) return index
        }
        return -1
    }

    fun isMouseInViewport(x: Float, y: Float): Boolean {
        return viewportIndexAt(x, y) != -1
    }

    fun keepCursorVisible() {
        glfwSetInputMode(SMF.window.id, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
        cursorDisabled = false
    }

    fun updateCursorMode() {
        if (cursorDisabled == viewportMouseLookActive) return

        val mode = if (viewportMouseLookActive) GLFW_CURSOR_DISABLED else GLFW_CURSOR_NORMAL
        glfwSetInputMode(SMF.window.id, GLFW_CURSOR, mode)
        cursorDisabled = viewportMouseLookActive
    }
}

internal data class EditorObjectState(
    val name: String,
    val id: String,
    val resourcePath: String,
    val folder: String,
    val transform: Transform,
    val collisions: List<EditorCollisionState>,
)

internal data class EditorSpawnPointState(
    val name: String,
    val id: String,
    val type: String,
    val position: Vector3f,
    val rotation: Vector3f,
)

internal data class EditorEventAreaState(
    val name: String,
    val id: String,
    val kind: EditorEventAreaKind,
    val folder: String,
    val position: Vector3f,
    val rotation: Vector3f,
    val size: Vector3f,
    val spawnPoints: List<EditorSpawnPointState>,
)

internal data class EditorHierarchyFolderState(
    val name: String,
    val expanded: Boolean,
)

internal data class EditorTerrainState(
    val width: Int,
    val height: Int,
    val heights: FloatArray,
    val mapSize: Float,
)

internal data class EditorSplatmapState(
    val width: Int,
    val height: Int,
    val maxTextureIndex: Int,
    val maps: Map<Int, FloatArray>,
)

internal data class EditorSnapshot(
    val objects: List<EditorObjectState>,
    val eventAreas: List<EditorEventAreaState>,
    val folders: List<EditorHierarchyFolderState>,
    val eventAreaFolders: List<EditorHierarchyFolderState>,
    val pathFolders: List<EditorHierarchyFolderState>,
    val terrain: EditorTerrainState,
    val splatmaps: EditorSplatmapState,
)

internal enum class EditorFolderCategory {
    OBJECT,
    EVENT_AREA,
    PATH,
}

internal enum class EditorEventAreaKind {
    AREA_TRIGGER,
    SPAWN_POINT,
}

internal data class EditorHierarchyFolder(
    var name: String,
    var expanded: Boolean = true,
)

internal enum class EditorBottomPanelTab {
    ASSETS,
    LEVEL,
}

internal class EditorTerrainData(
    initialResolution: Int = 513,
) {
    var width: Int = initialResolution
        private set
    var height: Int = initialResolution
        private set
    var heights = FloatArray(width * height)
        private set

    fun get(x: Int, y: Int): Float {
        if (x !in 0 until width || y !in 0 until height) return 0f
        return heights[y * width + x]
    }

    fun set(x: Int, y: Int, value: Float) {
        if (x !in 0 until width || y !in 0 until height) return
        heights[y * width + x] = value.coerceIn(SIGNED_MIN_CM, SIGNED_MAX_CM)
    }

    fun resize(newResolution: Int) {
        val safeResolution = newResolution.coerceAtLeast(3)
        if (safeResolution == width && safeResolution == height) return

        val oldWidth = width
        val oldHeight = height
        val oldHeights = heights
        val newHeights = FloatArray(safeResolution * safeResolution)
        for (y in 0 until safeResolution) {
            for (x in 0 until safeResolution) {
                val oldX = ((x.toFloat() / (safeResolution - 1)) * (oldWidth - 1)).toInt().coerceIn(0, oldWidth - 1)
                val oldY = ((y.toFloat() / (safeResolution - 1)) * (oldHeight - 1)).toInt().coerceIn(0, oldHeight - 1)
                newHeights[y * safeResolution + x] = oldHeights[oldY * oldWidth + oldX]
            }
        }

        width = safeResolution
        height = safeResolution
        heights = newHeights
    }

    fun replace(newWidth: Int, newHeight: Int, newHeights: FloatArray) {
        if (newWidth < 1 || newHeight < 1 || newHeights.size != newWidth * newHeight) return
        width = newWidth
        height = newHeight
        heights = newHeights.copyOf()
    }

    companion object {
        const val SIGNED_MIN_CM = -32768f
        const val SIGNED_MAX_CM = 32767f
    }
}

internal class EditorSplatmapData(
    initialResolution: Int = 513,
) {
    var width: Int = initialResolution
        private set
    var height: Int = initialResolution
        private set
    private val maps = mutableMapOf<Int, FloatArray>()
    var maxTextureIndex = 0
        private set

    fun channel(textureIndex: Int): FloatArray {
        val safeIndex = textureIndex.coerceAtLeast(0)
        maxTextureIndex = max(maxTextureIndex, safeIndex)
        return maps.getOrPut(safeIndex / 4) { FloatArray(width * height * 4) }
    }

    fun mapsForExport(): List<Pair<Int, FloatArray>> {
        if (maps.isEmpty()) channel(0)
        return maps.toSortedMap().map { it.key to it.value }
    }

    fun clear() {
        maps.clear()
        maxTextureIndex = 0
    }

    fun setMap(mapIndex: Int, data: FloatArray) {
        if (data.size != width * height * 4) return
        val safeMapIndex = mapIndex.coerceAtLeast(0)
        maps[safeMapIndex] = data
        maxTextureIndex = max(maxTextureIndex, safeMapIndex * 4 + 3)
    }

    fun snapshotMaps(): Map<Int, FloatArray> {
        return maps.mapValues { it.value.copyOf() }
    }

    fun replace(newWidth: Int, newHeight: Int, newMaxTextureIndex: Int, newMaps: Map<Int, FloatArray>) {
        if (newWidth < 1 || newHeight < 1) return
        val expectedSize = newWidth * newHeight * 4
        if (newMaps.values.any { it.size != expectedSize }) return

        width = newWidth
        height = newHeight
        maps.clear()
        maps.putAll(newMaps.mapValues { it.value.copyOf() })
        maxTextureIndex = newMaxTextureIndex.coerceAtLeast(0)
    }

    fun paint(x: Int, y: Int, textureIndex: Int, amount: Float) {
        if (x !in 0 until width || y !in 0 until height) return
        val safeIndex = textureIndex.coerceAtLeast(0)
        val mapIndex = safeIndex / 4
        val channelIndex = safeIndex % 4
        val target = channel(safeIndex)
        val pixel = (y * width + x) * 4
        target[pixel + channelIndex] = (target[pixel + channelIndex] + amount).coerceIn(0f, 1f)
        normalizePixel(x, y)
    }

    fun resize(newResolution: Int) {
        val safeResolution = newResolution.coerceAtLeast(3)
        if (safeResolution == width && safeResolution == height) return

        val oldWidth = width
        val oldHeight = height
        val resized = mutableMapOf<Int, FloatArray>()
        for ((mapIndex, oldMap) in maps) {
            val newMap = FloatArray(safeResolution * safeResolution * 4)
            for (y in 0 until safeResolution) {
                for (x in 0 until safeResolution) {
                    val oldX = ((x.toFloat() / (safeResolution - 1)) * (oldWidth - 1)).toInt().coerceIn(0, oldWidth - 1)
                    val oldY = ((y.toFloat() / (safeResolution - 1)) * (oldHeight - 1)).toInt().coerceIn(0, oldHeight - 1)
                    val oldPixel = (oldY * oldWidth + oldX) * 4
                    val newPixel = (y * safeResolution + x) * 4
                    newMap[newPixel] = oldMap[oldPixel]
                    newMap[newPixel + 1] = oldMap[oldPixel + 1]
                    newMap[newPixel + 2] = oldMap[oldPixel + 2]
                    newMap[newPixel + 3] = oldMap[oldPixel + 3]
                }
            }
            resized[mapIndex] = newMap
        }

        width = safeResolution
        height = safeResolution
        maps.clear()
        maps.putAll(resized)
    }

    private fun normalizePixel(x: Int, y: Int) {
        val pixel = (y * width + x) * 4
        var total = 0f
        val maxMap = maxTextureIndex / 4
        for (mapIndex in 0..maxMap) {
            val map = maps.getOrPut(mapIndex) { FloatArray(width * height * 4) }
            total += map[pixel] + map[pixel + 1] + map[pixel + 2] + map[pixel + 3]
        }
        if (total <= 0.00001f) return
        for (mapIndex in 0..maxMap) {
            val map = maps.getValue(mapIndex)
            map[pixel] /= total
            map[pixel + 1] /= total
            map[pixel + 2] /= total
            map[pixel + 3] /= total
        }
    }
}

internal enum class EditorTerrainBrushMode {
    RAISE,
    LOWER,
    SMOOTH,
    FLATTEN,
    NOISE,
    PAINT,
}

internal enum class EditorEditMode {
    OBJECT,
    TERRAIN,
}

internal enum class EditorTimeOfDay(val jsonName: String, val label: String, val skyColor: Vector3f) {
    MORNING("morning", "Morning", Vector3f(0.72f, 0.62f, 0.48f)),
    NOON("noon", "Noon", Vector3f(0.46f, 0.58f, 0.68f)),
    EVENING("evening", "Evening", Vector3f(0.56f, 0.32f, 0.24f)),
    NIGHT("night", "Night", Vector3f(0.025f, 0.03f, 0.055f));

    companion object {
        fun fromJson(value: String?): EditorTimeOfDay {
            return entries.firstOrNull { it.jsonName.equals(value, ignoreCase = true) } ?: NOON
        }
    }
}

internal data class EditorPlacedObject(
    var name: String,
    var id: String,
    val resourcePath: String,
    val root: ObjectEntity,
    var folder: String = "",
    val collisions: MutableList<EditorCollisionState> = mutableListOf(),
) {
    fun editorMin(): Vector3f {
        val center = root.localTransform.position
        val half = editorHalfExtent()
        return Vector3f(center.x - half.x, center.y - half.y, center.z - half.z)
    }

    fun editorMax(): Vector3f {
        val center = root.localTransform.position
        val half = editorHalfExtent()
        return Vector3f(center.x + half.x, center.y + half.y, center.z + half.z)
    }

    private fun editorHalfExtent(): Vector3f {
        val scale = root.localTransform.scale
        return Vector3f(
            max(12f, abs(scale.x) * 0.75f),
            max(12f, abs(scale.y) * 0.75f),
            max(12f, abs(scale.z) * 0.75f),
        )
    }
}

internal data class EditorSpawnPoint(
    var name: String = "Spawnpoint",
    var id: String = "spawn_point",
    var type: String = "player",
    var position: Vector3f = Vector3f(),
    var rotation: Vector3f = Vector3f(),
)

internal data class EditorEventArea(
    var name: String = "EventArea",
    var id: String = "event_area",
    var kind: EditorEventAreaKind = EditorEventAreaKind.AREA_TRIGGER,
    var folder: String = "",
    var position: Vector3f = Vector3f(0f, 0.5f, 0f),
    var rotation: Vector3f = Vector3f(),
    var size: Vector3f = Vector3f(2f, 1f, 2f),
    val spawnPoints: MutableList<EditorSpawnPoint> = mutableListOf(),
)

internal data class EditorCollisionState(
    var name: String = "Collision",
    var shape: EditorCollisionShape,
    var position: Vector3f = Vector3f(),
    var rotation: Vector3f = Vector3f(),
    var size: Vector3f = Vector3f(1f, 1f, 1f),
    var radius: Float = 1f,
)

internal enum class EditorCollisionShape(val jsonName: String) {
    BOX("box"),
    SPHERE("sphere");

    companion object {
        fun fromJson(value: String?): EditorCollisionShape {
            return entries.firstOrNull { it.jsonName.equals(value, ignoreCase = true) } ?: BOX
        }
    }
}

internal data class Ray(
    val origin: Vector3f,
    val direction: Vector3f,
)

internal fun Transform.deepCopy(): Transform {
    return Transform(
        position = Vector3f(position.x, position.y, position.z),
        rotation = Quaternion(rotation.x, rotation.y, rotation.z, rotation.w),
        scale = Vector3f(scale.x, scale.y, scale.z),
    )
}

internal fun Quaternion.toEulerDegrees(): Vector3f {
    val q = normalize()
    val sinrCosp = 2f * (q.w * q.x + q.y * q.z)
    val cosrCosp = 1f - 2f * (q.x * q.x + q.y * q.y)
    val roll = atan2(sinrCosp, cosrCosp)

    val sinp = 2f * (q.w * q.y - q.z * q.x)
    val pitch = if (abs(sinp) >= 1f) {
        if (sinp >= 0f) Math.PI.toFloat() / 2f else -Math.PI.toFloat() / 2f
    } else {
        asin(sinp)
    }

    val sinyCosp = 2f * (q.w * q.z + q.x * q.y)
    val cosyCosp = 1f - 2f * (q.y * q.y + q.z * q.z)
    val yaw = atan2(sinyCosp, cosyCosp)

    return Vector3f(
        Math.toDegrees(roll.toDouble()).toFloat(),
        Math.toDegrees(pitch.toDouble()).toFloat(),
        Math.toDegrees(yaw.toDouble()).toFloat(),
    )
}

internal fun Quaternion.add(deltaEulerDegrees: Vector3f): Quaternion {
    return multiply(Quaternion.fromEulerDegrees(deltaEulerDegrees)).normalize()
}

internal fun EditorCollisionState.deepCopy(): EditorCollisionState {
    return EditorCollisionState(
        name = name,
        shape = shape,
        position = Vector3f(position.x, position.y, position.z),
        rotation = Vector3f(rotation.x, rotation.y, rotation.z),
        size = Vector3f(size.x, size.y, size.z),
        radius = radius,
    )
}

internal fun EditorSpawnPoint.deepCopy(): EditorSpawnPoint {
    return EditorSpawnPoint(
        name = name,
        id = id,
        type = type,
        position = Vector3f(position.x, position.y, position.z),
        rotation = Vector3f(rotation.x, rotation.y, rotation.z),
    )
}

internal fun EditorEventArea.deepCopy(): EditorEventArea {
    return EditorEventArea(
        name = name,
        id = id,
        kind = kind,
        folder = folder,
        position = Vector3f(position.x, position.y, position.z),
        rotation = Vector3f(rotation.x, rotation.y, rotation.z),
        size = Vector3f(size.x, size.y, size.z),
        spawnPoints = spawnPoints.map { it.deepCopy() }.toMutableList(),
    )
}

internal fun EditorEventArea.isSpawnpoint(): Boolean {
    return kind == EditorEventAreaKind.SPAWN_POINT
}

internal fun Camera.horizontalFront(): Vector3f {
    return Vector3f(getFront().x, 0f, getFront().z).normalize()
}
