package qorrnsmj.smf.state

import org.tinylog.kotlin.Logger

class StateMachine : State() {
    private var currentState: State = States.EMPTY

    fun changeState(newState: State) {
        Logger.info("Changing state: \"$currentState\" to \"$newState\"")

        currentState.stop()
        currentState = newState
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

    override fun toString(): String {
        return "StateMachine[current: \"${currentState}\"]"
    }
}
