package qorrnsmj.smf.state.custom

import qorrnsmj.smf.state.State

class EmptyState : State() {
    override fun enter() {
    }

    override fun input() {
    }

    override fun update(delta: Float) {
    }

    override fun render(alpha: Float) {
    }

    override fun exit() {
    }

    override fun resize(width: Int, height: Int) {
    }
}
