package qorrnsmj.smf.game.level

import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.graphic.Scene

abstract class Level {
    val scene: Scene = Scene()
    lateinit var player: Player

    abstract fun load()

    open fun unload() {}

    open fun start() {}

    open fun input(delta: Float) {}

    open fun update(delta: Float) {}
}
