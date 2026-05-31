package qorrnsmj.smf.physics.collision.data

import qorrnsmj.smf.game.entity.custom.Entity

/**
 * Represents a collision between two entities.
 */
data class CollisionPair(
    val entity1: Entity,
    val entity2: Entity,
    val result: CollisionResult
)
