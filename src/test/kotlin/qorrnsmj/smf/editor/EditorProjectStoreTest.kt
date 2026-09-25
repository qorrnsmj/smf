package qorrnsmj.smf.editor

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EditorProjectStoreTest {
    @TempDir lateinit var temporary: Path
    private val mapper = ObjectMapper()

    @Test fun duplicateKeepsEditorStateButUsesIndependentFilesAndId() {
        val store = EditorProjectStore(temporary.resolve("projects"))
        val original = store.create("Original")
        assertEquals("Original", original.id)
        assertEquals(temporary.resolve("projects/Original"), original.root)
        Files.writeString(original.root.resolve("EditorData.json"), "{\"cameras\":[]}")
        val copy = store.duplicate(original)
        assertNotEquals(original.id, copy.id)
        assertEquals("Original_Copy", copy.id)
        assertEquals("Original Copy", copy.name)
        assertEquals(Files.readString(original.root.resolve("EditorData.json")), Files.readString(copy.root.resolve("EditorData.json")))
        Files.writeString(copy.level, "{}")
        assertNotEquals(Files.readString(original.level), Files.readString(copy.level))
        store.delete(copy)
        assertTrue(Files.exists(original.level))
        assertFalse(Files.exists(copy.root))
    }

    @Test fun legacyImportAndZipRoundTripBundleOnlyRuntimeAssets() {
        val store = EditorProjectStore(temporary.resolve("projects"))
        val source = temporary.resolve("legacy")
        Files.createDirectories(source.resolve("level"))
        Files.createDirectories(source.resolve("model"))
        Files.createDirectories(source.resolve("texture"))
        Files.write(source.resolve("model/tree.glb"), byteArrayOf(1, 2, 3))
        Files.write(source.resolve("texture/height.png"), byteArrayOf(4, 5, 6))
        val level = source.resolve("level/test.json")
        Files.writeString(level, """{"terrain":{"heightmap":"assets/texture/height.png"},"objects":[{"model":"model/tree.glb"}]}""")
        val imported = store.importLevel(level)
        Files.writeString(imported.root.resolve("EditorData.json"), "{}")
        Files.writeString(imported.root.resolve("unused.txt"), "not exported")
        val zip = temporary.resolve("map.zip")
        store.export(imported, zip)
        ZipFile(zip.toFile()).use { archive ->
            val names = archive.entries().asSequence().map { it.name }.toSet()
            assertEquals(setOf("project.json", "level/map.json", "texture/height.png", "model/tree.glb"), names)
        }
        val restored = store.importZip(zip)
        assertNotEquals(imported.id, restored.id)
        assertEquals(Files.readString(imported.level), Files.readString(restored.level))
        assertTrue(Files.readAllBytes(restored.root.resolve("model/tree.glb")).contentEquals(byteArrayOf(1, 2, 3)))
        assertFalse(Files.exists(restored.root.resolve("EditorData.json")))
        assertEquals("assets/texture/height.png", mapper.readTree(level.toFile()).path("terrain").path("heightmap").asText())
    }

    @Test fun missingAssetsDoNotLeavePartialProjectsOrOverwriteExports() {
        val store = EditorProjectStore(temporary.resolve("projects"))
        val project = store.create("Broken")
        Files.writeString(project.level, """{"static_objects":[{"model":"model/missing.glb"}]}""")
        val destination = temporary.resolve("existing.zip")
        Files.writeString(destination, "original")
        assertFailsWith<IllegalStateException> { store.export(project, destination) }
        assertEquals("original", Files.readString(destination))
        assertFailsWith<IllegalStateException> { store.importLevel(project.level) }
        assertEquals(1, store.list().size)
    }

    @Test fun zipTraversalAndExternalReferencesAreRejected() {
        val store = EditorProjectStore(temporary.resolve("projects"))
        val zip = temporary.resolve("bad.zip")
        fun archive(name: String, body: String) {
            ZipOutputStream(Files.newOutputStream(zip)).use { output ->
                output.putNextEntry(ZipEntry(name))
                output.write(body.toByteArray())
                output.closeEntry()
            }
        }
        archive("../escaped.txt", "bad")
        assertFailsWith<IllegalArgumentException> { store.importZip(zip) }
        archive("level/map.json", """{"terrain":{"heightmap":"../../private.png"}}""")
        assertFailsWith<IllegalArgumentException> { store.importZip(zip) }
        assertTrue(store.list().isEmpty())
    }

    @Test fun absoluteSkyboxFacesKeepOneCommonPrefix() {
        val store = EditorProjectStore(temporary.resolve("projects"))
        val prefix = temporary.resolve("sky").toString().replace('\\', '/')
        for (suffix in listOf("right", "left", "top", "bottom", "front", "back")) {
            Files.write(Path.of("${prefix}_$suffix.png"), byteArrayOf(1))
        }
        val level = temporary.resolve("legacy.json")
        Files.writeString(level, """{"environment":{"skybox":"$prefix"}}""")
        val project = store.importLevel(level)
        val imported = mapper.readTree(project.level.toFile()).path("environment").path("skybox").asText()
        assertFalse(Path.of(imported).isAbsolute)
        for (suffix in listOf("right", "left", "top", "bottom", "front", "back")) {
            assertTrue(Files.isRegularFile(project.root.resolve("${imported}_$suffix.png")))
        }
    }

    @Test fun renameDoesNotMoveMapAndDeleteRejectsOutsideRoot() {
        val store = EditorProjectStore(temporary.resolve("projects"))
        val project = store.create("First")
        assertEquals(project.level, store.rename(project, "Second").level)
        assertEquals("Second", store.list().single().name)
        assertFailsWith<IllegalArgumentException> { store.delete(project.copy(root = temporary)) }
        assertTrue(Files.isRegularFile(project.level))
    }

    @Test fun legacyUuidDirectoryMigratesToReadableProjectId() {
        val root = temporary.resolve("projects")
        val legacy = root.resolve("170226b0-41ac-4d5d-9396-fd6d38ad72a5")
        Files.createDirectories(legacy.resolve("level"))
        EditorProjectStore.writeJson(legacy.resolve("project.json"), mapOf("name" to "test", "level" to "level/map.json"))
        Files.writeString(legacy.resolve("level/map.json"), "{}")

        val project = EditorProjectStore(root).list().single()

        assertEquals("test", project.id)
        assertEquals(root.resolve("test"), project.root)
        assertFalse(Files.exists(legacy))
        assertEquals("test", mapper.readTree(project.root.resolve("project.json").toFile()).path("id").asText())
    }
}
