package qorrnsmj.smf.graphic.render

import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.math.Matrix4f

data class RenderProfile(
    val shadowsEnabled: Boolean = true,
)

object RenderProfiles {
    val SHADOWED = RenderProfile(shadowsEnabled = true)
    val UNSHADOWED = RenderProfile(shadowsEnabled = false)

    fun fromName(name: String?): RenderProfile {
        return when (name?.trim()?.lowercase()) {
            null, "", "shadowed", "shadows", "default" -> SHADOWED
            "unshadowed", "no_shadows", "no-shadows", "flat" -> UNSHADOWED
            else -> error("Unknown render profile: $name")
        }
    }
}

object RenderProfileManager {
    fun applyTo(scene: Scene, profile: RenderProfile) {
        scene.renderProfile = profile
    }

    fun createShadowState(scene: Scene, shadowRenderer: ShadowRenderer): ShadowRenderState {
        return if (scene.renderProfile.shadowsEnabled) {
            shadowRenderer.render(scene)
        } else {
            ShadowRenderState(false, Matrix4f(), 0)
        }
    }
}
