# Physics System Implementation

## Overview

This implementation adds collision detection and gravity physics to the SMF game engine. The system uses AABB (Axis-Aligned Bounding Box) collision detection for simple and efficient collision checks between the player and entities.

## What Was Added

### 1. Physics Package (`src/main/kotlin/qorrnsmj/smf/physics/`)

#### `AABB.kt`
- Axis-Aligned Bounding Box collision detection class
- Methods:
  - `intersects(other: AABB)`: Check if two AABBs collide
  - `getCenter()`: Get center point of the box
  - `getSize()`: Get dimensions of the box
  - `fromCenterAndSize()`: Factory method to create AABB from center and half-extents

#### `PhysicsComponent.kt`
- Optional component for entities that need physics
- Properties:
  - `velocity`: Current velocity vector
  - `gravity`: Gravity strength (default 0.1)
  - `useGravity`: Enable/disable gravity
  - `collisionBounds`: Half-extents for collision box
  - `grounded`: Whether entity is on ground
  - `mass`: Entity mass (for future features)

#### `PhysicsSystem.kt`
- Main physics update system
- Features:
  - Applies gravity to entities with physics components
  - Updates entity positions based on velocity
  - Checks ground collision using terrain height
  - Detects player-entity collisions
  - Resolves collisions by pushing player away

### 2. Entity Updates

#### `Entity.kt`
- Added optional `physics: PhysicsComponent?` field
- Backward compatible - physics is optional

#### `StallEntity.kt`
- Added collision bounds (static object, no gravity)
- Player can no longer walk through stalls

### 3. Vector3f Enhancements

#### `Vector3f.kt`
- Added Kotlin operator overloads:
  - `+`: Add vectors
  - `-`: Subtract vectors
  - `*`: Multiply by scalar
  - `/`: Divide by scalar
  - `unaryMinus`: Negate vector

#### `Camera.kt`
- Removed duplicate operator definitions
- Now uses Vector3f operators

### 4. Level Integration

#### `Level.kt`
- Added physics update in base `update()` method
- Calls `PhysicsSystem.update()` automatically for all levels

### 5. Test Scene

#### `PhysicsTestLevel.kt`
- Test level demonstrating physics features:
  - **Falling boxes**: Test gravity
  - **Static walls**: Test collision detection
  - **Platform**: Test static elevated surfaces
- Player spawns at Y=50 to see falling objects
- Can be used to load this level in the game

## How It Works

### Physics Update Loop

```
Level.update(delta)
  └─> PhysicsSystem.update(entities, player, delta, terrain)
       ├─> For each entity with physics:
       │    ├─> Apply gravity (if enabled)
       │    ├─> Update position based on velocity
       │    └─> Check ground collision with terrain
       └─> Check player-entity collisions
            └─> Resolve collisions (push player away)
```

### Collision Detection

1. **AABB Creation**: Each entity with physics gets an AABB based on its position, scale, and collision bounds
2. **Intersection Check**: Simple axis-aligned box overlap test
3. **Collision Resolution**: Push player out on the axis with smallest overlap

### Gravity System

- Entities with `useGravity = true` accelerate downward
- Velocity accumulates: `velocity.y -= gravity * delta`
- Position updates: `position += velocity`
- Ground check stops falling when hitting terrain

## Usage

### Adding Physics to an Entity

```kotlin
val box = Entity(
    position = Vector3f(0f, 10f, 0f),
    scale = Vector3f(1f, 1f, 1f),
    physics = PhysicsComponent(
        velocity = Vector3f(0f, 0f, 0f),
        gravity = 0.15f,
        useGravity = true,
        collisionBounds = Vector3f(0.5f, 0.5f, 0.5f) // Half extents
    )
)
```

### Static Objects (No Gravity)

```kotlin
val wall = Entity(
    position = Vector3f(0f, 2f, 0f),
    physics = PhysicsComponent(
        useGravity = false,
        collisionBounds = Vector3f(5f, 2f, 1f)
    )
)
```

### Without Physics

```kotlin
val decoration = Entity(
    position = Vector3f(0f, 0f, 0f),
    physics = null  // No collision or physics
)
```

## Testing

### Manual Testing

1. Load `PhysicsTestLevel` in the game
2. Watch boxes fall and land on terrain
3. Try walking through walls - should be blocked
4. Try walking under platform - should collide

### What to Verify

- ✅ Boxes fall with gravity
- ✅ Boxes stop on ground (don't fall through)
- ✅ Player cannot walk through walls
- ✅ Player cannot walk through fallen boxes
- ✅ Collision pushes player smoothly away

## Architecture Decisions

### Why AABB?
- Simple and fast
- Good enough for box-shaped objects
- No rotation support needed yet
- Easy to debug and visualize

### Why Optional Physics?
- Not all entities need physics (decorative objects)
- Saves performance
- Backward compatible with existing code

### Why Player-Entity Only?
- Simplifies collision logic
- Entity-entity collision not needed yet
- Can be added later if needed

## Performance Notes

- Linear collision check (O(n) where n = entity count)
- Fine for small scenes (<100 entities with physics)
- Future: Add spatial partitioning (octree/grid) for larger scenes

## Future Enhancements

- [ ] Rotated bounding boxes (OBB)
- [ ] Entity-entity collision
- [ ] Spatial partitioning for performance
- [ ] Physics materials (bounce, friction)
- [ ] Impulse-based collision response
- [ ] Debug visualization for AABBs

## Files Changed

**Created:**
- `src/main/kotlin/qorrnsmj/smf/physics/AABB.kt`
- `src/main/kotlin/qorrnsmj/smf/physics/PhysicsComponent.kt`
- `src/main/kotlin/qorrnsmj/smf/physics/PhysicsSystem.kt`
- `src/main/kotlin/qorrnsmj/smf/game/level/test/PhysicsTestLevel.kt`

**Modified:**
- `src/main/kotlin/qorrnsmj/smf/math/Vector3f.kt`
- `src/main/kotlin/qorrnsmj/smf/game/camera/Camera.kt`
- `src/main/kotlin/qorrnsmj/smf/game/entity/Entity.kt`
- `src/main/kotlin/qorrnsmj/smf/game/entity/custom/StallEntity.kt`
- `src/main/kotlin/qorrnsmj/smf/game/level/Level.kt`
