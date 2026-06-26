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
            val scaledLocalPosition = localTransform.position.multiply(p.scale)
            Transform(
                position = p.position.add(p.rotation.rotate(scaledLocalPosition)),
                rotation = p.rotation.multiply(localTransform.rotation).normalize(),
                scale = p.scale.multiply(localTransform.scale)
            )
        } ?: localTransform

    var parent: Entity? = null
        private set
    private val _children: MutableList<Entity> = mutableListOf()
    val children: List<Entity> get() = _children

    fun addChild(child: Entity) {
        child.parent?.removeChild(child)
        _children.add(child)
        child.parent = this
    }

    fun removeChild(child: Entity) {
        _children.remove(child)
        child.parent = null
    }

    open fun update(deltaTime: Float) {
        updateChildren(deltaTime)
    }

    protected fun updateChildren(deltaTime: Float) {
        children.forEach { it.update(deltaTime) }
    }
}
