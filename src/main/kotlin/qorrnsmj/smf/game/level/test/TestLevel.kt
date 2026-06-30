package qorrnsmj.smf.game.level.test

import org.lwjgl.glfw.GLFW.GLFW_KEY_F2
import org.lwjgl.glfw.GLFW.GLFW_KEY_F3
import org.lwjgl.glfw.GLFW.GLFW_KEY_F4
import org.lwjgl.glfw.GLFW.GLFW_KEY_F6
import org.lwjgl.glfw.GLFW.GLFW_KEY_F7
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.custom.StallEntity
import qorrnsmj.smf.game.entity.mob.SlimeEntity
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.entity.projectile.ArrowEntity
import qorrnsmj.smf.game.entity.projectile.ProjectileEntity
import qorrnsmj.smf.game.level.GlbLevel
import qorrnsmj.smf.game.level.GlbLevelApplier
import qorrnsmj.smf.game.level.GlbLevelLoader
import qorrnsmj.smf.game.level.GlbTrigger
import qorrnsmj.smf.game.level.Level
import qorrnsmj.smf.game.level.LevelDefinition
import qorrnsmj.smf.game.progress.GameProgress
import qorrnsmj.smf.game.progress.GameProgressManager
import qorrnsmj.smf.game.task.cutscene.IntroductionCutscene
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
import qorrnsmj.smf.game.task.trigger.ContainmentMode
import qorrnsmj.smf.game.task.trigger.TeleportFacing
import qorrnsmj.smf.game.task.trigger.TeleportLook
import qorrnsmj.smf.game.task.trigger.TeleportTrigger
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.text.DebugTextManager
import qorrnsmj.smf.graphic.text.FontLoader
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsWorld
import qorrnsmj.smf.physics.collision.shape.CapsuleCollider

class TestLevel(
    private val definition: LevelDefinition,
) : Level() {
    private companion object {
        const val USE_TEMPORARY_GLB_LEVEL = false
    }

    private lateinit var pointLight: PointLight
    private lateinit var cutsceneCamera: Camera
    private val triggers: MutableList<AreaEnterTrigger> = mutableListOf()
    private val debugTextManager = DebugTextManager()
    private val skyColorTestPalette = listOf(
        Vector3f(0.55f, 0.72f, 0.98f),
        Vector3f(0.95f, 0.55f, 0.35f),
        Vector3f(0.22f, 0.28f, 0.45f)
    )
    private var skyColorPaletteIndex = 0
    private var skyColorTogglePressed = false
    private val progressManager = GameProgressManager(definition.name)
    private var saveProgressPressed = false
    private var loadProgressPressed = false
    private var moveToSavePointPressed = false
    private var levelSwitchTestPressed = false

    override fun load() {
        player = Player(moveSpeed = 5.5f, jumpSpeed = 10f)
        scene.entities.add(player)
        logPlayerCapsuleCollision()

        loadGlbLevel()
        loadSavedProgressIfPresent()
        addTeleportTriggerTestFixture()

        debugTextManager.initialize()
        cutscenes.setSubtitleFont(FontLoader.loadAssetFont("Inconsolata.ttf", 28f))
        cutscenes.showDebugControls = true

        pointLight = PointLight().apply {
            position = Vector3f(0f, 3000f, 0f)
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
        scene.entities.add(SlimeEntity(Vector3f(120f, 5f, 160f)))
        scene.entities.add(ArrowEntity(Vector3f(80f, 80f, 160f), Vector3f(1f, 0f, 0f)))

        SMF.renderer.debugRenderer.setCollisionDebugEnabled(false)
    }

    private fun logPlayerCapsuleCollision() {
        val capsule = player.physicsComponent.collider as? CapsuleCollider
        if (capsule != null) {
            Logger.info("Player capsule collision enabled: radius={}, height={}", capsule.radius, capsule.height)
        }
    }

    override fun start() {
        cutsceneCamera = Camera().apply {
            val playerEye = player.camera.position
            position = playerEye.add(Vector3f(-60f, 40f, 60f))
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
            handleProgressTestInput()
            handleLevelSwitchTestInput()
            return
        }

        player.handleInput(SMF.window, delta)
        handleSkyColorTestInput()
        handleProgressTestInput()
        handleLevelSwitchTestInput()
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

    private fun handleProgressTestInput() {
        val isSavePressed = glfwGetKey(SMF.window.id, GLFW_KEY_F2) == GLFW_PRESS
        if (isSavePressed && !saveProgressPressed) {
            capturePlayerProgress()
            progressManager.addFlag("test_level_visited")
            progressManager.save()
            Logger.info("Game progress saved to {}", progressManager.getSavePath())
        }
        saveProgressPressed = isSavePressed

        val isLoadPressed = glfwGetKey(SMF.window.id, GLFW_KEY_F3) == GLFW_PRESS
        if (isLoadPressed && !loadProgressPressed) {
            loadSavedProgressIfPresent()
        }
        loadProgressPressed = isLoadPressed

        val isMovePressed = glfwGetKey(SMF.window.id, GLFW_KEY_F4) == GLFW_PRESS
        if (isMovePressed && !moveToSavePointPressed) {
            movePlayerToSavedProgressPoint()
        }
        moveToSavePointPressed = isMovePressed
    }

    private fun loadSavedProgressIfPresent() {
        val loadedProgress = progressManager.loadOrNull()
        if (loadedProgress != null) {
            applyProgress(loadedProgress)
        }
    }

    private fun movePlayerToSavedProgressPoint() {
        val loadedProgress = progressManager.loadOrNull()
        if (loadedProgress == null) {
            Logger.info("Cannot move to saved progress point because no save exists at {}", progressManager.getSavePath())
            return
        }

        applyProgress(loadedProgress)
        Logger.info(
            "Moved player to saved progress point: stage={}, position={}, facing={}",
            loadedProgress.currentStageName,
            loadedProgress.playerPosition,
            loadedProgress.playerFacing,
        )
    }

    private fun capturePlayerProgress() {
        progressManager.setCurrentStage(definition.name)
        progressManager.updatePlayerState(
            position = player.worldTransform.position,
            facing = player.camera.getFront(),
        )
    }

    private fun applyProgress(progress: GameProgress) {
        player.setFeetPosition(progress.playerPosition)
        player.camera.setFront(progress.playerFacing)
        scene.camera = player.camera
    }

    private fun handleLevelSwitchTestInput() {
        val isPressed = glfwGetKey(SMF.window.id, GLFW_KEY_F7) == GLFW_PRESS
        if (isPressed && !levelSwitchTestPressed) {
            val targetLevel = if (definition.name == "home") "test_level" else "home"
            Logger.info("TestLevel manual level switch requested: {} -> {}", definition.name, targetLevel)
            switchLevel(targetLevel)
        }

        levelSwitchTestPressed = isPressed
    }

    private fun loadGlbLevel() {
        val glbLevel = if (USE_TEMPORARY_GLB_LEVEL) {
            GlbLevel.createTemporaryTestLevel()
        } else {
            GlbLevelLoader.load(definition.glbPath ?: error("TestLevel requires glb in ${definition.resourcePath}"))
        }
        triggers.addAll(
            GlbLevelApplier.apply(
                level = glbLevel,
                scene = scene,
                player = player,
                onAreaTriggerEvent = { eventName, trigger ->
                    handleAreaTriggerEvent(eventName, trigger)
                },
            )
        )
    }

    private fun addTeleportTriggerTestFixture() {
        val origin = player.worldTransform.position
        val triggerCenter = origin.add(Vector3f(180f, 85f, 0f))
        val destination = origin.add(Vector3f(360f, 0f, 180f))

        triggers.add(
            TeleportTrigger(
                areaCenter = triggerCenter,
                areaHalfExtents = Vector3f(45f, 100f, 45f),
                player = player,
                destinationFeet = destination,
                facing = TeleportFacing.Direction(Vector3f(0f, 0f, 1f)),
                look = TeleportLook.LookAt(destination.add(Vector3f(0f, 170f, 300f))),
                containmentMode = ContainmentMode.FULLY_INSIDE,
                onTeleport = {
                    Logger.info(
                        "TestLevel teleport trigger fired: destination={}, look={}",
                        destination,
                        player.camera.getFront(),
                    )
                },
            )
        )
    }

    private fun handleAreaTriggerEvent(eventName: String, trigger: GlbTrigger) {
        val targetLevel = trigger.properties["switchLevel"]
            ?: trigger.properties["levelName"]
            ?: eventName.substringAfter("switchLevel:", missingDelimiterValue = "")
                .takeIf { eventName.startsWith("switchLevel:") }

        if (!targetLevel.isNullOrBlank()) {
            Logger.info("TestLevel trigger requested level switch: {} -> {}", trigger.name, targetLevel)
            switchLevel(targetLevel)
            return
        }

        Logger.info("TestLevel GLB AreaTrigger fired: {} ({})", eventName, trigger.name)
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
        scene.entities.forEach { it.update(delta) }
        scene.entities.removeAll { it is ProjectileEntity && it.isExpired }

        for (trigger in triggers) {
            trigger.update(delta)
        }
    }

    override fun unload() {
        triggers.clear()
        super.unload()
    }
}
