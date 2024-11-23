package qorrnsmj.smf.state

abstract class State {
    abstract fun start()

    abstract fun input()

    abstract fun update(delta: Float)

    abstract fun render(alpha: Float)

    abstract fun stop()

    override fun toString(): String {
        return this.javaClass.simpleName
    }
}
