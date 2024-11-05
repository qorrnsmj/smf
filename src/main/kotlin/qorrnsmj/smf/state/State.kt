package qorrnsmj.smf.state

import qorrnsmj.smf.core.Game

abstract class State {
    /**
     * Gets executed when entering the state, useful for initialization.
     */
    abstract fun enter()

    /**
     * Handles input of the state.
     */
    abstract fun input()

    /**
     * Updates the state (variable timestep)
     *
     * @param delta Time difference in seconds
     */
    abstract fun update(delta: Float = 1f / Game.TARGET_UPS)

    /**
     * Renders the state (with interpolation).
     *
     * @param alpha Alpha value, needed for interpolation
     */
    abstract fun render(alpha: Float = 1f)

    /**
     * Gets executed when leaving the state, useful for disposing.
     */
    abstract fun exit()

    /**
     * Resizes the state.
     *
     * @param width New width
     * @param height New height
     */
    abstract fun resize(width: Int, height: Int)

    override fun toString(): String {
        return this.javaClass.simpleName
    }
}
