package qorrnsmj.smf.game.task.sequence

import qorrnsmj.smf.game.task.Task

abstract class Sequence : Task() {
    protected var elapsedTime: Float = 0f

    override fun update(delta: Float) {
        if (finished) return
        elapsedTime += delta
    }

    override fun reset() {
        elapsedTime = 0f
        finished = false
    }
}
