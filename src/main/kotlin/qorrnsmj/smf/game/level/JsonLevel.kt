package qorrnsmj.smf.game.level

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsWorld

open class JsonLevel(
    protected val definition: LevelDefinition,
) : Level() {
    private val triggers: MutableList<AreaEnterTrigger> = mutableListOf()

    override fun load() {
        EntityModels.loadStageModels(definition.entityModels)

        player = Player()
        scene.entities.add(player)

        definition.glbPath?.let { glbPath ->
            triggers.addAll(
                GlbLevelApplier.apply(
                    level = GlbLevelLoader.load(glbPath),
                    scene = scene,
                    player = player,
                    onAreaTriggerEvent = ::handleAreaTriggerEvent,
                )
            )
        }

        scene.lights.add(
            PointLight().apply {
                position = Vector3f(0f, 3000f, 0f)
                ambient = Vector3f(0.1f, 0.1f, 0.1f)
                diffuse = Vector3f(1f, 1f, 1f)
                specular = Vector3f(1f, 1f, 1f)
                shininess = 32f
                constant = 1f
                linear = 0f
                quadratic = 0f
            }
        )
        scene.skybox = Skyboxes.SKY1
    }

    override fun input(delta: Float) {
        player.handleInput(SMF.window, delta)
    }

    override fun update(delta: Float) {
        PhysicsWorld.update(scene.entities, scene.terrainHeightProvider ?: scene.terrain, delta)
        player.update()
        triggers.forEach { it.update(delta) }
    }

    protected open fun handleAreaTriggerEvent(eventName: String, trigger: GlbTrigger) {
        val targetLevel = trigger.properties["switchLevel"]
            ?: trigger.properties["levelName"]
            ?: eventName.substringAfter("switchLevel:", missingDelimiterValue = "")
                .takeIf { eventName.startsWith("switchLevel:") }

        if (!targetLevel.isNullOrBlank()) {
            Logger.info("Switching level from trigger: {} -> {}", trigger.name, targetLevel)
            switchLevel(targetLevel)
            return
        }

        Logger.info("GLB area trigger fired: {} ({})", eventName, trigger.name)
    }

    override fun unload() {
        triggers.clear()
        super.unload()
    }
}
