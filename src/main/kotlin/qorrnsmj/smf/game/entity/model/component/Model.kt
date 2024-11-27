package qorrnsmj.smf.game.entity.model.component

open class Model(
    val name: String,
    val mesh: Mesh,
    val material: Material,

    val hasTransparency: Boolean = false,
    val useFakeLighting: Boolean = false,
)
