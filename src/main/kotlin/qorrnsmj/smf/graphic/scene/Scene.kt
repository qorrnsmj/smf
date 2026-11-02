package qorrnsmj.smf.graphic.scene

data class Scene(
    val world: SceneWorld = SceneWorld(),
    val environment: SceneEnvironment = SceneEnvironment(),
    val sceneEffects: SceneEffects = SceneEffects(),
)
