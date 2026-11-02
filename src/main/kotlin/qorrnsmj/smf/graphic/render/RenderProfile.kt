package qorrnsmj.smf.graphic.render

enum class RenderProfile(
    val shadowsEnabled: Boolean,
) {
    SHADOWED(true),
    UNSHADOWED(false),
    ;

    companion object {
        fun fromId(id: String?): RenderProfile {
            if (id == null) return SHADOWED
            return entries.firstOrNull { it.name == id }
                ?: error("Unknown render profile: $id")
        }
    }
}
