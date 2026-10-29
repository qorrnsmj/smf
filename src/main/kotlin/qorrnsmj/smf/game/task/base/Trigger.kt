package qorrnsmj.smf.game.task.base


abstract class Trigger : Task() {
    protected abstract fun check(): Boolean

    protected open fun fire() {}

    override fun update(delta: Float) {
        if (finished) return

        if (check()) {
            fire()
            finished = true
        }
    }

    override fun reset() {
        finished = false
    }
}
