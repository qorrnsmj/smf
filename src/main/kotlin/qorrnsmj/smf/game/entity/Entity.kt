package qorrnsmj.smf.game.entity

import qorrnsmj.smf.game.entity.component.Model
import qorrnsmj.smf.math.Vector3f

// TODO: GameObjectに名前変える パッケージも
abstract class Entity(
    var position: Vector3f = Vector3f(0.0f, 0.0f, 0.0f),
    var rot: Vector3f = Vector3f(0.0f, 0.0f, 0.0f),
    var scale: Vector3f = Vector3f(1.0f, 1.0f, 1.0f)
) {
    abstract fun getModels(): List<Model>
}
