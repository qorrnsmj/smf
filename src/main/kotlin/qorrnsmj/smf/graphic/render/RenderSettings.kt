package qorrnsmj.smf.graphic.render

data class RenderSettings(
    var profile: RenderProfile = RenderProfile.SHADOWED,
    var shadingMode: ShadingMode = ShadingMode.RENDERED,
    var cullingEnabled: Boolean = true,
)

enum class ShadingMode {
    WIRE,
    SOLID,
    MATERIAL,
    RENDERED,
}
