//package qorrnsmj.test.t5
//
//import org.lwjgl.glfw.GLFW
//import org.lwjgl.glfw.GLFWErrorCallback
//import org.lwjgl.glfw.GLFWKeyCallback
//import org.lwjgl.opengl.GL
//import org.lwjgl.opengl.GL30.*
//import org.lwjgl.system.MemoryStack
//import org.lwjgl.system.MemoryUtil
//import qorrnsmj.smf.graphic.render.View
//import qorrnsmj.smf.graphic.render.Projection.getPerspectiveMatrix
//import qorrnsmj.smf.math.Vector3f
//
//// FIXME: 一個の立方体が回ってるものに直す
//// https://stackoverflow.com/questions/45474204/lwjgl-a-function-that-is-not-available-in-the-current-context-was-called
///** 回転する立方体を描画
// *
// *     これをmainに追加
// *     glEnable(GL_DEPTH_TEST)
// *
// *     これをrender()前に呼び出さないと、depth-bufferがクリアされない
// *     glClear(GL_DEPTH_BUFFER_BIT)
// *
// * 注意!
// * OpenGL3.2以降、特にCore Profileでは、GL_TRIANGLESやGL_QUADSなどの
// * プリミティブ描画コールを使用する際に、いくつかの制約と変更が導入された。次の設定にする時は注意!
// *
// *     glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
// *     glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2)
// *     glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
// *     glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
// *
// *  1. パフォーマンスの向上
// * OpenGL 3.2 以降、GPU の性能を最大限に引き出すため、より効率的な描画方法（バッチ処理やインスタンシングなど）が推奨されています。
// * これにより、ドローコールの数を減らし、GPU のパイプラインをより効率的に利用できます。
// * 2. 固定機能パイプラインの廃止
// * OpenGL 3.2 以降は、固定機能パイプラインが廃止され、プログラマブルシェーダーが推奨されるようになりました。
// * プリミティブ描画コールは、固定機能パイプラインに依存していたため、コアプロファイルでは利用できなくなりました。
// * 3. シェーダーの利用
// * OpenGL 3.2 以降では、すべての描画においてシェーダープログラムを使用することが前提となります。これにより、より柔軟で強力なグラフィックスの処理が可能になります。
// * プリミティブの描画方法もシェーダーを通じて定義されるため、より自由な形で描画が行えるようになります。
// * 4. 描画呼び出しの簡素化
// * コアプロファイルでは、データを頂点バッファオブジェクト (VBO) に格納し、描画する際には glDrawArrays や glDrawElements を使用することが一般的です。
// * これにより、描画に必要なデータの管理がしやすくなり、GPU によるデータの処理が効率的になります。
// */
//object Test5 {
//    const val width = 1600
//    const val height = 1000
//
//    private val errorCallback = GLFWErrorCallback.createPrint(System.err)
//
//    private val keyCallback = object : GLFWKeyCallback() {
//        override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
//            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
//                GLFW.glfwSetWindowShouldClose(window, true)
//            }
//        }
//    }
//
//    @JvmStatic
//    fun main(args: Array<String>) {
//        /* Set the error callback */
//        //GLFW.glfwSetErrorCallback(errorCallback)
//
//        /* Initialize GLFW */
//        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }
//
//        /* Create window */
//        val window = GLFW.glfwCreateWindow(width, height, "Test5", MemoryUtil.NULL, MemoryUtil.NULL)
//        check(window != MemoryUtil.NULL) { "Failed to create the GLFW window" }
//
//        /* Center the window on screen */
//        val vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())!!
//        GLFW.glfwSetWindowPos(
//            window,
//            (vidMode.width() - width) / 2,
//            (vidMode.height() - height) / 2
//        )
//
//        /* Create OpenGL context */
//        GLFW.glfwMakeContextCurrent(window)
//        GL.createCapabilities()
//
//        // Enable depth test
//        // Enable back face culling
//        glEnable(GL_DEPTH_TEST)
//        glEnable(GL_CULL_FACE)
//        glCullFace(GL_BACK)
//
//        /* Enable vertical synchronization */
//        GLFW.glfwSwapInterval(1)
//
//        /* Set the key callback */
//        GLFW.glfwSetKeyCallback(window, keyCallback)
//
//        /* Declare buffers for using inside the loop */
//        val width = MemoryUtil.memAllocInt(1)
//        val height = MemoryUtil.memAllocInt(1)
//
//        /* Loop until window gets closed */
//        while (!GLFW.glfwWindowShouldClose(window)) {
//            /* Get width and height to calculate the ratio */
//            GLFW.glfwGetFramebufferSize(window, width, height)
//            val ratio = width.get() / height.get().toFloat()
//
//            /* Rewind buffers for next get */
//            width.rewind()
//            height.rewind()
//
//            /* Set viewport and clear screen */
//            glViewport(0, 0, width.get(), height.get())
//            glClear(GL_COLOR_BUFFER_BIT)
//
//            /* Set orthographic projection */
//            glMatrixMode(GL_PROJECTION)
//            glLoadIdentity()
//            glOrtho(-ratio.toDouble(), ratio.toDouble(), -1.0, 1.0, 1.0, -1.0)
//
//            glClear(GL_DEPTH_BUFFER_BIT)
//            render()
//
//            /* Swap buffers and poll Events */
//            GLFW.glfwSwapBuffers(window)
//            GLFW.glfwPollEvents()
//
//            /* Flip buffers for next loop */
//            width.flip()
//            height.flip()
//        }
//
//        /* Free buffers */
//        MemoryUtil.memFree(width)
//        MemoryUtil.memFree(height)
//
//        /* Release window and its callbacks */
//        GLFW.glfwDestroyWindow(window)
//        keyCallback.free()
//
//        /* Terminate GLFW and release the error callback */
//        GLFW.glfwTerminate()
//        errorCallback.free()
//    }
//
//    //private var rotationAngle = 0.0f
//
//    private fun render() {
//        // Enable back face culling
//        //glEnable(GL_CULL_FACE)
//        //glCullFace(GL_BACK)
//
//        // Set the projection matrix
//        glMatrixMode(GL_PROJECTION)
//        glLoadIdentity()
//        setPerspective(width / height.toFloat())
//
//        // Set the modelview matrix
//        glMatrixMode(GL_MODELVIEW)
//        glLoadIdentity()
//        View.setView(
//            Vector3f(5f, 15f, 5f), // Camera position
//            Vector3f(0f, 5f, 0f), // Look at point
//            Vector3f(0f, 1f, 0f) // Up vector
//        )
//
//        // Update the rotation angle and apply the rotation
//        //rotationAngle += 1.0f // Adjust the speed of rotation by changing this value
//        //glRotatef(rotationAngle, 1f, 0.5f, 0.25f)
//
//        // Create a cube and draw it
//        val cube1 = Cube(0f, 0f, 0f)
//        val cube2 = Cube(0f, 0f, -10f)
//        cube1.draw()
//        cube2.draw()
//    }
//
//    fun setPerspective(aspect: Float) {
//        MemoryStack.stackPush().use { stack ->
//            val buffer = stack.mallocFloat(4 * 4)
//            val matrix = getPerspectiveMatrix(aspect)
//            matrix.toBuffer(buffer)
//            glMultMatrixf(buffer)
//        }
//    }
//}
