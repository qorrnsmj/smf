package qorrnsmj.smf.game.level.test

import org.lwjgl.glfw.GLFW.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.audio.Audio
import qorrnsmj.smf.audio.AudioManager
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

        introductionCutscene = IntroductionCutscene(
            camera = cutsceneCamera,
            questAreaPosition = player.camera.position
        )
    }

    override fun input(delta: Float) {
        if (!introductionCutscene.isFinished()) return

        player.handleInput(SMF.window, delta)
        
        // Audio testing controls
        handleAudioInput()
    }

    private fun handleAudioInput() {
        val window = SMF.window.id
        
        // BGM Controls
        if (glfwGetKey(window, GLFW_KEY_B) == GLFW_PRESS) {
            Audio.playBGM("test_bgm") // test_bgm.ogg
            Logger.info("Playing BGM: test_bgm")
        }
        
        // SFX Controls
        if (glfwGetKey(window, GLFW_KEY_N) == GLFW_PRESS) {
            Audio.playSFX("test_se") // test_se.ogg
            Logger.info("Playing SFX: test_se")
        }
        
        // Volume Controls
        if (glfwGetKey(window, GLFW_KEY_EQUAL) == GLFW_PRESS) { // + key
            val newVolume = (AudioManager.getMasterVolume() + 0.1f).coerceAtMost(1.0f)
            AudioManager.setMasterVolume(newVolume)
            Logger.info("Master volume: ${(newVolume * 100).toInt()}%")
        }
        if (glfwGetKey(window, GLFW_KEY_MINUS) == GLFW_PRESS) {
            val newVolume = (AudioManager.getMasterVolume() - 0.1f).coerceAtLeast(0.0f)
            AudioManager.setMasterVolume(newVolume)
            Logger.info("Master volume: ${(newVolume * 100).toInt()}%")
        }
        
        // BGM Volume Controls
        if (glfwGetKey(window, GLFW_KEY_LEFT_BRACKET) == GLFW_PRESS) { // [ key
            val newVolume = (AudioManager.getBGMVolume() - 0.1f).coerceAtLeast(0.0f)
            AudioManager.setBGMVolume(newVolume)
            Logger.info("BGM volume: ${(newVolume * 100).toInt()}%")
        }
        if (glfwGetKey(window, GLFW_KEY_RIGHT_BRACKET) == GLFW_PRESS) { // ] key
            val newVolume = (AudioManager.getBGMVolume() + 0.1f).coerceAtMost(1.0f)
            AudioManager.setBGMVolume(newVolume)
            Logger.info("BGM volume: ${(newVolume * 100).toInt()}%")
        }
        
        // Audio System Info
        if (glfwGetKey(window, GLFW_KEY_I) == GLFW_PRESS) {
            val stats = AudioManager.getStats()
            Logger.info("=== Audio System Status ===")
            Logger.info("Master Volume: ${(stats.masterVolume * 100).toInt()}%")
            Logger.info("BGM Volume: ${(stats.bgmVolume * 100).toInt()}%")
            Logger.info("SFX Volume: ${(stats.sfxVolume * 100).toInt()}%")
            Logger.info("BGM Playing: ${stats.isBGMPlaying}")
            Logger.info("Active Sources: ${stats.activeSources}/${stats.totalSources}")
            Logger.info("===========================")
        }
    }

    override fun update(delta: Float) {
        player.update(delta, scene.terrain as HeightProvider)

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
    }

    override fun unload() {
        scene.entities.clear()
        scene.lights.clear()
    }
}
