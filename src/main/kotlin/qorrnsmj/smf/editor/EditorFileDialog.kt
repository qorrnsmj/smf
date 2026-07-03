package qorrnsmj.smf.editor

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

internal object EditorFileDialog {
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
