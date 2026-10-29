package qorrnsmj.smf.game.task.base

abstract class Task {
    protected var finished: Boolean = false

    abstract fun update(delta: Float)

    abstract fun reset()

    fun isFinished(): Boolean = finished
}
