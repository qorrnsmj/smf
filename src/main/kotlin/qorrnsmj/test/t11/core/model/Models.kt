package qorrnsmj.test.t11.core.model

object Models {
    val TREE_MODEL = loadModel("test11_tree.obj", "../../test/test11_tree.png")
    val SHIP1_MODEL = loadModel("test11_ship1.obj", "../../test/test10_white.png")
    val SHIP3_MODEL = loadModel("test11_ship3.obj", "../../test/test10_white.png")
    val STALL_MODEL = loadModel("test11_stall.obj", "../../test/test11_stall.png")

    private fun loadModel(modelFile: String, imageFile: String): Model {
        return Model(OBJLoader.loadModel(modelFile), Loader.loadTexture(imageFile))
    }
}