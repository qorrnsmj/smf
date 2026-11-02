package qorrnsmj.smf.graphic.scene.sky

import qorrnsmj.smf.graphic.scene.SceneEnvironment

class SkySystem(
    private val environment: SceneEnvironment,
) {
    var sky: Skydome?
        get() = environment.sky.skydome
        set(value) {
            environment.sky.skydome = value
        }

    var paused: Boolean = false

    fun update(delta: Float) {
        if (!paused) sky?.update(delta)
    }

    fun clear() {
        sky = null
        paused = false
    }
}
