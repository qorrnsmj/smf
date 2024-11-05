package qorrnsmj.smf.state

import org.tinylog.kotlin.Logger

class StateMachine : State() {
    private var currentState = States.EMPTY.instance

    fun changeState(newState: States) {
        val newInstance = newState.instance
        Logger.debug("Changing state: \"$currentState\" to \"$newInstance\"")

        currentState.exit()
        currentState = newInstance
        currentState.enter()
    }

    override fun enter() {
        currentState.enter()
    }

    override fun input() {
        currentState.input()
    }

    override fun update(delta: Float) {
        currentState.update(delta)
    }

    override fun render(alpha: Float) {
        currentState.render(alpha)
    }

    override fun exit() {
        currentState.exit()
    }

    override fun resize(width: Int, height: Int) {
        currentState.resize(width, height)
    }

    override fun toString(): String {
        return currentState.toString()
    }
}
