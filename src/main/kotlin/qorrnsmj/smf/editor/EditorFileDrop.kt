package qorrnsmj.smf.editor

import org.lwjgl.glfw.GLFWDropCallback
import java.nio.file.Paths

internal class EditorFileDrop(
    private val context: EditorContext,
    private val document: EditorDocument,
) {
    private var callback: GLFWDropCallback? = null

    fun install(windowId: Long) {
        callback = GLFWDropCallback.create { _, count, names ->
            for (index in 0 until count) {
                val path = GLFWDropCallback.getName(names, index)
                handleDrop(path)
            }
        }.set(windowId)
    }

    fun dispose() {
        callback?.free()
        callback = null
    }

    private fun handleDrop(path: String) {
        if (!path.endsWith(".glb", ignoreCase = true)) return

        val normalized = Paths.get(path).toAbsolutePath().normalize().toString()
        document.registerExternalGlb(normalized)

        val ray = EditorPicking.currentMouseRay(context)
        val position = EditorPicking.intersectGround(ray)
            ?: context.activeCamera().position.add(context.activeCamera().getFront().scale(12f))
        document.addAsset(normalized, position)
    }
}
