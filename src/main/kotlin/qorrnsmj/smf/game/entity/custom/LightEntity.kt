package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.graphic.resource.model.Model
import qorrnsmj.smf.graphic.scene.light.Light
import qorrnsmj.smf.graphic.scene.light.PointLight
import qorrnsmj.smf.graphic.scene.light.SpotLight
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.component.IPhysicsComponent
import qorrnsmj.smf.physics.component.StaticPhysics

open class LightEntity(
    transform: Transform = Transform(),
    val light: Light = PointLight(),
    model: Model = EntityModels.EMPTY,
    physicsComponent: IPhysicsComponent = StaticPhysics(),
    displayName: String = "Light",
) : Entity(transform, model, physicsComponent, displayName) {
    var localDirection: Vector3f = (light as? SpotLight)?.direction ?: Vector3f(0f, -1f, 0f)

    internal fun resolveLight(): Light {
        val transform = worldTransform
        light.position = transform.position
        if (light is SpotLight) {
            light.direction = transform.rotation.rotate(localDirection)
        }
        return light
    }
}
