package qorrnsmj.smf.game.event

import qorrnsmj.smf.math.Vector3f

data class EventAreaDefinition(
    val name: String,
    val id: String,
    val folder: String,
    val position: Vector3f,
    val rotation: Vector3f,
    val size: Vector3f,
    val properties: Map<String, String> = emptyMap(),
)
