package qorrnsmj.smf.game.level.test

import org.lwjgl.glfw.GLFW.GLFW_KEY_F7
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.level.GlbLevel
import qorrnsmj.smf.game.level.GlbLevelApplier
import qorrnsmj.smf.game.level.GlbLevelLoader
import qorrnsmj.smf.game.level.Level
import qorrnsmj.smf.game.task.cutscene.IntroductionCutscene
import qorrnsmj.smf.game.task.trigger.AreaEnterTrigger
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.terrain.TerrainLoader
import qorrnsmj.smf.graphic.terrain.component.BlendedTexture
import qorrnsmj.smf.graphic.text.DebugTextManager
import qorrnsmj.smf.graphic.text.FontLoader
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsWorld
import qorrnsmj.smf.physics.collision.shape.CapsuleCollider

class TestLevel : Level() {
    private companion object {
        const val USE_TEMPORARY_GLB_LEVEL = false
        const val USE_WIDE_TERRAIN_FOG_TEST = true
    }

    private lateinit var pointLight: PointLight
    private lateinit var cutsceneCamera: Camera
    private val triggers: MutableList<AreaEnterTrigger> = mutableListOf()
    private val debugTextManager = DebugTextManager()
    private var fogEnabled = true
    private var fogTogglePressed = false

    override fun load() {
        player = Player(moveSpeed = 5.5f, jumpSpeed = 10f)
        scene.entities.add(player)
        logPlayerCapsuleCollision()

        loadGlbLevel()
        if (USE_WIDE_TERRAIN_FOG_TEST) {
            addWideTerrainFogTest()
        }

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

        scene.skyboxEnabled = false
        applyFogTestState()

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
            handleMistTestInput()
            return
        }

        player.handleInput(SMF.window, delta)
        handleMistTestInput()
    }

    private fun handleMistTestInput() {
        val isPressed = glfwGetKey(SMF.window.id, GLFW_KEY_F7) == GLFW_PRESS
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
            Vector3f(0f, 0f, 0f)
        }
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

    private fun loadGlbLevel() {
        val glbLevel = if (USE_TEMPORARY_GLB_LEVEL) {
            GlbLevel.createTemporaryTestLevel()
        } else {
            GlbLevelLoader.load("assets/level/test_level.glb")
        }
        triggers.addAll(
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

        for (trigger in triggers) {
            trigger.update(delta)
        }
    }

    override fun unload() {
        super.unload()
    }
}
