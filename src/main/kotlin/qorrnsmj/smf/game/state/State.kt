package qorrnsmj.smf.game.state

import qorrnsmj.smf.util.Resizable

abstract class State : Resizable {
    abstract fun start()

    abstract fun input()

    abstract fun update(delta: Float)

    abstract fun render(alpha: Float)

    abstract fun stop()

    override fun toString(): String {
        return this.javaClass.simpleName
    }
}
