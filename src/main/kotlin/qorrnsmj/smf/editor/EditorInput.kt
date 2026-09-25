package qorrnsmj.smf.editor

import org.lwjgl.glfw.GLFW.GLFW_KEY_E
import org.lwjgl.glfw.GLFW.GLFW_KEY_Q
import org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_R
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_S
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
        if (context.projectDialogActive || context.closeConfirmationActive || context.suppressEditorInputUntilMouseRelease) {
            val leftDown = glfwGetMouseButton(SMF.window.id, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
            val rightDown = glfwGetMouseButton(SMF.window.id, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS
            context.terrainBrushWasDown = false
            context.leftMouseWasDown = leftDown
            context.rightMouseWasDown = rightDown
            context.viewportMouseLookActive = false
            if (!context.closeConfirmationActive && !leftDown && !rightDown) {
                context.suppressEditorInputUntilMouseRelease = false
            }
            return
        }
        val rightMouseDown = glfwGetMouseButton(SMF.window.id, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS
        val leftMouseDown = glfwGetMouseButton(SMF.window.id, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
        val mouseInViewport = if (context.viewportMouseLookActive) {
            true
        } else {
            updateActiveViewportFromMouse()
        }
        if (rightMouseDown && !context.rightMouseWasDown) {
            context.viewportMouseLookActive = if (context.viewportMouseLookActive) {
                false
            } else {
                mouseInViewport && !context.lastGizmoWantsMouse
            }
        }
        context.rightMouseWasDown = rightMouseDown
        val keyboardBlocked = context.editorKeyboardBlocked && !context.viewportMouseLookActive

        handleUndoRedoShortcuts()
        handleSaveShortcut()
        handleDeleteShortcut()
        if (!keyboardBlocked) {
            updateTerrainModeShortcuts()
            if (context.editMode == EditorEditMode.OBJECT && !context.viewportMouseLookActive) {
                updateGizmoShortcuts()
            }
        } else {
            context.terrainPreviousModeWasDown = false
            context.terrainNextModeWasDown = false
        }

        context.activeCameraController().update(
            delta,
            context.viewportMouseLookActive && !keyboardBlocked,
            context.cameraSensitivity,
        )
        val reticleBrushWhileNavigating = context.viewportMouseLookActive &&
            context.editMode == EditorEditMode.TERRAIN &&
            context.terrainPointerMode == EditorTerrainPointerMode.RETICLE
        if (context.viewportMouseLookActive && !reticleBrushWhileNavigating) {
            context.terrainBrushWasDown = false
            context.leftMouseWasDown = leftMouseDown
            return
        }

        val terrainBrushConsumed = handleTerrainBrush(mouseInViewport, leftMouseDown, delta)
        if (!terrainBrushConsumed && !context.viewportMouseLookActive) {
            handleMousePicking()
        }
    }

    fun addSelectedAssetAtCursor() {
        val ray = EditorPicking.currentMouseRay(context)
        val position = EditorPicking.intersectGround(ray)
            ?: context.scene.world.camera.position.add(context.scene.world.camera.getFront().scale(12f))
        document.addSelectedAsset(position)
    }

    private fun updateGizmoShortcuts() {
        val window = SMF.window.id
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) context.gizmoOperation = Operation.TRANSLATE
        if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) context.gizmoOperation = Operation.ROTATE
        if (glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS) context.gizmoOperation = Operation.SCALE
    }

    private fun updateTerrainModeShortcuts() {
        val terrainMode = context.editMode == EditorEditMode.TERRAIN
        val previousDown = terrainMode && glfwGetKey(SMF.window.id, GLFW_KEY_Q) == GLFW_PRESS
        val nextDown = terrainMode && glfwGetKey(SMF.window.id, GLFW_KEY_E) == GLFW_PRESS
        if (previousDown && !context.terrainPreviousModeWasDown) cycleTerrainMode(-1)
        if (nextDown && !context.terrainNextModeWasDown) cycleTerrainMode(1)
        context.terrainPreviousModeWasDown = previousDown
        context.terrainNextModeWasDown = nextDown
    }

    private fun cycleTerrainMode(offset: Int) {
        val modes = EditorTerrainBrushMode.entries
        val current = modes.indexOf(context.terrainBrushMode)
        context.terrainBrushMode = modes[(current + offset + modes.size) % modes.size]
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

        if (!context.editorKeyboardBlocked && undoDown && !context.undoShortcutWasDown) {
            document.undo()
        }
        if (!context.editorKeyboardBlocked && redoDown && !context.redoShortcutWasDown) {
            document.redo()
        }

        context.undoShortcutWasDown = undoDown
        context.redoShortcutWasDown = redoDown
    }

    private fun handleDeleteShortcut() {
        val deleteDown = glfwGetKey(SMF.window.id, GLFW_KEY_DELETE) == GLFW_PRESS
        if (!context.editorKeyboardBlocked && deleteDown && !context.deleteShortcutWasDown) {
            document.deleteSelected()
        }
        context.deleteShortcutWasDown = deleteDown
    }

    private fun handleSaveShortcut() {
        val window = SMF.window.id
        val ctrlDown = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS ||
            glfwGetKey(window, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS
        val saveDown = ctrlDown && glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS

        if (saveDown && !context.saveShortcutWasDown) {
            try {
                document.saveMap()
            } catch (error: Throwable) {
                document.reportError("Save failed", error)
            }
        }
        context.saveShortcutWasDown = saveDown
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

    private fun handleTerrainBrush(mouseInViewport: Boolean, leftDown: Boolean, delta: Float): Boolean {
        val active = context.editMode == EditorEditMode.TERRAIN &&
            mouseInViewport &&
            terrainBrushMouseDown(leftDown) &&
            !context.assetDragInProgress &&
            !context.panelResizeHandleHovered &&
            !context.panelResizeInProgress &&
            !context.lastGizmoWantsMouse

        var applied = false
        if (active) {
            val point = EditorPicking.intersectGround(terrainBrushRay())
            val grid = point?.let { document.terrainGridAt(it) }
            if (grid != null) {
                document.applyTerrainBrush(grid.first, grid.second, delta, !context.terrainBrushWasDown)
                applied = true
            }
        }

        context.terrainBrushWasDown = applied
        if (active) context.leftMouseWasDown = leftDown
        return active
    }

    private fun terrainBrushMouseDown(leftDown: Boolean): Boolean {
        return leftDown
    }

    private fun terrainBrushRay(): Ray {
        return when (context.terrainPointerMode) {
            EditorTerrainPointerMode.MOUSE -> EditorPicking.currentMouseRay(context)
            EditorTerrainPointerMode.RETICLE -> EditorPicking.viewportCenterRay(context)
        }
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
