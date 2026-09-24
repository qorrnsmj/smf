package qorrnsmj.smf.editor

import imgui.ImGui
import org.lwjgl.opengl.GL33C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL33C.glBindFramebuffer
import org.lwjgl.opengl.GL33C.glViewport
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.graphic.scene.settings.ViewportShadingSettings
import qorrnsmj.smf.graphic.debug.DebugBox
import qorrnsmj.smf.graphic.debug.DebugCapsule
import qorrnsmj.smf.graphic.debug.DebugLine
import qorrnsmj.smf.graphic.debug.DebugPrimitive
import qorrnsmj.smf.graphic.debug.DebugSphere
import qorrnsmj.smf.graphic.resource.buffer.FrameBufferObject
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

internal class EditorViewport(private val context: EditorContext) {
    private var fbo: FrameBufferObject? = null
    private var width = 1
    private var height = 1

    fun renderSceneToTexture(width: Int, height: Int, camera: Camera, shadingMode: ViewportShadingSettings, timeOfDay: EditorTimeOfDay): Int {
        resize(width, height)

        val target = fbo ?: return 0
        resizeRendererForEditorViewport(this.width, this.height)
        val previousCamera = context.scene.world.camera
        val previousMode = context.scene.renderSettings.viewportShadingMode
        val previousGray = context.scene.renderSettings.terrainGrayView
        val previousWire = context.scene.renderSettings.terrainWireframeView
        val previousSky = context.scene.environment.skyVisible
        val previousSkyColor = context.scene.environment.skyColor

        try {
            target.bind()
            updateDebugPrimitives()
            context.scene.world.camera = camera
            context.scene.renderSettings.viewportShadingMode = shadingMode
            context.scene.renderSettings.terrainGrayView = shadingMode == ViewportShadingSettings.SOLID || shadingMode == ViewportShadingSettings.WIRE
            context.scene.renderSettings.terrainWireframeView = shadingMode == ViewportShadingSettings.WIRE
            context.scene.environment.skyVisible = shadingMode == ViewportShadingSettings.RENDERED
            context.scene.environment.skyColor = timeOfDay.skyColor
            SMF.renderer.render(context.scene)
        } finally {
            context.scene.world.camera = previousCamera
            context.scene.renderSettings.viewportShadingMode = previousMode
            context.scene.renderSettings.terrainGrayView = previousGray
            context.scene.renderSettings.terrainWireframeView = previousWire
            context.scene.environment.skyVisible = previousSky
            context.scene.environment.skyColor = previousSkyColor
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            resizeRendererForEditorViewport(SMF.window.width, SMF.window.height)
        }

        return target.colorTexture.id
    }

    private fun resizeRendererForEditorViewport(width: Int, height: Int) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        glViewport(0, 0, safeWidth, safeHeight)

        SMF.renderer.modelRenderer.resize(safeWidth, safeHeight)
        SMF.renderer.billboardRenderer.resize(safeWidth, safeHeight)
        SMF.renderer.terrainRenderer.resize(safeWidth, safeHeight)
        SMF.renderer.skyboxRenderer.resize(safeWidth, safeHeight)
        SMF.renderer.skydomeRenderer.resize(safeWidth, safeHeight)
        SMF.renderer.debugRenderer.resize(safeWidth, safeHeight)
        SMF.renderer.textRenderer.resize(safeWidth, safeHeight)
    }

    private fun resize(width: Int, height: Int): Boolean {
        val safeWidth = max(1, width)
        val safeHeight = max(1, height)
        if (fbo != null && this.width == safeWidth && this.height == safeHeight) return false

        fbo?.delete()
        this.width = safeWidth
        this.height = safeHeight
        fbo = FrameBufferObject(safeWidth, safeHeight)
        return true
    }

    fun renderImage(
        width: Float,
        height: Float,
        camera: Camera,
        shadingMode: ViewportShadingSettings,
        timeOfDay: EditorTimeOfDay,
    ) {
        val textureWidth = width.toInt()
        val textureHeight = height.toInt()
        val texture = renderSceneToTexture(textureWidth, textureHeight, camera, shadingMode, timeOfDay)
        if (texture != 0) {
            ImGui.image(texture.toLong(), width, height, 0f, 1f, 1f, 0f)
        }
    }

    fun dispose() {
        fbo?.delete()
        fbo = null
        SMF.renderer.debugRenderer.clearOverlayPrimitives()
    }

    private fun updateDebugPrimitives() {
        val primitives = mutableListOf<DebugPrimitive>()

        context.placedObjects.forEachIndexed { index, placed ->
            val transform = placed.root.localTransform
            val base = transform.position
            val parentScale = transform.scale.absComponents()
            val selected = index in context.selectedIndices
            val boxColor = if (selected) Vector4f(1f, 0.85f, 0.15f, 1f) else Vector4f(1f, 0.65f, 0.15f, 0.65f)
            val sphereColor = if (selected) Vector4f(0.2f, 1f, 0.95f, 1f) else Vector4f(0.2f, 0.9f, 1f, 0.65f)

            for (collision in placed.collisions) {
                val center = base.add(transform.rotation.rotate(collision.position.multiply(parentScale)))
                val rotation = transform.rotation.toEulerDegrees().add(collision.rotation)
                when (collision.shape) {
                    EditorCollisionShape.BOX -> primitives.add(DebugBox(center, collision.size.multiply(parentScale), rotation, boxColor))
                    EditorCollisionShape.SPHERE -> primitives.add(DebugSphere(center, collision.radius * parentScale.average(), sphereColor))
                }
            }
        }

        context.eventAreas.forEachIndexed { index, area ->
            val selected = context.selectedEventAreaIndex == index
            val color = if (selected) Vector4f(1f, 0.35f, 0.9f, 1f) else Vector4f(1f, 0.35f, 0.9f, 0.7f)
            primitives.add(DebugBox(area.position, area.size, area.rotation, color))

            area.spawnPoints.forEachIndexed { spawnIndex, spawn ->
                val spawnSelected = context.selectedEventAreaIndex == index && context.selectedSpawnPointIndex == spawnIndex
                val spawnColor = if (spawnSelected) Vector4f(0.25f, 1f, 0.35f, 1f) else Vector4f(0.25f, 1f, 0.35f, 0.7f)
                primitives.add(DebugCapsule(spawn.position, PLAYER_CAPSULE_RADIUS, PLAYER_CAPSULE_HEIGHT, spawnColor))
            }
        }

        primitives.addAll(terrainDebugLines())
        SMF.renderer.debugRenderer.setOverlayPrimitives(primitives)
    }

    private fun terrainDebugLines(): List<DebugLine> {
        val lines = mutableListOf<DebugLine>()
        if (context.terrainMeshViewEnabled) {
            lines.addAll(context.terrainPreview?.wireframeLines() ?: emptyList())
        }
        lines.addAll(terrainBrushCircleLines())
        return lines
    }

    private fun terrainBrushCircleLines(): List<DebugLine> {
        if (context.editMode != EditorEditMode.TERRAIN) return emptyList()
        if (context.viewportMouseLookActive) return emptyList()
        if (!isMouseOverActiveViewport()) return emptyList()

        val center = EditorPicking.intersectGround(EditorPicking.currentMouseRay(context)) ?: return emptyList()
        val radius = terrainBrushWorldRadius()
        if (radius <= 0f) return emptyList()

        val lines = ArrayList<DebugLine>(BRUSH_CIRCLE_SEGMENTS + 4)
        val color = brushColor()
        val centerPoint = terrainBrushPoint(center.x, center.z) ?: return emptyList()
        val angleStep = (Math.PI.toFloat() * 2f) / BRUSH_CIRCLE_SEGMENTS

        var previous = terrainBrushPoint(center.x + radius, center.z)
        for (index in 1..BRUSH_CIRCLE_SEGMENTS) {
            val angle = index * angleStep
            val next = terrainBrushPoint(
                center.x + radius * cos(angle),
                center.z + radius * sin(angle),
            )
            if (previous != null && next != null) {
                lines.add(DebugLine(previous, next, color))
            }
            previous = next
        }

        terrainBrushPoint(center.x - radius, center.z)?.let { lines.add(DebugLine(centerPoint, it, color)) }
        terrainBrushPoint(center.x + radius, center.z)?.let { lines.add(DebugLine(centerPoint, it, color)) }
        terrainBrushPoint(center.x, center.z - radius)?.let { lines.add(DebugLine(centerPoint, it, color)) }
        terrainBrushPoint(center.x, center.z + radius)?.let { lines.add(DebugLine(centerPoint, it, color)) }
        return lines
    }

    private fun isMouseOverActiveViewport(): Boolean {
        val mouse = ImGui.getMousePos()
        return context.viewportIndexAt(mouse.x, mouse.y) == context.activeViewportIndex
    }

    private fun terrainBrushWorldRadius(): Float {
        val resolution = max(context.terrain.width, context.terrain.height).coerceAtLeast(1)
        return context.terrainBrushRadius * context.terrainMapSize.coerceAtLeast(1f) / resolution
    }

    private fun terrainBrushPoint(worldX: Float, worldZ: Float): Vector3f? {
        val halfSize = context.terrainMapSize * 0.5f
        if (worldX !in -halfSize..halfSize || worldZ !in -halfSize..halfSize) return null

        val normalizedX = ((worldX + halfSize) / context.terrainMapSize).coerceIn(0f, 1f)
        val normalizedZ = ((worldZ + halfSize) / context.terrainMapSize).coerceIn(0f, 1f)
        val gridX = (normalizedX * (context.terrain.width - 1)).toInt().coerceIn(0, context.terrain.width - 1)
        val gridZ = (normalizedZ * (context.terrain.height - 1)).toInt().coerceIn(0, context.terrain.height - 1)
        val height = context.terrain.get(gridX, gridZ) * CM_TO_METERS + BRUSH_PREVIEW_HEIGHT_OFFSET
        return Vector3f(worldX, height, worldZ)
    }

    private fun brushColor(): Vector4f {
        return Vector4f(0.7f, 1f, 0.15f, 1f)
    }

    private fun Vector3f.absComponents(): Vector3f {
        return Vector3f(kotlin.math.abs(x), kotlin.math.abs(y), kotlin.math.abs(z))
    }

    private fun Vector3f.average(): Float {
        return (x + y + z) / 3f
    }

    private fun rotateEuler(value: Vector3f, rotationDegrees: Vector3f): Vector3f {
        val rx = Math.toRadians(rotationDegrees.x.toDouble()).toFloat()
        val ry = Math.toRadians(rotationDegrees.y.toDouble()).toFloat()
        val rz = Math.toRadians(rotationDegrees.z.toDouble()).toFloat()

        val cx = cos(rx)
        val sx = sin(rx)
        val cy = cos(ry)
        val sy = sin(ry)
        val cz = cos(rz)
        val sz = sin(rz)

        val y1 = value.y * cx - value.z * sx
        val z1 = value.y * sx + value.z * cx
        val x2 = value.x * cy + z1 * sy
        val z2 = -value.x * sy + z1 * cy
        val x3 = x2 * cz - y1 * sz
        val y3 = x2 * sz + y1 * cz
        return Vector3f(x3, y3, z2)
    }

    private companion object {
        const val PLAYER_CAPSULE_RADIUS = 0.22f
        const val PLAYER_CAPSULE_HEIGHT = 1.7f
        const val BRUSH_CIRCLE_SEGMENTS = 48
        const val BRUSH_PREVIEW_HEIGHT_OFFSET = 0.04f
        const val CM_TO_METERS = 0.01f
    }
}
