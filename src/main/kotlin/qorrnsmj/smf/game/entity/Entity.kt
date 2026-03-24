package qorrnsmj.smf.game.entity

import qorrnsmj.smf.graphic.`object`.Model
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsComponent
import kotlin.math.*

open class Entity(
    // Local transform (relative to parent)
    localPosition: Vector3f = Vector3f(0f, 0f, 0f),
    localRotation: Vector3f = Vector3f(0f, 0f, 0f),
    localScale: Vector3f = Vector3f(1f, 1f, 1f),
    val model: Model = EntityModels.EMPTY,
    val children: MutableList<Entity> = mutableListOf(),
    var physicsComponent: PhysicsComponent? = null  // Optional physics component
) {
    var localPosition: Vector3f = localPosition
        set(value) {
            field = value
            markTransformDirty()
        }

    var localRotation: Vector3f = localRotation
        set(value) {
            field = value
            markTransformDirty()
        }

    var localScale: Vector3f = localScale
        set(value) {
            field = value
            markTransformDirty()
        }

    // Legacy properties for backward compatibility
    var position: Vector3f
        get() = if (parent == null) localPosition else computeWorldPosition()
        set(value) { 
            if (parent == null) {
                localPosition = value
            } else {
                localPosition = parent!!.worldToLocal(value)
            }
        }
    
    var rotation: Vector3f
        get() = if (parent == null) localRotation else computeWorldRotation()
        set(value) { 
            if (parent == null) {
                localRotation = value
            } else {
                localRotation = parent!!.worldToLocalRotation(value)
            }
        }
    
    var scale: Vector3f
        get() = if (parent == null) localScale else computeWorldScale()
        set(value) { 
            if (parent == null) {
                localScale = value
            } else {
                localScale = value.divide(parent!!.scale)
            }
        }
    
    // Parent reference (set automatically when added to children)
    var parent: Entity? = null
        private set
    
    // Cached world transform (updated when needed)
    private var cachedWorldPosition: Vector3f? = null
    private var cachedWorldRotation: Vector3f? = null
    private var cachedWorldScale: Vector3f? = null
    private var transformDirty: Boolean = true
    
    /**
     * Get world position (absolute position in scene)
     */
    fun getWorldPosition(): Vector3f {
        if (parent == null) return localPosition
        if (transformDirty || cachedWorldPosition == null) {
            cachedWorldPosition = computeWorldPosition()
            transformDirty = false
        }
        return cachedWorldPosition!!
    }
    
    /**
     * Get world rotation (absolute rotation in scene)
     */
    fun getWorldRotation(): Vector3f {
        if (parent == null) return localRotation
        if (transformDirty || cachedWorldRotation == null) {
            cachedWorldRotation = computeWorldRotation()
        }
        return cachedWorldRotation!!
    }
    
    /**
     * Get world scale (absolute scale in scene)
     */
    fun getWorldScale(): Vector3f {
        if (parent == null) return localScale
        if (transformDirty || cachedWorldScale == null) {
            cachedWorldScale = computeWorldScale()
        }
        return cachedWorldScale!!
    }
    
    /**
     * Add a child entity and set parent relationship
     */
    fun addChild(child: Entity) {
        if (!children.contains(child)) {
            children.add(child)
            child.parent = this
            child.markTransformDirty()
        }
    }
    
    /**
     * Remove a child entity and clear parent relationship
     */
    fun removeChild(child: Entity) {
        if (children.remove(child)) {
            child.parent = null
            child.markTransformDirty()
        }
    }
    
    /**
     * Update transform hierarchy (call on root entities)
     */
    fun updateTransform() {
        markTransformDirty()
        children.forEach { it.updateTransform() }
    }
    
    /**
     * Apply physics result to this entity and propagate to children
     */
    fun applyPhysicsTransform(physicsPosition: Vector3f) {
        if (parent == null) {
            // Root entity: physics directly affects local position
            localPosition = physicsPosition
        } else {
            // Child entity: convert physics position to local space
            localPosition = parent!!.worldToLocal(physicsPosition)
        }
        markTransformDirty()
    }
    
    // Private helper methods
    private fun computeWorldPosition(): Vector3f {
        if (parent == null) return localPosition
        
        val parentWorldPos = parent!!.getWorldPosition()
        val parentWorldRot = parent!!.getWorldRotation()
        val parentWorldScale = parent!!.getWorldScale()
        
        // Apply parent rotation to local position
        val rotatedLocal = rotateVector(localPosition.multiply(parentWorldScale), parentWorldRot)
        return parentWorldPos.add(rotatedLocal)
    }
    
    private fun computeWorldRotation(): Vector3f {
        if (parent == null) return localRotation
        return parent!!.getWorldRotation().add(localRotation)
    }
    
    private fun computeWorldScale(): Vector3f {
        if (parent == null) return localScale
        return parent!!.getWorldScale().multiply(localScale)
    }
    
    private fun worldToLocal(worldPos: Vector3f): Vector3f {
        if (parent == null) return worldPos
        
        val parentWorldPos = getWorldPosition()
        val parentWorldRot = getWorldRotation()
        val parentWorldScale = getWorldScale()
        
        val relativePos = worldPos.subtract(parentWorldPos)
        val unrotatedPos = rotateVector(relativePos, parentWorldRot.negate())
        return unrotatedPos.divide(parentWorldScale)
    }
    
    private fun worldToLocalRotation(worldRot: Vector3f): Vector3f {
        if (parent == null) return worldRot
        return worldRot.subtract(getWorldRotation())
    }
    
    private fun markTransformDirty() {
        transformDirty = true
        cachedWorldPosition = null
        cachedWorldRotation = null
        cachedWorldScale = null
        
        // Mark all children as dirty too
        children.forEach { it.markTransformDirty() }
    }
    
    private fun rotateVector(vector: Vector3f, rotation: Vector3f): Vector3f {
        // Simple euler rotation (Y -> X -> Z order)
        var result = vector
        
        // Rotate around Y axis
        if (rotation.y != 0f) {
            val cosY = cos(rotation.y)
            val sinY = sin(rotation.y)
            result = Vector3f(
                result.x * cosY - result.z * sinY,
                result.y,
                result.x * sinY + result.z * cosY
            )
        }
        
        // Rotate around X axis
        if (rotation.x != 0f) {
            val cosX = cos(rotation.x)
            val sinX = sin(rotation.x)
            result = Vector3f(
                result.x,
                result.y * cosX - result.z * sinX,
                result.y * sinX + result.z * cosX
            )
        }
        
        // Rotate around Z axis
        if (rotation.z != 0f) {
            val cosZ = cos(rotation.z)
            val sinZ = sin(rotation.z)
            result = Vector3f(
                result.x * cosZ - result.y * sinZ,
                result.x * sinZ + result.y * cosZ,
                result.z
            )
        }
        
        return result
    }
}
