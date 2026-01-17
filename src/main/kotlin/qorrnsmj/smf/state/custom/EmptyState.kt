package qorrnsmj.smf.state.custom

import qorrnsmj.smf.state.State

class EmptyState : State() {
    override fun start() {
        super.start()
    }


    override fun update(delta: Float) {
        super.update(delta)
    }

    override fun render(alpha: Float) {
        super.render(alpha)
    }

    override fun stop() {
        super.stop()
    }
}
