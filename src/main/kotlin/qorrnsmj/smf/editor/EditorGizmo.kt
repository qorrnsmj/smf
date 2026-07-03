package qorrnsmj.smf.editor

import imgui.ImGui
import imgui.extension.imguizmo.ImGuizmo
import imgui.extension.imguizmo.flag.Mode
import imgui.extension.imguizmo.flag.Operation
import qorrnsmj.smf.math.Quaternion
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.MVP
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal class EditorGizmo(
    private val context: EditorContext,
    private val document: EditorDocument,
) {
    fun render() {
        val selectedEventArea = context.eventAreas.getOrNull(context.selectedEventAreaIndex)
        if (selectedEventArea != null && context.selectedSpawnPointIndex == -1) {
            renderEventAreaGizmo(selectedEventArea)
            return
        }

        val selected = context.selectedObject() ?: return
        val collision = context.selectedCollision()
        val transform = selected.root.localTransform
        val parentScale = transform.scale.absComponents()
        val model = FloatArray(16)
        val position: Vector3f
        val rotation: Vector3f
        val scale: Vector3f

        if (collision != null) {
            position = transform.position.add(transform.rotation.rotate(collision.position.multiply(parentScale)))
            rotation = transform.rotation.toEulerDegrees().add(collision.rotation)
            scale = when (collision.shape) {
                EditorCollisionShape.BOX -> collision.size.multiply(parentScale)
                EditorCollisionShape.SPHERE -> {
                    val diameter = collision.radius * 2f * parentScale.average()
                    Vector3f(diameter, diameter, diameter)
                }
            }
        } else {
            position = transform.position
            rotation = transform.rotation.toEulerDegrees()
            scale = transform.scale
        }

        ImGuizmo.recomposeMatrixFromComponents(
            floatArrayOf(position.x, position.y, position.z),
            floatArrayOf(rotation.x, rotation.y, rotation.z),
            floatArrayOf(scale.x, scale.y, scale.z),
            model,
        )

        val view = context.activeCamera().getViewMatrix().toFloatArray()
        val projection = MVP.getPerspectiveMatrix(context.viewportWidth / context.viewportHeight).toFloatArray()
        val delta = identityMatrixArray()

        ImGuizmo.setOrthographic(false)
        ImGuizmo.setID(if (collision != null) 2 else 1)
        ImGuizmo.setDrawList(ImGui.getWindowDrawList())
        ImGuizmo.setRect(context.viewportX, context.viewportY, context.viewportWidth, context.viewportHeight)
        ImGuizmo.manipulate(view, projection, context.gizmoOperation, Mode.LOCAL, model, delta)
        context.lastGizmoWantsMouse = context.lastGizmoWantsMouse || ImGuizmo.isOver() || ImGuizmo.isUsing()

        if (ImGuizmo.isUsing()) {
            if (!context.gizmoEditInProgress) {
                document.pushUndoSnapshot()
                context.gizmoEditInProgress = true
            }
            if (collision != null) {
                applyCollisionDelta(collision, delta, transform.rotation, parentScale)
            } else {
                applyObjectDelta(delta)
            }
        } else {
            context.gizmoEditInProgress = false
        }
    }

    private fun renderEventAreaGizmo(area: EditorEventArea) {
        val model = FloatArray(16)
        ImGuizmo.recomposeMatrixFromComponents(
            floatArrayOf(area.position.x, area.position.y, area.position.z),
            floatArrayOf(area.rotation.x, area.rotation.y, area.rotation.z),
            floatArrayOf(area.size.x, area.size.y, area.size.z),
            model,
        )

        val view = context.activeCamera().getViewMatrix().toFloatArray()
        val projection = MVP.getPerspectiveMatrix(context.viewportWidth / context.viewportHeight).toFloatArray()
        val delta = identityMatrixArray()

        ImGuizmo.setOrthographic(false)
        ImGuizmo.setID(3)
        ImGuizmo.setDrawList(ImGui.getWindowDrawList())
        ImGuizmo.setRect(context.viewportX, context.viewportY, context.viewportWidth, context.viewportHeight)
        ImGuizmo.manipulate(view, projection, context.gizmoOperation, Mode.LOCAL, model, delta)
        context.lastGizmoWantsMouse = context.lastGizmoWantsMouse || ImGuizmo.isOver() || ImGuizmo.isUsing()

        if (ImGuizmo.isUsing()) {
            if (!context.gizmoEditInProgress) {
                document.pushUndoSnapshot()
                context.gizmoEditInProgress = true
            }
            applyEventAreaDelta(area, delta)
        } else {
            context.gizmoEditInProgress = false
        }
    }

    private fun applyEventAreaDelta(area: EditorEventArea, delta: FloatArray) {
        val deltaPosition = FloatArray(3)
        val deltaRotation = FloatArray(3)
        val deltaScale = FloatArray(3)
        ImGuizmo.decomposeMatrixToComponents(delta, deltaPosition, deltaRotation, deltaScale)

        when (context.gizmoOperation) {
            Operation.TRANSLATE -> {
                area.position = area.position.add(Vector3f(deltaPosition[0], deltaPosition[1], deltaPosition[2]))
            }

            Operation.ROTATE -> {
                area.rotation = area.rotation.add(Vector3f(deltaRotation[0], deltaRotation[1], deltaRotation[2]))
            }

            Operation.SCALE -> {
                area.size = Vector3f(
                    (area.size.x * deltaScale[0]).coerceAtLeast(0.01f),
                    (area.size.y * deltaScale[1]).coerceAtLeast(0.01f),
                    (area.size.z * deltaScale[2]).coerceAtLeast(0.01f),
                )
            }
        }
    }

    private fun applyObjectDelta(delta: FloatArray) {
        val deltaPosition = FloatArray(3)
        val deltaRotation = FloatArray(3)
        val deltaScale = FloatArray(3)
        ImGuizmo.decomposeMatrixToComponents(delta, deltaPosition, deltaRotation, deltaScale)

        val targets = context.selectedIndices
            .filter { it in context.placedObjects.indices }
            .ifEmpty { listOf(context.selectedIndex).filter { it in context.placedObjects.indices } }

        for (index in targets) {
            val current = context.placedObjects[index].root.localTransform
            context.placedObjects[index].root.localTransform = when (context.gizmoOperation) {
                Operation.TRANSLATE -> current.copy(
                    position = current.position.add(Vector3f(deltaPosition[0], deltaPosition[1], deltaPosition[2])),
                )

                Operation.ROTATE -> current.copy(
                    rotation = current.rotation.add(Vector3f(deltaRotation[0], deltaRotation[1], deltaRotation[2])),
                )

                Operation.SCALE -> current.copy(
                    scale = Vector3f(
                        (current.scale.x * deltaScale[0]).coerceAtLeast(0.01f),
                        (current.scale.y * deltaScale[1]).coerceAtLeast(0.01f),
                        (current.scale.z * deltaScale[2]).coerceAtLeast(0.01f),
                    ),
                )

                else -> current
            }
        }
    }

    private fun applyCollisionDelta(
        collision: EditorCollisionState,
        delta: FloatArray,
        parentRotation: Quaternion,
        parentScale: Vector3f,
    ) {
        val deltaPosition = FloatArray(3)
        val deltaRotation = FloatArray(3)
        val deltaScale = FloatArray(3)
        ImGuizmo.decomposeMatrixToComponents(delta, deltaPosition, deltaRotation, deltaScale)

        when (context.gizmoOperation) {
            Operation.TRANSLATE -> {
                val localDelta = parentRotation.conjugate()
                    .rotate(Vector3f(deltaPosition[0], deltaPosition[1], deltaPosition[2]))
                    .divide(parentScale.safeComponents())
                collision.position = collision.position.add(localDelta)
            }

            Operation.ROTATE -> {
                if (collision.shape == EditorCollisionShape.BOX) {
                    collision.rotation = collision.rotation.add(Vector3f(deltaRotation[0], deltaRotation[1], deltaRotation[2]))
                }
            }

            Operation.SCALE -> {
                when (collision.shape) {
                    EditorCollisionShape.BOX -> {
                        collision.size = Vector3f(
                            (collision.size.x * deltaScale[0]).coerceAtLeast(0.01f),
                            (collision.size.y * deltaScale[1]).coerceAtLeast(0.01f),
                            (collision.size.z * deltaScale[2]).coerceAtLeast(0.01f),
                        )
                    }

                    EditorCollisionShape.SPHERE -> {
                        val scale = (abs(deltaScale[0]) + abs(deltaScale[1]) + abs(deltaScale[2])) / 3f
                        collision.radius = (collision.radius * scale).coerceAtLeast(0.01f)
                    }
                }
            }
        }
    }

    private fun Vector3f.absComponents(): Vector3f {
        return Vector3f(abs(x), abs(y), abs(z))
    }

    private fun Vector3f.average(): Float {
        return (x + y + z) / 3f
    }

    private fun Vector3f.safeComponents(): Vector3f {
        return Vector3f(x.safeScale(), y.safeScale(), z.safeScale())
    }

    private fun Float.safeScale(): Float {
        return if (abs(this) < 0.0001f) 1f else this
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
}
