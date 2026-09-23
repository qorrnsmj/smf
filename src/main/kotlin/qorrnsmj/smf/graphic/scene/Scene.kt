package qorrnsmj.smf.graphic.scene

data class Scene(
    val world: SceneWorld = SceneWorld(),
    val environment: SkyEnvironment = SkyEnvironment(),
    val renderSettings: SceneRenderSettings = SceneRenderSettings(),
    val effects: SceneEffects = SceneEffects(),
)
