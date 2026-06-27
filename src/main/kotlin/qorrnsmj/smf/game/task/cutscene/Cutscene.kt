package qorrnsmj.smf.game.task.cutscene

import qorrnsmj.smf.SMF
import qorrnsmj.smf.audio.AudioBuffer
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.task.Task
import qorrnsmj.smf.math.Vector3f

abstract class Cutscene(
    private val camera: Camera,
    val freezeWorld: Boolean = true,
) : Task() {
    private val cameraTrack = CameraTrack(camera)
    private val events = mutableListOf<TimedCutsceneEvent>()
    private val fades = mutableListOf<FadeCue>()
    private val subtitles = mutableListOf<SubtitleCue>()
    private val bgmFades = mutableListOf<BgmFadeCue>()
    private var elapsedSeconds = 0f
    private var started = false
    private var explicitEndSeconds = 0f
    private var letterboxRatio = 0f

    val currentTimeSeconds: Float
        get() = elapsedSeconds

    val durationSeconds: Float
        get() = maxOf(
            cameraTrack.endTimeSeconds,
            events.maxOfOrNull { it.timeSeconds } ?: 0f,
            fades.maxOfOrNull { it.endSeconds } ?: 0f,
            subtitles.maxOfOrNull { it.endSeconds } ?: 0f,
            bgmFades.maxOfOrNull { it.endSeconds } ?: 0f,
            explicitEndSeconds,
        )

    override fun update(delta: Float) {
        if (finished) return
        require(delta >= 0f) { "Cutscene delta must be zero or greater" }

        if (!started) {
            started = true
            onStart()
            cameraTrack.apply(0f)
            fireEventsThrough(0f)
        }

        elapsedSeconds = (elapsedSeconds + delta).coerceAtMost(durationSeconds)
        cameraTrack.apply(elapsedSeconds)
        fireEventsThrough(elapsedSeconds)
        applyBgmFades()

        if (elapsedSeconds >= durationSeconds) {
            finished = true
            onFinished()
        }
    }

    override fun reset() {
        elapsedSeconds = 0f
        started = false
        finished = false
        events.forEach { it.fired = false }
        cameraTrack.apply(0f)
        onReset()
    }

    fun skip() {
        if (finished) return

        if (!started) {
            started = true
            onStart()
        }
        elapsedSeconds = durationSeconds
        cameraTrack.apply(elapsedSeconds)
        fireSkipEvents()
        applyBgmFades()
        finished = true
        onFinished()
    }

    protected fun cameraKeyframe(
        timeSeconds: Float,
        position: Vector3f,
        lookAt: Vector3f,
        easingToNext: Easing = Easing.SMOOTH_STEP,
        interpolationToNext: CameraInterpolation = CameraInterpolation.CATMULL_ROM,
    ) {
        cameraTrack.add(
            CameraKeyframe(
                timeSeconds = timeSeconds,
                position = position.copy(),
                lookAt = lookAt.copy(),
                easingToNext = easingToNext,
                interpolationToNext = interpolationToNext,
            )
        )
    }

    protected fun event(
        timeSeconds: Float,
        name: String = "event",
        skipPolicy: CutsceneSkipPolicy = CutsceneSkipPolicy.SKIP,
        action: () -> Unit,
    ) {
        events.add(TimedCutsceneEvent(timeSeconds, name, skipPolicy, action))
        events.sortBy { it.timeSeconds }
    }

    protected fun playSound(
        timeSeconds: Float,
        sound: AudioBuffer,
        volume: Float = 1f,
        pitch: Float = 1f,
    ) {
        event(timeSeconds, "sound", CutsceneSkipPolicy.SKIP) {
            SMF.audioManager.playSFX(sound, volume, pitch)
        }
    }

    protected fun playBgm(
        timeSeconds: Float,
        music: AudioBuffer,
        volume: Float = 1f,
        loop: Boolean = true,
        skipPolicy: CutsceneSkipPolicy = CutsceneSkipPolicy.SKIP,
    ) {
        event(timeSeconds, "play-bgm", skipPolicy) {
            SMF.audioManager.playBGM(music, volume, loop)
        }
    }

    protected fun stopBgm(
        timeSeconds: Float,
        skipPolicy: CutsceneSkipPolicy = CutsceneSkipPolicy.FIRE_ON_SKIP,
    ) {
        event(timeSeconds, "stop-bgm", skipPolicy) {
            SMF.audioManager.stopBGM()
        }
    }

    protected fun fadeBgm(
        startSeconds: Float,
        durationSeconds: Float,
        fromVolume: Float,
        toVolume: Float,
    ) {
        require(startSeconds >= 0f) { "BGM fade start time must be zero or greater" }
        require(durationSeconds >= 0f) { "BGM fade duration must be zero or greater" }
        bgmFades.add(
            BgmFadeCue(
                startSeconds = startSeconds,
                durationSeconds = durationSeconds,
                fromVolume = fromVolume.coerceIn(0f, 1f),
                toVolume = toVolume.coerceIn(0f, 1f),
            )
        )
        bgmFades.sortBy { it.startSeconds }
    }

    protected fun endAt(timeSeconds: Float) {
        require(timeSeconds >= 0f) { "Cutscene end time must be zero or greater" }
        explicitEndSeconds = maxOf(explicitEndSeconds, timeSeconds)
    }

    protected fun letterbox(ratio: Float = 0.1f) {
        letterboxRatio = ratio.coerceIn(0f, 0.45f)
    }

    protected fun fade(
        startSeconds: Float,
        durationSeconds: Float,
        fromAlpha: Float,
        toAlpha: Float,
        color: Vector3f = Vector3f(0f, 0f, 0f),
    ) {
        require(startSeconds >= 0f) { "Fade start time must be zero or greater" }
        require(durationSeconds >= 0f) { "Fade duration must be zero or greater" }
        fades.add(
            FadeCue(
                startSeconds = startSeconds,
                durationSeconds = durationSeconds,
                fromAlpha = fromAlpha.coerceIn(0f, 1f),
                toAlpha = toAlpha.coerceIn(0f, 1f),
                color = color.copy(),
            )
        )
        fades.sortBy { it.startSeconds }
    }

    protected fun subtitle(
        startSeconds: Float,
        durationSeconds: Float,
        text: String,
        color: Vector3f = Vector3f(1f, 1f, 1f),
    ) {
        require(startSeconds >= 0f) { "Subtitle start time must be zero or greater" }
        require(durationSeconds >= 0f) { "Subtitle duration must be zero or greater" }
        subtitles.add(
            SubtitleCue(
                startSeconds = startSeconds,
                durationSeconds = durationSeconds,
                text = text,
                color = color.copy(),
            )
        )
        subtitles.sortBy { it.startSeconds }
    }

    internal fun visualState(): CutsceneVisualState {
        val fade = fades.lastOrNull { elapsedSeconds >= it.startSeconds }
        val subtitle = subtitles.lastOrNull {
            elapsedSeconds >= it.startSeconds && elapsedSeconds < it.endSeconds
        }
        return CutsceneVisualState(
            fadeAlpha = fade?.alphaAt(elapsedSeconds) ?: 0f,
            fadeColor = fade?.color?.copy() ?: Vector3f(0f, 0f, 0f),
            letterboxRatio = letterboxRatio,
            subtitle = subtitle,
        )
    }

    protected open fun onStart() {}

    protected open fun onFinished() {}

    protected open fun onReset() {}

    private fun fireEventsThrough(timeSeconds: Float) {
        events
            .asSequence()
            .filter { !it.fired && it.timeSeconds <= timeSeconds }
            .forEach {
                it.fired = true
                it.action()
            }
    }

    private fun fireSkipEvents() {
        events
            .asSequence()
            .filter { !it.fired && it.skipPolicy == CutsceneSkipPolicy.FIRE_ON_SKIP }
            .forEach {
                it.fired = true
                it.action()
            }
    }

    private fun applyBgmFades() {
        bgmFades
            .lastOrNull { elapsedSeconds >= it.startSeconds }
            ?.let { SMF.audioManager.setCurrentBGMVolume(it.volumeAt(elapsedSeconds)) }
    }

    private fun Vector3f.copy(): Vector3f = Vector3f(x, y, z)
}
