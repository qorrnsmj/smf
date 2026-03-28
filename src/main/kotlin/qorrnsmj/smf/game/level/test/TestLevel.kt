package qorrnsmj.smf.game.level.test

import org.lwjgl.glfw.GLFW.GLFW_KEY_F6
import org.lwjgl.glfw.GLFW.GLFW_KEY_R
import org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT
import org.lwjgl.glfw.GLFW.GLFW_KEY_UP
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.level.Level
import qorrnsmj.smf.game.task.cutscene.IntroductionCutscene
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.terrain.HeightProvider
import qorrnsmj.smf.graphic.terrain.Terrains
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsComponent
import qorrnsmj.smf.physics.PhysicsWorld
import qorrnsmj.smf.physics.collision.BoxCollider
import qorrnsmj.smf.physics.collision.SphereCollider
import qorrnsmj.smf.graphic.text.DebugTextManager

class TestLevel : Level() {
    private lateinit var pointLight: PointLight
    private val triggers: MutableList<AreaEnterTrigger> = mutableListOf()
    private lateinit var introductionCutscene: IntroductionCutscene
    private lateinit var cutsceneCamera: Camera
    private val debugTextManager = DebugTextManager()
    private val skyColorTestPalette = listOf(
        Vector3f(0.55f, 0.72f, 0.98f),
        Vector3f(0.95f, 0.55f, 0.35f),
        Vector3f(0.22f, 0.28f, 0.45f)
    )
    private var skyColorPaletteIndex = 0
    private var skyColorTogglePressed = false
    private lateinit var sideCollisionWall: Entity
    private lateinit var sideCollisionMover: Entity
    private var sideCollisionVerified = false
    private lateinit var sphereCollisionWall: Entity
    private lateinit var sphereCollisionMover: Entity
    private var sphereCollisionVerified = false
    private val sideCollisionMoverStart = Vector3f(96f, 36f, 100f)
    private val sphereCollisionMoverStart = Vector3f(96f, 36f, 112f)
    private val sphereAutoVelocity = Vector3f(0.1f, 0f, 0f)
    private val moverControlSpeed = 0.1f
    private var moverResetPressed = false

    override fun load() {
        player = Player().apply { camera.position = Vector3f(100f, 37f, 100f) }

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

        scene.terrain = Terrains.PLANE
        scene.skybox = Skyboxes.SKY1
        scene.skyColor = skyColorTestPalette[skyColorPaletteIndex]

        // Add test entities with physics components for collision debug visualization
        addTestPhysicsEntities()
        SMF.renderer.debugRenderer.setCollisionDebugEnabled(true)

        triggers.add(
            object : AreaEnterTrigger(
                areaCenter = Vector3f(0f, 0f, 0f),
                areaRadius = 10f,
                playerPosition = { player.camera.position }
            ) {
                override fun fire() {
                    scene.skybox = Skyboxes.DEFAULT
                    scene.skyColor = skyColorTestPalette[1]
                    Logger.info("Sky/Fog color test trigger fired: {}", scene.skyColor)
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
        handleSideCollisionMoverInput()

        if (!introductionCutscene.isFinished()) {
            handleSkyColorTestInput()
            return
        }

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
    
    /**
     * Prepare deterministic collision test entities.
     * A dynamic box moves toward a static box and should stop on contact.
     */
    private fun addTestPhysicsEntities() {
        sideCollisionWall = Entity(
            localPosition = Vector3f(112f, 36f, 100f),
            model = EntityModels.EMPTY,
            physicsComponent = PhysicsComponent(
                useGravity = false,
                isStatic = true,
                collider = BoxCollider(width = 6f, height = 6f, depth = 6f)
            )
        )
        scene.entities.add(sideCollisionWall)

        sideCollisionMover = Entity(
            localPosition = sideCollisionMoverStart,
            model = EntityModels.EMPTY,
            physicsComponent = PhysicsComponent(
                velocity = Vector3f(0f, 0f, 0f),
                useGravity = false,
                isStatic = false,
                collider = BoxCollider(width = 4f, height = 4f, depth = 4f)
            )
        )
        scene.entities.add(sideCollisionMover)

        sphereCollisionWall = Entity(
            localPosition = Vector3f(112f, 36f, 112f),
            model = EntityModels.EMPTY,
            physicsComponent = PhysicsComponent(
                useGravity = false,
                isStatic = true,
                collider = SphereCollider(radius = 3f)
            )
        )
        scene.entities.add(sphereCollisionWall)

        sphereCollisionMover = Entity(
            localPosition = sphereCollisionMoverStart,
            model = EntityModels.EMPTY,
            physicsComponent = PhysicsComponent(
                velocity = Vector3f(sphereAutoVelocity.x, sphereAutoVelocity.y, sphereAutoVelocity.z),
                useGravity = false,
                isStatic = false,
                collider = SphereCollider(radius = 2f)
            )
        )
        scene.entities.add(sphereCollisionMover)

        Logger.info("Collision tests ready: box-box (arrow keys) and sphere-sphere (auto)")
    }

    private fun handleSideCollisionMoverInput() {
        val moverPhysics = sideCollisionMover.physicsComponent ?: return

        var moveX = 0f
        var moveZ = 0f

        if (glfwGetKey(SMF.window.id, GLFW_KEY_LEFT) == GLFW_PRESS) moveX -= 1f
        if (glfwGetKey(SMF.window.id, GLFW_KEY_RIGHT) == GLFW_PRESS) moveX += 1f
        if (glfwGetKey(SMF.window.id, GLFW_KEY_UP) == GLFW_PRESS) moveZ -= 1f
        if (glfwGetKey(SMF.window.id, GLFW_KEY_DOWN) == GLFW_PRESS) moveZ += 1f

        val inputLength = kotlin.math.sqrt(moveX * moveX + moveZ * moveZ)
        if (inputLength > 0f) {
            moverPhysics.velocity = Vector3f(
                (moveX / inputLength) * moverControlSpeed,
                0f,
                (moveZ / inputLength) * moverControlSpeed
            )
        } else {
            moverPhysics.velocity = Vector3f(0f, 0f, 0f)
        }

        val resetPressed = glfwGetKey(SMF.window.id, GLFW_KEY_R) == GLFW_PRESS
        if (resetPressed && !moverResetPressed) {
            sideCollisionMover.position = Vector3f(sideCollisionMoverStart.x, sideCollisionMoverStart.y, sideCollisionMoverStart.z)
            moverPhysics.stop()
            sideCollisionVerified = false
            sphereCollisionMover.position = Vector3f(sphereCollisionMoverStart.x, sphereCollisionMoverStart.y, sphereCollisionMoverStart.z)
            sphereCollisionMover.physicsComponent?.velocity = Vector3f(sphereAutoVelocity.x, sphereAutoVelocity.y, sphereAutoVelocity.z)
            sphereCollisionVerified = false
            Logger.info("Collision test reset: mover returned to start position")
        }
        moverResetPressed = resetPressed
    }

    override fun update(delta: Float) {
        // Player input and movement (existing logic)
        player.update(delta, scene.terrain as HeightProvider)

        // Physics simulation for all entities
        PhysicsWorld.update(scene.entities, scene.terrain as HeightProvider, delta)

        if (!sideCollisionVerified) {
            val moverPhysics = sideCollisionMover.physicsComponent
            if (moverPhysics != null) {
                val nearWall = sideCollisionMover.position.distanceTo(sideCollisionWall.position) < 6f
                val almostStopped = kotlin.math.abs(moverPhysics.velocity.x) < 0.05f
                if (nearWall && almostStopped) {
                    Logger.info("Side collision verified: mover stopped at wall boundary")
                    sideCollisionVerified = true
                }
            }
        }

        if (!sphereCollisionVerified) {
            val spherePhysics = sphereCollisionMover.physicsComponent
            if (spherePhysics != null) {
                val nearSphereWall = sphereCollisionMover.position.distanceTo(sphereCollisionWall.position) < 5f
                val sphereAlmostStopped = kotlin.math.abs(spherePhysics.velocity.x) < 0.03f
                if (nearSphereWall && sphereAlmostStopped) {
                    Logger.info("Sphere collision verified: sphere mover stopped at sphere boundary")
                    sphereCollisionVerified = true
                }
            }
        }

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


        // Update debug text display
        val collisionDebugEnabled = SMF.renderer.debugRenderer.isCollisionDebugEnabled()
        debugTextManager.updateDebugInfo(scene.camera.position, SMF.timer.getFPS(), SMF.timer.getUPS(), collisionDebugEnabled)
        scene.textElements.clear()
        scene.textElements.addAll(debugTextManager.getDebugElements())
    }

    override fun unload() {
        debugTextManager.cleanup()
        super.unload()
    }
}
