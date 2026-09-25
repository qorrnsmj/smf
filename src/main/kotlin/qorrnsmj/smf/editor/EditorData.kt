package qorrnsmj.smf.editor

import com.fasterxml.jackson.databind.ObjectMapper
import qorrnsmj.smf.math.Vector3f
import java.nio.file.Files
import java.nio.file.Path

internal object EditorData {
    fun save(context: EditorContext, root: Path) {
        EditorProjectStore.writeJson(root.resolve("EditorData.json"), mapOf(
            "version" to 1,
            "cameras" to (0..1).map { index ->
                val camera = context.viewportCamera(index)
                mapOf("position" to camera.position.components(), "front" to camera.getFront().components())
            },
            "activeViewport" to context.activeViewportIndex,
            "editMode" to context.editMode.name,
        ))
    }

    fun load(context: EditorContext, root: Path) {
        context.scene.world.camera.position = Vector3f(0f, 8f, 22f)
        context.scene.world.camera.setFront(Vector3f(0f, -0.25f, -1f))
        context.secondaryCamera.position = Vector3f(18f, 10f, 18f)
        context.secondaryCamera.setFront(Vector3f(-1f, -0.35f, -1f))
        context.editMode = EditorEditMode.OBJECT
        context.setActiveViewport(0)
        val path = root.resolve("EditorData.json")
        if (Files.isRegularFile(path)) {
            val json = ObjectMapper().readTree(path.toFile())
            val cameras = json.path("cameras")
            for (index in 0..1) {
                val data = cameras.path(index)
                fun vector(key: String): Vector3f? {
                    val values = data.path(key)
                    if (!values.isArray || values.size() != 3 || values.any { !it.isNumber || !it.floatValue().isFinite() }) return null
                    return Vector3f(values[0].floatValue(), values[1].floatValue(), values[2].floatValue())
                }
                vector("position")?.let { context.viewportCamera(index).position = it }
                vector("front")?.takeIf { it.x * it.x + it.y * it.y + it.z * it.z > 0.00001f }
                    ?.let { context.viewportCamera(index).setFront(it) }
            }
            context.setActiveViewport(json.path("activeViewport").asInt(0).coerceIn(0, 1))
            runCatching { EditorEditMode.valueOf(json.path("editMode").asText()) }.getOrNull()?.let { context.editMode = it }
        }
        context.cameraController.syncFromCamera()
        context.secondaryCameraController.syncFromCamera()
    }

    private fun Vector3f.components(): List<Float> = listOf(x, y, z)
}
