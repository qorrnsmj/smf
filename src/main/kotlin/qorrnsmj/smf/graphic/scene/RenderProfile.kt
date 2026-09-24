package qorrnsmj.smf.graphic.scene

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
