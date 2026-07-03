package qorrnsmj.smf.editor

import org.lwjgl.glfw.GLFW.GLFW_KEY_E
import org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_R
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_W
import org.lwjgl.glfw.GLFW.GLFW_KEY_Z
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.lwjgl.glfw.GLFW.glfwGetMouseButton
import org.lwjgl.glfw.GLFW.glfwGetCursorPos
import imgui.extension.imguizmo.flag.Operation
import qorrnsmj.smf.SMF

internal class EditorInput(
    private val context: EditorContext,
    private val document: EditorDocument,
) {
    fun update(delta: Float) {
        val rightMouseDown = glfwGetMouseButton(SMF.window.id, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS
        val mouseLookLockedToViewport = rightMouseDown && context.viewportMouseLookActive
        val mouseInViewport = if (mouseLookLockedToViewport) {
            true
        } else {
            updateActiveViewportFromMouse()
        }
        val canStartMouseLook = mouseInViewport && !context.lastGizmoWantsMouse
        context.viewportMouseLookActive = if (rightMouseDown) {
            context.viewportMouseLookActive || canStartMouseLook
        } else {
            false
        }

        handleUndoRedoShortcuts()
        handleDeleteShortcut()
        if (!context.lastWantCaptureKeyboard) {
            if (!mouseInViewport || !rightMouseDown) {
                updateGizmoShortcuts()
            }
        }

        context.activeCameraController().update(
            delta,
            !context.viewportMouseLookActive,
        )
        val terrainBrushConsumed = handleTerrainBrush(mouseInViewport, rightMouseDown, delta)
        if (!terrainBrushConsumed) {
            handleMousePicking()
        }
    }

    fun addSelectedAssetAtCursor() {
        val ray = EditorPicking.currentMouseRay(context)
        val position = EditorPicking.intersectGround(ray)
            ?: context.scene.camera.position.add(context.scene.camera.getFront().scale(12f))
        document.addSelectedAsset(position)
    }

    private fun updateGizmoShortcuts() {
        val window = SMF.window.id
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) context.gizmoOperation = Operation.TRANSLATE
        if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) context.gizmoOperation = Operation.ROTATE
        if (glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS) context.gizmoOperation = Operation.SCALE
    }

    private fun handleUndoRedoShortcuts() {
        val window = SMF.window.id
        val ctrlDown = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS ||
            glfwGetKey(window, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS
        val shiftDown = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS ||
            glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS
        val zDown = glfwGetKey(window, GLFW_KEY_Z) == GLFW_PRESS
        val undoDown = ctrlDown && !shiftDown && zDown
        val redoDown = ctrlDown && shiftDown && zDown

        if (undoDown && !context.undoShortcutWasDown) {
            document.undo()
        }
        if (redoDown && !context.redoShortcutWasDown) {
            document.redo()
        }

        context.undoShortcutWasDown = undoDown
        context.redoShortcutWasDown = redoDown
    }

    private fun handleDeleteShortcut() {
        val deleteDown = glfwGetKey(SMF.window.id, GLFW_KEY_DELETE) == GLFW_PRESS
        if (!context.lastWantCaptureKeyboard && deleteDown && !context.deleteShortcutWasDown) {
            document.deleteSelected()
        }
        context.deleteShortcutWasDown = deleteDown
    }

    private fun handleMousePicking() {
        if (context.editMode != EditorEditMode.OBJECT) return

        val leftDown = glfwGetMouseButton(SMF.window.id, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
        if (leftDown && !context.leftMouseWasDown && !context.lastGizmoWantsMouse && updateActiveViewportFromMouse()) {
            val ray = EditorPicking.currentMouseRay(context)
            context.selectedIndex = EditorPicking.pickObject(context, ray)
            context.selectedIndices.clear()
            if (context.selectedIndex in context.placedObjects.indices) {
                context.selectedSpawnPointIndex = -1
                context.selectedEventAreaIndex = -1
                context.selectedIndices.add(context.selectedIndex)
                context.hierarchyRangeAnchorIndex = context.selectedIndex
            } else {
                context.selectedEventAreaIndex = EditorPicking.pickEventArea(context, ray)
                context.selectedSpawnPointIndex = -1
                context.hierarchyRangeAnchorIndex = -1
            }
        }

        context.leftMouseWasDown = leftDown
    }

    private fun handleTerrainBrush(mouseInViewport: Boolean, rightMouseDown: Boolean, delta: Float): Boolean {
        val leftDown = glfwGetMouseButton(SMF.window.id, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
        val active = context.editMode == EditorEditMode.TERRAIN &&
            mouseInViewport &&
            leftDown &&
            !rightMouseDown &&
            !context.lastGizmoWantsMouse

        if (active) {
            val point = EditorPicking.intersectGround(EditorPicking.currentMouseRay(context))
            val grid = point?.let { document.terrainGridAt(it) }
            if (grid != null) {
                document.applyTerrainBrush(grid.first, grid.second, delta, !context.terrainBrushWasDown)
            }
        }

        context.terrainBrushWasDown = active
        if (active) context.leftMouseWasDown = leftDown
        return active
    }

    private fun updateActiveViewportFromMouse(): Boolean {
        val x = DoubleArray(1)
        val y = DoubleArray(1)
        glfwGetCursorPos(SMF.window.id, x, y)
        val index = context.viewportIndexAt(x[0].toFloat(), y[0].toFloat())
        if (index != -1) context.setActiveViewport(index)
        return index != -1
    }
}
