package qorrnsmj.smf.editor

import imgui.ImGui
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import imgui.type.ImString
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF
import java.nio.file.Path

internal class EditorProjectUi(private val context: EditorContext, private val document: EditorDocument) {
    private var projects = emptyList<EditorProject>()
    private var selected: EditorProject? = null
    private val name = ImString("Untitled", 256)
    private var pending: (() -> Unit)? = null
    private var deleting: EditorProject? = null
    private var status = ""
    private val open = ImBoolean(true)

    private fun action(block: () -> Unit) {
        try { block() } catch (error: Exception) { Logger.warn(error, "Project operation failed") }
    }

    private fun refresh() {
        projects = document.projects.list()
        selected = projects.firstOrNull { it.id == selected?.id } ?: projects.firstOrNull { it.id == context.project?.id }
    }

    private fun switchProject(block: () -> Unit) {
        if (context.project != null && document.hasUnsavedMapChanges()) pending = block else block()
    }

    fun export() {
        val project = checkNotNull(context.project) { "No project is open" }
        val path = EditorFileDialog.chooseZip(true, project.name) ?: return
        document.saveMap()
        document.projects.export(project, Path.of(path))
        status = "Exported: $path"
    }

    fun render() {
        if (context.projectsOpen) {
            context.projectsOpen = false
            status = ""
            open.set(true)
            action { refresh() }
            ImGui.openPopup("Projects")
        }
        ImGui.setNextWindowSize(minOf(720f, SMF.window.width - 32f), minOf(560f, SMF.window.height - 32f), ImGuiCond.Appearing)
        context.projectDialogActive = ImGui.beginPopupModal("Projects", open, ImGuiWindowFlags.NoSavedSettings)
        if (!open.get()) {
            pending = null
            deleting = null
        }
        if (!context.projectDialogActive) return
        try {
            if (pending != null) {
                ImGui.textWrapped("Save changes to ${context.project?.name} before continuing?")
                if (ImGui.button("Save and Continue")) action {
                    document.saveMap()
                    val next = pending
                    pending = null
                    next?.invoke()
                }
                ImGui.sameLine()
                if (ImGui.button("Discard Changes")) action {
                    val next = pending
                    pending = null
                    next?.invoke()
                }
                ImGui.sameLine()
                if (ImGui.button("Cancel")) pending = null
            } else if (deleting != null) {
                ImGui.textWrapped("Delete ${deleting?.name}? Its map and assets will be permanently removed.")
                if (ImGui.button("Delete Project")) action {
                    val target = checkNotNull(deleting)
                    if (context.project?.id == target.id) {
                        val replacement = projects.firstOrNull { it.id != target.id } ?: document.projects.create("Untitled")
                        document.openProject(replacement)
                    }
                    document.projects.delete(target)
                    deleting = null
                    selected = null
                    refresh()
                }
                ImGui.sameLine()
                if (ImGui.button("Cancel")) deleting = null
            } else {
                ImGui.setNextItemWidth(-1f)
                ImGui.inputText("##ProjectName", name)
                context.lastTextInputActive = context.lastTextInputActive || ImGui.isItemActive()
                if (ImGui.button("New")) action { switchProject {
                    val created = document.projects.create(name.get())
                    document.openProject(created)
                    selected = created
                    refresh()
                } }
                ImGui.sameLine()
                if (ImGui.button("Import ZIP")) action {
                    val path = EditorFileDialog.chooseZip(false)
                    if (path != null) switchProject {
                        val imported = document.projects.importZip(Path.of(path))
                        document.openProject(imported)
                        selected = imported
                        refresh()
                    }
                }
                ImGui.sameLine()
                if (ImGui.button("Import Level")) action {
                    val path = EditorFileDialog.chooseJsonFile()
                    if (path != null) switchProject {
                        val imported = document.projects.importLevel(Path.of(path))
                        document.openProject(imported)
                        selected = imported
                        refresh()
                    }
                }
                ImGui.sameLine()
                if (ImGui.button("Refresh")) action { refresh() }
                ImGui.separator()
                ImGui.beginChild("ProjectList", 0f, maxOf(80f, ImGui.getContentRegionAvailY() - 120f), true)
                for (project in projects) {
                    val active = if (context.project?.id == project.id) " (Open)" else ""
                    if (ImGui.selectable("${project.name}$active##${project.id}", selected?.id == project.id)) {
                        selected = project
                        name.set(project.name)
                    }
                }
                ImGui.endChild()
                selected?.let { project ->
                    if (ImGui.button("Open")) action { switchProject {
                        document.openProject(project)
                        ImGui.closeCurrentPopup()
                    } }
                    ImGui.sameLine()
                    if (ImGui.button("Duplicate")) action {
                        if (project.id == context.project?.id) document.saveMap()
                        selected = document.projects.duplicate(project)
                        refresh()
                        name.set(selected?.name ?: "Untitled")
                    }
                    ImGui.sameLine()
                    if (ImGui.button("Rename")) action {
                        val renamed = document.projects.rename(project, name.get())
                        if (context.project?.id == project.id) context.project = renamed
                        selected = renamed
                        refresh()
                    }
                    ImGui.sameLine()
                    if (ImGui.button("Delete")) deleting = project
                    ImGui.sameLine()
                    if (ImGui.button("Export ZIP")) action {
                        val destination = EditorFileDialog.chooseZip(true, project.name)
                        if (destination != null) {
                            if (project.id == context.project?.id) document.saveMap()
                            document.projects.export(project, Path.of(destination))
                            status = "Exported: $destination"
                        }
                    }
                }
            }
            if (status.isNotBlank()) ImGui.textWrapped(status)
        } finally { ImGui.endPopup() }
    }
}
