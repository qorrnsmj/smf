package qorrnsmj.smf.editor

import imgui.ImGui
import imgui.extension.imguizmo.ImGuizmo
import imgui.flag.ImGuiConfigFlags
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.graphic.ViewportShadingMode

class EditorApp {
    private val imGuiGlfw = ImGuiImplGlfw()
    private val imGuiGl3 = ImGuiImplGl3()
    private val context = EditorContext()
    private val document = EditorDocument(context)
    private val input = EditorInput(context, document)
    private val ui = EditorUi(context, document)
    private val gizmo = EditorGizmo(context, document)
    private val viewport = EditorViewport(context)
    private val secondaryViewport = EditorViewport(context)
    private val fileDrop = EditorFileDrop(context, document)

    private var initialized = false

    fun initialize() {
        if (initialized) return

        Logger.info("EditorApp initializing...")

        EditorConfig.load(context)
        context.terrainGrayViewEnabled = context.viewportShadingMode == ViewportShadingMode.SOLID ||
            context.viewportShadingMode == ViewportShadingMode.WIRE
        context.terrainMeshViewEnabled = context.viewportShadingMode == ViewportShadingMode.WIRE
        context.skyVisible = context.viewportShadingMode == ViewportShadingMode.RENDERED
        ui.addAssetAtCursor = input::addSelectedAssetAtCursor
        ui.viewport = viewport
        ui.secondaryViewport = secondaryViewport
        ui.renderGizmo = gizmo::render
        EditorScene.configure(context.scene)
        context.cameraController.syncFromCamera()
        context.secondaryCamera.position = qorrnsmj.smf.math.Vector3f(18f, 10f, 18f)
        context.secondaryCamera.setFront(qorrnsmj.smf.math.Vector3f(-1f, -0.35f, -1f))
        context.secondaryCameraController.syncFromCamera()
        context.scene.terrainGrayView = context.terrainGrayViewEnabled
        context.scene.terrainWireframeView = context.terrainMeshViewEnabled
        context.scene.viewportShadingMode = context.viewportShadingMode
        context.scene.cullingEnabled = context.cullingEnabled
        context.scene.skyVisible = context.skyVisible
        context.scene.skyColor = context.timeOfDay.skyColor
        context.terrainPreview = EditorTerrainPreview(context.terrain, context.terrainMapSize)
        context.scene.terrain = context.terrainPreview?.terrain
        context.scene.terrainHeightProvider = context.terrainPreview?.terrain
        if (context.skyboxPath.get().isNotBlank()) document.setSkyboxPath(context.skyboxPath.get())
        document.setTimeOfDay(context.timeOfDay)
        document.refreshAssets()
        ImGui.createContext()
        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard)
        ImGui.getIO().setIniFilename(null)
        ImGuizmo.setImGuiContext(ImGui.getCurrentContext())

        imGuiGlfw.init(SMF.window.id, true)
        imGuiGl3.init("#version 330 core")
        fileDrop.install(SMF.window.id)
        resize(SMF.window.width, SMF.window.height)

        initialized = true
        Logger.info("EditorApp initialized!")
    }

    fun resize(width: Int, height: Int) {
        if (!initialized && ImGui.getCurrentContext() == null) return

        context.windowFontScale = (height / 1080f).coerceIn(0.75f, 1.5f)
        ImGui.getIO().setFontGlobalScale(context.effectiveFontScale())
    }

    fun update(delta: Float) {
        if (!initialized) return

        input.update(delta)
        context.updateCursorMode()
    }

    fun renderScene(alpha: Float) {
        return
    }

    fun renderUi() {
        if (!initialized) return

        imGuiGl3.newFrame()
        imGuiGlfw.newFrame()
        ImGui.newFrame()
        ImGuizmo.beginFrame()
        context.lastGizmoWantsMouse = false

        ui.render()

        context.lastWantCaptureMouse = ImGui.getIO().getWantCaptureMouse()
        context.lastWantCaptureKeyboard = ImGui.getIO().getWantCaptureKeyboard()

        ImGui.render()
        imGuiGl3.renderDrawData(ImGui.getDrawData())
        context.updateCursorMode()
    }

    fun dispose() {
        if (!initialized) return

        Logger.info("EditorApp disposing...")

        EditorConfig.save(context)
        imGuiGl3.shutdown()
        imGuiGlfw.shutdown()
        fileDrop.dispose()
        context.terrainPreview?.dispose()
        context.terrainPreview = null
        viewport.dispose()
        secondaryViewport.dispose()
        ImGui.destroyContext()
        context.keepCursorVisible()

        initialized = false
        Logger.info("EditorApp disposed!")
    }
}
