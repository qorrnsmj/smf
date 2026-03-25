package qorrnsmj.smf.game.level.test

import org.lwjgl.glfw.GLFW.GLFW_KEY_F6
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.camera.Camera
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


        // Update debug text display
        debugTextManager.updateDebugInfo(scene.camera.position, SMF.timer.getFPS(), SMF.timer.getUPS())
        scene.textElements.clear()
        scene.textElements.addAll(debugTextManager.getDebugElements())
    }

    override fun unload() {
        debugTextManager.cleanup()
        super.unload()
    }
}
