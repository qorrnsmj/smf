package qorrnsmj.smf.game.level.test

import org.lwjgl.glfw.GLFW.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
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
import qorrnsmj.smf.physics.PhysicsWorld
import qorrnsmj.smf.physics.collision.CollisionDetection

class TestLevel : Level() {
    private lateinit var collisionStallA: StallEntity
    private lateinit var collisionStallB: StallEntity
    private lateinit var pointLight: PointLight
    private val triggers: MutableList<AreaEnterTrigger> = mutableListOf()
    private val keyPressState: MutableMap<Int, Boolean> = mutableMapOf()
    private lateinit var introductionCutscene: IntroductionCutscene
    private lateinit var cutsceneCamera: Camera
    private var collisionPairOverlapping = false
    private var useSideCollisionScenario = false

    private val collisionStartA = Vector3f(80f, 45f, 80f)
    private val collisionStartB = Vector3f(80f, 35f, 80f)
    private val collisionSideStartA = Vector3f(72f, 35f, 80f)

    override fun load() {
        player = Player().apply { camera.position = Vector3f(100f, 37f, 100f) }

        collisionStallA = StallEntity().apply {
            position = Vector3f(collisionStartA.x, collisionStartA.y, collisionStartA.z)
            physicsComponent?.apply {
                useGravity = false
                isStatic = false
                stop()
            }
        }

        collisionStallB = StallEntity().apply {
            position = Vector3f(collisionStartB.x, collisionStartB.y, collisionStartB.z)
            physicsComponent?.apply {
                useGravity = false
                isStatic = true
                stop()
            }
        }

        scene.entities.add(collisionStallA)
        scene.entities.add(collisionStallB)

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

        // Launch collision test (upper Stall A -> lower Stall B)
        if (isKeyJustPressed(window, GLFW_KEY_C)) {
            useSideCollisionScenario = false
            launchCollisionTest()
        }

        // Launch collision test (left Stall A -> right Stall B)
        if (isKeyJustPressed(window, GLFW_KEY_X)) {
            useSideCollisionScenario = true
            launchCollisionTest()
        }

        // Reset collision test pair
        if (isKeyJustPressed(window, GLFW_KEY_V)) {
            resetCollisionTestPair()
        }
    }

    private fun isKeyJustPressed(window: Long, key: Int): Boolean {
        val pressed = glfwGetKey(window, key) == GLFW_PRESS
        val wasPressed = keyPressState[key] ?: false
        keyPressState[key] = pressed
        return pressed && !wasPressed
    }

    private fun launchCollisionTest() {
        collisionStallA.physicsComponent?.stop()
        collisionStallA.position = if (useSideCollisionScenario) {
            Vector3f(collisionSideStartA.x, collisionSideStartA.y, collisionSideStartA.z)
        } else {
            Vector3f(collisionStartA.x, collisionStartA.y, collisionStartA.z)
        }
        collisionStallB.position = Vector3f(collisionStartB.x, collisionStartB.y, collisionStartB.z)

        val impulse = if (useSideCollisionScenario) Vector3f(8f, 0f, 0f) else Vector3f(0f, -6f, 0f)
        collisionStallA.physicsComponent?.applyImpulse(impulse)

        if (useSideCollisionScenario) {
            Logger.info("Collision test launched: StallA (left) -> StallB (right) (press V to reset)")
        } else {
            Logger.info("Collision test launched: StallA (top) -> StallB (bottom) (press V to reset)")
        }
    }

    private fun resetCollisionTestPair() {
        collisionStallA.physicsComponent?.stop()
        collisionStallA.position = if (useSideCollisionScenario) {
            Vector3f(collisionSideStartA.x, collisionSideStartA.y, collisionSideStartA.z)
        } else {
            Vector3f(collisionStartA.x, collisionStartA.y, collisionStartA.z)
        }
        collisionStallB.position = Vector3f(collisionStartB.x, collisionStartB.y, collisionStartB.z)
        collisionPairOverlapping = false
        Logger.info("Collision test pair reset")
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

        val collisions = CollisionDetection.detectCollisions(scene.entities)
        val currentOverlap = collisions.any {
            (it.entity1 == collisionStallA && it.entity2 == collisionStallB) ||
                (it.entity1 == collisionStallB && it.entity2 == collisionStallA)
        }

        if (currentOverlap && !collisionPairOverlapping) {
            val pair = collisions.first {
                (it.entity1 == collisionStallA && it.entity2 == collisionStallB) ||
                    (it.entity1 == collisionStallB && it.entity2 == collisionStallA)
            }
            Logger.info("Collision ENTER: depth=${pair.result.penetrationDepth}, normal=${pair.result.collisionNormal}, contact=${pair.result.contactPoint}")
        } else if (!currentOverlap && collisionPairOverlapping) {
            Logger.info("Collision EXIT: StallA and StallB")
        }
        collisionPairOverlapping = currentOverlap
    }
}
