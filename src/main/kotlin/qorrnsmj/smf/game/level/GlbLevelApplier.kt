package qorrnsmj.smf.game.level

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.Entities
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
import qorrnsmj.smf.game.task.trigger.ContainmentMode
import qorrnsmj.smf.game.task.trigger.TeleportFacing
import qorrnsmj.smf.game.task.trigger.TeleportLook
import qorrnsmj.smf.game.task.trigger.TeleportTrigger
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.math.Vector3f

object GlbLevelApplier {
    fun apply(
        level: GlbLevel,
        scene: Scene,
        player: Player,
        onAreaTriggerEvent: (String, GlbTrigger) -> Unit = { eventName, _ ->
            Logger.info("GLB area trigger fired: {}", eventName)
        },
    ): List<AreaEnterTrigger> {
        level.terrain?.let { terrain ->
            scene.entities.add(terrain.entity)
            scene.terrainHeightProvider = terrain
        }

        scene.entities.addAll(level.staticMeshes)
        level.entitySpawns.forEach { spawnEntity(it, scene, player) }

        return level.areaTriggers.map { trigger ->
            createAreaTrigger(trigger, player, onAreaTriggerEvent)
        }
    }

    private fun createAreaTrigger(
        trigger: GlbTrigger,
        player: Player,
        onAreaTriggerEvent: (String, GlbTrigger) -> Unit,
    ): AreaEnterTrigger {
        val containmentMode = trigger.properties.containmentMode()
        val destination = trigger.properties.vector3(
            "teleportDestination",
            "destinationFeet",
            "destination",
            "tpDestination",
            "teleportTo",
            "tpTo",
        )

        if (destination != null) {
            return TeleportTrigger(
                areaCenter = trigger.center,
                areaHalfExtents = trigger.halfExtents,
                player = player,
                destinationFeet = destination,
                facing = trigger.properties.teleportFacing(),
                look = trigger.properties.teleportLook(),
                containmentMode = containmentMode,
                onTeleport = {
                    Logger.info("GLB teleport trigger fired: {} -> {}", trigger.name, destination)
                    onAreaTriggerEvent(trigger.eventName, trigger)
                },
            )
        }

        return object : AreaEnterTrigger(
            areaCenter = trigger.center,
            areaHalfExtents = trigger.halfExtents,
            aabb = { player.getAabb() },
            containmentMode = containmentMode,
        ) {
            override fun fire() {
                onAreaTriggerEvent(trigger.eventName, trigger)
            }
        }
    }

    private fun spawnEntity(spawn: GlbEntity, scene: Scene, player: Player) {
        if (spawn.type.normalized() in PLAYER_SPAWN_TYPES) {
            player.setFeetPosition(spawn.transform.position)
            setPlayerFront(player, spawn.transform.rotation.rotate(Vector3f(1f, 0f, 0f)))
            Logger.info("GLB player spawn applied: {}", spawn.name)
            return
        }

        val entity = Entities.create(spawn.type, spawn.transform, spawn.properties)
        if (entity == null) {
            Logger.info("GLB entity skipped: {} ({})", spawn.name, spawn.type)
            return
        }

        scene.entities.add(entity)
        Logger.info("GLB entity spawned: {} ({})", spawn.name, spawn.type)
    }

    private fun setPlayerFront(player: Player, front: Vector3f) {
        val horizontalFront = Vector3f(front.x, 0f, front.z)
        if (horizontalFront.lengthSquared() > 0f) {
            player.camera.setFront(horizontalFront)
        }
    }

    private fun String.normalized(): String =
        lowercase().filter { it.isLetterOrDigit() }

    private fun Map<String, String>.containmentMode(): ContainmentMode {
        return when (string("containmentMode", "containment").normalized()) {
            "center", "centerinside", "feet", "feetinside" -> ContainmentMode.CENTER_INSIDE
            "full", "fullyinside", "inside", "contained" -> ContainmentMode.FULLY_INSIDE
            else -> ContainmentMode.TOUCHING
        }
    }

    private fun Map<String, String>.teleportFacing(): TeleportFacing {
        val mode = string("facing", "teleportFacing").normalized()
        return when {
            mode == "lookat" -> vector3("facingTarget", "teleportFacingTarget")
                ?.let { TeleportFacing.LookAt(it) }
                ?: TeleportFacing.Preserve
            mode == "direction" || mode == "dir" -> vector3("facingDirection", "teleportFacingDirection")
                ?.let { TeleportFacing.Direction(it) }
                ?: TeleportFacing.Preserve
            vector3("facingTarget", "teleportFacingTarget") != null ->
                TeleportFacing.LookAt(vector3("facingTarget", "teleportFacingTarget")!!)
            vector3("facingDirection", "teleportFacingDirection") != null ->
                TeleportFacing.Direction(vector3("facingDirection", "teleportFacingDirection")!!)
            else -> TeleportFacing.Preserve
        }
    }

    private fun Map<String, String>.teleportLook(): TeleportLook {
        val mode = string("look", "teleportLook").normalized()
        return when {
            mode == "preserve" || mode == "keep" -> TeleportLook.Preserve
            mode == "lookat" -> vector3("lookTarget", "teleportLookTarget")
                ?.let { TeleportLook.LookAt(it) }
                ?: TeleportLook.SameAsFacing
            mode == "direction" || mode == "dir" -> vector3("lookDirection", "teleportLookDirection")
                ?.let { TeleportLook.Direction(it) }
                ?: TeleportLook.SameAsFacing
            vector3("lookTarget", "teleportLookTarget") != null ->
                TeleportLook.LookAt(vector3("lookTarget", "teleportLookTarget")!!)
            vector3("lookDirection", "teleportLookDirection") != null ->
                TeleportLook.Direction(vector3("lookDirection", "teleportLookDirection")!!)
            else -> TeleportLook.SameAsFacing
        }
    }

    private fun Map<String, String>.string(vararg keys: String): String =
        keys.firstNotNullOfOrNull { this[it] } ?: ""

    private fun Map<String, String>.vector3(vararg keys: String): Vector3f? {
        for (key in keys) {
            val vector = this[key]?.toVector3f()
            if (vector != null) return vector
        }
        return null
    }

    private fun String.toVector3f(): Vector3f? {
        val parts = trim()
            .removePrefix("[")
            .removeSuffix("]")
            .split(',', ' ', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (parts.size < 3) return null
        return Vector3f(
            parts[0].toFloatOrNull() ?: return null,
            parts[1].toFloatOrNull() ?: return null,
            parts[2].toFloatOrNull() ?: return null,
        )
    }

    private val PLAYER_SPAWN_TYPES = setOf("player", "spawnplayer")
}
