package qorrnsmj.smf.editor

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

internal object EditorFileDialog {
    fun chooseZip(save: Boolean, name: String = "map"): String? {
        val chooser = JFileChooser()
        chooser.fileFilter = FileNameExtensionFilter("Map project (*.zip)", "zip")
        if (save) chooser.selectedFile = File(name.replace(Regex("[\\\\/:*?\"<>|]"), "_") + ".zip")
        val result = if (save) chooser.showSaveDialog(null) else chooser.showOpenDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) return null
        val selected = chooser.selectedFile.absoluteFile.normalize()
        val file = if (save && !selected.name.endsWith(".zip", true)) File(selected.path + ".zip") else selected
        if (save && file.exists() && javax.swing.JOptionPane.showConfirmDialog(null,
                "Replace ${file.name}?", "Export", javax.swing.JOptionPane.YES_NO_OPTION) != javax.swing.JOptionPane.YES_OPTION) return null
        return file.path
    }

    fun chooseGlbFile(): String? {
        val chooser = JFileChooser(File("."))
        chooser.fileFilter = FileNameExtensionFilter("GLB model (*.glb)", "glb")
        chooser.isMultiSelectionEnabled = false

        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absoluteFile.normalize().path
        } else {
            null
        }
    }

    fun choosePngFile(initialDirectory: String? = null): String? {
        val chooser = JFileChooser(initialDirectory?.takeIf { it.isNotBlank() }?.let { File(it) } ?: File("."))
        chooser.fileFilter = FileNameExtensionFilter("PNG image (*.png)", "png")
        chooser.isMultiSelectionEnabled = false

        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absoluteFile.normalize().path
        } else {
            null
        }
    }

    fun chooseJsonFile(initialDirectory: String? = null): String? {
        val chooser = JFileChooser(initialDirectory?.takeIf { it.isNotBlank() }?.let { File(it) } ?: File("."))
        chooser.fileFilter = FileNameExtensionFilter("JSON level (*.json)", "json")
        chooser.isMultiSelectionEnabled = false

        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absoluteFile.normalize().path
        } else {
            null
        }
    }

    fun chooseDirectory(): String? {
        val chooser = JFileChooser(File("."))
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.isMultiSelectionEnabled = false

        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absoluteFile.normalize().path
        } else {
            null
        }
    }
}
