package qorrnsmj.smf.game.event

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.task.Task
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
import qorrnsmj.smf.math.Vector3f
import kotlin.math.cos
import kotlin.math.sin

object EventAreaTypes {
    private val types = mutableMapOf<String, EventAreaType>()

    init {
        register(SpawnPointEvent)
        register(AreaTriggerEvent)
    }

    fun register(type: EventAreaType) {
        types[type.category] = type
    }

    fun load(category: String, definition: EventAreaDefinition, context: EventLoadContext): List<Task> {
        val type = types[category]
        if (type == null) {
            Logger.warn("Event area skipped: unknown category={} id={} name={}", category, definition.id, definition.name)
            return emptyList()
        }
        return type.load(definition, context)
    }
}

object SpawnPointEvent : EventAreaType {
    override val category: String = "spawn_points"

    override fun load(definition: EventAreaDefinition, context: EventLoadContext): List<Task> {
        context.player.setFeetPosition(definition.position)
        setPlayerYaw(context.player, definition.rotation.y)
        Logger.info("Spawn point applied: {}", definition.name)
        return emptyList()
    }

    private fun setPlayerYaw(player: qorrnsmj.smf.game.entity.player.Player, yawDegrees: Float) {
        val radians = Math.toRadians(yawDegrees.toDouble())
        player.camera.setFront(Vector3f(cos(radians).toFloat(), 0f, sin(radians).toFloat()))
    }
}

object AreaTriggerEvent : EventAreaType {
    override val category: String = "area_triggers"

    override fun load(definition: EventAreaDefinition, context: EventLoadContext): List<Task> {
        val halfExtents = definition.size.scale(0.5f)
        return listOf(
            object : AreaEnterTrigger(
                areaCenter = definition.position,
                areaHalfExtents = halfExtents,
                aabb = { context.player.getAabb() },
            ) {
                override fun fire() {
                    context.onAreaTriggerEvent(definition)
                }
            }
        )
    }
}
