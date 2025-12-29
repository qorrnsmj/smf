package qorrnsmj.smf.game.model.component

open class Model(
    val name: String,
    val mesh: Mesh,
    val material: Material,
    val fakeLighting: Boolean = false,
)
