package qorrnsmj.smf.game.task.cutscene

import org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
import org.lwjgl.glfw.GLFW.GLFW_KEY_F8
import org.lwjgl.glfw.GLFW.GLFW_KEY_F9
import org.lwjgl.glfw.GLFW.GLFW_KEY_F10
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.graphic.text.Font
import qorrnsmj.smf.graphic.text.TextAnchor
import qorrnsmj.smf.graphic.text.TextElement
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.window.Window

class CutsceneManager(
    private val scene: Scene,
    private var subtitleFont: Font? = null,
) {
    private var activeCutscene: Cutscene? = null
    private var gameplayCamera: Camera? = null
    private var cutsceneCamera: Camera? = null
    private var synchronizeEyePosition: ((Vector3f) -> Unit)? = null
    private var skipPressed = false
    private var pausePressed = false
    private var fastForwardPressed = false
    private var restartPressed = false

    var showDebugControls: Boolean = false

    var isPaused: Boolean = false
        private set

    var playbackSpeed: Float = 1f
        private set

    val isPlaying: Boolean
        get() = activeCutscene != null

    val isWorldPaused: Boolean
        get() = activeCutscene?.freezeWorld == true

    fun play(
        cutscene: Cutscene,
        camera: Camera,
        returnTo: Camera = scene.camera,
        synchronizeEyePosition: ((Vector3f) -> Unit)? = null,
    ) {
        stop(restoreCamera = false)
        activeCutscene = cutscene
        gameplayCamera = returnTo
        cutsceneCamera = camera
        this.synchronizeEyePosition = synchronizeEyePosition
        scene.camera = camera
        isPaused = false
        playbackSpeed = 1f
        cutscene.reset()
        applyVisuals(cutscene)
    }

    fun update(deltaSeconds: Float) {
        val cutscene = activeCutscene ?: return
        if (!isPaused) {
            cutscene.update(deltaSeconds * playbackSpeed)
        }
        applyVisuals(cutscene)
        if (cutscene.isFinished()) {
            finish()
        }
    }

    fun handleInput(window: Window) {
        if (!isPlaying) return

        handleEdge(window, GLFW_KEY_ESCAPE, skipPressed, { skipPressed = it }) {
            activeCutscene?.skip()
        }
        handleEdge(window, GLFW_KEY_F8, pausePressed, { pausePressed = it }) {
            isPaused = !isPaused
        }
        handleEdge(window, GLFW_KEY_F9, fastForwardPressed, { fastForwardPressed = it }) {
            playbackSpeed = if (playbackSpeed == 1f) 4f else 1f
        }
        handleEdge(window, GLFW_KEY_F10, restartPressed, { restartPressed = it }) {
            activeCutscene?.reset()
            isPaused = false
            playbackSpeed = 1f
        }
    }

    fun stop(restoreCamera: Boolean = true) {
        if (restoreCamera) {
            restoreGameplayCamera()
        }
        activeCutscene = null
        cutsceneCamera = null
        gameplayCamera = null
        synchronizeEyePosition = null
        isPaused = false
        playbackSpeed = 1f
        scene.cinematicOverlay.clear()
    }

    fun setSubtitleFont(font: Font) {
        subtitleFont = font
    }

    private fun finish() {
        val camera = cutsceneCamera
        val returnCamera = gameplayCamera
        if (camera != null && returnCamera != null) {
            val finalEyePosition = camera.position.copy()
            synchronizeEyePosition?.invoke(finalEyePosition)
            returnCamera.position = finalEyePosition
            returnCamera.setFront(camera.getFront())
        }
        restoreGameplayCamera()
        activeCutscene = null
        cutsceneCamera = null
        gameplayCamera = null
        synchronizeEyePosition = null
        isPaused = false
        playbackSpeed = 1f
        scene.cinematicOverlay.clear()
    }

    private fun restoreGameplayCamera() {
        gameplayCamera?.let { scene.camera = it }
    }

    private fun applyVisuals(cutscene: Cutscene) {
        val state = cutscene.visualState()
        scene.cinematicOverlay.fadeAlpha = state.fadeAlpha
        scene.cinematicOverlay.fadeColor = state.fadeColor
        scene.cinematicOverlay.letterboxRatio = state.letterboxRatio
        scene.cinematicOverlay.subtitle = state.subtitle?.let { cue ->
            subtitleFont?.let { font ->
                TextElement(
                    text = cue.text,
                    font = font,
                    x = 0f,
                    y = -80f,
                    color = cue.color,
                    anchor = TextAnchor.BOTTOM_CENTER,
                )
            }
        }
        scene.cinematicOverlay.debugStatus = if (showDebugControls) {
            subtitleFont?.let { font ->
                val pauseLabel = if (isPaused) "PAUSED | " else ""
                TextElement(
                    text = String.format(
                        "[CUTSCENE] %s%.1f / %.1fs | x%.0f | Esc Skip, F8 Pause, F9 Speed, F10 Restart",
                        pauseLabel,
                        cutscene.currentTimeSeconds,
                        cutscene.durationSeconds,
                        playbackSpeed,
                    ),
                    font = font,
                    x = 10f,
                    y = 130f,
                    color = qorrnsmj.smf.math.Vector3f(1f, 0.85f, 0.25f),
                )
            }
        } else {
            null
        }
    }

    private fun handleEdge(
        window: Window,
        key: Int,
        wasPressed: Boolean,
        setPressed: (Boolean) -> Unit,
        action: () -> Unit,
    ) {
        val pressed = glfwGetKey(window.id, key) == GLFW_PRESS
        if (pressed && !wasPressed) action()
        setPressed(pressed)
    }

    private fun Vector3f.copy(): Vector3f = Vector3f(x, y, z)
}
