package qorrnsmj.smf.game.level.test

import org.lwjgl.glfw.GLFW.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.physics.PhysicsWorld
import qorrnsmj.smf.physics.debug.PhysicsDebugRenderer
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.custom.StallEntity
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.level.Level
import qorrnsmj.smf.game.task.cutscene.IntroductionCutscene
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.terrain.HeightProvider
import qorrnsmj.smf.graphic.terrain.Terrains
import qorrnsmj.smf.math.Vector3f

class TestLevel : Level() {
    private lateinit var stall: StallEntity
    private lateinit var pointLight: PointLight
    private val triggers: MutableList<AreaEnterTrigger> = mutableListOf()
    private val keyPressState: MutableMap<Int, Boolean> = mutableMapOf()
    private lateinit var introductionCutscene: IntroductionCutscene
    private lateinit var cutsceneCamera: Camera

    override fun load() {
        player = Player().apply { camera.position = Vector3f(100f, 37f, 100f) }
        stall = StallEntity()
        scene.entities.add(stall)

        pointLight = PointLight().apply {
            position = Vector3f(0f, 10f, 0f)
            ambient = Vector3f(0.1f, 0.1f, 0.1f)
            diffuse = Vector3f(1f, 1f, 1f)
            specular = Vector3f(1f, 1f, 1f)
            shininess = 32f
            constant = 1f
            linear = 0f
            quadratic = 0f
        }
        scene.lights.add(pointLight)

        scene.terrain = Terrains.PLANE
        scene.skybox = Skyboxes.SKY1

        triggers.add(
            object : AreaEnterTrigger(
                areaCenter = Vector3f(0f, 0f, 0f),
                areaRadius = 10f,
                playerPosition = { player.camera.position }
            ) {
                override fun fire() {
                    scene.skybox = Skyboxes.DEFAULT
                }
            }
        )
    }

    override fun start() {
        cutsceneCamera = Camera().apply {
            position = Vector3f(0f, 100f, 50f)
            setFront(Vector3f(0f, -1f, 1f))
            scene.camera = this
        }

        // Initialize physics debug renderer
        PhysicsDebugRenderer.initialize()

        introductionCutscene = IntroductionCutscene(
            camera = cutsceneCamera,
            questAreaPosition = player.camera.position
        )
    }

    override fun input(delta: Float) {
        if (!introductionCutscene.isFinished()) return

        player.handleInput(SMF.window, delta)

        // Physics testing controls only
        handlePhysicsInput()
    }

    private fun handlePhysicsInput() {
        val window = SMF.window.id

        // Physics debug visualization toggle
        if (isKeyJustPressed(window, GLFW_KEY_P)) {
            PhysicsDebugRenderer.toggleDebug()
            Logger.info("Physics debug visualization toggled")
        }

        // Reset stall position (for testing gravity)
        if (isKeyJustPressed(window, GLFW_KEY_R)) {
            stall.children.forEach { it.position = Vector3f(80f, 50f, 80f) }
            stall.physicsComponent?.stop()
            Logger.info("Reset stall position for gravity test")
        }

        // Toggle stall physics
        if (isKeyJustPressed(window, GLFW_KEY_T)) {
            stall.physicsComponent?.let { physics ->
                physics.useGravity = !physics.useGravity
                Logger.info("Stall gravity: ${if (physics.useGravity) "ENABLED" else "DISABLED"}")
            }
        }

        // Apply upward impulse to stall
        if (isKeyJustPressed(window, GLFW_KEY_U)) {
            stall.physicsComponent?.applyImpulse(Vector3f(0f, 5f, 0f))
            Logger.info("Applied upward impulse to stall")
        }

        // Apply forward impulse (towards +Z)
        if (isKeyJustPressed(window, GLFW_KEY_J)) {
            stall.physicsComponent?.applyImpulse(Vector3f(0f, 0f, 5f))
            Logger.info("Applied forward impulse to stall")
        }

        // Apply sideways impulse (towards +X)
        if (isKeyJustPressed(window, GLFW_KEY_K)) {
            stall.physicsComponent?.applyImpulse(Vector3f(5f, 0f, 0f))
            Logger.info("Applied sideways impulse to stall")
        }

        // Hard stop to verify friction and resting behavior
        if (isKeyJustPressed(window, GLFW_KEY_Y)) {
            stall.physicsComponent?.stop()
            Logger.info("Stopped stall velocity")
        }

        // Debug options toggle
        if (isKeyJustPressed(window, GLFW_KEY_D)) {
            PhysicsDebugRenderer.setDebugOptions(
                colliders = true,
                velocity = true,
                forces = true
            )
            Logger.info("Physics debug options updated (colliders, velocity, forces all enabled)")
        }

        // Physics system status
        if (isKeyJustPressed(window, GLFW_KEY_O)) {
            val stats = PhysicsWorld.getStats()
            val physics = stall.physicsComponent
            Logger.info("=== Physics System Status ===")
            Logger.info("Initialized: ${stats.isInitialized}")
            Logger.info("Active Entities: ${stats.activeEntities}")
            Logger.info("Update Count: ${stats.updateCount}")
            Logger.info("Last Update Time: ${stats.lastUpdateTimeMs}ms")
            Logger.info("Stall Position: ${stall.position}")
            Logger.info("Stall Velocity: ${physics?.velocity}")
            Logger.info("Stall Grounded: ${physics?.isGrounded}")
            Logger.info("Stall Gravity: ${physics?.useGravity}")
            Logger.info("============================")
        }
    }

    private fun isKeyJustPressed(window: Long, key: Int): Boolean {
        val pressed = glfwGetKey(window, key) == GLFW_PRESS
        val wasPressed = keyPressState[key] ?: false
        keyPressState[key] = pressed
        return pressed && !wasPressed
    }

    override fun update(delta: Float) {
        // Player input and movement (existing logic)
        player.update(delta, scene.terrain as HeightProvider)

        // Physics simulation for all entities
        PhysicsWorld.update(scene.entities, scene.terrain as HeightProvider, delta)

        if (!introductionCutscene.isFinished()) {
            introductionCutscene.update(delta)

            if (introductionCutscene.isFinished()) {
                player.camera.setFront(cutsceneCamera.getFront())
                scene.camera = player.camera
            }
        }

        for (trigger in triggers) {
            trigger.update(delta)
        }
        
        // Render debug visualization after scene update
        if (scene.entities.any { it.physicsComponent != null }) {
            PhysicsDebugRenderer.renderDebug(scene.entities.filter { it.physicsComponent != null })
        }

        // TEST: Entity hierarchy physics system
        Logger.debug("Stall World: ${stall.position}, Local: ${stall.localPosition}")
        if (stall.children.isNotEmpty()) {
            val cover = stall.children[0]  // cover entity
            Logger.debug("Cover World: ${cover.getWorldPosition()}, Local: ${cover.localPosition}")
        }
        
        // Test: Add a dynamic cargo to the stall every 5 seconds
        testTimer += delta
        if (testTimer > 5f && !cargoAdded) {
            val cargo = Entity(
                localPosition = Vector3f(0f, 1.2f, 0.2f),  // On top of table
                localScale = Vector3f(0.1f, 0.1f, 0.1f),   // Small cargo
                model = EntityModels.getModel(EntityModels.STALL, "Fruits")  // Reuse fruits model
            )
            stall.addCargo(cargo, Vector3f(0.5f, 1.2f, 0f))
            Logger.info("Added cargo to stall at local position ${cargo.localPosition}")
            cargoAdded = true
        }
    }

    // Test timer for dynamic cargo addition
    private var testTimer = 0f
    private var cargoAdded = false
    }

    override fun unload() {
        scene.entities.clear()
        scene.lights.clear()
        PhysicsDebugRenderer.cleanup()
    }
}
