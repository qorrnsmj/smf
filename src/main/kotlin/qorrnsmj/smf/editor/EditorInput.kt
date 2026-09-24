package qorrnsmj.smf.editor

import org.lwjgl.glfw.GLFW.GLFW_KEY_E
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
        if (context.closeConfirmationActive || context.suppressEditorInputUntilMouseRelease) {
            val leftDown = glfwGetMouseButton(SMF.window.id, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
            val rightDown = glfwGetMouseButton(SMF.window.id, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS
            context.terrainBrushWasDown = false
            context.leftMouseWasDown = leftDown
            context.viewportMouseLookActive = false
            if (!context.closeConfirmationActive && !leftDown && !rightDown) {
                context.suppressEditorInputUntilMouseRelease = false
            }
            return
        }
        val rightMouseDown = glfwGetMouseButton(SMF.window.id, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS
        val leftMouseDown = glfwGetMouseButton(SMF.window.id, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
        val mouseLookLockedToViewport = rightMouseDown && context.viewportMouseLookActive
        val mouseInViewport = if (mouseLookLockedToViewport) {
            true
        } else {
            updateActiveViewportFromMouse()
        }
        val terrainMouseBrushInput = context.editMode == EditorEditMode.TERRAIN &&
            context.terrainPointerMode == EditorTerrainPointerMode.MOUSE
        val canStartMouseLook = mouseInViewport && !context.lastGizmoWantsMouse && !terrainMouseBrushInput
        context.viewportMouseLookActive = if (rightMouseDown) {
            context.viewportMouseLookActive || canStartMouseLook
        } else {
            false
        }
        val keyboardBlocked = context.editorKeyboardBlocked && !context.viewportMouseLookActive
        val panelResizeBlocksTerrain = context.panelResizeHandleHovered || context.panelResizeInProgress

        handleUndoRedoShortcuts()
        handleSaveShortcut()
        handleDeleteShortcut()
        if (!keyboardBlocked) {
            updateTerrainReticleActionShortcut()
            if (context.editMode == EditorEditMode.OBJECT && (!mouseInViewport || !rightMouseDown)) {
                updateGizmoShortcuts()
            }
        } else {
            context.terrainReticleActionToggleWasDown = false
        }

        val terrainInputActive = context.editMode == EditorEditMode.TERRAIN &&
            mouseInViewport &&
            !context.assetDragInProgress &&
            !panelResizeBlocksTerrain &&
            !context.lastGizmoWantsMouse &&
            terrainBrushMouseDown(leftMouseDown, rightMouseDown)
        val terrainReticleNavigationActive = context.editMode == EditorEditMode.TERRAIN &&
            context.terrainPointerMode == EditorTerrainPointerMode.RETICLE &&
            mouseInViewport &&
            !context.assetDragInProgress &&
            !panelResizeBlocksTerrain

        context.activeCameraController().update(
            delta,
            !context.viewportMouseLookActive || keyboardBlocked,
            forceMovement = (terrainInputActive || terrainReticleNavigationActive) && !keyboardBlocked,
        )
        val terrainBrushConsumed = handleTerrainBrush(mouseInViewport, leftMouseDown, rightMouseDown, delta)
        if (!terrainBrushConsumed) {
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

    private fun updateTerrainReticleActionShortcut() {
        val toggleDown = context.editMode == EditorEditMode.TERRAIN &&
            context.terrainPointerMode == EditorTerrainPointerMode.RETICLE &&
            glfwGetKey(SMF.window.id, GLFW_KEY_E) == GLFW_PRESS
        if (toggleDown && !context.terrainReticleActionToggleWasDown) {
            context.terrainReticleAction = when (context.terrainReticleAction) {
                EditorTerrainReticleAction.RAISE -> EditorTerrainReticleAction.LOWER
                EditorTerrainReticleAction.LOWER -> EditorTerrainReticleAction.RAISE
            }
        }
        context.terrainReticleActionToggleWasDown = toggleDown
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
                context.errorPopupTitle = "Save failed"
                context.errorPopupMessage = error.message ?: error::class.simpleName ?: "Unknown error"
                context.errorPopupOpen = true
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

    private fun handleTerrainBrush(mouseInViewport: Boolean, leftDown: Boolean, rightDown: Boolean, delta: Float): Boolean {
        val active = context.editMode == EditorEditMode.TERRAIN &&
            mouseInViewport &&
            terrainBrushMouseDown(leftDown, rightDown) &&
            !context.assetDragInProgress &&
            !context.panelResizeHandleHovered &&
            !context.panelResizeInProgress &&
            !context.lastGizmoWantsMouse

        var applied = false
        if (active) {
            val point = EditorPicking.intersectGround(terrainBrushRay())
            val grid = point?.let { document.terrainGridAt(it) }
            if (grid != null) {
                val direction = terrainBrushDirection(leftDown, rightDown)
                document.applyTerrainBrush(grid.first, grid.second, delta, !context.terrainBrushWasDown, direction)
                applied = true
            }
        }

        context.terrainBrushWasDown = applied
        if (active) context.leftMouseWasDown = leftDown
        return active
    }

    private fun terrainBrushMouseDown(leftDown: Boolean, rightDown: Boolean): Boolean {
        return when (context.terrainPointerMode) {
            EditorTerrainPointerMode.MOUSE -> leftDown || rightDown
            EditorTerrainPointerMode.RETICLE -> leftDown
        }
    }

    private fun terrainBrushDirection(leftDown: Boolean, rightDown: Boolean): Float {
        return when (context.terrainPointerMode) {
            EditorTerrainPointerMode.MOUSE -> if (rightDown && !leftDown) -1f else 1f
            EditorTerrainPointerMode.RETICLE -> when (context.terrainBrushMode) {
                EditorTerrainBrushMode.RAISE -> if (context.terrainReticleAction == EditorTerrainReticleAction.RAISE) 1f else -1f
                EditorTerrainBrushMode.LOWER -> if (context.terrainReticleAction == EditorTerrainReticleAction.LOWER) 1f else -1f
                else -> 1f
            }
        }
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
