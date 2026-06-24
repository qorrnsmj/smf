package qorrnsmj.smf.game.map

object Maps {
    lateinit var TEST: GameMap

    fun load() {
        TEST = MapLoader.load("assets/map/test.map")
    }
}
