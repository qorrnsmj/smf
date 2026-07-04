package qorrnsmj.smf.game.level.custom

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.event.EditorMapEventLoader
import qorrnsmj.smf.game.event.EventAreaDefinition
import qorrnsmj.smf.game.level.LevelDefinition
import qorrnsmj.smf.game.level.LevelDefinitionLoader
import qorrnsmj.smf.game.task.Task
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.light.SunLight
import qorrnsmj.smf.graphic.render.RenderProfileManager
import qorrnsmj.smf.graphic.render.RenderProfiles
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsWorld

open class BaseLevel(
    val levelId: String,
) : Level() {
    protected lateinit var definition: LevelDefinition
    private val eventTasks: MutableList<Task> = mutableListOf()

    override fun load() {
        definition = LevelDefinitionLoader.load(levelId)
        EntityModels.loadStageModels(definition.entityModels)
        RenderProfileManager.applyTo(scene, RenderProfiles.fromName(definition.renderProfile))

        player = Player()
        scene.entities.add(player)

        LevelDefinitionLoader.loadInto(scene, definition)
        eventTasks.addAll(
            EditorMapEventLoader.loadInto(
                scene = scene,
                player = player,
                path = definition.resourcePath,
                onAreaTriggerEvent = ::handleAreaTriggerEvent,
            )
        )
        scene.camera = player.camera

        scene.sunLight = SunLight(
            direction = Vector3f(-0.35f, -1f, -0.25f),
            color = Vector3f(1f, 0.92f, 0.78f),
            intensity = 2.4f,
            ambientColor = Vector3f(0.38f, 0.45f, 0.55f),
            ambientIntensity = 0.2f,
            shadowStrength = 0.68f,
        )
        scene.lights.add(
            PointLight().apply {
                position = Vector3f(180f, 180f, 180f)
                diffuse = Vector3f(1f, 0.62f, 0.32f)
                specular = Vector3f(1f, 0.8f, 0.6f)
                intensity = 4f
                constant = 1f
                linear = 0.006f
                quadratic = 0.00002f
                castsShadow = true
                shadowStrength = 0.5f
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
        eventTasks.forEach { it.update(delta) }
    }

    protected open fun handleAreaTriggerEvent(eventArea: EventAreaDefinition) {
        val targetLevel = eventArea.properties["switchLevel"]
            ?: eventArea.properties["levelName"]
            ?: eventArea.id.substringAfter("switchLevel:", missingDelimiterValue = "")
                .takeIf { eventArea.id.startsWith("switchLevel:") }

        if (!targetLevel.isNullOrBlank()) {
            Logger.info("Switching level from editor trigger: {} -> {}", eventArea.name, targetLevel)
            switchLevel(targetLevel)
            return
        }

        Logger.info("Editor area trigger fired: {} ({})", eventArea.id, eventArea.name)
    }

    override fun unload() {
        eventTasks.clear()
        super.unload()
    }
}
