package qorrnsmj.smf.editor

import imgui.ImGui
import org.lwjgl.opengl.GL33C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL33C.glBindFramebuffer
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.graphic.ViewportShadingMode
import qorrnsmj.smf.graphic.render.EditorDebugBox
import qorrnsmj.smf.graphic.render.EditorDebugCapsule
import qorrnsmj.smf.graphic.render.EditorDebugSphere
import qorrnsmj.smf.graphic.`object`.FrameBufferObject
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal class EditorViewport(private val context: EditorContext) {
    private var fbo: FrameBufferObject? = null
    private var width = 1
    private var height = 1

    fun renderSceneToTexture(width: Int, height: Int, camera: Camera, shadingMode: ViewportShadingMode, timeOfDay: EditorTimeOfDay): Int {
        resize(width, height)

        val target = fbo ?: return 0
        target.bind()
        SMF.renderer.resizeForViewport(this.width, this.height)
        updateCollisionDebug()
        val previousCamera = context.scene.camera
        val previousMode = context.scene.viewportShadingMode
        val previousGray = context.scene.terrainGrayView
        val previousWire = context.scene.terrainWireframeView
        val previousSky = context.scene.skyVisible
        val previousSkyColor = context.scene.skyColor
        context.scene.camera = camera
        context.scene.viewportShadingMode = shadingMode
        context.scene.terrainGrayView = shadingMode == ViewportShadingMode.SOLID || shadingMode == ViewportShadingMode.WIRE
        context.scene.terrainWireframeView = shadingMode == ViewportShadingMode.WIRE
        context.scene.skyVisible = shadingMode == ViewportShadingMode.RENDERED
        context.scene.skyColor = timeOfDay.skyColor
        SMF.renderer.render(context.scene)
        context.scene.camera = previousCamera
        context.scene.viewportShadingMode = previousMode
        context.scene.terrainGrayView = previousGray
        context.scene.terrainWireframeView = previousWire
        context.scene.skyVisible = previousSky
        context.scene.skyColor = previousSkyColor
        glBindFramebuffer(GL_FRAMEBUFFER, 0)

        return target.colorTexture.id
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
        shadingMode: ViewportShadingMode,
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
        SMF.renderer.debugRenderer.setEditorCollisionDebug(emptyList(), emptyList())
    }

    private fun updateCollisionDebug() {
        val boxes = mutableListOf<EditorDebugBox>()
        val spheres = mutableListOf<EditorDebugSphere>()
        val capsules = mutableListOf<EditorDebugCapsule>()

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
                    EditorCollisionShape.BOX -> boxes.add(EditorDebugBox(center, collision.size.multiply(parentScale), rotation, boxColor))
                    EditorCollisionShape.SPHERE -> spheres.add(EditorDebugSphere(center, collision.radius * parentScale.average(), sphereColor))
                }
            }
        }

        context.eventAreas.forEachIndexed { index, area ->
            val selected = context.selectedEventAreaIndex == index
            val color = if (selected) Vector4f(1f, 0.35f, 0.9f, 1f) else Vector4f(1f, 0.35f, 0.9f, 0.7f)
            boxes.add(EditorDebugBox(area.position, area.size, area.rotation, color))

            area.spawnPoints.forEachIndexed { spawnIndex, spawn ->
                val spawnSelected = context.selectedEventAreaIndex == index && context.selectedSpawnPointIndex == spawnIndex
                val spawnColor = if (spawnSelected) Vector4f(0.25f, 1f, 0.35f, 1f) else Vector4f(0.25f, 1f, 0.35f, 0.7f)
                capsules.add(EditorDebugCapsule(spawn.position, PLAYER_CAPSULE_RADIUS, PLAYER_CAPSULE_HEIGHT, spawnColor))
            }
        }

        SMF.renderer.debugRenderer.setEditorCollisionDebug(boxes, spheres, capsules)
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
    }
}
