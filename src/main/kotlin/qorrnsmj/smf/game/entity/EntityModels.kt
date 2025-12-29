package qorrnsmj.smf.game.entity

import qorrnsmj.smf.game.model.component.Mesh
import qorrnsmj.smf.game.model.component.Material
import qorrnsmj.smf.game.model.component.Model

object EntityModels {
    lateinit var EMPTY: Model

    lateinit var STALL: Map<String, Model>
//    lateinit var PLANE: Model
//    lateinit var NORM_CUBE: Map<String, Model>

    // solar system
//    lateinit var SUN: Model
//    lateinit var MERCURY: Model
//    lateinit var VENUS: Model
//    lateinit var EARTH: Model
//    lateinit var MOON: Model
//    lateinit var MARS: Model
//    lateinit var JUPITER: Model
//    lateinit var SATURN_RING: Model
//    lateinit var SATURN: Model
//    lateinit var URANUS: Model
//    lateinit var NEPTUNE: Model

    fun load() {
        EMPTY = Model("empty", Mesh(), Material())

        STALL = EntityLoader.loadModel("stall.glb")
//        PLANE = PlaneModel("plane", "test_plane.png")
//        NORM_CUBE = loadModel("cube.fbx")

        // TODO: Emptyってのやめたい
//        SUN = loadModel("sun.fbx")["sun"] ?: EMPTY
//        MERCURY = loadModel("mercury.fbx")["mercury"] ?: EMPTY
//        VENUS = loadModel("venus.fbx")["venus"] ?: EMPTY
//        EARTH = loadModel("earth.fbx")["earth"] ?: EMPTY
//        MOON = loadModel("moon.fbx")["moon"] ?: EMPTY
//        MARS = loadModel("mars.fbx")["mars"] ?: EMPTY
//        JUPITER = loadModel("jupiter.fbx")["jupiter"] ?: EMPTY
//        SATURN_RING = PlaneModel("saturn_ring", "saturn_ring.png")
//        SATURN = loadModel("saturn.fbx")["saturn"] ?: EMPTY
//        URANUS = loadModel("uranus.fbx")["uranus"] ?: EMPTY
//        NEPTUNE = loadModel("neptune.fbx")["neptune"] ?: EMPTY
    }

    fun getModel(map: Map<String, Model>, key: String): Model {
        return map[key]!!
    }
}
