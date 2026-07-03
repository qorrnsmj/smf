package qorrnsmj.smf.game.event

import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.task.Task
import qorrnsmj.smf.graphic.Scene

interface EventAreaType {
    val category: String

    fun load(definition: EventAreaDefinition, context: EventLoadContext): List<Task>
}

data class EventLoadContext(
    val scene: Scene,
    val player: Player,
    val onAreaTriggerEvent: (EventAreaDefinition) -> Unit = {},
)
