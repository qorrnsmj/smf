package qorrnsmj.smf.state.custom

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.custom.Cube
import qorrnsmj.smf.state.State

class ExampleState1 : State() {
    private val cube1 = Cube(0f, 0f, 0f, 1f)

    override fun enter() {
    }

    override fun input() {
    }

    override fun update(delta: Float) {
        cube1.updateAngle(cube1.angle + 0.02f)
        Logger.debug("fps: ${SMF.timer.getFPS()}")
    }

    override fun render(alpha: Float) {
        SMF.renderer.clear()
        SMF.renderer.begin()

        cube1.draw()

        SMF.renderer.end()
    }

    override fun exit() {
    }

    override fun resize(width: Int, height: Int) {
    }
}
