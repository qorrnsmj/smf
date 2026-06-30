package qorrnsmj.smf.state.custom

import qorrnsmj.smf.state.State

class InGameState : State() {
    override fun start() {
        super.start()

        levelManager.loadHomeLevel()
    }
}
