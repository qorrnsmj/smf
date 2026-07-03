package qorrnsmj.smf.editor

import com.fasterxml.jackson.databind.ObjectMapper
import org.tinylog.kotlin.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal object EditorConfig {
    private val objectMapper = ObjectMapper()
    private val configPath: Path by lazy {
        val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
            ?: Paths.get(System.getProperty("user.home"), "AppData", "Roaming").toString()
        Paths.get(appData).resolve("SMF").resolve("editor").resolve("editor_config.json")
    }

    fun load(context: EditorContext) {
        if (!Files.isRegularFile(configPath)) return

        try {
            val config = objectMapper.readValue(configPath.toFile(), Map::class.java)
            config.string("workspaceRoot")?.let { context.workspaceRoot.set(it) }
            config.string("projectName")?.let { context.projectName.set(it) }
            config.string("mapPath")?.let { context.mapPath.set(it) }
            config.string("skyboxPath")?.let { context.skyboxPath.set(it) }
            config.string("terrainHeightmapPath")?.let { context.terrainHeightmapPath.set(it) }
            config.string("terrainSplatmapPath")?.let { context.terrainSplatmapPath.set(it) }
            config.string("assetModelRoot")?.let { context.assetModelRoot.set(it) }
            config.string("assetBrowserPath")?.let { context.assetBrowserPath.set(it) }
            config.float("editorFontScale")?.let { context.editorFontScale = it }
            config.float("leftPanelWidth")?.let { context.leftPanelWidth = it }
            config.float("rightPanelWidth")?.let { context.rightPanelWidth = it }
            config.float("viewportSplitRatio")?.let { context.viewportSplitRatio = it.coerceIn(0.1f, 0.9f) }
            config.float("assetsTabHeight")?.let { context.assetsTabHeight = it }
            config.float("terrainTabHeight")?.let { context.terrainTabHeight = it }
            config.float("objectTabHeight")?.let { context.objectTabHeight = it }
            config.float("collisionsTabHeight")?.let { context.collisionsTabHeight = it }
            config.boolean("showLeftPanel")?.let { context.showLeftPanel = it }
            config.boolean("showViewportPanel")?.let { context.showViewportPanel = it }
            config.boolean("showSecondaryViewport")?.let { context.showSecondaryViewport = it }
            config.boolean("showRightPanel")?.let { context.showRightPanel = it }
            config.boolean("showAssetsTab")?.let { context.showAssetsTab = it }
            config.boolean("showMapsTab")?.let { context.showMapsTab = it }
            config.boolean("showLevelTab")?.let { context.showLevelTab = it }
            config.boolean("terrainGrayViewEnabled")?.let { context.terrainGrayViewEnabled = it }
            config.boolean("terrainMeshViewEnabled")?.let { context.terrainMeshViewEnabled = it }
            config.string("viewportShadingMode")?.let { value ->
                runCatching { qorrnsmj.smf.graphic.ViewportShadingMode.valueOf(value) }.getOrNull()?.let { context.viewportShadingMode = it }
            }
            config.string("secondaryViewportShadingMode")?.let { value ->
                runCatching { qorrnsmj.smf.graphic.ViewportShadingMode.valueOf(value) }.getOrNull()?.let { context.secondaryViewportShadingMode = it }
            }
            config.boolean("cullingEnabled")?.let { context.cullingEnabled = it }
            config.boolean("skyVisible")?.let { context.skyVisible = it }
            config.boolean("nightMode")?.let {
                context.nightMode = it
                if (it && config.string("timeOfDay") == null) context.timeOfDay = EditorTimeOfDay.NIGHT
            }
            config.string("timeOfDay")?.let { context.timeOfDay = EditorTimeOfDay.fromJson(it) }
            config.string("secondaryTimeOfDay")?.let { context.secondaryTimeOfDay = EditorTimeOfDay.fromJson(it) }
            config.string("editMode")?.let { value ->
                runCatching { EditorEditMode.valueOf(value) }.getOrNull()?.let { context.editMode = it }
            }
            config.string("bottomPanelTab")?.let { value ->
                runCatching { EditorBottomPanelTab.valueOf(value) }.getOrNull()?.let { context.bottomPanelTab = it }
            }
            Logger.info("Editor config loaded: {}", configPath)
        } catch (error: Throwable) {
            Logger.warn(error, "Editor config could not be loaded: {}", configPath)
        }
    }

    fun save(context: EditorContext) {
        val config = EditorConfigData(
            workspaceRoot = context.workspaceRoot.get(),
            projectName = context.projectName.get(),
            mapPath = context.mapPath.get(),
            skyboxPath = context.skyboxPath.get(),
            terrainHeightmapPath = context.terrainHeightmapPath.get(),
            terrainSplatmapPath = context.terrainSplatmapPath.get(),
            assetModelRoot = context.assetModelRoot.get(),
            assetBrowserPath = context.assetBrowserPath.get(),
            editorFontScale = context.editorFontScale,
            leftPanelWidth = context.leftPanelWidth,
            rightPanelWidth = context.rightPanelWidth,
            viewportSplitRatio = context.viewportSplitRatio,
            assetsTabHeight = context.assetsTabHeight,
            terrainTabHeight = context.terrainTabHeight,
            objectTabHeight = context.objectTabHeight,
            collisionsTabHeight = context.collisionsTabHeight,
            showLeftPanel = context.showLeftPanel,
            showViewportPanel = context.showViewportPanel,
            showSecondaryViewport = context.showSecondaryViewport,
            showRightPanel = context.showRightPanel,
            showAssetsTab = context.showAssetsTab,
            showMapsTab = context.showMapsTab,
            showLevelTab = context.showLevelTab,
            terrainGrayViewEnabled = context.terrainGrayViewEnabled,
            terrainMeshViewEnabled = context.terrainMeshViewEnabled,
            viewportShadingMode = context.viewportShadingMode.name,
            secondaryViewportShadingMode = context.secondaryViewportShadingMode.name,
            cullingEnabled = context.cullingEnabled,
            skyVisible = context.skyVisible,
            nightMode = context.nightMode,
            timeOfDay = context.timeOfDay.jsonName,
            secondaryTimeOfDay = context.secondaryTimeOfDay.jsonName,
            editMode = context.editMode.name,
            bottomPanelTab = context.bottomPanelTab.name,
        )

        try {
            Files.createDirectories(configPath.parent)
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), config)
            Logger.info("Editor config saved: {}", configPath)
        } catch (error: Throwable) {
            Logger.warn(error, "Editor config could not be saved: {}", configPath)
        }
    }
}

private fun Map<*, *>.string(key: String): String? = this[key] as? String

private fun Map<*, *>.float(key: String): Float? = (this[key] as? Number)?.toFloat()

private fun Map<*, *>.boolean(key: String): Boolean? = this[key] as? Boolean

internal data class EditorConfigData(
    val workspaceRoot: String? = null,
    val projectName: String? = null,
    val mapPath: String? = null,
    val skyboxPath: String? = null,
    val terrainHeightmapPath: String? = null,
    val terrainSplatmapPath: String? = null,
    val assetModelRoot: String? = null,
    val assetBrowserPath: String? = null,
    val editorFontScale: Float? = null,
    val leftPanelWidth: Float? = null,
    val rightPanelWidth: Float? = null,
    val viewportSplitRatio: Float? = null,
    val assetsTabHeight: Float? = null,
    val terrainTabHeight: Float? = null,
    val objectTabHeight: Float? = null,
    val collisionsTabHeight: Float? = null,
    val showLeftPanel: Boolean? = null,
    val showViewportPanel: Boolean? = null,
    val showSecondaryViewport: Boolean? = null,
    val showRightPanel: Boolean? = null,
    val showAssetsTab: Boolean? = null,
    val showMapsTab: Boolean? = null,
    val showLevelTab: Boolean? = null,
    val terrainGrayViewEnabled: Boolean? = null,
    val terrainMeshViewEnabled: Boolean? = null,
    val viewportShadingMode: String? = null,
    val secondaryViewportShadingMode: String? = null,
    val cullingEnabled: Boolean? = null,
    val skyVisible: Boolean? = null,
    val nightMode: Boolean? = null,
    val timeOfDay: String? = null,
    val secondaryTimeOfDay: String? = null,
    val editMode: String? = null,
    val bottomPanelTab: String? = null,
)
