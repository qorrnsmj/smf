package qorrnsmj.smf.game.task.cutscene

import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.task.sequence.CameraMovementSequence
import qorrnsmj.smf.math.Vector3f

class IntroductionCutscene(camera: Camera, questAreaPosition: Vector3f) : Cutscene() {
    init {
        val start = Vector3f(questAreaPosition.x, questAreaPosition.y + 100f, questAreaPosition.z - 100f)
        val end = Vector3f(questAreaPosition.x, questAreaPosition.y, questAreaPosition.z)
        addSequence(CameraMovementSequence(camera, start, end, 100f))
    }
}
