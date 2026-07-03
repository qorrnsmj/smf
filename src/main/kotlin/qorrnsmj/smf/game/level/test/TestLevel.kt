package qorrnsmj.smf.game.level.test

import org.lwjgl.glfw.GLFW.GLFW_KEY_F6
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.event.EventAreaDefinition
import qorrnsmj.smf.game.event.EditorMapEventLoader
import qorrnsmj.smf.game.level.GlbLevel
import qorrnsmj.smf.game.level.GlbLevelApplier
import qorrnsmj.smf.game.level.GlbLevelLoader
import qorrnsmj.smf.game.level.Level
import qorrnsmj.smf.game.level.EditorMapLevelLoader
import qorrnsmj.smf.game.level.EditorMapStaticObjectLoader
import qorrnsmj.smf.game.task.Task
import qorrnsmj.smf.game.task.cutscene.IntroductionCutscene
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.text.DebugTextManager
import qorrnsmj.smf.graphic.text.FontLoader
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsWorld
import qorrnsmj.smf.physics.collision.shape.CapsuleCollider

class TestLevel : Level() {
    private companion object {
        const val USE_TEMPORARY_GLB_LEVEL = false
        const val LOAD_GLB_TEST_LEVEL = false
        const val EDITOR_STAGE_PATH = "assets/level/stage.json"
    }

    private lateinit var pointLight: PointLight
    private lateinit var cutsceneCamera: Camera
    private val eventTasks: MutableList<Task> = mutableListOf()
    private val eventHandlers: Map<String, (EventAreaDefinition) -> Unit> by lazy {
        mapOf(
            "cutscene_start_trigger" to { startIntroductionCutscene() },
        )
    }
    private val debugTextManager = DebugTextManager()
    private val skyColorTestPalette = listOf(
        Vector3f(0.55f, 0.72f, 0.98f),
        Vector3f(0.95f, 0.55f, 0.35f),
        Vector3f(0.22f, 0.28f, 0.45f)
    )
    private var skyColorPaletteIndex = 0
    private var skyColorTogglePressed = false

    override fun load() {
        player = Player(moveSpeed = 0.09f, jumpSpeed = 0.18f)
        scene.entities.add(player)
        logPlayerCapsuleCollision()

        if (LOAD_GLB_TEST_LEVEL) {
            loadGlbLevel()
        }
        EditorMapLevelLoader.loadInto(scene, EDITOR_STAGE_PATH)
        EditorMapStaticObjectLoader.loadInto(scene, EDITOR_STAGE_PATH)
        eventTasks.addAll(
            EditorMapEventLoader.loadInto(
                scene = scene,
                player = player,
                path = EDITOR_STAGE_PATH,
                onAreaTriggerEvent = { eventArea ->
                    Logger.info("TestLevel editor event fired: {} ({})", eventArea.id, eventArea.name)
                    eventHandlers[eventArea.id]?.invoke(eventArea)
                        ?: Logger.info("No editor event handler registered: {}", eventArea.id)
                },
            )
        )
        scene.camera = player.camera

        debugTextManager.initialize()
        cutscenes.setSubtitleFont(FontLoader.loadAssetFont("Inconsolata.ttf", 28f))
        cutscenes.showDebugControls = true

        pointLight = PointLight().apply {
            position = Vector3f(0f, 30f, 0f)
            ambient = Vector3f(0.1f, 0.1f, 0.1f)
            diffuse = Vector3f(1f, 1f, 1f)
            specular = Vector3f(1f, 1f, 1f)
            shininess = 32f
            constant = 1f
            linear = 0f
            quadratic = 0f
        }
        scene.lights.add(pointLight)

        if (scene.skybox == Skyboxes.DEFAULT) {
            scene.skybox = Skyboxes.SKY1
        }
        if (isDefaultSkyColor()) {
            scene.skyColor = skyColorTestPalette[skyColorPaletteIndex]
        }

        SMF.renderer.debugRenderer.setCollisionDebugEnabled(false)
    }

    private fun logPlayerCapsuleCollision() {
        val capsule = player.physicsComponent.collider as? CapsuleCollider
        if (capsule != null) {
            Logger.info("Player capsule collision enabled: radius={}, height={}", capsule.radius, capsule.height)
        }
    }

    private fun isDefaultSkyColor(): Boolean {
        return scene.skyColor.x == 1f && scene.skyColor.y == 1f && scene.skyColor.z == 1f
    }

    override fun start() {
    }

    private fun startIntroductionCutscene() {
        if (cutscenes.isPlaying) return

        cutsceneCamera = Camera().apply {
            val playerEye = player.camera.position
            position = playerEye.add(Vector3f(-0.6f, 0.4f, 0.6f))
            setFront(playerEye.subtract(position))
            scene.camera = this
        }

        val introductionCutscene = IntroductionCutscene(
            camera = cutsceneCamera,
            focusPosition = player.camera.position,
            destinationEyePosition = player.camera.position,
            destinationFront = player.camera.getFront(),
            onAreaReveal = {
                pointLight.diffuse = Vector3f(1f, 0.75f, 0.35f)
                scene.skyColor = skyColorTestPalette[1]
                Logger.info("Introduction cutscene event fired at 2.5 seconds")
            },
            onComplete = {
                Logger.info("Introduction cutscene finished")
            },
        )
        cutscenes.play(
            cutscene = introductionCutscene,
            camera = cutsceneCamera,
            returnTo = player.camera,
        )
    }

    override fun input(delta: Float) {
        if (handleCutsceneInput()) {
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

    private fun loadGlbLevel() {
        val glbLevel = if (USE_TEMPORARY_GLB_LEVEL) {
            GlbLevel.createTemporaryTestLevel()
        } else {
            GlbLevelLoader.load("assets/level/test_level.glb")
        }
        eventTasks.addAll(
            GlbLevelApplier.apply(
                level = glbLevel,
                scene = scene,
                player = player,
                onAreaTriggerEvent = { eventName, trigger ->
                    Logger.info("TestLevel GLB AreaTrigger fired: {} ({})", eventName, trigger.name)
                },
            )
        )
    }

    override fun update(delta: Float) {
        updateCutscenes(delta)

        if (!cutscenes.isWorldPaused) {
            updateWorld(delta)
        }

        val collisionDebugEnabled = SMF.renderer.debugRenderer.isCollisionDebugEnabled()
        debugTextManager.updateDebugInfo(player.worldTransform.position, SMF.timer.getFPS(), SMF.timer.getUPS(), collisionDebugEnabled)
        scene.textElements.clear()
        scene.textElements.addAll(debugTextManager.getDebugElements())
    }

    private fun updateWorld(delta: Float) {
        PhysicsWorld.update(scene.entities, scene.terrainHeightProvider ?: scene.terrain, delta)
        player.update()

        for (task in eventTasks) {
            task.update(delta)
        }
    }

    override fun unload() {
        super.unload()
    }
}
