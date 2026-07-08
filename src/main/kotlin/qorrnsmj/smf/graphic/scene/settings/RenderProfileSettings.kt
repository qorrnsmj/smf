package qorrnsmj.smf.graphic.scene.settings

import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.graphic.shadow.ShadowRenderer

data class RenderProfileSettings(
    val shadowsEnabled: Boolean = true,
)

object RenderProfileSettingsPresets {
    val SHADOWED = RenderProfileSettings(shadowsEnabled = true)
    val UNSHADOWED = RenderProfileSettings(shadowsEnabled = false)

    fun fromName(name: String?): RenderProfileSettings {
        return when (name?.trim()?.lowercase()) {
            null, "", "shadowed", "shadows", "default" -> SHADOWED
            "unshadowed", "no_shadows", "no-shadows", "flat" -> UNSHADOWED
            else -> error("Unknown render profile: $name")
        }
    }
}

object RenderProfileSettingsManager {
    fun applyTo(scene: Scene, profile: RenderProfileSettings) {
        scene.renderSettings.renderProfile = profile
    }

    fun createShadowState(scene: Scene, shadowRenderer: ShadowRenderer) {
        shadowRenderer.render(scene)
    }
}
