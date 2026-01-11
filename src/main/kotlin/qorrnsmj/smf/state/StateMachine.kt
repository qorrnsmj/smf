package qorrnsmj.smf.state

import org.tinylog.kotlin.Logger

class StateMachine {
    private var currentState: State = States.EMPTY

    fun changeState(newState: State) {
        Logger.info("Changing state: \"$currentState\" to \"$newState\"")

        currentState.stop()
        currentState = newState
        currentState.start()
    }

    fun start() {
        currentState.start()
    }

    fun input() {
        currentState.input()
    }

    fun update() {
        currentState.update(1f)
    }

    fun update(delta: Float) {
        currentState.update(delta)
    }

    fun render() {
        currentState.render(1f)
    }

    fun render(alpha: Float) {
        currentState.render(alpha)
    }

    fun stop() {
        currentState.stop()
    }

    override fun toString(): String {
        return "StateMachine[current: \"${currentState}\"]"
    }
}
