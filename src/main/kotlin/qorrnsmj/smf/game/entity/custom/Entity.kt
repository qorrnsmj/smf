package qorrnsmj.smf.game.entity.custom

import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.physics.component.IPhysicsComponent
import qorrnsmj.smf.physics.component.StaticPhysics

abstract class Entity(
    transform: Transform = Transform(),
    val model: Model = EntityModels.EMPTY,
    var physicsComponent: IPhysicsComponent = StaticPhysics()
) {
    var localTransform: Transform = transform
    val worldTransform: Transform
        get() = parent?.worldTransform?.let { p ->
            Transform(
                position = p.position.add(localTransform.position),
                rotation = p.rotation.add(localTransform.rotation),
                scale = p.scale.multiply(localTransform.scale)
            )
        } ?: localTransform

    var parent: Entity? = null
        private set
    val children: MutableList<Entity> = mutableListOf()

    fun addChild(child: Entity) {
        child.parent?.removeChild(child)
        children.add(child)
        child.parent = this
    }

    fun removeChild(child: Entity) {
        children.remove(child)
        child.parent = null
    }

    open fun update(deltaTime: Float) {
        updateChildren(deltaTime)
    }

    protected fun updateChildren(deltaTime: Float) {
        children.forEach { it.update(deltaTime) }
    }
}
