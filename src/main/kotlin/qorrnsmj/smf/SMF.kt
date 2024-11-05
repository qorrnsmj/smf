package qorrnsmj.smf

import qorrnsmj.smf.core.FixedTimestepGame

object SMF : FixedTimestepGame() {
    @JvmStatic
    fun main(args: Array<String>) {
        start()
    }

    override fun start() {
        gameLoop()
        dispose()
    }

    override fun gameLoop() {
        super.gameLoop()
    }
}
