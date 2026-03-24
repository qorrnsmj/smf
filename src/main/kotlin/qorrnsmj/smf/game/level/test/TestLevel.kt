package qorrnsmj.smf.game.level.test

import org.lwjgl.glfw.GLFW.*
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.audio.Audio
import qorrnsmj.smf.audio.AudioManager
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
import qorrnsmj.smf.math.Vector3
import qorrnsmj.smf.math.Matrix2
import qorrnsmj.smf.math.Matrix3
import qorrnsmj.smf.math.Vector2f

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
        
        // Physics testing controls
        handlePhysicsInput()
        
        // Audio testing controls
        handleAudioInput()
    }

    private fun handlePhysicsInput() {
        val window = SMF.window.id
        
        // Physics debug visualization toggle
        if (glfwGetKey(window, GLFW_KEY_P) == GLFW_PRESS) {
            PhysicsDebugRenderer.toggleDebug()
            Logger.info("Physics debug visualization toggled")
        }
        
        // Reset stall position (for testing gravity)
        if (glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS) {
            stall.position = Vector3f(80f, 50f, 80f)  // High above ground
            stall.physicsComponent?.velocity = Vector3f(0f, 0f, 0f)
            Logger.info("Reset stall position for gravity test")
        }
        
        // Toggle stall physics
        if (glfwGetKey(window, GLFW_KEY_T) == GLFW_PRESS) {
            stall.physicsComponent?.let { physics ->
                physics.useGravity = !physics.useGravity
                Logger.info("Stall gravity: ${if (physics.useGravity) "ENABLED" else "DISABLED"}")
            }
        }
        
        // Apply upward impulse to stall
        if (glfwGetKey(window, GLFW_KEY_U) == GLFW_PRESS) {
            stall.physicsComponent?.applyImpulse(Vector3f(0f, 5f, 0f))
            Logger.info("Applied upward impulse to stall")
        }
        
        // Debug options toggle
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            PhysicsDebugRenderer.setDebugOptions(
                colliders = true,
                velocity = true,
                forces = true
            )
            Logger.info("Physics debug options updated (colliders, velocity, forces all enabled)")
        }
        
        // Physics system status
        if (glfwGetKey(window, GLFW_KEY_O) == GLFW_PRESS) {
            val stats = PhysicsWorld.getStats()
            Logger.info("=== Physics System Status ===")
            Logger.info("Initialized: ${stats.isInitialized}")
            Logger.info("Active Entities: ${stats.activeEntities}")
            Logger.info("Update Count: ${stats.updateCount}")
            Logger.info("Last Update Time: ${stats.lastUpdateTimeMs}ms")
            Logger.info("============================")
        }
        
        // Matrix testing controls
        handleMatrixTesting()
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

    private fun handleMatrixTesting() {
        val window = SMF.window.id
        
        // Test Matrix2 functionality with M key
        if (glfwGetKey(window, GLFW_KEY_M) == GLFW_PRESS) {
            testMatrix2Functionality()
        }
        
        // Test Matrix4 functionality with 4 key
        if (glfwGetKey(window, GLFW_KEY_4) == GLFW_PRESS) {
            testMatrix4Functionality()
        }
    }
    
    private fun testMatrix2Functionality() {
        Logger.info("=== Testing Matrix2 Functionality ===")
        
        // Test constructor and identity
        val identity = Matrix2.identity()
        val matrix1 = Matrix2(Vector2f(2f, 0f), Vector2f(1f, 3f))
        Logger.info("Identity matrix: $identity")
        Logger.info("Matrix1: $matrix1")
        
        // Test operator overloading
        val matrix2 = Matrix2(Vector2f(1f, 3f), Vector2f(2f, 4f))
        Logger.info("Matrix2: $matrix2")
        
        val addition = matrix1 + matrix2
        Logger.info("Matrix1 + Matrix2: $addition")
        
        val multiplication = matrix1 * matrix2
        Logger.info("Matrix1 * Matrix2: $multiplication")
        
        val scalarMult = matrix1 * 2f
        Logger.info("Matrix1 * 2: $scalarMult")
        
        // Test vector multiplication
        val vector = Vector2f(1f, 2f)
        val vectorResult = matrix1 * vector
        Logger.info("Matrix1 * Vector(1,2): (${vectorResult.x}, ${vectorResult.y})")
        
        // Test transpose
        val transposed = matrix1.transpose()
        Logger.info("Matrix1 transposed: $transposed")
        
        // Test determinant and inverse
        Logger.info("Matrix1 determinant: ${matrix1.determinant()}")
        val inverse = matrix1.inverse()
        if (inverse != null) {
            Logger.info("Matrix1 inverse: $inverse")
            val shouldBeIdentity = matrix1 * inverse
            Logger.info("Matrix1 * Inverse (should be ~identity): $shouldBeIdentity")
        }
        
        // Test buffer conversion
        val buffer = matrix1.toBuffer()
        val array = matrix1.toFloatArray()
        Logger.info("Matrix1 as array: [${array.joinToString(", ") { "%.3f".format(it) }}]")
        
        Logger.info("=== Matrix2 Test Complete ===")
    }
    
    private fun testMatrix4Functionality() {
        Logger.info("=== Testing Matrix4 Functionality ===")
        
        // Test identity and constructor
        val identity = Matrix4.IDENTITY
        Logger.info("Identity matrix: ${formatMatrix4(identity)}")
        
        // Test column constructor
        val matrix1 = Matrix4(
            Vector4f(1f, 0f, 0f, 0f),
            Vector4f(0f, 1f, 0f, 0f),
            Vector4f(0f, 0f, 1f, 0f),
            Vector4f(5f, 10f, 15f, 1f) // Translation
        )
        Logger.info("Translation matrix: ${formatMatrix4(matrix1)}")
        
        // Test transformation matrices from companion object
        val translation = Matrix4.translate(2f, 3f, 4f)
        Logger.info("Translation(2,3,4): ${formatMatrix4(translation)}")
        
        val rotation = Matrix4.rotate(90f, 0f, 0f, 1f) // 90 degrees around Z-axis
        Logger.info("Rotation(90°,Z): ${formatMatrix4(rotation)}")
        
        val scale = Matrix4.scale(2f, 2f, 2f)
        Logger.info("Scale(2,2,2): ${formatMatrix4(scale)}")
        
        // Test operator overloading
        val matrix2 = Matrix4.translate(1f, 1f, 1f)
        val combined = matrix1 * matrix2
        Logger.info("Matrix1 * Matrix2: ${formatMatrix4(combined)}")
        
        val scaled = matrix1 * 2f
        Logger.info("Matrix1 * 2: ${formatMatrix4(scaled)}")
        
        val added = matrix1 + matrix2
        Logger.info("Matrix1 + Matrix2: ${formatMatrix4(added)}")
        
        // Test vector multiplication
        val vector = Vector4f(1f, 2f, 3f, 1f)
        val vectorResult = matrix1 * vector
        Logger.info("Matrix1 * Vector(1,2,3,1): (${vectorResult.x}, ${vectorResult.y}, ${vectorResult.z}, ${vectorResult.w})")
        
        // Test transpose
        val transposed = matrix1.transpose()
        Logger.info("Matrix1 transposed: ${formatMatrix4(transposed)}")
        
        // Test perspective matrix
        val perspective = Matrix4.perspective(60f, 1.333f, 0.1f, 100f)
        Logger.info("Perspective matrix: ${formatMatrix4(perspective)}")
        
        // Test orthographic matrix
        val ortho = Matrix4.orthographic(-10f, 10f, -10f, 10f, 0.1f, 100f)
        Logger.info("Orthographic matrix: ${formatMatrix4(ortho)}")
        
        // Test inversion
        try {
            val inverse = matrix1.invert()
            Logger.info("Matrix1 inverse: ${formatMatrix4(inverse)}")
            
            val shouldBeIdentity = matrix1 * inverse
            Logger.info("Matrix1 * Inverse (should be ~identity): ${formatMatrix4(shouldBeIdentity)}")
        } catch (e: IllegalStateException) {
            Logger.info("Matrix inversion failed: ${e.message}")
        }
        
        // Test Vector3f convenience methods
        val translationVec = Matrix4.translate(Vector3f(5f, 6f, 7f))
        Logger.info("Translation(Vector3f): ${formatMatrix4(translationVec)}")
        
        val rotationVec = Matrix4.rotate(45f, Vector3f(1f, 1f, 0f))
        Logger.info("Rotation(45°, Vector3f): ${formatMatrix4(rotationVec)}")
        
        val scaleVec = Matrix4.scale(Vector3f(3f, 4f, 5f))
        Logger.info("Scale(Vector3f): ${formatMatrix4(scaleVec)}")
        
        val uniformScale = Matrix4.scale(1.5f)
        Logger.info("Uniform Scale(1.5): ${formatMatrix4(uniformScale)}")
        
        Logger.info("=== Matrix4 Test Complete ===")
    }
    
    private fun formatMatrix4(matrix: Matrix4): String {
        return """
        [${String.format("%6.2f", matrix.m00)} ${String.format("%6.2f", matrix.m01)} ${String.format("%6.2f", matrix.m02)} ${String.format("%6.2f", matrix.m03)}]
        [${String.format("%6.2f", matrix.m10)} ${String.format("%6.2f", matrix.m11)} ${String.format("%6.2f", matrix.m12)} ${String.format("%6.2f", matrix.m13)}]
        [${String.format("%6.2f", matrix.m20)} ${String.format("%6.2f", matrix.m21)} ${String.format("%6.2f", matrix.m22)} ${String.format("%6.2f", matrix.m23)}]
        [${String.format("%6.2f", matrix.m30)} ${String.format("%6.2f", matrix.m31)} ${String.format("%6.2f", matrix.m32)} ${String.format("%6.2f", matrix.m33)}]
        """.trimIndent()
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
    }

    override fun unload() {
        scene.entities.clear()
        scene.lights.clear()
        PhysicsDebugRenderer.cleanup()
    }
}
