package qorrnsmj.smf.game.level.test

import org.lwjgl.glfw.GLFW.GLFW_KEY_F2
import org.lwjgl.glfw.GLFW.GLFW_KEY_F3
import org.lwjgl.glfw.GLFW.GLFW_KEY_F4
import org.lwjgl.glfw.GLFW.GLFW_KEY_F5
import org.lwjgl.glfw.GLFW.GLFW_KEY_F6
import org.lwjgl.glfw.GLFW.GLFW_KEY_F7
import org.lwjgl.glfw.GLFW.GLFW_KEY_F8
import org.lwjgl.glfw.GLFW.GLFW_KEY_F12
import org.lwjgl.glfw.GLFW.GLFW_KEY_R
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.custom.ShadowTestBlockEntity
import qorrnsmj.smf.game.entity.custom.StallEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.game.entity.mob.SlimeEntity
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.entity.projectile.ArrowEntity
import qorrnsmj.smf.game.entity.projectile.ProjectileEntity
import qorrnsmj.smf.game.event.EditorMapEventLoader
import qorrnsmj.smf.game.event.EventAreaDefinition
import qorrnsmj.smf.game.level.EditorMapLevelLoader
import qorrnsmj.smf.game.level.EditorMapStaticObjectLoader
import qorrnsmj.smf.game.level.Level
import qorrnsmj.smf.game.level.LevelDefinition
import qorrnsmj.smf.game.progress.GameProgress
import qorrnsmj.smf.game.progress.GameProgressManager
import qorrnsmj.smf.game.task.Task
import qorrnsmj.smf.game.task.cutscene.IntroductionCutscene
import qorrnsmj.smf.game.task.trigger.ContainmentMode
import qorrnsmj.smf.game.task.trigger.TeleportFacing
import qorrnsmj.smf.game.task.trigger.TeleportLook
import qorrnsmj.smf.game.task.trigger.TeleportTrigger
import qorrnsmj.smf.graphic.text.TextElement
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.light.SpotLight
import qorrnsmj.smf.graphic.light.SunLight
import qorrnsmj.smf.graphic.render.RenderProfileManager
import qorrnsmj.smf.graphic.render.RenderProfiles
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.terrain.TerrainLoader
import qorrnsmj.smf.graphic.terrain.component.BlendedTexture
import qorrnsmj.smf.graphic.text.DebugTextManager
import qorrnsmj.smf.graphic.text.Font
import qorrnsmj.smf.graphic.text.FontLoader
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.math.Quaternion
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.smf.physics.PhysicsWorld
import qorrnsmj.smf.physics.collision.shape.CapsuleCollider

class TestLevel(
    private val definition: LevelDefinition,
) : Level() {
    private companion object {
        const val EDITOR_STAGE_PATH = "assets/level/stage.json"
        const val USE_WIDE_TERRAIN_FOG_TEST = true
        const val SHADOW_NORMAL_STRENGTH = 0.86f
        const val SHADOW_STRONG_STRENGTH = 0.96f
        const val SHADOW_NORMAL_AMBIENT = 0.12f
        const val SHADOW_STRONG_AMBIENT = 0.06f
    }

    private lateinit var pointLight: PointLight
    private lateinit var spotLight: SpotLight
    private lateinit var cutsceneCamera: Camera
    private lateinit var shadowDebugFont: Font
    private val eventTasks: MutableList<Task> = mutableListOf()
    private val eventHandlers: Map<String, (EventAreaDefinition) -> Unit> by lazy {
        mapOf(
            "cutscene_start_trigger" to { startIntroductionCutscene() },
        )
    }
    private val debugTextManager = DebugTextManager()
    private val skyColorTestPalette = listOf(
        Vector3f(0.035f, 0.045f, 0.095f),
        Vector3f(0.06f, 0.07f, 0.13f),
        Vector3f(0.11f, 0.10f, 0.16f)
    )
    private var skyColorPaletteIndex = 0
    private var skyColorTogglePressed = false
    private var fogEnabled = true
    private var fogTogglePressed = false
    private val progressManager = GameProgressManager(definition.name)
    private var saveProgressPressed = false
    private var loadProgressPressed = false
    private var moveToSavePointPressed = false
    private var levelSwitchTestPressed = false
    private var shadowTogglePressed = false
    private var shadowMode = ShadowDebugMode.NORMAL
    private var renderProfileTogglePressed = false
    private var localLightShadowTogglePressed = false
    private var localLightShadowMode = LocalLightShadowMode.BOTH

    override fun load() {
        RenderProfileManager.applyTo(scene, RenderProfiles.SHADOWED)

        player = Player(moveSpeed = 5.5f, jumpSpeed = 10f)
        scene.entities.add(player)
        logPlayerCapsuleCollision()

        loadEditorStage()
        loadSavedProgressIfPresent()
        addTeleportTriggerTestFixture()
        if (USE_WIDE_TERRAIN_FOG_TEST) {
            addWideTerrainFogTest()
        }

        debugTextManager.initialize()
        shadowDebugFont = FontLoader.loadAssetFont("Inconsolata.ttf", 16f)
        cutscenes.setSubtitleFont(FontLoader.loadAssetFont("Inconsolata.ttf", 28f))
        cutscenes.showDebugControls = true

        pointLight = PointLight().apply {
            position = Vector3f(270f, 130f, 320f)
            diffuse = Vector3f(1f, 0.58f, 0.28f)
            specular = Vector3f(1f, 0.82f, 0.58f)
            shininess = 32f
            intensity = 12.0f
            constant = 1f
            linear = 0.006f
            quadratic = 0.00002f
            castsShadow = true
            shadowStrength = 0.62f
        }
        scene.sunLight = SunLight(
            direction = Vector3f(-0.42f, -1f, -0.28f),
            color = Vector3f(1f, 0.92f, 0.78f),
            intensity = 0f,
            ambientColor = Vector3f(0.08f, 0.09f, 0.14f),
            ambientIntensity = 0.12f,
            shadowStrength = 0f,
        )
        scene.lights.add(pointLight)
        spotLight = SpotLight().apply {
            position = Vector3f(235f, 260f, 360f)
            direction = Vector3f(0f, -1.0f, 0f)
            diffuse = Vector3f(0.55f, 0.72f, 1f)
            specular = Vector3f(0.7f, 0.86f, 1f)
            intensity = 42f
            constant = 1f
            linear = 0.006f
            quadratic = 0.00002f
            innerCutOffDegrees = 18f
            outerCutOffDegrees = 30f
            castsShadow = true
            shadowStrength = 0.7f
        }
        scene.lights.add(spotLight)

        scene.skyboxEnabled = false
        applyFogTestState()
        scene.skyboxEnabled = false
        scene.skybox = Skyboxes.SKY1
        scene.entities.add(StallEntity().apply {
            localTransform = Transform(
                position = Vector3f(180f, 2f, 400f),
                rotation = Quaternion.fromEulerDegrees(Vector3f(90f, 0f, 0f)),
                scale = Vector3f(35f, 35f, 35f),
            )
        })
        scene.entities.add(
            ShadowTestBlockEntity(
                transform = Transform(
                    position = Vector3f(220f, 32f, 332f),
                    scale = Vector3f(56f, 64f, 56f),
                ),
                color = Vector4f(0.76f, 0.72f, 0.64f, 1f),
            )
        )
        scene.entities.add(
            ShadowTestBlockEntity(
                transform = Transform(
                    position = Vector3f(255f, 46f, 355f),
                    scale = Vector3f(56f, 92f, 56f),
                ),
                color = Vector4f(0.62f, 0.68f, 0.76f, 1f),
            )
        )
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
                skyColorPaletteIndex = 1
                applyFogTestState()
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
            handleFogTestInput()
            handleProgressTestInput()
            handleLevelSwitchTestInput()
            handleShadowDebugInput()
            handleRenderProfileInput()
            handleLocalLightShadowInput()
            return
        }

        player.handleInput(SMF.window, delta)
        handleSkyColorTestInput()
        handleFogTestInput()
        handleProgressTestInput()
        handleLevelSwitchTestInput()
        handleShadowDebugInput()
        handleRenderProfileInput()
        handleLocalLightShadowInput()
    }

    private fun handleShadowDebugInput() {
        val isPressed = glfwGetKey(SMF.window.id, GLFW_KEY_F5) == GLFW_PRESS
        if (isPressed && !shadowTogglePressed) {
            shadowMode = shadowMode.next()
            applyShadowDebugMode()
            Logger.info("Shadow debug mode switched to {}", shadowMode.label)
        }

        shadowTogglePressed = isPressed
    }

    private fun applyShadowDebugMode() {
        val sun = scene.sunLight ?: return
        when (shadowMode) {
            ShadowDebugMode.OFF -> {
                sun.shadowStrength = 0f
                sun.ambientIntensity = SHADOW_NORMAL_AMBIENT
            }
            ShadowDebugMode.NORMAL -> {
                sun.shadowStrength = SHADOW_NORMAL_STRENGTH
                sun.ambientIntensity = SHADOW_NORMAL_AMBIENT
            }
            ShadowDebugMode.STRONG -> {
                sun.shadowStrength = SHADOW_STRONG_STRENGTH
                sun.ambientIntensity = SHADOW_STRONG_AMBIENT
            }
        }
    }

    private fun handleRenderProfileInput() {
        val isPressed = glfwGetKey(SMF.window.id, GLFW_KEY_R) == GLFW_PRESS
        if (isPressed && !renderProfileTogglePressed) {
            val nextProfile = if (scene.renderProfile.shadowsEnabled) {
                RenderProfiles.UNSHADOWED
            } else {
                RenderProfiles.SHADOWED
            }
            RenderProfileManager.applyTo(scene, nextProfile)
            Logger.info("Render profile switched: shadowsEnabled={}", nextProfile.shadowsEnabled)
        }

        renderProfileTogglePressed = isPressed
    }

    private fun handleLocalLightShadowInput() {
        val isPressed = glfwGetKey(SMF.window.id, GLFW_KEY_F12) == GLFW_PRESS
        if (isPressed && !localLightShadowTogglePressed) {
            localLightShadowMode = localLightShadowMode.next()
            applyLocalLightShadowMode()
            Logger.info("Local light shadow caster switched to {}", localLightShadowMode.label)
        }

        localLightShadowTogglePressed = isPressed
    }

    private fun applyLocalLightShadowMode() {
        pointLight.castsShadow = localLightShadowMode == LocalLightShadowMode.BOTH || localLightShadowMode == LocalLightShadowMode.POINT
        spotLight.castsShadow = localLightShadowMode == LocalLightShadowMode.BOTH || localLightShadowMode == LocalLightShadowMode.SPOT
    }

    private fun handleSkyColorTestInput() {
        val isPressed = glfwGetKey(SMF.window.id, GLFW_KEY_F6) == GLFW_PRESS
        if (isPressed && !skyColorTogglePressed) {
            skyColorPaletteIndex = (skyColorPaletteIndex + 1) % skyColorTestPalette.size
            applyFogTestState()
            Logger.info("Sky/Fog color switched to {}", scene.skyColor)
        }

        skyColorTogglePressed = isPressed
    }

    private fun handleFogTestInput() {
        val isPressed = glfwGetKey(SMF.window.id, GLFW_KEY_F8) == GLFW_PRESS
        if (isPressed && !fogTogglePressed) {
            fogEnabled = !fogEnabled
            applyFogTestState()
            Logger.info("Height fog {}", if (fogEnabled) "enabled" else "disabled")
        }

        fogTogglePressed = isPressed
    }

    private fun applyFogTestState() {
        scene.fog.enabled = fogEnabled
        scene.skyColor = if (fogEnabled) {
            scene.fog.color
        } else {
            skyColorTestPalette[skyColorPaletteIndex]
        }
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
            val targetLevel = "stage"
            Logger.info("TestLevel manual level switch requested: {} -> {}", definition.name, targetLevel)
            switchLevel(targetLevel)
        }

        levelSwitchTestPressed = isPressed
    }

    private fun addWideTerrainFogTest() {
        scene.terrain = TerrainLoader.loadModel(
            sizeX = 20000f,
            sizeY = 20000f,
            vertexCount = 96,
            position = Vector3f(-10000f, -4f, -10000f),
            textureMode = BlendedTexture(
                blendMap = Textures.TERRAIN_BLEND_MAP,
                baseTexture = Textures.TERRAIN_GRASS,
                rTexture = Textures.TERRAIN_DIRT,
                gTexture = Textures.TERRAIN_FLOWER,
                bTexture = Textures.TERRAIN_PATH,
            )
        )
        Logger.info("Wide terrain fog test enabled")
    }

    private fun addTeleportTriggerTestFixture() {
        val origin = player.worldTransform.position
        val triggerCenter = origin.add(Vector3f(180f, 85f, 0f))
        val destination = origin.add(Vector3f(360f, 0f, 180f))

        eventTasks.add(
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

    private fun loadEditorStage() {
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
    }

    private fun handleAreaTriggerEvent(eventArea: EventAreaDefinition) {
        val targetLevel = eventArea.properties["switchLevel"]
            ?: eventArea.properties["levelName"]
            ?: eventArea.id.substringAfter("switchLevel:", missingDelimiterValue = "")
                .takeIf { eventArea.id.startsWith("switchLevel:") }

        if (!targetLevel.isNullOrBlank()) {
            Logger.info("TestLevel trigger requested level switch: {} -> {}", eventArea.name, targetLevel)
            switchLevel(targetLevel)
            return
        }

        Logger.info("TestLevel editor AreaTrigger fired: {} ({})", eventArea.id, eventArea.name)
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
        addShadowDebugText()
    }

    private fun addShadowDebugText() {
        val color = if (shadowMode == ShadowDebugMode.OFF) Vector3f(0.3f, 0.3f, 0.3f) else Vector3f(0.2f, 0.75f, 0.35f)
        val profileLabel = if (scene.renderProfile.shadowsEnabled) "Shadowed" else "Unshadowed"
        scene.textElements.add(
            TextElement(
                text = "Render $profileLabel (R) / ${localLightShadowMode.label} local shadows (F12) / Sun shadow ${shadowMode.label} (F5) / Height fog ${if (fogEnabled) "On" else "Off"} (F8)",
                font = shadowDebugFont,
                x = 10f,
                y = 125f,
                color = color,
            )
        )
    }

    private fun updateWorld(delta: Float) {
        PhysicsWorld.update(scene.entities, scene.terrainHeightProvider ?: scene.terrain, delta)
        scene.entities.forEach { it.update(delta) }
        scene.entities.removeAll { it is ProjectileEntity && it.isExpired }

        for (task in eventTasks) {
            task.update(delta)
        }
    }

    private enum class ShadowDebugMode(val label: String) {
        OFF("OFF"),
        NORMAL("NORMAL"),
        STRONG("STRONG");

        fun next(): ShadowDebugMode = when (this) {
            OFF -> NORMAL
            NORMAL -> STRONG
            STRONG -> OFF
        }
    }
    private enum class LocalLightShadowMode(val label: String) {
        BOTH("Both"),
        SPOT("Spot"),
        POINT("Point"),
        OFF("Off");

        fun next(): LocalLightShadowMode = when (this) {
            BOTH -> SPOT
            SPOT -> POINT
            POINT -> OFF
            OFF -> BOTH
        }
    }

    override fun unload() {
        eventTasks.clear()
        super.unload()
    }
}
