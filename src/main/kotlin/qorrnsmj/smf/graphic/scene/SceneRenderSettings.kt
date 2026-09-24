package qorrnsmj.smf.graphic.scene

import qorrnsmj.smf.graphic.scene.settings.ViewportShadingSettings

data class SceneRenderSettings(
    var renderProfile: RenderProfile = RenderProfiles.SHADOWED,
    var viewportShadingMode: ViewportShadingSettings = ViewportShadingSettings.RENDERED,
    var cullingEnabled: Boolean = true,

    var terrainGrayView: Boolean = false,
    var terrainWireframeView: Boolean = false,
)
