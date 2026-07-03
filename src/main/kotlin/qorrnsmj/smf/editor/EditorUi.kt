package qorrnsmj.smf.editor

import imgui.ImGui
import imgui.ImColor
import imgui.extension.imguizmo.flag.Operation
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiInputTextFlags
import imgui.flag.ImGuiKey
import imgui.flag.ImGuiMouseCursor
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImFloat
import imgui.type.ImInt
import imgui.type.ImString
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.graphic.ViewportShadingMode
import qorrnsmj.smf.math.Vector3f
import kotlin.math.max
import kotlin.math.min

internal class EditorUi(
    private val context: EditorContext,
    private val document: EditorDocument,
) {
    lateinit var addAssetAtCursor: () -> Unit
    lateinit var viewport: EditorViewport
    lateinit var secondaryViewport: EditorViewport
    lateinit var renderGizmo: () -> Unit

    fun render() {
        ImGui.setNextWindowPos(0f, 0f, ImGuiCond.Always)
        ImGui.setNextWindowSize(SMF.window.width.toFloat(), SMF.window.height.toFloat(), ImGuiCond.Always)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8f, 8f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)

        val flags = ImGuiWindowFlags.NoTitleBar or
            ImGuiWindowFlags.NoResize or
            ImGuiWindowFlags.NoMove or
            ImGuiWindowFlags.NoCollapse or
            ImGuiWindowFlags.NoSavedSettings or
            ImGuiWindowFlags.MenuBar

        if (ImGui.begin("LevelEditor", flags)) {
            renderMainMenuBar()
            handleUiDeleteShortcut()
            val freezeViewportResize = context.panelResizeInProgress
            context.panelResizeInProgress = false

            val contentWidth = ImGui.getContentRegionAvailX()
            val contentHeight = ImGui.getContentRegionAvailY()
            coercePanelWidths(contentWidth)
            val leftVisible = context.showLeftPanel
            val viewportVisible = context.showViewportPanel
            val rightVisible = context.showRightPanel
            val bottomVisible = viewportVisible && (context.showAssetsTab || context.showMapsTab)
            val leftWidth = if (leftVisible) context.leftPanelWidth else 0f
            val leftSplitterWidth = if (leftVisible && (viewportVisible || rightVisible || bottomVisible)) SPLITTER_WIDTH else 0f
            val rightAreaWidth = max(1f, contentWidth - leftWidth - leftSplitterWidth)
            val rightSplitterWidth = if (viewportVisible && rightVisible) SPLITTER_WIDTH else 0f
            val reservedRightWidth = if (rightVisible) context.rightPanelWidth + rightSplitterWidth else 0f
            val bottomWidth = if (viewportVisible) max(1f, rightAreaWidth - reservedRightWidth) else rightAreaWidth
            if (bottomVisible) {
                coerceAssetsHeight(contentHeight)
            }
            val bottomHeight = if (bottomVisible) context.assetsTabHeight else 0f
            val bottomSplitterHeight = if (bottomVisible) TAB_SPLITTER_HEIGHT else 0f
            val topHeight = max(1f, contentHeight - bottomHeight - bottomSplitterHeight)

            val startX = ImGui.getCursorPosX()
            val startY = ImGui.getCursorPosY()
            if (leftVisible) {
                renderLeftPanel(context.leftPanelWidth, contentHeight)
            }
            if (leftVisible && (viewportVisible || rightVisible || bottomVisible)) {
                ImGui.sameLine(0f, 0f)
                renderVerticalSplitter("LeftSplitter", contentHeight) { delta ->
                    val maxWidth = max(MIN_PANEL_WIDTH, contentWidth - context.rightPanelWidth - minViewportAreaWidth() - SPLITTER_WIDTH * 2f)
                    context.leftPanelWidth = context.leftPanelWidth.plus(delta).coerceIn(MIN_PANEL_WIDTH, maxWidth)
                }
            }

            val rightAreaX = startX + leftWidth + leftSplitterWidth
            ImGui.setCursorPos(rightAreaX, startY)
            renderUpperWorkArea(rightAreaWidth, topHeight, contentHeight, freezeViewportResize || context.panelResizeInProgress)

            if (bottomVisible) {
                ImGui.setCursorPos(rightAreaX, startY + topHeight)
                renderHorizontalSplitter("WorkAssetsSplitter", bottomWidth) { delta ->
                    context.assetsTabHeight = (context.assetsTabHeight - delta).coerceIn(MIN_FREE_TAB_HEIGHT, max(MIN_FREE_TAB_HEIGHT, contentHeight - MIN_VIEWPORT_HEIGHT))
                }
                ImGui.setCursorPos(rightAreaX, startY + topHeight + TAB_SPLITTER_HEIGHT)
                renderBottomPanel(bottomWidth, bottomHeight)
            }
            renderErrorPopup()
        }

        ImGui.end()
        ImGui.popStyleVar(2)
    }

    private fun renderMainMenuBar() {
        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("File")) {
                ImGui.endMenu()
            }
            if (ImGui.beginMenu("View")) {
                if (ImGui.menuItem(if (context.cullingEnabled) "Culling ON" else "Culling OFF")) {
                    context.cullingEnabled = !context.cullingEnabled
                    context.scene.cullingEnabled = context.cullingEnabled
                }
                if (ImGui.menuItem(if (context.showSecondaryViewport) "Viewport: 2 Panes" else "Viewport: 1 Pane")) {
                    context.showSecondaryViewport = !context.showSecondaryViewport
                    if (!context.showSecondaryViewport && context.activeViewportIndex == 1) {
                        context.setActiveViewport(0)
                    }
                }
                if (ImGui.beginMenu("Tab")) {
                    renderTabVisibilityMenuItem("Objects") { context.showLeftPanel = !context.showLeftPanel }
                    renderTabVisibilityMenuItem("Viewport") { context.showViewportPanel = !context.showViewportPanel }
                    renderTabVisibilityMenuItem("Properties") { context.showRightPanel = !context.showRightPanel }
                    renderTabVisibilityMenuItem("Level") {
                        context.showMapsTab = !context.showMapsTab
                        if (context.showMapsTab) context.bottomPanelTab = EditorBottomPanelTab.LEVEL
                    }
                    renderTabVisibilityMenuItem("Assets") {
                        context.showAssetsTab = !context.showAssetsTab
                        if (context.showAssetsTab) context.bottomPanelTab = EditorBottomPanelTab.ASSETS
                    }
                    renderTabVisibilityMenuItem("Hierarchy") { context.showLevelTab = !context.showLevelTab }
                    ImGui.endMenu()
                }
                ImGui.endMenu()
            }
            if (ImGui.beginMenu("Settings")) {
                renderEditorSettings()
                ImGui.endMenu()
            }

            ImGui.endMenuBar()
        }
    }

    private fun renderTabVisibilityMenuItem(label: String, onClick: () -> Unit) {
        val visible = when (label) {
            "Objects" -> context.showLeftPanel
            "Viewport" -> context.showViewportPanel
            "Properties" -> context.showRightPanel
            "Level" -> context.showMapsTab
            "Assets" -> context.showAssetsTab
            "Hierarchy" -> context.showLevelTab
            else -> false
        }
        if (ImGui.menuItem("${if (visible) "[x]" else "[ ]"} $label")) onClick()
    }

    private fun renderEditorSettings() {
        val fontScale = floatArrayOf(context.editorFontScale)
        ImGui.setNextItemWidth(240f)
        if (ImGui.sliderFloat("Font Size", fontScale, 0.75f, 1.6f, "%.2fx")) {
            context.editorFontScale = fontScale[0]
            ImGui.getIO().setFontGlobalScale(context.effectiveFontScale())
        }
    }

    private fun handleUiDeleteShortcut() {
        if (ImGui.isKeyPressed(ImGuiKey.Delete) && !ImGui.isAnyItemActive()) {
            document.deleteSelected()
        }
    }

    private fun renderLeftPanel(width: Float, height: Float) {
        ImGui.beginChild("LeftPanel", width, height, false)
        renderSidePanelPadding { renderLeftPanelTabs() }
        ImGui.endChild()
    }

    private fun renderLeftPanelTabs() {
        val availableHeight = max(1f, ImGui.getContentRegionAvailY())
        if (context.showLevelTab) {
            renderNestedTabChild(
                "Hierarchy",
                "Hierarchy",
                { context.levelTabExpanded },
                { context.levelTabExpanded = !context.levelTabExpanded },
                availableHeight,
                { renderHierarchy() },
            )
        }
    }

    private fun renderNestedTabChild(
        label: String,
        id: String,
        expanded: () -> Boolean,
        onToggle: () -> Unit,
        expandedHeight: Float = 0f,
        content: () -> Unit,
    ) {
        ImGui.beginChild(
            "${id}Tab",
            max(1f, ImGui.getContentRegionAvailX() - CHILD_TAB_RIGHT_MARGIN),
            if (expanded()) expandedHeight else collapsedTabHeight(),
            false,
        )
        renderToggleTab(label, expanded, onToggle)
        if (expanded()) renderContentPadding(content)
        ImGui.endChild()
    }

    private fun renderContentPadding(content: () -> Unit) {
        ImGui.indent(CONTENT_PADDING_X)
        ImGui.setCursorPosY(ImGui.getCursorPosY() + CONTENT_PADDING_Y)
        content()
        ImGui.unindent(CONTENT_PADDING_X)
        ImGui.setCursorPosY(ImGui.getCursorPosY() + CONTENT_PADDING_Y)
    }

    private fun renderSidePanelPadding(content: () -> Unit) {
        ImGui.indent(CONTENT_PADDING_X)
        content()
        ImGui.unindent(CONTENT_PADDING_X)
    }

    private fun tabHeight(expandedHeight: Float, expanded: Boolean): Float {
        return if (expanded) expandedHeight else collapsedTabHeight()
    }

    private fun collapsedTabHeight(): Float {
        return ImGui.getFrameHeight() + TAB_VERTICAL_PADDING * 2f + 6f
    }

    private fun renderMapControls() {
        renderProjectNameRow()

        renderLevelPathRow("Workspace", "workspace_path", context.workspaceRoot, "Browse", "Save", "Load", "Refresh") {
            if (ImGui.button("Browse##workspace_browse")) {
                val dir = EditorFileDialog.chooseDirectory()
                if (dir != null) {
                    document.openWorkspace(dir, loadLevel = false)
                }
            }
            ImGui.sameLine()
            if (ImGui.button("Save##workspace_save")) runEditorAction("Save failed") { document.saveMap() }
            ImGui.sameLine()
            if (ImGui.button("Load##workspace_load")) runEditorAction("Workspace load failed") { document.openWorkspace(context.workspaceRoot.get(), loadLevel = true) }
            ImGui.sameLine()
            if (ImGui.button("Refresh##workspace_refresh")) runEditorAction("Workspace refresh failed") { document.openWorkspace(context.workspaceRoot.get(), loadLevel = false) }
        }

        renderLevelPathRow("Level", "level_path", context.mapPath, "Browse", "Save", "Load", "Refresh", inputFlags = ImGuiInputTextFlags.ReadOnly) {
            if (ImGui.button("Browse##level_browse")) {
                val path = EditorFileDialog.chooseJsonFile(levelBrowserInitialDirectory())
                if (path != null) document.setLevelPath(path)
            }
            ImGui.sameLine()
            if (ImGui.button("Save##level_save")) runEditorAction("Save failed") { document.saveMap() }
            ImGui.sameLine()
            if (ImGui.button("Load##level_load")) runEditorAction("Level load failed") { document.loadMap() }
            ImGui.sameLine()
            if (ImGui.button("Refresh##level_refresh")) runEditorAction("Level refresh failed") { document.openWorkspace(context.workspaceRoot.get(), loadLevel = false) }
        }

        renderLevelPathRow("Heightmap", "heightmap_path", context.terrainHeightmapPath, "Browse", "Save", "Load", "Refresh") {
            if (ImGui.button("Browse##heightmap_browse")) {
                val path = EditorFileDialog.choosePngFile(terrainHeightmapInitialDirectory())
                if (path != null) context.terrainHeightmapPath.set(path)
            }
            ImGui.sameLine()
            if (ImGui.button("Save##heightmap_save")) runEditorAction("Heightmap save failed") { document.exportTerrainHeightmap() }
            ImGui.sameLine()
            if (ImGui.button("Load##heightmap_load")) runEditorAction("Heightmap load failed") { document.importTerrainHeightmap(context.terrainHeightmapPath.get()) }
            ImGui.sameLine()
            if (ImGui.button("Refresh##heightmap_refresh")) document.refreshTerrainMapPaths()
        }

        renderLevelPathRow("Splatmap", "splatmap_path", context.terrainSplatmapPath, "Browse", "Save", "Load", "Refresh") {
            if (ImGui.button("Browse##splatmap_browse")) {
                val path = EditorFileDialog.choosePngFile(terrainSplatmapInitialDirectory())
                if (path != null) context.terrainSplatmapPath.set(path)
            }
            ImGui.sameLine()
            if (ImGui.button("Save##splatmap_save")) runEditorAction("Splatmap save failed") { document.exportTerrainSplatmaps() }
            ImGui.sameLine()
            if (ImGui.button("Load##splatmap_load")) runEditorAction("Splatmap load failed") { document.importTerrainSplatmap(context.terrainSplatmapPath.get()) }
            ImGui.sameLine()
            if (ImGui.button("Refresh##splatmap_refresh")) document.refreshTerrainMapPaths()
        }

        renderLevelPathRow("Skybox", "skybox_path", context.skyboxPath, "Browse", "Load") {
            if (ImGui.button("Browse##skybox_browse")) {
                val path = EditorFileDialog.choosePngFile(skyboxBrowserInitialDirectory())
                if (path != null) document.setSkyboxPath(path)
            }
            ImGui.sameLine()
            if (ImGui.button("Load##skybox_load")) runEditorAction("Skybox load failed") { document.setSkyboxPath(context.skyboxPath.get()) }
        }
    }

    private fun runEditorAction(title: String, action: () -> Unit) {
        try {
            action()
        } catch (error: Throwable) {
            Logger.warn(error, title)
            context.errorPopupTitle = title
            context.errorPopupMessage = error.message ?: error::class.simpleName ?: "Unknown error"
            context.errorPopupOpen = true
        }
    }

    private fun renderErrorPopup() {
        if (context.errorPopupOpen) {
            ImGui.openPopup(context.errorPopupTitle)
            context.errorPopupOpen = false
        }
        if (ImGui.beginPopupModal(context.errorPopupTitle)) {
            ImGui.textWrapped(context.errorPopupMessage)
            addOneLineSpace()
            if (ImGui.button("OK", 120f, 0f)) {
                ImGui.closeCurrentPopup()
            }
            ImGui.endPopup()
        }
    }

    private fun renderProjectNameRow() {
        renderLevelPathRow(
            "Project ID",
            "project_name",
            context.projectName,
            onInputChanged = { document.setProjectName(context.projectName.get()) },
        ) {
            // Keep Project aligned with the path rows.
        }
    }

    private fun levelBrowserInitialDirectory(): String? {
        if (context.mapPath.get().isNotBlank()) {
            return java.nio.file.Paths.get(context.mapPath.get()).parent?.toString()
        }
        if (context.workspaceRoot.get().isNotBlank()) {
            return java.nio.file.Paths.get(context.workspaceRoot.get()).resolve("level").toString()
        }
        return null
    }

    private fun skyboxBrowserInitialDirectory(): String? {
        if (context.workspaceRoot.get().isNotBlank()) {
            return java.nio.file.Paths.get(context.workspaceRoot.get()).resolve("texture").resolve("skybox").toString()
        }
        return null
    }

    private fun terrainHeightmapInitialDirectory(): String? {
        if (context.workspaceRoot.get().isNotBlank()) {
            return java.nio.file.Paths.get(context.workspaceRoot.get()).resolve("texture").resolve("terrain").resolve("map").toString()
        }
        return null
    }

    private fun terrainSplatmapInitialDirectory(): String? {
        if (context.workspaceRoot.get().isNotBlank()) {
            return java.nio.file.Paths.get(context.workspaceRoot.get()).resolve("texture").resolve("terrain").resolve("map").toString()
        }
        return null
    }

    private fun renderTimeOfDayButton(label: String, nightMode: Boolean) {
        val selected = context.nightMode == nightMode
        if (selected) {
            ImGui.pushStyleColor(ImGuiCol.Button, ImColor.rgba(214, 168, 45, 255))
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImColor.rgba(232, 188, 62, 255))
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImColor.rgba(196, 145, 34, 255))
        }
        if (ImGui.button(label)) {
            document.setNightMode(nightMode)
        }
        if (selected) ImGui.popStyleColor(3)
    }

    private fun renderTerrainControls() {
        for ((index, mode) in EditorTerrainBrushMode.entries.withIndex()) {
            if (index > 0) ImGui.sameLine()
            val selected = context.editMode == EditorEditMode.TERRAIN && context.terrainBrushMode == mode
            if (selected) {
                ImGui.pushStyleColor(ImGuiCol.Button, ImColor.rgba(214, 168, 45, 255))
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImColor.rgba(232, 188, 62, 255))
                ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImColor.rgba(196, 145, 34, 255))
            }
            if (ImGui.button(terrainModeLabel(mode))) {
                context.editMode = EditorEditMode.TERRAIN
                context.selectedCollisionIndex = -1
                context.terrainBrushMode = mode
            }
            if (selected) ImGui.popStyleColor(3)
        }
        addOneLineSpace()

        val radius = floatArrayOf(context.terrainBrushRadius)
        if (renderTerrainFloatSlider("Radius", "terrain_radius", radius, 1f, 128f, "%.0f")) {
            context.terrainBrushRadius = radius[0]
        }

        val mapSize = floatArrayOf(context.terrainMapSize)
        val mapSizeChanged = renderTerrainFloatSlider("Map Size", "terrain_map_size", mapSize, 32f, 2048f, "%.0f")
        if (ImGui.isItemActivated()) {
            document.pushUndoSnapshot()
        }
        if (mapSizeChanged) {
            document.setTerrainMapSize(mapSize[0])
        }
        ImGui.textDisabled("Heightmap ${context.terrain.width} x ${context.terrain.height}")

        val strength = floatArrayOf(context.terrainBrushStrength)
        if (renderTerrainFloatSlider("Strength", "terrain_strength", strength, 0.01f, 1f, "%.2f")) {
            context.terrainBrushStrength = strength[0]
        }

        val falloff = floatArrayOf(context.terrainBrushFalloff)
        if (renderTerrainFloatSlider("Falloff", "terrain_falloff", falloff, 0.1f, 6f, "%.2f")) {
            context.terrainBrushFalloff = falloff[0]
        }

        val textureIndex = intArrayOf(context.terrainPaintTextureIndex)
        if (renderTerrainIntSlider("Texture ID", "terrain_texture_index", textureIndex, 0, 31)) {
            context.terrainPaintTextureIndex = textureIndex[0].coerceAtLeast(0)
        }
    }

    private fun terrainModeLabel(mode: EditorTerrainBrushMode): String {
        return when (mode) {
            EditorTerrainBrushMode.RAISE -> "Raise"
            EditorTerrainBrushMode.LOWER -> "Lower"
            EditorTerrainBrushMode.SMOOTH -> "Smooth"
            EditorTerrainBrushMode.FLATTEN -> "Flat"
            EditorTerrainBrushMode.NOISE -> "Noise"
            EditorTerrainBrushMode.PAINT -> "Paint"
        }
    }

    private fun renderTerrainFloatSlider(
        label: String,
        id: String,
        value: FloatArray,
        minValue: Float,
        maxValue: Float,
        format: String,
    ): Boolean {
        renderControlLabel(label)
        ImGui.setNextItemWidth(controlInputWidth())
        var changed = ImGui.sliderFloat("##$id", value, minValue, maxValue, format)
        if (ImGui.beginPopupContextItem("${id}_value_popup")) {
            val inputValue = ImFloat(value[0])
            ImGui.setNextItemWidth(120f)
            if (ImGui.inputFloat("##${id}_value", inputValue)) {
                value[0] = inputValue.get()
                changed = true
            }
            ImGui.endPopup()
        }
        if (changed) value[0] = value[0].coerceIn(minValue, maxValue)
        return changed
    }

    private fun renderTerrainIntSlider(
        label: String,
        id: String,
        value: IntArray,
        minValue: Int,
        maxValue: Int,
    ): Boolean {
        renderControlLabel(label)
        ImGui.setNextItemWidth(controlInputWidth())
        var changed = ImGui.sliderInt("##$id", value, minValue, maxValue)
        if (ImGui.beginPopupContextItem("${id}_value_popup")) {
            val inputValue = ImInt(value[0])
            ImGui.setNextItemWidth(120f)
            if (ImGui.inputInt("##${id}_value", inputValue)) {
                value[0] = inputValue.get()
                changed = true
            }
            ImGui.endPopup()
        }
        if (changed) value[0] = value[0].coerceIn(minValue, maxValue)
        return changed
    }

    private fun renderControlLabel(label: String) {
        val labelX = ImGui.getCursorPosX()
        ImGui.alignTextToFramePadding()
        ImGui.text(label)
        ImGui.sameLine(labelX + TERRAIN_PARAM_LABEL_WIDTH)
    }

    private fun renderLabeledInputText(label: String, id: String, value: ImString): Boolean {
        val labelX = ImGui.getCursorPosX()
        ImGui.alignTextToFramePadding()
        ImGui.text(label)
        ImGui.sameLine(labelX + OBJECT_FIELD_LABEL_WIDTH)
        ImGui.setNextItemWidth(max(80f, ImGui.getContentRegionAvailX()))
        return ImGui.inputText("##$id", value)
    }

    private fun controlInputWidth(): Float {
        return max(80f, ImGui.getContentRegionAvailX() - CONTROL_RIGHT_PADDING)
    }

    private fun addOneLineSpace() {
        ImGui.setCursorPosY(ImGui.getCursorPosY() + ImGui.getFrameHeight())
    }

    private fun renderLevelPathRow(
        label: String,
        id: String,
        value: ImString,
        vararg buttonLabels: String,
        inputFlags: Int = 0,
        onInputChanged: (() -> Unit)? = null,
        buttons: () -> Unit,
    ) {
        val labelX = ImGui.getCursorPosX()
        ImGui.alignTextToFramePadding()
        ImGui.text(label)
        ImGui.sameLine(labelX + LEVEL_LABEL_WIDTH)
        val buttonWidth = buttonGroupWidth(*buttonLabels)
        val spacing = ImGui.getStyle().itemSpacingX
        ImGui.setNextItemWidth(mapPathInputWidth(buttonWidth + spacing + LEVEL_ROW_RIGHT_PADDING))
        if (ImGui.inputText("##$id", value, inputFlags)) {
            onInputChanged?.invoke()
        }
        if (buttonLabels.isNotEmpty()) {
            ImGui.sameLine()
            buttons()
        }
    }

    private fun mapPathInputWidth(buttonAreaWidth: Float): Float {
        val availableWidth = ImGui.getContentRegionAvailX()
        val maxPathWidth = availableWidth * PATH_INPUT_WIDTH_RATIO
        val fitButtonWidth = availableWidth - buttonAreaWidth
        return max(72f, min(maxPathWidth, fitButtonWidth))
    }

    private fun buttonGroupWidth(vararg labels: String): Float {
        if (labels.isEmpty()) return 0f
        val style = ImGui.getStyle()
        val buttons = labels.sumOf { label ->
            (ImGui.calcTextSizeX(label.substringBefore("##")) + style.framePaddingX * 2f + BUTTON_WIDTH_SAFETY).toDouble()
        }.toFloat()
        return buttons + style.itemSpacingX * (labels.size - 1)
    }

    private fun renderAssets() {
        renderLevelPathRow(
            "Path",
            "asset_path",
            context.assetBrowserPath,
            "Up",
            "Browse",
            "Refresh",
            onInputChanged = { document.refreshAssets() },
        ) {
            if (ImGui.button("Up##asset_up")) {
                document.leaveAssetDirectory()
            }
            ImGui.sameLine()
            if (ImGui.button("Browse##asset_browse")) {
                val dir = EditorFileDialog.chooseDirectory()
                if (dir != null) {
                    document.setAssetBrowserPath(dir)
                }
            }
            ImGui.sameLine()
            if (ImGui.button("Refresh##asset_refresh")) {
                document.refreshAssets()
            }
        }

        val directories = context.availableAssetDirectories.toList()
        val assets = context.availableGlbs.toList()
        val totalItems = directories.size + assets.size
        if (totalItems == 0) {
            ImGui.textDisabled("Empty")
            return
        }

        val tileWidth = ASSET_TILE_WIDTH
        val tileHeight = ASSET_TILE_HEIGHT
        var currentLineWidth = 0f
        var tileIndex = 0
        for (dir in directories) {
            renderAssetTileLayout(tileIndex++, tileWidth, currentLineWidth)
            currentLineWidth = updateAssetTileLineWidth(currentLineWidth, tileWidth)
            renderAssetDirectoryTile(dir, tileWidth, tileHeight)
        }
        for ((index, asset) in assets.withIndex()) {
            renderAssetTileLayout(tileIndex++, tileWidth, currentLineWidth)
            currentLineWidth = updateAssetTileLineWidth(currentLineWidth, tileWidth)
            renderAssetModelTile(asset, index, tileWidth, tileHeight)
        }
    }

    private fun renderAssetTileLayout(index: Int, tileWidth: Float, currentLineWidth: Float) {
        if (index > 0 && currentLineWidth + tileWidth <= ImGui.getContentRegionAvailX()) ImGui.sameLine()
    }

    private fun updateAssetTileLineWidth(currentLineWidth: Float, tileWidth: Float): Float {
        return if (currentLineWidth + tileWidth <= ImGui.getContentRegionAvailX()) {
            currentLineWidth + tileWidth + ImGui.getStyle().itemSpacingX
        } else {
            tileWidth + ImGui.getStyle().itemSpacingX
        }
    }

    private fun renderAssetDirectoryTile(name: String, width: Float, height: Float) {
        val pos = ImGui.getCursorScreenPos()
        ImGui.invisibleButton("dir_$name", width, height)
        val hovered = ImGui.isItemHovered()
        val drawList = ImGui.getWindowDrawList()
        val bg = if (hovered) ImColor.rgba(72, 82, 94, 255) else ImColor.rgba(58, 63, 70, 255)
        drawList.addRectFilled(pos.x, pos.y, pos.x + width, pos.y + height, bg, 3f)
        drawList.addRectFilled(pos.x + 10f, pos.y + 13f, pos.x + width - 10f, pos.y + height - 18f, ImColor.rgba(202, 150, 54, 255), 2f)
        drawList.addRectFilled(pos.x + 14f, pos.y + 9f, pos.x + 58f, pos.y + 18f, ImColor.rgba(224, 174, 70, 255), 2f)
        drawList.addText(
            pos.x + 8f,
            pos.y + height - 16f,
            ImColor.rgba(240, 242, 247, 255),
            ellipsizeText(name, width - 16f),
        )
        if (hovered) {
            ImGui.setTooltip(name)
            if (ImGui.isMouseDoubleClicked(0)) {
                document.enterAssetDirectory(name)
            }
        }
    }

    private fun renderAssetModelTile(asset: String, index: Int, width: Float, height: Float) {
        val selected = index == context.selectedAssetIndex
        val pos = ImGui.getCursorScreenPos()
        ImGui.invisibleButton("asset_$index", width, height)
        val hovered = ImGui.isItemHovered()
        if (ImGui.isItemClicked()) context.selectedAssetIndex = index
        if (hovered && ImGui.isMouseDoubleClicked(0)) document.addAsset(asset, assetPlacementPosition())

        val drawList = ImGui.getWindowDrawList()
        val border = when {
            selected -> ImColor.rgba(47, 112, 177, 255)
            hovered -> ImColor.rgba(73, 118, 163, 255)
            else -> ImColor.rgba(32, 70, 111, 255)
        }
        drawList.addRectFilled(pos.x, pos.y, pos.x + width, pos.y + height, ImColor.rgba(60, 62, 63, 255), 2f)
        drawList.addRect(pos.x, pos.y, pos.x + width, pos.y + height, border, 2f, 0, if (selected) 3f else 2f)
        val previewX1 = pos.x + 8f
        val previewY1 = pos.y + 8f
        val previewX2 = pos.x + width - 8f
        val previewY2 = pos.y + height - 24f
        drawList.addRectFilled(previewX1, previewY1, previewX2, previewY2, ImColor.rgba(82, 86, 88, 255), 1f)
        drawList.addCircleFilled(pos.x + width * 0.5f, pos.y + height * 0.42f, 15f, ImColor.rgba(82, 134, 142, 255), 18)
        drawList.addLine(pos.x + width * 0.5f - 18f, pos.y + height * 0.42f + 14f, pos.x + width * 0.5f + 18f, pos.y + height * 0.42f - 14f, ImColor.rgba(120, 185, 191, 255), 3f)
        val assetName = asset.substringAfterLast('/')
        drawList.addText(
            pos.x + 8f,
            pos.y + height - 17f,
            ImColor.rgba(240, 242, 247, 255),
            ellipsizeText(assetName, width - 16f),
        )
        if (hovered) ImGui.setTooltip(asset)
        if (ImGui.beginDragDropSource()) {
            ImGui.setDragDropPayload("SMF_GLB_ASSET", asset)
            ImGui.text(asset)
            ImGui.endDragDropSource()
        }
    }

    private fun assetPlacementPosition(): Vector3f {
        return context.activeCamera().position.add(context.activeCamera().getFront().scale(12f))
    }

    private fun markerPlacementPosition(): Vector3f {
        val rayPosition = runCatching {
            EditorPicking.intersectGround(EditorPicking.currentMouseRay(context))
        }.getOrNull()
        return rayPosition ?: context.activeCamera().position.add(context.activeCamera().getFront().scale(12f))
    }

    private fun ellipsizeText(text: String, maxWidth: Float): String {
        if (ImGui.calcTextSizeX(text) <= maxWidth) return text
        val ellipsis = "..."
        var end = text.length
        while (end > 0 && ImGui.calcTextSizeX(text.take(end) + ellipsis) > maxWidth) {
            end--
        }
        return if (end <= 0) ellipsis else text.take(end) + ellipsis
    }

    private fun renderHierarchy() {
        renderEmptyLevelGroup("Terrain")
        renderObjectLevelGroup("StaticObjects")
        renderEventAreaLevelGroup()
        renderPathLevelGroup()
    }

    private fun renderViewport(width: Float, height: Float, freezeResize: Boolean) {
        ImGui.beginChild("Viewports", width, height, false, ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse)
        val viewWidth = max(1f, ImGui.getContentRegionAvailX())
        val viewHeight = max(1f, ImGui.getContentRegionAvailY())
        if (!context.showSecondaryViewport) {
            context.setViewportBounds(1, 0f, 0f, 0f, 0f)
            renderViewportTab(
                0,
                viewport,
                "Viewport",
                { context.viewportExpanded },
                { context.viewportExpanded = !context.viewportExpanded },
                viewWidth,
                viewHeight,
                freezeResize,
            )
            ImGui.endChild()
            return
        }

        val paneWidths = viewportPaneWidths(viewWidth)

        renderViewportTab(
            0,
            viewport,
            "Viewport 1",
            { context.viewportExpanded },
            { context.viewportExpanded = !context.viewportExpanded },
            paneWidths.first,
            viewHeight,
            freezeResize,
        )
        ImGui.sameLine(0f, 0f)
        renderVerticalSplitter("ViewportTabsSplitter", viewHeight) { delta ->
            resizeViewportTabs(viewWidth, paneWidths.first, delta)
        }
        ImGui.sameLine(0f, 0f)
        renderViewportTab(
            1,
            secondaryViewport,
            "Viewport 2",
            { context.secondaryViewportExpanded },
            { context.secondaryViewportExpanded = !context.secondaryViewportExpanded },
            paneWidths.second,
            viewHeight,
            freezeResize,
        )
        ImGui.endChild()
    }

    private fun renderViewportTab(
        index: Int,
        target: EditorViewport,
        label: String,
        expanded: () -> Boolean,
        onToggle: () -> Unit,
        width: Float,
        height: Float,
        freezeResize: Boolean,
    ) {
        ImGui.beginChild("${label}Tab", width, height, false, ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse)
        renderToggleTab(label, expanded, onToggle)
        if (expanded()) {
            renderViewportPane(
                index,
                target,
                max(1f, ImGui.getContentRegionAvailX()),
                max(1f, ImGui.getContentRegionAvailY()),
                freezeResize,
            )
        } else {
            context.setViewportBounds(index, 0f, 0f, 0f, 0f)
        }
        ImGui.endChild()
    }

    private fun renderViewportPane(index: Int, target: EditorViewport, width: Float, height: Float, freezeResize: Boolean) {
        val pos = ImGui.getCursorScreenPos()
        context.setViewportBounds(index, pos.x, pos.y, width, height)
        target.renderImage(
            width,
            height,
            context.viewportCamera(index),
            context.viewportShadingMode(index),
            context.viewportTimeOfDay(index),
        )
        val active = context.activeViewportIndex == index
        if (ImGui.isItemHovered() && !context.viewportMouseLookActive) context.setActiveViewport(index)
        if (active) {
            acceptViewportDrops()
            if (context.editMode == EditorEditMode.OBJECT) renderGizmo()
            renderViewportDebugOverlay(pos.x, pos.y, width, index)
        }
        renderViewportTerrainViewButtons(pos.x, pos.y, index)
    }

    private fun renderViewportTerrainViewButtons(viewportX: Float, viewportY: Float, viewportIndex: Int) {
        val oldX = ImGui.getCursorPosX()
        val oldY = ImGui.getCursorPosY()
        ImGui.setCursorScreenPos(viewportX + 8f, viewportY + 8f)
        renderViewportShadingCombo(viewportIndex)
        ImGui.sameLine(0f, 4f)
        renderTimeOfDayCombo(viewportIndex)
        ImGui.setCursorPos(oldX, oldY)
    }

    private fun renderViewportShadingCombo(viewportIndex: Int) {
        ImGui.setNextItemWidth(124f)
        val current = context.viewportShadingMode(viewportIndex)
        if (ImGui.beginCombo("##viewport_shading_mode_$viewportIndex", viewportShadingLabel(current))) {
            for (mode in ViewportShadingMode.entries) {
                if (ImGui.selectable(viewportShadingLabel(mode), current == mode)) {
                    setViewportShadingMode(viewportIndex, mode)
                }
            }
            ImGui.endCombo()
        }
    }

    private fun renderTimeOfDayCombo(viewportIndex: Int) {
        ImGui.setNextItemWidth(92f)
        val current = context.viewportTimeOfDay(viewportIndex)
        if (ImGui.beginCombo("##viewport_time_of_day_$viewportIndex", current.label)) {
            for (timeOfDay in EditorTimeOfDay.entries) {
                if (ImGui.selectable(timeOfDay.label, current == timeOfDay)) {
                    document.setViewportTimeOfDay(viewportIndex, timeOfDay)
                }
            }
            ImGui.endCombo()
        }
    }

    private fun viewportShadingLabel(mode: ViewportShadingMode): String = when (mode) {
        ViewportShadingMode.WIRE -> "Wire"
        ViewportShadingMode.SOLID -> "Solid"
        ViewportShadingMode.MATERIAL -> "Material"
        ViewportShadingMode.RENDERED -> "Rendered"
    }

    private fun setViewportShadingMode(viewportIndex: Int, mode: ViewportShadingMode) {
        context.setViewportShadingMode(viewportIndex, mode)
        if (viewportIndex == 0) {
            context.terrainGrayViewEnabled = mode == ViewportShadingMode.SOLID || mode == ViewportShadingMode.WIRE
            context.terrainMeshViewEnabled = mode == ViewportShadingMode.WIRE
            context.skyVisible = mode == ViewportShadingMode.RENDERED
            context.scene.viewportShadingMode = mode
            context.scene.terrainGrayView = context.terrainGrayViewEnabled
            context.scene.terrainWireframeView = context.terrainMeshViewEnabled
            context.scene.skyVisible = context.skyVisible
        }
    }

    private fun renderUpperWorkArea(width: Float, viewportHeight: Float, rightPanelHeight: Float, freezeResize: Boolean) {
        val viewportVisible = context.showViewportPanel
        val rightVisible = context.showRightPanel
        val splitterCount = if (viewportVisible && rightVisible) 1 else 0
        val rightWidth = if (rightVisible) context.rightPanelWidth else 0f
        val viewportWidth = max(1f, width - rightWidth - SPLITTER_WIDTH * splitterCount)

        var renderedPanel = false
        if (viewportVisible) {
            renderViewport(viewportWidth, viewportHeight, freezeResize)
            renderedPanel = true
        }
        if (viewportVisible && rightVisible) {
            ImGui.sameLine(0f, 0f)
            renderVerticalSplitter("RightSplitter", rightPanelHeight) { delta ->
                val maxWidth = max(MIN_PANEL_WIDTH, width - minViewportAreaWidth() - SPLITTER_WIDTH)
                context.rightPanelWidth = context.rightPanelWidth.minus(delta).coerceIn(MIN_PANEL_WIDTH, maxWidth)
            }
            renderedPanel = true
        }
        if (rightVisible) {
            if (renderedPanel) ImGui.sameLine(0f, 0f)
            renderRightPanel(context.rightPanelWidth, rightPanelHeight)
        }
    }

    private fun renderBottomPanel(width: Float, height: Float) {
        ImGui.beginChild("BottomAssetsPanel", width, height, false)
        normalizeBottomPanelTab()
        renderBottomTabBar()
        renderContentPadding {
            when (context.bottomPanelTab) {
                EditorBottomPanelTab.ASSETS -> renderAssets()
                EditorBottomPanelTab.LEVEL -> renderMapControls()
            }
        }
        ImGui.endChild()
    }

    private fun normalizeBottomPanelTab() {
        if (context.bottomPanelTab == EditorBottomPanelTab.ASSETS && !context.showAssetsTab && context.showMapsTab) {
            context.bottomPanelTab = EditorBottomPanelTab.LEVEL
        }
        if (context.bottomPanelTab == EditorBottomPanelTab.LEVEL && !context.showMapsTab && context.showAssetsTab) {
            context.bottomPanelTab = EditorBottomPanelTab.ASSETS
        }
    }

    private fun renderBottomTabBar() {
        if (context.showAssetsTab) {
            renderBottomTabButton("Assets", EditorBottomPanelTab.ASSETS)
        }
        if (context.showAssetsTab && context.showMapsTab) ImGui.sameLine(0f, 4f)
        if (context.showMapsTab) {
            renderBottomTabButton("Level", EditorBottomPanelTab.LEVEL)
        }
        ImGui.setCursorPosY(ImGui.getCursorPosY() + TAB_VERTICAL_PADDING)
    }

    private fun renderBottomTabButton(label: String, tab: EditorBottomPanelTab) {
        val selected = context.bottomPanelTab == tab
        if (selected) {
            ImGui.pushStyleColor(ImGuiCol.Button, ImColor.rgba(58, 68, 82, 255))
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImColor.rgba(68, 80, 96, 255))
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImColor.rgba(48, 58, 72, 255))
        } else {
            ImGui.pushStyleColor(ImGuiCol.Button, ImColor.rgba(38, 44, 53, 255))
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImColor.rgba(50, 58, 70, 255))
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImColor.rgba(45, 52, 62, 255))
        }
        if (ImGui.button(label, 96f, 0f)) {
            context.bottomPanelTab = tab
        }
        ImGui.popStyleColor(3)
    }

    private fun renderRightPanel(width: Float, height: Float) {
        ImGui.beginChild("RightPanel", width, height, false)
        renderSidePanelPadding { renderProperties() }
        ImGui.endChild()
    }

    private fun renderVerticalSplitter(id: String, height: Float, onDrag: (Float) -> Unit) {
        ImGui.invisibleButton(id, SPLITTER_WIDTH, height)
        if (ImGui.isItemHovered() || ImGui.isItemActive()) {
            ImGui.setMouseCursor(ImGuiMouseCursor.ResizeEW)
        }
        if (ImGui.isItemActive()) {
            context.panelResizeInProgress = true
            onDrag(ImGui.getIO().getMouseDeltaX())
        }
    }

    private fun renderHorizontalSplitter(id: String, width: Float = ImGui.getContentRegionAvailX(), onDrag: (Float) -> Unit) {
        ImGui.invisibleButton(id, width, TAB_SPLITTER_HEIGHT)
        if (ImGui.isItemHovered() || ImGui.isItemActive()) {
            ImGui.setMouseCursor(ImGuiMouseCursor.ResizeNS)
        }
        if (ImGui.isItemActive()) {
            context.panelResizeInProgress = true
            onDrag(ImGui.getIO().getMouseDeltaY())
        }
    }

    private fun renderFreeHorizontalSplitter(
        id: String,
        getter: () -> Float,
        setter: (Float) -> Unit,
        maxHeight: Float,
    ) {
        ImGui.invisibleButton(id, ImGui.getContentRegionAvailX(), TAB_SPLITTER_HEIGHT)
        if (ImGui.isItemHovered() || ImGui.isItemActive()) {
            ImGui.setMouseCursor(ImGuiMouseCursor.ResizeNS)
        }
        if (ImGui.isItemActive()) {
            context.panelResizeInProgress = true
            setter((getter() + ImGui.getIO().getMouseDeltaY()).coerceIn(MIN_FREE_TAB_HEIGHT, max(MIN_FREE_TAB_HEIGHT, maxHeight)))
        }
    }

    private fun renderToggleTab(label: String, expanded: () -> Boolean, onToggle: () -> Unit) {
        val headerClicked = renderTabHeader(label, expanded())
        if (headerClicked) onToggle()
        ImGui.setCursorPosY(ImGui.getCursorPosY() + TAB_VERTICAL_PADDING)
    }

    private fun renderTabHeader(label: String, expanded: Boolean): Boolean {
        val pos = ImGui.getCursorScreenPos()
        val width = ImGui.getContentRegionAvailX()
        val height = ImGui.getFrameHeight() + TAB_VERTICAL_PADDING
        val drawList = ImGui.getWindowDrawList()

        drawList.addRectFilled(pos.x, pos.y, pos.x + width, pos.y + height, ImColor.rgba(45, 52, 62, 255), 3f)
        ImGui.setCursorPosY(ImGui.getCursorPosY() + TAB_VERTICAL_PADDING)
        ImGui.setCursorPosX(ImGui.getCursorPosX() + 6f)
        val arrow = if (expanded) "v" else ">"
        ImGui.text("$arrow $label")
        val clicked = ImGui.isItemClicked()
        if (ImGui.isItemHovered()) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
        }
        return clicked
    }

    private fun coerceAssetsHeight(contentHeight: Float) {
        context.assetsTabHeight = context.assetsTabHeight.coerceIn(
            MIN_FREE_TAB_HEIGHT,
            max(MIN_FREE_TAB_HEIGHT, contentHeight - MIN_VIEWPORT_HEIGHT - TAB_SPLITTER_HEIGHT),
        )
    }

    private fun viewportPaneWidths(width: Float): Pair<Float, Float> {
        val availableWidth = max(2f, width - SPLITTER_WIDTH)
        val minPaneWidth = min(MIN_VIEWPORT_TAB_WIDTH, availableWidth * 0.5f)
        val firstWidth = (availableWidth * context.viewportSplitRatio)
            .coerceIn(minPaneWidth, availableWidth - minPaneWidth)
        return firstWidth to max(1f, availableWidth - firstWidth)
    }

    private fun resizeViewportTabs(totalWidth: Float, firstWidth: Float, delta: Float) {
        val availableWidth = max(2f, totalWidth - SPLITTER_WIDTH)
        val minPaneWidth = min(MIN_VIEWPORT_TAB_WIDTH, availableWidth * 0.5f)
        val nextFirstWidth = (firstWidth + delta).coerceIn(minPaneWidth, availableWidth - minPaneWidth)
        context.viewportSplitRatio = (nextFirstWidth / availableWidth).coerceIn(0.1f, 0.9f)
    }

    private fun resizeStackedTabs(
        delta: Float,
        upperGetter: () -> Float,
        upperSetter: (Float) -> Unit,
        lowerGetter: () -> Float,
        lowerSetter: (Float) -> Unit,
        minUpper: Float = MIN_FREE_TAB_HEIGHT,
        minLower: Float = MIN_FREE_TAB_HEIGHT,
    ) {
        val upper = upperGetter()
        val lower = lowerGetter()
        val minDelta = minUpper - upper
        val maxDelta = lower - minLower
        if (minDelta > maxDelta) return

        val clampedDelta = delta.coerceIn(minDelta, maxDelta)
        upperSetter(upper + clampedDelta)
        lowerSetter(lower - clampedDelta)
    }

    private fun getAssetsHeight(): Float = context.assetsTabHeight
    private fun setAssetsHeight(value: Float) {
        context.assetsTabHeight = value
    }

    private fun getLevelHeight(): Float = context.levelTabHeight
    private fun setLevelHeight(value: Float) {
        context.levelTabHeight = value
    }

    private fun getTerrainHeight(): Float = context.terrainTabHeight
    private fun setTerrainHeight(value: Float) {
        context.terrainTabHeight = value
    }

    private fun getObjectHeight(): Float = context.objectTabHeight
    private fun setObjectHeight(value: Float) {
        context.objectTabHeight = value
    }

    private fun getCollisionsHeight(): Float = context.collisionsTabHeight
    private fun setCollisionsHeight(value: Float) {
        context.collisionsTabHeight = value
    }

    private fun renderEmptyLevelGroup(label: String) {
        if (ImGui.treeNodeEx(label, ImGuiTreeNodeFlags.DefaultOpen)) {
            ImGui.textDisabled("(empty)")
            ImGui.treePop()
        }
    }

    private fun renderObjectLevelGroup(label: String) {
        if (ImGui.treeNodeEx(label, ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.beginPopupContextItem("static_objects_context")) {
                if (ImGui.menuItem("Add Folder")) {
                    val index = document.addHierarchyFolder()
                    beginFolderRename(EditorFolderCategory.OBJECT, index)
                }
                if (ImGui.menuItem("Delete Selected")) document.deleteObjects(selectedHierarchyIndices())
                ImGui.endPopup()
            }
            acceptHierarchyFolderDrop("")

            if (context.placedObjects.isEmpty() && context.hierarchyFolders.isEmpty()) {
                ImGui.textDisabled("(empty)")
            } else {
                context.hierarchyFolders.toList().forEachIndexed { folderIndex, folder ->
                    renderHierarchyFolder(folderIndex, folder)
                }
                context.placedObjects.toList().forEachIndexed { index, placed ->
                    if (placed.folder.isBlank()) renderHierarchyObject(index, placed)
                }
            }
            ImGui.treePop()
        }
    }

    private fun renderEventAreaLevelGroup() {
        if (ImGui.treeNodeEx("EventAreas", ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.beginPopupContextItem("event_areas_context")) {
                if (ImGui.menuItem("Add Folder")) {
                    val index = document.addEventAreaFolder()
                    beginFolderRename(EditorFolderCategory.EVENT_AREA, index)
                }
                if (ImGui.menuItem("Add EventArea")) document.addEventArea(markerPlacementPosition())
                if (ImGui.menuItem("Add Spawnpoint")) document.addSpawnPoint(markerPlacementPosition())
                if (ImGui.menuItem("Delete Selected")) document.deleteSelected()
                ImGui.endPopup()
            }
            acceptEventAreaFolderDrop("")
            if (context.eventAreas.isEmpty() && context.eventAreaFolders.isEmpty()) {
                ImGui.textDisabled("(empty)")
            } else {
                context.eventAreaFolders.toList().forEachIndexed { folderIndex, folder ->
                    renderEventAreaFolder(folderIndex, folder)
                }
                context.eventAreas.toList().forEachIndexed { index, area ->
                    if (area.folder.isBlank()) renderEventAreaHierarchyItem(index, area)
                }
            }
            ImGui.treePop()
        }
    }

    private fun renderPathLevelGroup() {
        if (ImGui.treeNodeEx("Paths", ImGuiTreeNodeFlags.DefaultOpen)) {
            if (ImGui.beginPopupContextItem("paths_context")) {
                if (ImGui.menuItem("Add Folder")) {
                    val index = document.addPathFolder()
                    beginFolderRename(EditorFolderCategory.PATH, index)
                }
                ImGui.endPopup()
            }
            if (context.pathFolders.isEmpty()) {
                ImGui.textDisabled("(empty)")
            } else {
                context.pathFolders.toList().forEachIndexed { folderIndex, folder ->
                    renderPathFolder(folderIndex, folder)
                }
            }
            ImGui.treePop()
        }
    }

    private fun renderEventAreaHierarchyItem(areaIndex: Int, area: EditorEventArea) {
        val selected = context.selectedEventAreaIndex == areaIndex && context.selectedSpawnPointIndex == -1
        val label = area.id.ifBlank { area.name }
        if (ImGui.selectable("- $label##event_area_$areaIndex", selected)) {
            document.selectEventArea(areaIndex)
        }
        renderEventAreaDragSource(areaIndex)
        if (ImGui.beginPopupContextItem("event_area_context_$areaIndex")) {
            if (ImGui.beginMenu("Move To")) {
                if (ImGui.menuItem("EventAreas")) document.moveEventAreasToFolder(selectedEventAreaIndices(), "")
                context.eventAreaFolders.forEach { folder ->
                    if (ImGui.menuItem(folder.name)) document.moveEventAreasToFolder(selectedEventAreaIndices(), folder.name)
                }
                ImGui.endMenu()
            }
            if (ImGui.menuItem("Delete")) {
                document.deleteEventArea(areaIndex)
                ImGui.endPopup()
                return
            }
            ImGui.endPopup()
        }
    }

    private fun renderHierarchyFolder(folderIndex: Int, folder: EditorHierarchyFolder) {
        if (context.renamingFolderCategory == EditorFolderCategory.OBJECT && context.renamingFolderIndex == folderIndex) {
            renderFolderRenameField(EditorFolderCategory.OBJECT, folder, folderIndex)
            return
        }

        val open = ImGui.treeNodeEx("${folder.name}##folder_$folderIndex", if (folder.expanded) ImGuiTreeNodeFlags.DefaultOpen else 0)
        folder.expanded = open
        if (ImGui.beginPopupContextItem("folder_context_$folderIndex")) {
            if (ImGui.menuItem("Rename")) beginFolderRename(EditorFolderCategory.OBJECT, folderIndex)
            if (ImGui.menuItem("Move Selected Here")) document.moveObjectsToFolder(selectedHierarchyIndices(), folder.name)
            if (ImGui.menuItem("Delete")) document.deleteHierarchyFolder(folderIndex)
            ImGui.endPopup()
        }
        acceptHierarchyFolderDrop(folder.name)
        if (open) {
            context.placedObjects.toList().forEachIndexed { index, placed ->
                if (placed.folder == folder.name) renderHierarchyObject(index, placed)
            }
            ImGui.treePop()
        }
    }

    private fun renderEventAreaFolder(folderIndex: Int, folder: EditorHierarchyFolder) {
        if (context.renamingFolderCategory == EditorFolderCategory.EVENT_AREA && context.renamingFolderIndex == folderIndex) {
            renderFolderRenameField(EditorFolderCategory.EVENT_AREA, folder, folderIndex)
            return
        }

        val open = ImGui.treeNodeEx("${folder.name}##event_area_folder_$folderIndex", if (folder.expanded) ImGuiTreeNodeFlags.DefaultOpen else 0)
        folder.expanded = open
        if (ImGui.beginPopupContextItem("event_area_folder_context_$folderIndex")) {
            if (ImGui.menuItem("Rename")) beginFolderRename(EditorFolderCategory.EVENT_AREA, folderIndex)
            if (ImGui.menuItem("Move Selected Here")) document.moveEventAreasToFolder(selectedEventAreaIndices(), folder.name)
            if (ImGui.menuItem("Delete")) document.deleteEventAreaFolder(folderIndex)
            ImGui.endPopup()
        }
        acceptEventAreaFolderDrop(folder.name)
        if (open) {
            context.eventAreas.toList().forEachIndexed { index, area ->
                if (area.folder == folder.name) renderEventAreaHierarchyItem(index, area)
            }
            ImGui.treePop()
        }
    }

    private fun renderPathFolder(folderIndex: Int, folder: EditorHierarchyFolder) {
        if (context.renamingFolderCategory == EditorFolderCategory.PATH && context.renamingFolderIndex == folderIndex) {
            renderFolderRenameField(EditorFolderCategory.PATH, folder, folderIndex)
            return
        }

        val open = ImGui.treeNodeEx("${folder.name}##path_folder_$folderIndex", if (folder.expanded) ImGuiTreeNodeFlags.DefaultOpen else 0)
        folder.expanded = open
        if (ImGui.beginPopupContextItem("path_folder_context_$folderIndex")) {
            if (ImGui.menuItem("Rename")) beginFolderRename(EditorFolderCategory.PATH, folderIndex)
            if (ImGui.menuItem("Delete")) document.deletePathFolder(folderIndex)
            ImGui.endPopup()
        }
        if (open) ImGui.treePop()
    }

    private fun renderHierarchyObject(index: Int, placed: EditorPlacedObject) {
        val itemLabel = "- ${placed.name}##static_object_$index"
        if (ImGui.selectable(itemLabel, index in context.selectedIndices)) selectHierarchyObject(index)
        renderHierarchyObjectDragSource(index)
        if (ImGui.beginPopupContextItem("static_object_context_$index")) {
            if (index !in context.selectedIndices) selectSingleHierarchyObject(index)
            if (ImGui.menuItem("Duplicate")) {
                document.duplicateSelected()
                ImGui.endPopup()
                return
            }
            if (ImGui.beginMenu("Move To")) {
                if (ImGui.menuItem("StaticObjects")) document.moveObjectsToFolder(selectedHierarchyIndices(), "")
                context.hierarchyFolders.forEach { folder ->
                    if (ImGui.menuItem(folder.name)) document.moveObjectsToFolder(selectedHierarchyIndices(), folder.name)
                }
                ImGui.endMenu()
            }
            if (ImGui.menuItem("Delete")) {
                document.deleteObjects(selectedHierarchyIndices())
                ImGui.endPopup()
                return
            }
            ImGui.endPopup()
        }
    }

    private fun selectHierarchyObject(index: Int) {
        context.editMode = EditorEditMode.OBJECT
        context.selectedSpawnPointIndex = -1
        context.selectedEventAreaIndex = -1
        val io = ImGui.getIO()
        when {
            io.getKeyShift() && context.hierarchyRangeAnchorIndex in context.placedObjects.indices -> {
                context.selectedIndices.clear()
                val start = min(context.hierarchyRangeAnchorIndex, index)
                val end = max(context.hierarchyRangeAnchorIndex, index)
                for (i in start..end) context.selectedIndices.add(i)
                context.selectedIndex = index
            }
            io.getKeyCtrl() -> {
                if (index in context.selectedIndices) context.selectedIndices.remove(index) else context.selectedIndices.add(index)
                context.selectedIndex = index
                context.hierarchyRangeAnchorIndex = index
            }
            else -> selectSingleHierarchyObject(index)
        }
        context.selectedCollisionIndex = -1
    }

    private fun selectSingleHierarchyObject(index: Int) {
        context.editMode = EditorEditMode.OBJECT
        document.selectObject(index)
        context.selectedIndices.clear()
        if (index in context.placedObjects.indices) context.selectedIndices.add(index)
        context.hierarchyRangeAnchorIndex = index
        context.selectedCollisionIndex = -1
    }

    private fun selectedHierarchyIndices(): Set<Int> {
        return context.selectedIndices.filter { it in context.placedObjects.indices }.toSet()
            .ifEmpty { if (context.selectedIndex in context.placedObjects.indices) setOf(context.selectedIndex) else emptySet() }
    }

    private fun renderHierarchyObjectDragSource(index: Int) {
        if (!ImGui.beginDragDropSource()) return
        if (index !in context.selectedIndices) selectSingleHierarchyObject(index)
        val payload = selectedHierarchyIndices().sorted().joinToString(",")
        ImGui.setDragDropPayload("SMF_HIERARCHY_OBJECTS", payload)
        ImGui.text("${selectedHierarchyIndices().size} object(s)")
        ImGui.endDragDropSource()
    }

    private fun acceptHierarchyFolderDrop(folder: String) {
        if (!ImGui.beginDragDropTarget()) return
        val payload = ImGui.acceptDragDropPayload("SMF_HIERARCHY_OBJECTS", String::class.java)
        if (payload != null) {
            val indices = payload.split(",").mapNotNull { it.toIntOrNull() }
            document.moveObjectsToFolder(indices, folder)
        }
        ImGui.endDragDropTarget()
    }

    private fun renderEventAreaDragSource(index: Int) {
        if (!ImGui.beginDragDropSource()) return
        val indices = selectedEventAreaIndices().ifEmpty { setOf(index) }
        val payload = indices.sorted().joinToString(",")
        ImGui.setDragDropPayload("SMF_EVENT_AREAS", payload)
        ImGui.text("${indices.size} event area(s)")
        ImGui.endDragDropSource()
    }

    private fun acceptEventAreaFolderDrop(folder: String) {
        if (!ImGui.beginDragDropTarget()) return
        val payload = ImGui.acceptDragDropPayload("SMF_EVENT_AREAS", String::class.java)
        if (payload != null) {
            val indices = payload.split(",").mapNotNull { it.toIntOrNull() }
            document.moveEventAreasToFolder(indices, folder)
        }
        ImGui.endDragDropTarget()
    }

    private fun beginFolderRename(category: EditorFolderCategory, index: Int) {
        val folder = when (category) {
            EditorFolderCategory.OBJECT -> context.hierarchyFolders.getOrNull(index)
            EditorFolderCategory.EVENT_AREA -> context.eventAreaFolders.getOrNull(index)
            EditorFolderCategory.PATH -> context.pathFolders.getOrNull(index)
        } ?: return
        context.renamingFolderCategory = category
        context.renamingFolderIndex = index
        context.renamingFolderName.set(folder.name)
        context.renamingFolderFocusPending = true
    }

    private fun renderFolderRenameField(category: EditorFolderCategory, folder: EditorHierarchyFolder, index: Int) {
        if (context.renamingFolderFocusPending) {
            ImGui.setKeyboardFocusHere()
            context.renamingFolderFocusPending = false
        }
        val committed = ImGui.inputText("Name##folder_${category.name.lowercase()}_$index", context.renamingFolderName, ImGuiInputTextFlags.EnterReturnsTrue)
        if (committed || ImGui.isItemDeactivatedAfterEdit()) {
            when (category) {
                EditorFolderCategory.OBJECT -> document.renameHierarchyFolder(index, context.renamingFolderName.get())
                EditorFolderCategory.EVENT_AREA -> document.renameEventAreaFolder(index, context.renamingFolderName.get())
                EditorFolderCategory.PATH -> document.renamePathFolder(index, context.renamingFolderName.get())
            }
            context.renamingFolderIndex = -1
            context.renamingFolderName.set("")
            context.renamingFolderFocusPending = false
        }
    }

    private fun selectedEventAreaIndices(): Set<Int> {
        return if (context.selectedEventAreaIndex in context.eventAreas.indices && context.selectedSpawnPointIndex == -1) {
            setOf(context.selectedEventAreaIndex)
        } else {
            emptySet()
        }
    }

    private fun coercePanelWidths(contentWidth: Float) {
        val availableForPanels = max(MIN_PANEL_WIDTH * 2f, contentWidth - minViewportAreaWidth() - SPLITTER_WIDTH * 2f)
        val maxPanelWidth = max(MIN_PANEL_WIDTH, availableForPanels - MIN_PANEL_WIDTH)
        context.leftPanelWidth = min(context.leftPanelWidth, maxPanelWidth).coerceAtLeast(MIN_PANEL_WIDTH)
        context.rightPanelWidth = min(context.rightPanelWidth, maxPanelWidth).coerceAtLeast(MIN_PANEL_WIDTH)
    }

    private fun minViewportAreaWidth(): Float {
        return if (context.showSecondaryViewport) MIN_VIEWPORT_AREA_WIDTH else MIN_VIEWPORT_TAB_WIDTH
    }

    private fun renderProperties() {
        val selected = context.selectedObject()
        val selectedEventArea = context.eventAreas.getOrNull(context.selectedEventAreaIndex)
        val selectedSpawnPoint = selectedEventArea?.spawnPoints?.getOrNull(context.selectedSpawnPointIndex)
        expandCollisionTabToAvailableHeight()
        renderNestedTabChild(
            "Terrain",
            "Terrain",
            { context.terrainTabExpanded },
            { context.terrainTabExpanded = !context.terrainTabExpanded },
            context.terrainTabHeight,
            { renderTerrainControls() },
        )
        renderHorizontalSplitter("TerrainObjectSplitter") { delta ->
            resizeStackedTabs(
                delta,
                ::getTerrainHeight,
                ::setTerrainHeight,
                ::getObjectHeight,
                ::setObjectHeight,
                MIN_FREE_TAB_HEIGHT,
                MIN_FREE_TAB_HEIGHT,
            )
        }

        if (selectedSpawnPoint != null) {
            renderSpawnPointEditor(selectedSpawnPoint)
            renderHorizontalSplitter("ObjectCollisionsSplitter") { delta ->
                resizeStackedTabs(
                    delta,
                    ::getObjectHeight,
                    ::setObjectHeight,
                    ::getCollisionsHeight,
                    ::setCollisionsHeight,
                    MIN_FREE_TAB_HEIGHT,
                    MIN_FREE_TAB_HEIGHT,
                )
            }
            renderEmptyPropertyTab("Collision", "Collision", { context.collisionsTabExpanded }, { context.collisionsTabExpanded = !context.collisionsTabExpanded }, context.collisionsTabHeight)
            return
        }

        if (selectedEventArea != null) {
            renderEventAreaEditor(selectedEventArea)
            renderHorizontalSplitter("ObjectCollisionsSplitter") { delta ->
                resizeStackedTabs(
                    delta,
                    ::getObjectHeight,
                    ::setObjectHeight,
                    ::getCollisionsHeight,
                    ::setCollisionsHeight,
                    MIN_FREE_TAB_HEIGHT,
                    MIN_FREE_TAB_HEIGHT,
                )
            }
            renderEmptyPropertyTab("Collision", "Collision", { context.collisionsTabExpanded }, { context.collisionsTabExpanded = !context.collisionsTabExpanded }, context.collisionsTabHeight)
            return
        }

        if (selected != null) {
            renderObjectEditor(selected)
            renderHorizontalSplitter("ObjectCollisionsSplitter") { delta ->
                resizeStackedTabs(
                    delta,
                    ::getObjectHeight,
                    ::setObjectHeight,
                    ::getCollisionsHeight,
                    ::setCollisionsHeight,
                    MIN_FREE_TAB_HEIGHT,
                    MIN_FREE_TAB_HEIGHT,
                )
            }
            renderCollisionEditor(selected)
            return
        }

        renderEmptyPropertyTab("Object", "Object", { context.objectTabExpanded }, { context.objectTabExpanded = !context.objectTabExpanded }, context.objectTabHeight)
        renderHorizontalSplitter("ObjectCollisionsSplitter") { delta ->
            resizeStackedTabs(
                delta,
                ::getObjectHeight,
                ::setObjectHeight,
                ::getCollisionsHeight,
                ::setCollisionsHeight,
                MIN_FREE_TAB_HEIGHT,
                MIN_FREE_TAB_HEIGHT,
            )
        }
        renderEmptyPropertyTab(
            "Collision",
            "Collision",
            { context.collisionsTabExpanded },
            { context.collisionsTabExpanded = !context.collisionsTabExpanded },
            context.collisionsTabHeight,
        )
    }

    private fun renderSpawnPointEditor(spawn: EditorSpawnPoint) {
        renderNestedTabChild(
            "Spawnpoint",
            "Spawnpoint",
            { context.objectTabExpanded },
            { context.objectTabExpanded = !context.objectTabExpanded },
            context.objectTabHeight,
        ) {
            val name = ImString(spawn.name, 128)
            if (renderLabeledInputText("Name", "spawnpoint_name", name)) {
                updatePropertyUndoState(true)
                spawn.name = name.get()
            }
            val id = ImString(spawn.id, 128)
            if (renderLabeledInputText("ID", "spawnpoint_id", id)) {
                updatePropertyUndoState(true)
                spawn.id = id.get().ifBlank { "spawn_point" }
            }
            val type = ImString(spawn.type, 128)
            if (renderLabeledInputText("Type", "spawnpoint_type", type)) {
                updatePropertyUndoState(true)
                spawn.type = type.get().ifBlank { "player" }
            }
            addOneLineSpace()

            val position = floatArrayOf(spawn.position.x, spawn.position.y, spawn.position.z)
            if (renderMarkerTransformRow("Position", "spawnpoint_position", position, 0.05f)) {
                spawn.position = Vector3f(position[0], position[1], position[2])
            }
            val rotation = floatArrayOf(spawn.rotation.x, spawn.rotation.y, spawn.rotation.z)
            if (renderMarkerTransformRow("Rotation", "spawnpoint_rotation", rotation, 0.25f)) {
                spawn.rotation = Vector3f(rotation[0], rotation[1], rotation[2])
            }
            addOneLineSpace()
            if (ImGui.button("Place On Ground")) document.snapSelectedMarkerToGround()
        }
    }

    private fun renderEventAreaEditor(area: EditorEventArea) {
        renderNestedTabChild(
            "EventArea",
            "EventArea",
            { context.objectTabExpanded },
            { context.objectTabExpanded = !context.objectTabExpanded },
            context.objectTabHeight,
        ) {
            val name = ImString(area.name, 128)
            if (renderLabeledInputText("Name", "event_area_name", name)) {
                updatePropertyUndoState(true)
                area.name = name.get()
            }
            val id = ImString(area.id, 128)
            if (renderLabeledInputText("ID", "event_area_id", id)) {
                updatePropertyUndoState(true)
                area.id = id.get().ifBlank { if (area.isSpawnpoint()) "spawn_point" else "area_trigger" }
            }
            addOneLineSpace()

            val position = floatArrayOf(area.position.x, area.position.y, area.position.z)
            val rotation = floatArrayOf(area.rotation.x, area.rotation.y, area.rotation.z)
            val size = floatArrayOf(area.size.x, area.size.y, area.size.z)

            var changed = false
            changed = renderEventAreaTransformRow("Position", "event_area_position", Operation.TRANSLATE, position, 0.05f) || changed
            changed = renderEventAreaTransformRow("Rotation", "event_area_rotation", Operation.ROTATE, rotation, 0.25f) || changed
            changed = renderEventAreaTransformRow("Size", "event_area_size", Operation.SCALE, size, 0.05f, 0.01f, 10000f) || changed

            if (changed) {
                area.position = Vector3f(position[0], position[1], position[2])
                area.rotation = Vector3f(rotation[0], rotation[1], rotation[2])
                area.size = Vector3f(size[0], size[1], size[2])
            }
            addOneLineSpace()
            if (ImGui.button("Place On Ground")) document.snapSelectedMarkerToGround()
        }
    }

    private fun expandCollisionTabToAvailableHeight() {
        if (!context.collisionsTabExpanded) return

        val availableHeight = max(1f, ImGui.getContentRegionAvailY())
        val usedBeforeCollisions =
            tabHeight(context.terrainTabHeight, context.terrainTabExpanded) +
                TAB_SPLITTER_HEIGHT +
                tabHeight(context.objectTabHeight, context.objectTabExpanded) +
                TAB_SPLITTER_HEIGHT
        val remainingHeight = availableHeight - usedBeforeCollisions
        if (remainingHeight > context.collisionsTabHeight) {
            context.collisionsTabHeight = remainingHeight
        }
    }

    private fun renderEmptyPropertyTab(
        label: String,
        id: String,
        expanded: () -> Boolean,
        onToggle: () -> Unit,
        expandedHeight: Float,
    ) {
        renderNestedTabChild(label, id, expanded, onToggle, expandedHeight) {
            ImGui.textDisabled("No object selected")
        }
    }

    private fun renderObjectEditor(selected: EditorPlacedObject) {
        renderNestedTabChild(
            "Object",
            "Object",
            { context.objectTabExpanded },
            { context.objectTabExpanded = !context.objectTabExpanded },
            context.objectTabHeight,
        ) {
            val name = ImString(selected.name, 128)
            if (renderLabeledInputText("Name", "object_name", name)) {
                document.renameObject(context.selectedIndex, name.get())
            }
            val id = ImString(selected.id, 128)
            if (renderLabeledInputText("ID", "object_id", id)) {
                selected.id = id.get()
            }
            addOneLineSpace()

            val transform = selected.root.localTransform
            val previousPosition = transform.position
            val previousRotation = transform.rotation.toEulerDegrees()
            val previousScale = transform.scale
            val position = floatArrayOf(transform.position.x, transform.position.y, transform.position.z)
            val rotationEuler = transform.rotation.toEulerDegrees()
            val rotation = floatArrayOf(rotationEuler.x, rotationEuler.y, rotationEuler.z)
            val scale = floatArrayOf(transform.scale.x, transform.scale.y, transform.scale.z)

            var changed = false
            changed = renderObjectTransformRow("Position", "object_position", Operation.TRANSLATE, position, 0.05f) || changed
            changed = renderObjectTransformRow("Rotation", "object_rotation", Operation.ROTATE, rotation, 0.25f) || changed
            changed = renderObjectTransformRow("Scale", "object_scale", Operation.SCALE, scale, 0.02f, 0.01f, 10000f) || changed

            if (changed) {
                applySelectedObjectTransformDelta(
                    Vector3f(position[0], position[1], position[2]).subtract(previousPosition),
                    Vector3f(rotation[0], rotation[1], rotation[2]).subtract(previousRotation),
                    Vector3f(
                        scale[0].safeScaleRatio(previousScale.x),
                        scale[1].safeScaleRatio(previousScale.y),
                        scale[2].safeScaleRatio(previousScale.z),
                    ),
                )
            }
        }
    }

    private fun renderCollisionEditor(selected: EditorPlacedObject) {
        renderNestedTabChild(
            "Collision",
            "Collision",
            { context.collisionsTabExpanded },
            { context.collisionsTabExpanded = !context.collisionsTabExpanded },
            context.collisionsTabHeight,
        ) content@{
            if (ImGui.button("Add Box")) {
                document.pushUndoSnapshot()
                selected.collisions.add(
                    EditorCollisionState(
                        name = "Collision ${selected.collisions.size + 1}",
                        shape = EditorCollisionShape.BOX,
                        size = Vector3f(1f, 1f, 1f),
                    )
                )
                context.selectedCollisionIndex = selected.collisions.lastIndex
            }
            ImGui.sameLine()
            if (ImGui.button("Add Sphere")) {
                document.pushUndoSnapshot()
                selected.collisions.add(
                    EditorCollisionState(
                        name = "Collision ${selected.collisions.size + 1}",
                        shape = EditorCollisionShape.SPHERE,
                        radius = 1f,
                    )
                )
                context.selectedCollisionIndex = selected.collisions.lastIndex
            }
            addOneLineSpace()

            selected.collisions.forEachIndexed { index, collision ->
                var deleteRequested = false
                val label = "${collision.name.ifBlank { "Collision ${index + 1}" }}##collision_tab_$index"
                val open = ImGui.treeNodeEx(label, ImGuiTreeNodeFlags.DefaultOpen)
                if (ImGui.isItemClicked()) {
                    context.selectedCollisionIndex = index
                }
                if (ImGui.beginPopupContextItem("collision_context_$index")) {
                    context.selectedCollisionIndex = index
                    if (ImGui.menuItem("Rename")) {
                        beginCollisionRename(collision, index)
                    }
                    if (ImGui.menuItem("Delete")) {
                        deleteRequested = true
                    }
                    ImGui.endPopup()
                }
                if (context.renamingCollisionIndex == index && !open) {
                    renderCollisionRenameField(collision, index)
                }
                if (open) {
                    if (context.renamingCollisionIndex == index) {
                        renderCollisionRenameField(collision, index)
                    }

                    val position = floatArrayOf(collision.position.x, collision.position.y, collision.position.z)
                    renderCollisionOperationButton("Position", index, Operation.TRANSLATE)
                    ImGui.sameLine()
                    if (dragCollisionFloat3("##collision_position_$index", position, 0.02f)) {
                        collision.position = Vector3f(position[0], position[1], position[2])
                    }

                    when (collision.shape) {
                        EditorCollisionShape.BOX -> {
                            val rotation = floatArrayOf(collision.rotation.x, collision.rotation.y, collision.rotation.z)
                            renderCollisionOperationButton("Rotation", index, Operation.ROTATE)
                            ImGui.sameLine()
                            if (dragCollisionFloat3("##collision_rotation_$index", rotation, 0.25f)) {
                                collision.rotation = Vector3f(rotation[0], rotation[1], rotation[2])
                            }
                            val size = floatArrayOf(collision.size.x, collision.size.y, collision.size.z)
                            renderCollisionOperationButton("Scale", index, Operation.SCALE)
                            ImGui.sameLine()
                            if (dragCollisionFloat3("##collision_scale_$index", size, 0.02f, 0.01f, 10000f)) {
                                collision.size = Vector3f(size[0], size[1], size[2])
                            }
                        }

                        EditorCollisionShape.SPHERE -> {
                            val radius = floatArrayOf(collision.radius)
                            renderCollisionOperationButton("Scale", index, Operation.SCALE)
                            ImGui.sameLine()
                            if (dragCollisionFloat("##collision_radius_$index", radius, 0.02f, 0.01f, 10000f)) {
                                collision.radius = radius[0]
                            }
                        }
                    }

                    ImGui.treePop()
                }
                if (deleteRequested) {
                    deleteCollision(selected, index)
                    return@content
                }
            }
        }
    }

    private fun applySelectedObjectTransformDelta(positionDelta: Vector3f, rotationDelta: Vector3f, scaleRatio: Vector3f) {
        val targets = context.selectedIndices
            .filter { it in context.placedObjects.indices }
            .ifEmpty { listOf(context.selectedIndex).filter { it in context.placedObjects.indices } }

        for (index in targets) {
            val transform = context.placedObjects[index].root.localTransform
            context.placedObjects[index].root.localTransform = Transform(
                position = transform.position.add(positionDelta),
                rotation = transform.rotation.add(rotationDelta),
                scale = Vector3f(
                    (transform.scale.x * scaleRatio.x).coerceAtLeast(0.01f),
                    (transform.scale.y * scaleRatio.y).coerceAtLeast(0.01f),
                    (transform.scale.z * scaleRatio.z).coerceAtLeast(0.01f),
                ),
            )
        }
    }

    private fun Float.safeScaleRatio(previous: Float): Float {
        return if (kotlin.math.abs(previous) < 0.0001f) 1f else this / previous
    }

    private fun deleteCollision(selected: EditorPlacedObject, index: Int) {
        if (index !in selected.collisions.indices) return

        document.pushUndoSnapshot()
        selected.collisions.removeAt(index)
        context.selectedCollisionIndex = when {
            context.selectedCollisionIndex == index -> -1
            context.selectedCollisionIndex > index -> context.selectedCollisionIndex - 1
            else -> context.selectedCollisionIndex
        }
        context.renamingCollisionIndex = when {
            context.renamingCollisionIndex == index -> -1
            context.renamingCollisionIndex > index -> context.renamingCollisionIndex - 1
            else -> context.renamingCollisionIndex
        }
        if (context.renamingCollisionIndex == -1) {
            context.renamingCollisionName.set("")
            context.renamingCollisionFocusPending = false
        }
    }

    private fun renderCollisionOperationButton(label: String, index: Int, operation: Int) {
        if (collisionOperationButton(label, index, operation)) {
            context.selectedCollisionIndex = index
            context.gizmoOperation = operation
        }
    }

    private fun beginCollisionRename(collision: EditorCollisionState, index: Int) {
        context.renamingCollisionIndex = index
        context.renamingCollisionName.set(collision.name.ifBlank { "Collision ${index + 1}" })
        context.renamingCollisionFocusPending = true
    }

    private fun renderCollisionRenameField(collision: EditorCollisionState, index: Int) {
        if (context.renamingCollisionFocusPending) {
            ImGui.setKeyboardFocusHere()
            context.renamingCollisionFocusPending = false
        }

        val committed = ImGui.inputText(
            "Name##collision$index",
            context.renamingCollisionName,
            ImGuiInputTextFlags.EnterReturnsTrue,
        )
        if (committed || ImGui.isItemDeactivatedAfterEdit()) {
            val newName = context.renamingCollisionName.get().trim()
            collision.name = newName.ifBlank { "Collision ${index + 1}" }
            context.renamingCollisionIndex = -1
            context.renamingCollisionName.set("")
            context.renamingCollisionFocusPending = false
        }
    }

    private fun renderViewportDebugOverlay(viewportX: Float, viewportY: Float, viewportWidth: Float, viewportIndex: Int) {
        val camera = context.viewportCamera(viewportIndex)
        val lines = listOf(
            "Tab: Viewport ${viewportIndex + 1}",
            "Editor Mode",
            "FPS/TPS: ${SMF.timer.getFPS()}/${SMF.timer.getUPS()}",
            "Camera: ${formatDebugFloat(camera.position.x)}, ${formatDebugFloat(camera.position.y)}, ${formatDebugFloat(camera.position.z)}",
            "Objects: ${context.placedObjects.size}",
            "Undo/Redo: ${context.undoStack.size}/${context.redoStack.size}",
        )
        val fontSize = ImGui.getFontSize().toFloat()
        val padding = 6f
        val lineHeight = fontSize + 3f
        val overlayWidth = lines.maxOf { ImGui.calcTextSizeX(it) } + padding * 2f
        val overlayHeight = lines.size * lineHeight + padding * 2f
        val drawList = ImGui.getWindowDrawList()
        val x = viewportX + viewportWidth - overlayWidth - 8f
        val y = viewportY + 8f

        drawList.addRectFilled(
            x,
            y,
            x + overlayWidth,
            y + overlayHeight,
            ImColor.rgba(0, 0, 0, 150),
            4f,
        )

        var textY = y + padding
        for (line in lines) {
            drawList.addText(x + padding, textY, ImColor.rgba(235, 238, 245, 255), line)
            textY += lineHeight
        }
    }

    private fun formatDebugFloat(value: Float): String {
        return String.format(java.util.Locale.US, "%.2f", value)
    }

    private fun acceptViewportDrops() {
        if (ImGui.beginDragDropTarget()) {
            val asset = ImGui.acceptDragDropPayload("SMF_GLB_ASSET", String::class.java)
            if (asset != null) {
                val position = EditorPicking.intersectGround(EditorPicking.currentMouseRay(context))
                    ?: context.activeCamera().position.add(context.activeCamera().getFront().scale(12f))
                document.addAsset(asset, position)
            }
            ImGui.endDragDropTarget()
        }
    }

    private fun chooseGlbFile() {
        val path = EditorFileDialog.chooseGlbFile() ?: return
        document.registerExternalGlb(path)
        addAssetAtCursor()
    }

    private fun collisionOperationButton(label: String, index: Int, operation: Int): Boolean {
        val selected = context.selectedCollisionIndex == index && context.gizmoOperation == operation
        val text = "$label##collision_op_${label}_$index"
        return coloredSelectionButton(text, selected)
    }

    private fun renderObjectTransformRow(label: String, id: String, operation: Int, values: FloatArray, speed: Float): Boolean {
        renderObjectTransformButton(label, operation)
        ImGui.sameLine(operationButtonWidth() + 16f)
        return dragTransformFloat3("##$id", values, speed)
    }

    private fun renderObjectTransformRow(label: String, id: String, operation: Int, values: FloatArray, speed: Float, min: Float, max: Float): Boolean {
        renderObjectTransformButton(label, operation)
        ImGui.sameLine(operationButtonWidth() + 16f)
        return dragTransformFloat3("##$id", values, speed, min, max)
    }

    private fun renderEventAreaTransformRow(label: String, id: String, operation: Int, values: FloatArray, speed: Float): Boolean {
        renderEventAreaTransformButton(label, operation)
        ImGui.sameLine(operationButtonWidth() + 16f)
        return dragTransformFloat3("##$id", values, speed)
    }

    private fun renderEventAreaTransformRow(label: String, id: String, operation: Int, values: FloatArray, speed: Float, min: Float, max: Float): Boolean {
        renderEventAreaTransformButton(label, operation)
        ImGui.sameLine(operationButtonWidth() + 16f)
        return dragTransformFloat3("##$id", values, speed, min, max)
    }

    private fun renderMarkerTransformRow(label: String, id: String, values: FloatArray, speed: Float): Boolean {
        ImGui.text(label)
        ImGui.sameLine(operationButtonWidth() + 16f)
        return dragTransformFloat3("##$id", values, speed)
    }

    private fun renderMarkerTransformRow(label: String, id: String, values: FloatArray, speed: Float, min: Float, max: Float): Boolean {
        ImGui.text(label)
        ImGui.sameLine(operationButtonWidth() + 16f)
        return dragTransformFloat3("##$id", values, speed, min, max)
    }

    private fun renderObjectTransformButton(label: String, operation: Int) {
        val selected = context.selectedCollisionIndex == -1 && context.gizmoOperation == operation
        val text = "$label##object_op_$label"
        if (coloredSelectionButton(text, selected)) {
            context.selectedCollisionIndex = -1
            context.gizmoOperation = operation
        }
    }

    private fun renderEventAreaTransformButton(label: String, operation: Int) {
        val selected = context.selectedEventAreaIndex in context.eventAreas.indices &&
            context.selectedSpawnPointIndex == -1 &&
            context.gizmoOperation == operation
        val text = "$label##event_area_op_$label"
        if (coloredSelectionButton(text, selected)) {
            context.selectedCollisionIndex = -1
            context.gizmoOperation = operation
        }
    }

    private fun coloredSelectionButton(label: String, selected: Boolean): Boolean {
        if (selected) {
            ImGui.pushStyleColor(ImGuiCol.Button, ImColor.rgba(214, 168, 45, 255))
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImColor.rgba(232, 188, 62, 255))
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImColor.rgba(196, 145, 34, 255))
        }
        val clicked = ImGui.button(label, operationButtonWidth(), 0f)
        if (selected) {
            ImGui.popStyleColor(3)
        }
        return clicked
    }

    private fun operationButtonWidth(): Float {
        val padding = ImGui.getStyle().framePaddingX * 2f + BUTTON_WIDTH_SAFETY
        val longest = maxOf(
            ImGui.calcTextSizeX("Position"),
            ImGui.calcTextSizeX("Rotation"),
            ImGui.calcTextSizeX("Scale"),
        )
        return max(COLLISION_OP_BUTTON_WIDTH, longest + padding)
    }

    private fun dragTransformFloat3(label: String, values: FloatArray, speed: Float): Boolean {
        val changed = ImGui.dragFloat3(label, values, speed)
        updatePropertyUndoState(changed)
        return changed
    }

    private fun dragTransformFloat3(label: String, values: FloatArray, speed: Float, min: Float, max: Float): Boolean {
        val changed = ImGui.dragFloat3(label, values, speed, min, max)
        updatePropertyUndoState(changed)
        return changed
    }

    private fun dragCollisionFloat3(label: String, values: FloatArray, speed: Float): Boolean {
        val changed = ImGui.dragFloat3(label, values, speed)
        updateCollisionUndoState(changed)
        return changed
    }

    private fun dragCollisionFloat3(label: String, values: FloatArray, speed: Float, min: Float, max: Float): Boolean {
        val changed = ImGui.dragFloat3(label, values, speed, min, max)
        updateCollisionUndoState(changed)
        return changed
    }

    private fun dragCollisionFloat(label: String, values: FloatArray, speed: Float, min: Float, max: Float): Boolean {
        val changed = ImGui.dragFloat(label, values, speed, min, max)
        updateCollisionUndoState(changed)
        return changed
    }

    private fun updatePropertyUndoState(changed: Boolean) {
        if (changed && !context.propertyEditInProgress) {
            document.pushUndoSnapshot()
            context.propertyEditInProgress = true
        }

        if (ImGui.isItemDeactivatedAfterEdit()) {
            context.propertyEditInProgress = false
        }
    }

    private fun updateCollisionUndoState(changed: Boolean) {
        if (changed && !context.propertyEditInProgress) {
            document.pushUndoSnapshot()
            context.propertyEditInProgress = true
        }

        if (ImGui.isItemDeactivatedAfterEdit()) {
            context.propertyEditInProgress = false
        }
    }

    private companion object {
        const val MIN_PANEL_WIDTH = 180f
        const val MIN_VIEWPORT_TAB_WIDTH = 120f
        const val MIN_VIEWPORT_HEIGHT = 180f
        const val SPLITTER_WIDTH = 6f
        const val MIN_VIEWPORT_AREA_WIDTH = MIN_VIEWPORT_TAB_WIDTH * 2f + SPLITTER_WIDTH
        const val TAB_SPLITTER_HEIGHT = 6f
        const val MIN_FREE_TAB_HEIGHT = 1f
        const val COLLISION_OP_BUTTON_WIDTH = 100f
        const val TAB_VERTICAL_PADDING = 3f
        const val CONTENT_PADDING_X = 6f
        const val CONTENT_PADDING_Y = 5f
        const val CHILD_TAB_RIGHT_MARGIN = 8f
        const val LEVEL_LABEL_WIDTH = 132f
        const val TERRAIN_PARAM_LABEL_WIDTH = 132f
        const val CONTROL_RIGHT_PADDING = 10f
        const val OBJECT_FIELD_LABEL_WIDTH = 76f
        const val LEVEL_ROW_RIGHT_PADDING = 12f
        const val PATH_INPUT_WIDTH_RATIO = 0.7f
        const val BUTTON_WIDTH_SAFETY = 6f
        const val ASSET_TILE_WIDTH = 142f
        const val ASSET_TILE_HEIGHT = 112f
    }
}
