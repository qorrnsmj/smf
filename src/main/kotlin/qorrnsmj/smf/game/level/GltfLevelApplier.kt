package qorrnsmj.smf.game.level

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.custom.StallEntity
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.math.Vector3f
import kotlin.math.cos
import kotlin.math.sin

object GltfLevelApplier {
    fun apply(
        level: GltfLevel,
        scene: Scene,
        player: Player,
        onAreaTriggerEvent: (String, GltfTrigger) -> Unit = { eventName, _ ->
            Logger.info("glTF area trigger fired: {}", eventName)
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

    private fun spawnEntity(spawn: GltfEntity, scene: Scene, player: Player) {
        when (spawn.type.normalized()) {
            "player", "spawnplayer" -> {
                player.setFeetPosition(spawn.transform.position)
                setPlayerYaw(player, spawn.transform.rotation.y)
                Logger.info("glTF player spawn applied: {}", spawn.name)
            }

            "stall", "stallentity" -> {
                scene.entities.add(
                    StallEntity().apply {
                        localTransform = spawn.transform
                    }
                )
                Logger.info("glTF entity spawned: {} ({})", spawn.name, spawn.type)
            }

            else -> Logger.info("glTF entity skipped: {} ({})", spawn.name, spawn.type)
        }
    }

    private fun setPlayerYaw(player: Player, yawDegrees: Float) {
        val radians = Math.toRadians(yawDegrees.toDouble())
        player.camera.setFront(Vector3f(cos(radians).toFloat(), 0f, sin(radians).toFloat()))
    }

    private fun String.normalized(): String =
        lowercase().filter { it.isLetterOrDigit() }
}
