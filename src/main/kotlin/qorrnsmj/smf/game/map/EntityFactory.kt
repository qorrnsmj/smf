package qorrnsmj.smf.game.map

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.math.Vector3f
import kotlin.math.cos
import kotlin.math.sin

object EntityFactory {
    fun spawn(mapEntities: List<MapEntity>, scene: Scene, player: Player? = null) {
        for (entity in mapEntities) {
            when (entity.classname) {
                "spawn_player" -> spawnPlayer(entity, player)
                else -> Logger.info("Map entity skipped: {}", entity.classname)
            }
        }
    }

    private fun spawnPlayer(entity: MapEntity, player: Player?) {
        val targetPlayer = player ?: return
        val origin = entity.properties["origin"]?.let(MapLoader::parseOrigin) ?: Vector3f()
        targetPlayer.setFeetPosition(origin)

        entity.properties["angle"]?.toFloatOrNull()?.let { angle ->
            val radians = Math.toRadians(angle.toDouble())
            targetPlayer.camera.setFront(Vector3f(cos(radians).toFloat(), 0f, sin(radians).toFloat()))
        }
    }
}
