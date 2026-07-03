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
        val destinationLookAt = destinationEyePosition.add(destinationFront.normalize().scale(0.1f))

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
            position = focusPosition.add(Vector3f(-0.6f, 0.4f, 0.6f)),
            lookAt = focusPosition,
            easingToNext = Easing.EASE_IN_OUT_CUBIC,
        )
        cameraKeyframe(
            timeSeconds = 2.5f,
            position = focusPosition.add(Vector3f(0.55f, 0.3f, 0.45f)),
            lookAt = focusPosition.add(Vector3f(0f, 0.05f, 0f)),
        )
        cameraKeyframe(
            timeSeconds = 5f,
            position = focusPosition.add(Vector3f(0.2f, 0.18f, 0.3f)),
            lookAt = focusPosition.add(Vector3f(0f, 0.04f, 0f)),
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
