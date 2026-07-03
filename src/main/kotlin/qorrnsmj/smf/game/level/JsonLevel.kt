package qorrnsmj.smf.game.level

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.light.SunLight
import qorrnsmj.smf.graphic.render.RenderProfileManager
import qorrnsmj.smf.graphic.render.RenderProfiles
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsWorld

open class JsonLevel(
    protected val definition: LevelDefinition,
) : Level() {
    private val triggers: MutableList<AreaEnterTrigger> = mutableListOf()

    override fun load() {
        EntityModels.loadStageModels(definition.entityModels)
        RenderProfileManager.applyTo(scene, RenderProfiles.fromName(definition.renderProfile))

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
