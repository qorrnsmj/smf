package qorrnsmj.smf.game.level

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.Entities
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
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
            object : AreaEnterTrigger(
                areaCenter = trigger.center,
                areaHalfExtents = trigger.halfExtents,
                aabb = { player.getAabb() },
            ) {
                override fun fire() {
                    onAreaTriggerEvent(trigger.eventName, trigger)
                }
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

    private val PLAYER_SPAWN_TYPES = setOf("player", "spawnplayer")
}
