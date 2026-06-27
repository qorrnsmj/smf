package qorrnsmj.smf.game.entity

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.game.entity.custom.StallEntity
import qorrnsmj.smf.game.entity.custom.Transform

object Entities {
    private val factories: MutableMap<String, (Transform, Map<String, String>) -> Entity> = mutableMapOf()

    fun load() {
        factories.clear()

        register("stall") { transform, _ ->
            StallEntity().apply {
                localTransform = transform
            }
        }
    }

    fun register(id: String, factory: (Transform, Map<String, String>) -> Entity) {
        val normalizedId = id.normalized()
        require(normalizedId.isNotBlank()) { "Entity id must not be blank." }

        factories[normalizedId] = factory
    }

    fun create(id: String, transform: Transform, properties: Map<String, String>): Entity? {
        val factory = factories[id.normalized()]
        if (factory == null) {
            Logger.warn("Entity type is not registered: {}", id)
            return null
        }

        return factory(transform, properties)
    }

    private fun String.normalized(): String =
        lowercase().filter { it.isLetterOrDigit() }
}
