package qorrnsmj.smf.game.entity.custom.solarsystem

import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.model.component.Model
import qorrnsmj.smf.math.Vector3f
import kotlin.math.cos
import kotlin.math.sin

abstract class PlanetEntity(model: Model) : Entity(model = model) {
    abstract val sunDistance: Float
    abstract val spinSpeed: Float
    abstract val orbitSpeed: Float
    abstract val axialTilt: Float

    fun spin(delta: Float) {
        rotation.y -= spinSpeed * delta
    }

    fun orbit(sunPosition: Vector3f, elapsedTime: Float) {
        val amount = orbitSpeed * elapsedTime
        val x = sunPosition.x + cos(-amount) * sunDistance * 10f
        val z = sunPosition.z + sin(-amount) * sunDistance * 10f
        position = Vector3f(x, 0f, z)
    }
}
