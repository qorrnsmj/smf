package qorrnsmj.smf.game.level.test

import org.lwjgl.glfw.GLFW.GLFW_KEY_F6
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.custom.StallEntity
import qorrnsmj.smf.game.map.EntityFactory
import qorrnsmj.smf.game.map.MapCollisionBuilder
import qorrnsmj.smf.game.map.Maps
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.level.Level
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsWorld
import qorrnsmj.smf.graphic.text.DebugTextManager
import qorrnsmj.smf.physics.collision.shape.CapsuleCollider

class TestLevel : Level() {
    private lateinit var pointLight: PointLight
    // private lateinit var introductionCutscene: IntroductionCutscene
    // private lateinit var cutsceneCamera: Camera
    private val triggers: MutableList<AreaEnterTrigger> = mutableListOf()
    private val debugTextManager = DebugTextManager()
    private val skyColorTestPalette = listOf(
        Vector3f(0.55f, 0.72f, 0.98f),
        Vector3f(0.95f, 0.55f, 0.35f),
        Vector3f(0.22f, 0.28f, 0.45f)
    )
    private var skyColorPaletteIndex = 0
    private var skyColorTogglePressed = false

    override fun load() {
        player = Player()
        scene.entities.add(player)
        logPlayerCapsuleCollision()
        scene.map = Maps.TEST
        scene.entities.addAll(MapCollisionBuilder.createCollisionEntities(Maps.TEST))
        EntityFactory.spawn(Maps.TEST.entities, scene, player)

        // Initialize debug text system
        debugTextManager.initialize()

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

        scene.skybox = Skyboxes.SKY1
        scene.skyColor = skyColorTestPalette[skyColorPaletteIndex]
        scene.entities.add(StallEntity())
        addSlideTeleportTrigger()

        SMF.renderer.debugRenderer.setCollisionDebugEnabled(false)
    }

    private fun logPlayerCapsuleCollision() {
        val capsule = player.physicsComponent.collider as? CapsuleCollider
        if (capsule != null) {
            Logger.info("Player capsule collision enabled: radius={}, height={}", capsule.radius, capsule.height)
        }
    }

    override fun start() {
        scene.camera = player.camera
//        cutsceneCamera = Camera().apply {
//            position = Vector3f(0f, 100f, 50f)
//            setFront(Vector3f(0f, -1f, 1f))
//            scene.camera = this
//        }
//
//        introductionCutscene = IntroductionCutscene(
//            camera = cutsceneCamera,
//            questAreaPosition = player.camera.position
//        )
    }

    override fun input(delta: Float) {
        player.handleInput(SMF.window, delta)
        handleSkyColorTestInput()
    }

    private fun handleSkyColorTestInput() {
        val isPressed = glfwGetKey(SMF.window.id, GLFW_KEY_F6) == GLFW_PRESS
        if (isPressed && !skyColorTogglePressed) {
            skyColorPaletteIndex = (skyColorPaletteIndex + 1) % skyColorTestPalette.size
            scene.skyColor = skyColorTestPalette[skyColorPaletteIndex]
            Logger.info("Sky/Fog color switched to {}", scene.skyColor)
        }

        skyColorTogglePressed = isPressed
    }

    private fun addSlideTeleportTrigger() {
        triggers.add(
            object : AreaEnterTrigger(
                areaCenter = Vector3f(300f, 50f, 300f),
                areaHalfExtents = Vector3f(90f, 60f, 90f),
                aabb = { player.getAabb() }
            ) {
                override fun fire() {
                    player.setFeetPosition(Vector3f(1875f, 9000f, 650f))
                    player.camera.setFront(Vector3f(0f, -0.2f, -1f))
                    Logger.info("Slide teleport triggered: {}", player.worldTransform.position)
                }
            }
        )
    }

    override fun update(delta: Float) {
        // Physics simulation for all entities (player included)
        PhysicsWorld.update(scene.entities, null, delta)

        // Keep camera synced to the physics-updated player entity.
        player.update()

//        if (!introductionCutscene.isFinished()) {
//            introductionCutscene.update(delta)
//
//            if (introductionCutscene.isFinished()) {
//                player.camera.setFront(cutsceneCamera.getFront())
//                scene.camera = player.camera
//            }
//        }

        for (trigger in triggers) {
            trigger.update(delta)
        }

        // Update debug text display
        val collisionDebugEnabled = SMF.renderer.debugRenderer.isCollisionDebugEnabled()
        debugTextManager.updateDebugInfo(player.worldTransform.position, SMF.timer.getFPS(), SMF.timer.getUPS(), collisionDebugEnabled)
        scene.textElements.clear()
        scene.textElements.addAll(debugTextManager.getDebugElements())
    }

    override fun unload() {
        super.unload()
    }
}
