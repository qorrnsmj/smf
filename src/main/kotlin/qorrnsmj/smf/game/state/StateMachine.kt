package qorrnsmj.smf.game.state

import org.tinylog.kotlin.Logger

class StateMachine : State() {
    var currentState = States.EMPTY.instance

    fun changeState(newState: States) {
        val newInstance = newState.instance
        Logger.info("Changing state: \"$currentState\" to \"$newInstance\"")

        currentState.stop()
        currentState = newInstance
        currentState.start()
    }

    override fun start() {
        currentState.start()
    }

    override fun input() {
        currentState.input()
    }

    fun update() {
        currentState.update(1f)
    }

    override fun update(delta: Float) {
        currentState.update(delta)
    }

    fun render() {
        currentState.render(1f)
    }

    override fun render(alpha: Float) {
        currentState.render(alpha)
    }

    override fun stop() {
        currentState.stop()
    }

    override fun resize(width: Int, height: Int) {
        currentState.resize(width, height)
    }

    override fun toString(): String {
        return "StateMachine[current: \"${currentState}\"]"
    }
}
