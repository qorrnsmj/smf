package qorrnsmj.smf.graphic.text

import qorrnsmj.smf.game.level.custom.Level
import qorrnsmj.smf.math.Vector3f
import java.util.Locale

class GameDebugHud {
    private val font: Font by lazy { FontLoader.loadAssetFont("Inconsolata.ttf", 16f) }

    fun update(
        level: Level,
        stageName: String,
        fps: Int,
        ups: Int,
        wireframeEnabled: Boolean,
        colliderDebugEnabled: Boolean,
    ) {
        val position = level.player.worldTransform.position
        val flying = level.player.isFlying
        val lines = listOf(
            "FPS/UPS: $fps/$ups",
            "Stage: $stageName",
            String.format(Locale.ROOT, "Position: %.1f, %.1f, %.1f", position.x, position.y, position.z),
            "Mode: ${if (flying) "FLY" else "WALK"} | Wire: ${onOff(wireframeEnabled)} | Colliders: ${onOff(colliderDebugEnabled)}",
            "WASD Move | Mouse Look | Space ${if (flying) "Up" else "Jump"}${if (flying) " | Left Shift Down" else ""}",
            "F Fly | E Wireframe | 0 Test | 1 Day | 2 Night",
            "G Wind | T Time | F1 Colliders | F12 Screenshot",
        )

        level.scene.world.debugTextElements.clear()
        lines.forEachIndexed { index, line ->
            val x = 12f
            val y = 24f + index * 21f
            level.scene.world.debugTextElements += TextElement(
                text = line,
                font = font,
                x = x + 1f,
                y = y + 1f,
                color = Vector3f(0.02f, 0.03f, 0.04f),
            )
            level.scene.world.debugTextElements += TextElement(
                text = line,
                font = font,
                x = x,
                y = y,
                color = Vector3f(0.94f, 0.97f, 1f),
            )
        }
    }

    private fun onOff(value: Boolean): String = if (value) "ON" else "OFF"
}
