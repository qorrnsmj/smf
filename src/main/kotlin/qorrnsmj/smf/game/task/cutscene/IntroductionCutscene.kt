package qorrnsmj.smf.game.task.cutscene

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.math.Vector3f

class IntroductionCutscene(
    camera: Camera,
    focusPosition: Vector3f,
    destinationEyePosition: Vector3f,
    destinationFront: Vector3f,
    private val onAreaReveal: () -> Unit = {},
    private val onComplete: () -> Unit = {},
) : Cutscene(camera) {
    init {
        val destinationLookAt = destinationEyePosition.add(destinationFront.normalize().scale(10f))

        letterbox(0.1f)
        fade(startSeconds = 0f, durationSeconds = 0.18f, fromAlpha = 0.35f, toAlpha = 0f)
        subtitle(
            startSeconds = 0.8f,
            durationSeconds = 2.8f,
            text = "Welcome to the test field",
        )
        subtitle(
            startSeconds = 3.8f,
            durationSeconds = 2.2f,
            text = "Explore the area ahead",
        )
        cameraKeyframe(
            timeSeconds = 0f,
            position = focusPosition.add(Vector3f(-60f, 40f, 60f)),
            lookAt = focusPosition,
            easingToNext = Easing.EASE_IN_OUT_CUBIC,
        )
        cameraKeyframe(
            timeSeconds = 2.5f,
            position = focusPosition.add(Vector3f(55f, 30f, 45f)),
            lookAt = focusPosition.add(Vector3f(0f, 5f, 0f)),
        )
        cameraKeyframe(
            timeSeconds = 5f,
            position = focusPosition.add(Vector3f(20f, 18f, 30f)),
            lookAt = focusPosition.add(Vector3f(0f, 4f, 0f)),
            interpolationToNext = CameraInterpolation.LINEAR,
        )
        cameraKeyframe(
            timeSeconds = 7f,
            position = destinationEyePosition,
            lookAt = destinationLookAt,
            easingToNext = Easing.SMOOTH_STEP,
        )

        event(
            timeSeconds = 2.5f,
            name = "area-reveal",
            skipPolicy = CutsceneSkipPolicy.FIRE_ON_SKIP,
            action = onAreaReveal,
        )
    }

    override fun onFinished() {
        onComplete()
    }
}
