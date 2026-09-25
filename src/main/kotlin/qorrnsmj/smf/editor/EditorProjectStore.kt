package qorrnsmj.smf.editor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class EditorProject(val id: String, val name: String, val root: Path) {
    val level: Path get() = root.resolve("level/map.json")
}

internal class EditorProjectStore(val root: Path = appRoot.resolve("projects")) {
    private val mapper = ObjectMapper()

    fun list(): List<EditorProject> {
        if (!Files.isDirectory(root)) return emptyList()
        migrateLegacyProjectDirectories()
        return Files.list(root).use { paths ->
            paths.filter { Files.isRegularFile(it.resolve("project.json")) }.map { directory ->
                val name = runCatching { mapper.readTree(directory.resolve("project.json").toFile())["name"].asText() }
                    .getOrDefault("Unreadable project: ${directory.fileName}")
                EditorProject(directory.fileName.toString(), name, directory)
            }.toList().sortedBy { it.name.lowercase() }
        }
    }

    fun create(name: String): EditorProject = build(name) { project ->
        val maps = project.root.resolve("texture/terrain/map")
        Files.createDirectories(maps)
        val height = java.awt.image.BufferedImage(513, 513, java.awt.image.BufferedImage.TYPE_USHORT_GRAY)
        height.raster.setSamples(0, 0, 513, 513, 0, IntArray(513 * 513) { 32768 })
        javax.imageio.ImageIO.write(height, "png", maps.resolve("map_height.png").toFile())
        val splat = java.awt.image.BufferedImage(513, 513, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        javax.imageio.ImageIO.write(splat, "png", maps.resolve("map_splat0.png").toFile())
        writeJson(project.level, mapper.readTree("""{"terrain":{"map_size":256,"heightmap":"texture/terrain/map/map_height.png","splatmaps":["texture/terrain/map/map_splat0.png"]},"static_objects":[]}"""))
    }

    fun rename(project: EditorProject, name: String): EditorProject {
        require(name.isNotBlank()) { "Project name is empty" }
        val renamed = project.copy(name = name.trim())
        writeProjectMetadata(renamed)
        return renamed
    }

    fun duplicate(project: EditorProject): EditorProject = build("${project.name} Copy") { target ->
        copyTree(project.root, target.root)
    }

    fun importLevel(level: Path, name: String = level.fileName.toString().substringBeforeLast('.')): EditorProject = build(name) { target ->
        val source = level.toAbsolutePath().normalize()
        require(Files.isRegularFile(source)) { "Level not found: $source" }
        val workspace = if (source.parent.fileName.toString().equals("level", true)) source.parent.parent else source.parent
        val json = mapper.readTree(source.toFile())
        bundle(json, workspace, target.root)
        writeJson(target.level, json)
    }

    fun export(project: EditorProject, destination: Path) {
        val output = destination.toAbsolutePath().normalize()
        require(!output.startsWith(project.root.toAbsolutePath().normalize())) { "Choose an export location outside the project" }
        val staging = Files.createTempDirectory("smf-export-")
        try {
            val json = mapper.readTree(project.level.toFile())
            bundle(json, project.root, staging)
            writeJson(staging.resolve("level/map.json"), json)
            writeProjectMetadata(project.copy(root = staging))
            Files.createDirectories(output.parent)
            val temporary = Files.createTempFile(output.parent, "smf-export-", ".zip")
            try {
                ZipOutputStream(Files.newOutputStream(temporary)).use { zip ->
                    Files.walk(staging).use { paths -> paths.filter { Files.isRegularFile(it) }.sorted().forEach { file ->
                        zip.putNextEntry(ZipEntry(staging.relativize(file).toString().replace('\\', '/')))
                        Files.copy(file, zip)
                        zip.closeEntry()
                    } }
                }
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING)
            } finally { Files.deleteIfExists(temporary) }
        } finally { removeTree(staging) }
    }

    fun importZip(archive: Path): EditorProject {
        val staging = Files.createTempDirectory("smf-import-")
        try {
            var bytes = 0L
            var count = 0
            ZipInputStream(Files.newInputStream(archive)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(++count <= 10000) { "Archive contains too many files" }
                    val name = entry.name.replace('\\', '/')
                    require(!name.contains(':')) { "Invalid archive path: $name" }
                    val file = staging.resolve(name).normalize()
                    require(file.startsWith(staging) && file != staging) { "Invalid archive path: $name" }
                    if (entry.isDirectory) Files.createDirectories(file) else {
                        Files.createDirectories(file.parent)
                        Files.newOutputStream(file, java.nio.file.StandardOpenOption.CREATE_NEW).use { output ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val read = zip.read(buffer)
                                if (read < 0) break
                                bytes += read
                                require(bytes <= 2L * 1024 * 1024 * 1024) { "Archive exceeds 2 GiB" }
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
            val metadata = staging.resolve("project.json")
            val metadataJson = if (Files.isRegularFile(metadata)) mapper.readTree(metadata.toFile()) else null
            val name = metadataJson?.path("name")?.asText("Imported Map") ?: "Imported Map"
            val requestedId = metadataJson?.path("id")?.asText(name) ?: name
            val level = staging.resolve("level/map.json")
            require(Files.isRegularFile(level)) { "Archive is missing level/map.json" }
            return build(name, requestedId) { project ->
                val json = mapper.readTree(level.toFile())
                bundle(json, staging, project.root, allowExternal = false)
                writeJson(project.level, json)
            }
        } finally { removeTree(staging) }
    }

    fun delete(project: EditorProject) {
        val expected = root.toAbsolutePath().normalize().resolve(project.id).normalize()
        require(expected.parent == root.toAbsolutePath().normalize() && expected == project.root.toAbsolutePath().normalize()) { "Invalid project directory" }
        removeTree(expected)
    }

    // Copy only referenced runtime assets; editor state never enters an exported package.
    private fun bundle(json: JsonNode, source: Path, target: Path, allowExternal: Boolean = true) {
        require(json.isObject) { "Level JSON must be an object" }
        val copied = mutableMapOf<String, String>()
        fun asset(value: String, model: Boolean = false, targetName: String? = null): String {
            val portable = value.replace('\\', '/').removePrefix("assets/")
            val raw = Path.of(portable)
            require(allowExternal || (!raw.isAbsolute && !raw.normalize().startsWith("..") && !portable.contains(':'))) {
                "Archive asset must be inside the project: $value"
            }
            val relative = if (model && !raw.isAbsolute && !portable.startsWith("model/")) "model/$portable" else portable
            return copied.getOrPut(relative) {
                val path = Path.of(relative)
                val destination = targetName ?: if (path.isAbsolute || path.normalize().startsWith("..")) {
                    "${if (model) "model" else "texture"}/imported/${UUID.randomUUID()}-${path.fileName}"
                } else path.normalize().toString().replace('\\', '/')
                val local = if (path.isAbsolute) path else source.resolve(path).normalize()
                val output = target.resolve(destination).normalize()
                require(output.startsWith(target.normalize())) { "Invalid asset path: $value" }
                Files.createDirectories(output.parent)
                if (Files.isRegularFile(local)) Files.copy(local, output, StandardCopyOption.REPLACE_EXISTING)
                else {
                    val stream = javaClass.classLoader.getResourceAsStream("assets/$relative")
                        ?: error("Missing asset: $value (searched $local)")
                    stream.use { Files.copy(it, output, StandardCopyOption.REPLACE_EXISTING) }
                }
                destination
            }
        }
        val terrain = json.path("terrain") as? ObjectNode
        terrain?.get("heightmap")?.takeIf { it.isTextual && it.asText().isNotBlank() }?.let { terrain.put("heightmap", asset(it.asText())) }
        terrain?.get("splatmaps")?.takeIf { it.isArray }?.let { maps ->
            val result = mapper.createArrayNode()
            maps.forEach { result.add(asset(it.asText())) }
            terrain.set<JsonNode>("splatmaps", result)
        }
        for (key in listOf("static_objects", "objects")) json.path(key).forEach { item ->
            if (item is ObjectNode) item.get("model")?.let { item.put("model", asset(it.asText(), true)) }
        }
        val environment = json.path("environment") as? ObjectNode
        environment?.get("skybox")?.asText()?.takeIf { it.isNotBlank() }?.let { prefix ->
            val suffixes = listOf("_right", "_left", "_top", "_bottom", "_back", "_front")
            val portable = prefix.replace('\\', '/').removePrefix("assets/")
            val destination = if (Path.of(portable).isAbsolute || Path.of(portable).normalize().startsWith(".."))
                "texture/skybox/${UUID.randomUUID()}/sky" else portable
            val imported = suffixes.map { asset("$prefix$it.png", targetName = "$destination$it.png") }
            environment.put("skybox", imported.first().removeSuffix("_right.png"))
        }
    }

    private fun build(name: String, requestedId: String = name, initialize: (EditorProject) -> Unit): EditorProject {
        require(name.isNotBlank()) { "Project name is empty" }
        Files.createDirectories(root)
        val id = uniqueProjectId(toProjectId(requestedId))
        val project = EditorProject(id, name.trim(), root.resolve(id))
        try {
            Files.createDirectories(project.root)
            initialize(project)
            return rename(project, name)
        } catch (error: Exception) {
            removeTree(project.root)
            throw error
        }
    }

    private fun writeProjectMetadata(project: EditorProject) {
        writeJson(project.root.resolve("project.json"), mapOf(
            "version" to 1,
            "id" to project.id,
            "name" to project.name,
            "level" to "level/map.json",
        ))
    }

    private fun migrateLegacyProjectDirectories() {
        val directories = Files.list(root).use { paths -> paths.filter { Files.isDirectory(it) }.toList() }
        for (directory in directories) {
            val oldId = directory.fileName.toString()
            if (!UUID_PATTERN.matches(oldId)) continue
            val metadata = directory.resolve("project.json")
            if (!Files.isRegularFile(metadata)) continue
            runCatching {
                val name = mapper.readTree(metadata.toFile()).path("name").asText(oldId)
                val id = uniqueProjectId(toProjectId(name))
                val target = root.resolve(id)
                Files.move(directory, target)
                writeProjectMetadata(EditorProject(id, name, target))
            }
        }
    }

    private fun uniqueProjectId(requested: String): String {
        if (!Files.exists(root.resolve(requested))) return requested
        var number = 2
        while (Files.exists(root.resolve("$requested-$number"))) number++
        return "$requested-$number"
    }

    private fun toProjectId(value: String): String {
        val id = value.trim()
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('_')
            .take(64)
        require(id.isNotBlank()) { "Project ID must contain a letter, number, '_' or '-'" }
        return id
    }

    private fun copyTree(source: Path, destination: Path) {
        Files.walk(source).use { paths -> paths.forEach { file ->
            require(!Files.isSymbolicLink(file)) { "Symbolic links are not supported: $file" }
            val target = destination.resolve(source.relativize(file))
            if (Files.isDirectory(file)) Files.createDirectories(target)
            else Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
        } }
    }

    companion object {
        private val UUID_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")
        val appRoot: Path get() = Path.of(System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
            ?: Path.of(System.getProperty("user.home"), "AppData", "Roaming").toString(), "SMF")

        fun writeJson(path: Path, value: Any) {
            Files.createDirectories(path.parent)
            val temporary = Files.createTempFile(path.parent, ".smf-", ".tmp")
            try {
                ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), value)
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            } finally { Files.deleteIfExists(temporary) }
        }

        private fun removeTree(root: Path) {
            if (Files.exists(root)) Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) } }
        }
    }
}
