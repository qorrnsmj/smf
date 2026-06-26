package qorrnsmj.smf.physics

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.custom.Collidable
import qorrnsmj.smf.game.entity.custom.CollisionShape
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.graphic.terrain.HeightProvider
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.component.DynamicPhysics
import qorrnsmj.smf.physics.component.KinematicPhysics
import qorrnsmj.smf.physics.component.StaticPhysics
import qorrnsmj.smf.physics.collision.data.AABB
import qorrnsmj.smf.physics.collision.shape.BoxCollider
import qorrnsmj.smf.physics.collision.CollisionDetection
import qorrnsmj.smf.physics.collision.shape.CapsuleCollider
import qorrnsmj.smf.physics.collision.shape.ConvexHullCollider
import qorrnsmj.smf.physics.collision.shape.SphereCollider
import kotlin.math.abs
import kotlin.math.acos

/**
 * Main physics simulation manager for the SMF engine
 * Handles physics updates, collision detection, and force calculations
 * Follows SMF's singleton pattern like AudioManager
 */
object PhysicsWorld {
    // Physics constants
    const val GRAVITY_STRENGTH = 1960f
    const val GRAVITY_EFFECT_SCALE = 1f / (60f * 60f)
    val GRAVITY_VECTOR = Vector3f(0f, -GRAVITY_STRENGTH, 0f)

    private const val COLLISION_SKIN = 0.001f
    private const val HORIZONTAL_PRIORITY_BIAS = 0.01f
    private const val PENETRATION_EPSILON = 0.0005f
    private const val RESTING_HORIZONTAL_SPEED_EPSILON = 0.0001f
    private const val MAX_STEP_HEIGHT = 32f
    private const val GROUND_SNAP_DOWN_DISTANCE = 4f
    private const val WALKABLE_SURFACE_MIN_Y = 0.5f
    private const val SLIDE_SURFACE_MIN_ANGLE_DEGREES = 45.0
    private val SLIDE_SURFACE_TEXTURES = setOf("grass")

    // Performance metrics
    private var lastUpdateTime = 0L
    private var physicsUpdateCount = 0L

    /**
     * Update the physics simulation
     */
    fun update(entities: List<Entity>, heightProvider: HeightProvider?, delta: Float) {
        val startTime = System.nanoTime()
        
        if (entities.isNotEmpty()) {
            resetGroundedState(entities)

            applyForces(entities)
            
            integrateMotion(entities, delta)
            
            handleEntityCollisions(entities)

            resolveCollidableEntityCollisions(entities)
            
            if (heightProvider != null) {
                handleTerrainCollisions(entities, heightProvider)
            }
            
            clearForces(entities)
        }
        
        // Update performance metrics
        lastUpdateTime = System.nanoTime() - startTime
        physicsUpdateCount++
    }

    private fun resetGroundedState(entities: List<Entity>) {
        for (entity in entities) {
            val physics = entity.physicsComponent
            if (physics !is StaticPhysics) {
                physics.isGrounded = false
            }
        }
    }
    
    /**
     * Apply forces to all physics entities
     */
    private fun applyForces(entities: List<Entity>) {
        for (entity in entities) {
            val physics = entity.physicsComponent

            if (physics !is DynamicPhysics) continue

            // Apply gravity
            physics.applyForce(GRAVITY_VECTOR.scale(physics.mass * GRAVITY_EFFECT_SCALE))
            
            // Drag is disabled in simple physics mode.
            // if (physics.drag > 0f && physics.velocity.length() > 0f) {
            //     val dragForce = physics.velocity.normalize().scale(-physics.drag * physics.velocity.length() * physics.velocity.length())
            //     physics.applyForce(dragForce)
            // }
        }
    }
    
    /**
     * Integrate motion using semi-implicit Euler method
     * Now supports hierarchical physics propagation
     */
    private fun integrateMotion(entities: List<Entity>, delta: Float) {
        for (entity in entities) {
            val physics = entity.physicsComponent

            when (physics) {
                is StaticPhysics -> continue

                is DynamicPhysics -> {
                    // Calculate acceleration from forces
                    physics.calculateAcceleration()

                    // Update velocity: v = v + a * dt
                    physics.velocity = physics.velocity.add(physics.acceleration.scale(delta))

                    // Update position: p = p + v * dt (root entities only)
                    if (entity.parent == null) {
                        val newPosition = entity.localTransform.position.add(physics.velocity.scale(delta))
                        entity.localTransform = entity.localTransform.copy(position = newPosition)

                        // Propagate physics result to all children
                        propagatePhysicsToChildren(entity, physics.velocity, delta)
                    }
                }

                is KinematicPhysics -> {
                    if (entity.parent == null) {
                        val newPosition = entity.localTransform.position.add(physics.velocity.scale(delta))
                        entity.localTransform = entity.localTransform.copy(position = newPosition)
                        propagatePhysicsToChildren(entity, physics.velocity, delta)
                    }
                }
            }
            
            // Angular motion is disabled in simple physics mode.
            // entity.rotation = entity.rotation.add(physics.angularVelocity.scale(delta))
        }
    }
    
    /**
     * Propagate parent's physics motion to all child entities recursively
     */
    private fun propagatePhysicsToChildren(parent: Entity, parentVelocity: Vector3f, delta: Float) {
        for (child in parent.children) {
            val childPhysics = child.physicsComponent

            if (childPhysics !is StaticPhysics) {
                // Child inherits parent velocity plus its own relative motion.
                val inheritedVelocity = parentVelocity.add(childPhysics.velocity)

                // Update child's world position based on inherited motion
                val childWorldPos = child.worldTransform.position
                val newChildWorldPos = childWorldPos.add(inheritedVelocity.scale(delta))
                val parentWorld = child.parent?.worldTransform
                val newLocalPos = if (parentWorld != null) {
                    newChildWorldPos.subtract(parentWorld.position).divide(parentWorld.scale)
                } else {
                    newChildWorldPos
                }
                child.localTransform = child.localTransform.copy(position = newLocalPos)
            }

            // Recursively propagate to grandchildren
            propagatePhysicsToChildren(child, parentVelocity, delta)
        }
    }
    
    /**
     * Handle entity-entity collisions
     */
    private fun handleEntityCollisions(entities: List<Entity>) {
        val collisions = CollisionDetection.detectCollisions(entities)
        CollisionDetection.resolveCollisions(collisions)
    }
    
    /**
     * Handle terrain collisions for all entities
     */
    private fun handleTerrainCollisions(entities: List<Entity>, heightProvider: HeightProvider) {
        for (entity in entities) {
            val physics = entity.physicsComponent

            if (physics is StaticPhysics) continue

            val entityWorldPos = entity.worldTransform.position
            val groundHeight = heightProvider.getHeight(entityWorldPos.x, entityWorldPos.z)
            
            // Check if entity is below ground
            if (entityWorldPos.y <= groundHeight) {
                // Position correction
                entity.localTransform = entity.localTransform.copy(position = Vector3f(entityWorldPos.x, groundHeight, entityWorldPos.z))
                
                // Ground contact response
                physics.isGrounded = true
                
                // Ground stop only in simple physics mode.
                if (physics.velocity.y < 0f) {
                    physics.velocity = Vector3f(physics.velocity.x, 0f, physics.velocity.z)
                }
            }
        }
    }

    private fun resolveCollidableEntityCollisions(entities: List<Entity>) {
        for (collidable in entities.filterIsInstance<Collidable>()) {
            val entity = collidable as Entity
            val physics = entity.physicsComponent
            if (physics is StaticPhysics) continue

            var groundedByCollider = false

            repeat(3) {
                var corrected = false

                for (obstacle in entities) {
                    if (obstacle === entity) continue

                    val correction = computeCollidableCorrection(collidable, obstacle)

                    if (correction != null) {
                        val surfaceNormal = if (correction.y > 0f) correction.normalize() else null
                        if (surfaceNormal == null && tryStepUp(entity, collidable, obstacle)) {
                            corrected = true
                            groundedByCollider = true
                            if (physics.velocity.y < 0f) {
                                physics.velocity = Vector3f(physics.velocity.x, 0f, physics.velocity.z)
                            }
                            continue
                        }
                        val correctionToApply = if (physics is DynamicPhysics && surfaceNormal != null) {
                            getSlopeAwareCorrection(physics, correction, surfaceNormal)
                        } else {
                            correction
                        }
                        entity.localTransform = entity.localTransform.copy(position = entity.localTransform.position.add(correctionToApply))
                        corrected = true

                        if (correction.y > 0f) {
                            if (physics is DynamicPhysics) {
                                applySlopeContactBehavior(physics, surfaceNormal!!)
                            }
                            groundedByCollider = groundedByCollider || surfaceNormal == null || isWalkableSlope(surfaceNormal)
                            if ((physics !is DynamicPhysics || groundedByCollider) && physics.velocity.y < 0f) {
                                physics.velocity = Vector3f(physics.velocity.x, 0f, physics.velocity.z)
                            }
                        }
                    }
                }

                if (!corrected) return@repeat
            }

            if (groundedByCollider || hasGroundSupport(entity, collidable, entities)) {
                physics.isGrounded = true
            }
        }
    }

    private fun hasGroundSupport(entity: Entity, collidable: Collidable, entities: List<Entity>): Boolean {
        for (obstacle in entities) {
            if (obstacle === entity) continue

            val correction = computeCollidableCorrection(collidable, obstacle, -collidable.collisionConfig.groundProbeDistance)

            if (correction != null && correction.y > 0f && isWalkableSlope(correction.normalize())) {
                return true
            }
        }

        return false
    }

    private fun tryStepUp(entity: Entity, collidable: Collidable, obstacle: Entity): Boolean {
        if (collidable.collisionConfig.shape != CollisionShape.CAPSULE) {
            return false
        }

        val collider = obstacle.physicsComponent.collider ?: return false
        val topY = getColliderTopY(collider, obstacle.worldTransform.position) ?: return false
        val feetY = collidable.getCollisionBasePosition().y
        val stepHeight = topY - feetY

        if (stepHeight <= 0f || stepHeight > MAX_STEP_HEIGHT) {
            return false
        }

        entity.localTransform = entity.localTransform.copy(
            position = entity.localTransform.position.add(Vector3f(0f, stepHeight + COLLISION_SKIN, 0f))
        )
        return true
    }

    private fun getColliderTopY(
        collider: qorrnsmj.smf.physics.collision.shape.Collider,
        position: Vector3f,
    ): Float? {
        return when (collider) {
            is BoxCollider -> collider.getBounds(position).max.y
            is ConvexHullCollider -> collider.getBounds(position).max.y
            is SphereCollider -> collider.getCenter(position).y + collider.radius
            is CapsuleCollider -> collider.getBounds(position).max.y
            else -> null
        }
    }

    private fun computeCollidableCorrection(
        collidable: Collidable,
        obstacle: Entity,
        feetOffset: Float = 0f,
    ): Vector3f? {
        val collider = obstacle.physicsComponent.collider ?: return null
        val obstaclePosition = obstacle.worldTransform.position

        return when (collidable.collisionConfig.shape) {
            CollisionShape.AABB -> {
                val bounds = collidable.getAabbWithFeetOffset(feetOffset)
                computeAabbObstacleCorrection(bounds, collider, obstaclePosition)
            }

            CollisionShape.CAPSULE -> {
                val (capsule, capsulePosition) = collidable.getCapsuleColliderWithFeetOffset(feetOffset)
                computeCapsuleObstacleCorrection(capsule, capsulePosition, collider, obstaclePosition)
            }
        }
    }

    private fun computeAabbObstacleCorrection(
        entityBounds: AABB,
        collider: qorrnsmj.smf.physics.collision.shape.Collider,
        obstaclePosition: Vector3f,
    ): Vector3f? {
        return when (collider) {
            is BoxCollider -> computeBoxCorrection(entityBounds, collider.getBounds(obstaclePosition))
            is ConvexHullCollider -> computeConvexHullCorrection(entityBounds, collider, obstaclePosition)
            is SphereCollider -> computeSphereCorrection(entityBounds, collider.getCenter(obstaclePosition), collider.radius)
            is CapsuleCollider -> computeCapsuleCorrection(entityBounds, collider, obstaclePosition)
            else -> null
        }
    }

    private fun computeCapsuleObstacleCorrection(
        capsule: CapsuleCollider,
        capsulePosition: Vector3f,
        collider: qorrnsmj.smf.physics.collision.shape.Collider,
        obstaclePosition: Vector3f,
    ): Vector3f? {
        return when (collider) {
            is BoxCollider -> CapsuleCollider.getCapsuleBoxCorrection(capsule, capsulePosition, collider.getBounds(obstaclePosition))
                ?.let { it.addSkin() }
            is ConvexHullCollider -> computeCharacterCapsuleConvexHullCorrection(capsule, capsulePosition, collider, obstaclePosition)
            is SphereCollider -> computeSphereCorrection(capsule, capsulePosition, collider.getCenter(obstaclePosition), collider.radius)
            is CapsuleCollider -> computeCapsuleCorrection(capsule, capsulePosition, collider, obstaclePosition)
            else -> null
        }
    }

    private fun computeBoxCorrection(entityBounds: AABB, obstacleBounds: AABB): Vector3f? {
        if (!entityBounds.intersects(obstacleBounds)) {
            return null
        }

        val overlapX = minOf(entityBounds.max.x - obstacleBounds.min.x, obstacleBounds.max.x - entityBounds.min.x)
        val overlapY = minOf(entityBounds.max.y - obstacleBounds.min.y, obstacleBounds.max.y - entityBounds.min.y)
        val overlapZ = minOf(entityBounds.max.z - obstacleBounds.min.z, obstacleBounds.max.z - entityBounds.min.z)

        if (overlapX <= 0f || overlapY <= 0f || overlapZ <= 0f) {
            return null
        }

        val minOverlap = minOf(overlapX, minOf(overlapY, overlapZ))
        if (minOverlap <= PENETRATION_EPSILON) {
            return null
        }

        val center = Vector3f(
            (entityBounds.min.x + entityBounds.max.x) * 0.5f,
            (entityBounds.min.y + entityBounds.max.y) * 0.5f,
            (entityBounds.min.z + entityBounds.max.z) * 0.5f
        )
        val obstacleCenter = Vector3f(
            (obstacleBounds.min.x + obstacleBounds.max.x) * 0.5f,
            (obstacleBounds.min.y + obstacleBounds.max.y) * 0.5f,
            (obstacleBounds.min.z + obstacleBounds.max.z) * 0.5f
        )

        val minHorizontalOverlap = minOf(overlapX, overlapZ)
        val resolveHorizontally = minHorizontalOverlap <= overlapY + HORIZONTAL_PRIORITY_BIAS

        return if (resolveHorizontally) {
            if (overlapX <= overlapZ) {
                Vector3f(if (center.x >= obstacleCenter.x) 1f else -1f, 0f, 0f).scale(overlapX + COLLISION_SKIN)
            } else {
                Vector3f(0f, 0f, if (center.z >= obstacleCenter.z) 1f else -1f).scale(overlapZ + COLLISION_SKIN)
            }
        } else {
            Vector3f(0f, if (center.y >= obstacleCenter.y) 1f else -1f, 0f).scale(overlapY + COLLISION_SKIN)
        }
    }

    private fun computeSphereCorrection(entityBounds: AABB, sphereCenter: Vector3f, sphereRadius: Float): Vector3f? {
        val closest = Vector3f(
            clamp(sphereCenter.x, entityBounds.min.x, entityBounds.max.x),
            clamp(sphereCenter.y, entityBounds.min.y, entityBounds.max.y),
            clamp(sphereCenter.z, entityBounds.min.z, entityBounds.max.z)
        )

        val direction = closest.subtract(sphereCenter)
        val distance = direction.length()
        if (distance >= sphereRadius) {
            return null
        }

        val penetration = sphereRadius - distance
        val normal = if (distance > 0f) direction.divide(distance) else Vector3f(0f, 1f, 0f)
        return normal.scale(penetration + COLLISION_SKIN)
    }

    private fun computeSphereCorrection(
        capsule: CapsuleCollider,
        capsulePosition: Vector3f,
        sphereCenter: Vector3f,
        sphereRadius: Float,
    ): Vector3f? {
        val closest = capsule.closestPointOnAxis(sphereCenter, capsulePosition)
        val direction = closest.subtract(sphereCenter)
        val distance = direction.length()
        val combinedRadius = capsule.radius + sphereRadius

        if (distance >= combinedRadius) {
            return null
        }

        val normal = if (distance > 0f) direction.divide(distance) else Vector3f(0f, 1f, 0f)
        return normal.scale(combinedRadius - distance + COLLISION_SKIN)
    }

    private fun computeCapsuleCorrection(
        entityBounds: AABB,
        capsule: CapsuleCollider,
        capsulePosition: Vector3f,
    ): Vector3f? {
        val closest = Vector3f(
            clamp(capsule.getCenter(capsulePosition).x, entityBounds.min.x, entityBounds.max.x),
            clamp(capsule.getCenter(capsulePosition).y, entityBounds.min.y, entityBounds.max.y),
            clamp(capsule.getCenter(capsulePosition).z, entityBounds.min.z, entityBounds.max.z),
        )
        val pointOnCapsule = capsule.closestPointOnAxis(closest, capsulePosition)
        val direction = closest.subtract(pointOnCapsule)
        val distance = direction.length()

        if (distance >= capsule.radius) {
            return null
        }

        val normal = if (distance > 0f) direction.divide(distance) else Vector3f(0f, 1f, 0f)
        return normal.scale(capsule.radius - distance + COLLISION_SKIN)
    }

    private fun computeCapsuleCorrection(
        capsule: CapsuleCollider,
        capsulePosition: Vector3f,
        obstacleCapsule: CapsuleCollider,
        obstaclePosition: Vector3f,
    ): Vector3f? {
        val center = capsule.getCenter(capsulePosition)
        val closestOnOther = obstacleCapsule.closestPointOnAxis(center, obstaclePosition)
        val closestOnThis = capsule.closestPointOnAxis(closestOnOther, capsulePosition)
        val direction = closestOnThis.subtract(closestOnOther)
        val distance = direction.length()
        val combinedRadius = capsule.radius + obstacleCapsule.radius

        if (distance >= combinedRadius) {
            return null
        }

        val normal = if (distance > 0f) direction.divide(distance) else Vector3f(0f, 1f, 0f)
        return normal.scale(combinedRadius - distance + COLLISION_SKIN)
    }

    private fun computeConvexHullCorrection(
        entityBounds: AABB,
        hull: ConvexHullCollider,
        hullPosition: Vector3f,
    ): Vector3f? {
        val center = Vector3f(
            (entityBounds.min.x + entityBounds.max.x) * 0.5f,
            (entityBounds.min.y + entityBounds.max.y) * 0.5f,
            (entityBounds.min.z + entityBounds.max.z) * 0.5f,
        )
        val halfExtents = Vector3f(
            (entityBounds.max.x - entityBounds.min.x) * 0.5f,
            (entityBounds.max.y - entityBounds.min.y) * 0.5f,
            (entityBounds.max.z - entityBounds.min.z) * 0.5f,
        )

        var bestNormal: Vector3f? = null
        var bestPenetration = Float.POSITIVE_INFINITY

        for (plane in hull.planes) {
            val shiftedDistance = plane.distance + plane.normal.dot(hullPosition)
            val centerDistance = plane.normal.dot(center) - shiftedDistance
            val projectedRadius =
                abs(plane.normal.x) * halfExtents.x +
                    abs(plane.normal.y) * halfExtents.y +
                    abs(plane.normal.z) * halfExtents.z

            if (centerDistance > projectedRadius) {
                return null
            }

            if (plane.isGhost) {
                continue
            }

            val penetration = projectedRadius - centerDistance
            if (penetration < bestPenetration) {
                bestPenetration = penetration
                bestNormal = plane.normal
            }
        }

        val normal = bestNormal ?: return null
        if (bestPenetration <= PENETRATION_EPSILON) {
            return null
        }

        return normal.scale(bestPenetration + COLLISION_SKIN)
    }

    private fun computeConvexHullCorrection(
        capsule: CapsuleCollider,
        capsulePosition: Vector3f,
        hull: ConvexHullCollider,
        hullPosition: Vector3f,
    ): Vector3f? {
        val center = capsule.getCenter(capsulePosition)
        val segmentHalfLength = (capsule.height - capsule.radius * 2f) * 0.5f

        var bestNormal: Vector3f? = null
        var bestPenetration = Float.POSITIVE_INFINITY

        for (plane in hull.planes) {
            val shiftedDistance = plane.distance + plane.normal.dot(hullPosition)
            val centerDistance = plane.normal.dot(center) - shiftedDistance
            val projectedRadius = capsule.radius + abs(plane.normal.y) * segmentHalfLength

            if (centerDistance > projectedRadius) {
                return null
            }

            if (plane.isGhost) {
                continue
            }

            val penetration = projectedRadius - centerDistance
            if (penetration < bestPenetration) {
                bestPenetration = penetration
                bestNormal = plane.normal
            }
        }

        val normal = bestNormal ?: return null
        if (bestPenetration <= PENETRATION_EPSILON) {
            return null
        }

        return normal.scale(bestPenetration + COLLISION_SKIN)
    }

    private fun computeCharacterCapsuleConvexHullCorrection(
        capsule: CapsuleCollider,
        capsulePosition: Vector3f,
        hull: ConvexHullCollider,
        hullPosition: Vector3f,
    ): Vector3f? {
        val groundCorrection = computeWalkableHullSurfaceCorrection(capsule, capsulePosition, hull, hullPosition)
        if (groundCorrection != null) {
            return groundCorrection
        }

        return computeConvexHullCorrection(capsule, capsulePosition, hull, hullPosition)
            ?.let { correction ->
                if (correction.y > 0f) {
                    null
                } else {
                    correction
                }
            }
    }

    private fun computeWalkableHullSurfaceCorrection(
        capsule: CapsuleCollider,
        capsulePosition: Vector3f,
        hull: ConvexHullCollider,
        hullPosition: Vector3f,
    ): Vector3f? {
        var bestSurfaceY: Float? = null
        var bestPlane: qorrnsmj.smf.physics.collision.shape.Plane? = null
        val supportPoints = getCapsuleSupportPoints(capsulePosition)

        for (plane in hull.planes) {
            if (plane.isGhost) {
                continue
            }

            if (plane.normal.y < WALKABLE_SURFACE_MIN_Y) {
                continue
            }

            for (supportPoint in supportPoints) {
                val shiftedDistance = plane.distance + plane.normal.dot(hullPosition)
                val surfaceY = (shiftedDistance - plane.normal.x * supportPoint.x - plane.normal.z * supportPoint.z) / plane.normal.y
                val surfacePoint = Vector3f(supportPoint.x, surfaceY, supportPoint.z)

                if (!isPointInsideHull(surfacePoint, hull, hullPosition, ignoredPlane = plane)) {
                    continue
                }

                if (bestSurfaceY == null || surfaceY > bestSurfaceY) {
                    bestSurfaceY = surfaceY
                    bestPlane = plane
                }
            }
        }

        val surfaceY = bestSurfaceY ?: return null
        val plane = bestPlane ?: return null
        val feetY = capsulePosition.y
        val verticalDelta = surfaceY - feetY

        if (verticalDelta < -GROUND_SNAP_DOWN_DISTANCE || verticalDelta > MAX_STEP_HEIGHT) {
            return null
        }

        return if (isSlidingSurface(plane)) {
            plane.normal.scale((verticalDelta + COLLISION_SKIN) / plane.normal.y)
        } else {
            Vector3f(0f, verticalDelta + COLLISION_SKIN, 0f)
        }
    }

    private fun getCapsuleSupportPoints(capsulePosition: Vector3f): List<Vector3f> {
        return listOf(capsulePosition)
    }

    private fun isPointInsideHull(
        point: Vector3f,
        hull: ConvexHullCollider,
        hullPosition: Vector3f,
        ignoredPlane: qorrnsmj.smf.physics.collision.shape.Plane,
    ): Boolean {
        for (plane in hull.planes) {
            if (plane === ignoredPlane) continue

            val shiftedDistance = plane.distance + plane.normal.dot(hullPosition)
            if (plane.normal.dot(point) - shiftedDistance > COLLISION_SKIN) {
                return false
            }
        }

        return true
    }

    private fun isSlidingSurface(plane: qorrnsmj.smf.physics.collision.shape.Plane): Boolean {
        return plane.texture in SLIDE_SURFACE_TEXTURES &&
            getSlopeAngle(plane.normal) >= Math.toRadians(SLIDE_SURFACE_MIN_ANGLE_DEGREES).toFloat()
    }

    private fun Vector3f.addSkin(): Vector3f {
        val length = length()
        if (length <= 0f) {
            return this
        }
        return normalize().scale(length + COLLISION_SKIN)
    }

    private fun applySlopeContactBehavior(
        physics: DynamicPhysics,
        surfaceNormal: Vector3f,
    ) {
        cancelDownwardVelocityOnShallowSlope(physics, surfaceNormal)
    }

    private fun cancelDownwardVelocityOnShallowSlope(
        physics: DynamicPhysics,
        surfaceNormal: Vector3f,
    ) {
        if (getSlopeAngle(surfaceNormal) >= Math.toRadians(45.0).toFloat()) return
        if (physics.velocity.y < 0f) {
            physics.velocity = Vector3f(physics.velocity.x, 0f, physics.velocity.z)
        }
    }

    private fun getSlopeAwareCorrection(
        physics: DynamicPhysics,
        correction: Vector3f,
        surfaceNormal: Vector3f,
    ): Vector3f {
        if (surfaceNormal.y >= 0.9999f) {
            return correction
        }

        if (getSlopeAngle(surfaceNormal) >= Math.toRadians(45.0).toFloat()) {
            return correction
        }

        if (surfaceNormal.y <= 0.0001f) {
            return correction
        }

        val verticalCorrection = correction.length() / surfaceNormal.y
        return Vector3f(0f, verticalCorrection, 0f)
    }

    private fun getSlopeAngle(surfaceNormal: Vector3f): Float {
        val up = Vector3f(0f, 1f, 0f)
        val cosAngle = surfaceNormal.dot(up).coerceIn(-1f, 1f)
        return acos(cosAngle)
    }

    private fun isWalkableSlope(surfaceNormal: Vector3f): Boolean {
        return getSlopeAngle(surfaceNormal) < Math.toRadians(45.0).toFloat()
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
    
    /**
     * Clear forces from all physics entities
     */
    private fun clearForces(entities: List<Entity>) {
        entities.forEach { entity ->
            val physics = entity.physicsComponent
            if (physics is DynamicPhysics) {
                physics.clearForces()
            }
        }
    }
    
    /**
     * Get physics performance statistics
     */
    fun getStats(): PhysicsStats {
        return PhysicsStats(
            updateCount = physicsUpdateCount,
            lastUpdateTimeNs = lastUpdateTime
        )
    }

    /**
     * Data class for physics performance statistics
     */
    data class PhysicsStats(
        val updateCount: Long,
        val lastUpdateTimeNs: Long
    ) {
        val lastUpdateTimeMs: Float get() = lastUpdateTimeNs / 1_000_000f
    }
}
