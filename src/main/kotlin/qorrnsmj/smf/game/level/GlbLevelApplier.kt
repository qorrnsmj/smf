package qorrnsmj.smf.game.level

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.Entities
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.math.Vector3f
import kotlin.math.cos
import kotlin.math.sin

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
            setPlayerYaw(player, spawn.transform.rotation.y)
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

    private fun setPlayerYaw(player: Player, yawDegrees: Float) {
        val radians = Math.toRadians(yawDegrees.toDouble())
        player.camera.setFront(Vector3f(cos(radians).toFloat(), 0f, sin(radians).toFloat()))
    }

    private fun String.normalized(): String =
        lowercase().filter { it.isLetterOrDigit() }

    private val PLAYER_SPAWN_TYPES = setOf("player", "spawnplayer")
}
