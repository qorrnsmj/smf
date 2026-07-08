package qorrnsmj.smf.graphic.scene

data class Scene(
    val world: SceneWorld = SceneWorld(),
    val environment: SceneEnvironment = SceneEnvironment(),
    val renderSettings: SceneRenderSettings = SceneRenderSettings(),
    val effects: SceneEffects = SceneEffects(),
)
