package qorrnsmj.smf.debug

import com.sun.net.httpserver.HttpServer
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.level.custom.Level
import qorrnsmj.smf.graphic.capture.ScreenshotCapture
import qorrnsmj.smf.math.Vector3f
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GameDebugControl(
    private val level: () -> Level?,
    private val screenshots: ScreenshotCapture,
) : AutoCloseable {
    private data class Request(val command: DebugCommand, val response: CompletableFuture<String>)
    private val requests = ArrayBlockingQueue<Request>(16)
    private val token = UUID.randomUUID().toString()
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 8)
    private val executor = Executors.newFixedThreadPool(2) { Thread(it, "game-debug-http").apply { isDaemon = true } }
    private val sessionFile = Path.of(".smf-debug", "${ProcessHandle.current().pid()}.json").toAbsolutePath()
    private var stepsRemaining = 0
    private var stepping: Request? = null
    private var renderedFrames = 0L
    private var simulationTicks = 0L
    var paused = false
        private set

    init {
        server.executor = executor
        server.createContext("/command") { exchange ->
            var code = 200
            val response = try {
                require(exchange.requestMethod == "POST" && exchange.requestURI.path == "/command") { "POST /command required" }
                check(exchange.requestHeaders.getFirst("X-SMF-Token") == token) { "Invalid session token" }
                val bytes = exchange.requestBody.use { it.readNBytes(1025) }
                require(bytes.size <= 1024) { "Command is too long" }
                val request = Request(DebugCommand.parse(bytes.toString(Charsets.UTF_8)), CompletableFuture())
                check(requests.offer(request)) { "Debug command queue is full" }
                try {
                    request.response.get(30, TimeUnit.SECONDS)
                } catch (exception: Exception) {
                    request.response.cancel(false)
                    throw exception
                }
            } catch (exception: Exception) {
                code = 400
                DebugJson.encode(mapOf("ok" to false, "error" to (exception.message ?: exception.javaClass.simpleName)))
            }
            try {
                val bytes = response.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
                exchange.sendResponseHeaders(code, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } finally {
                exchange.close()
            }
        }
        try {
            server.start()
            Files.createDirectories(sessionFile.parent)
            Files.writeString(sessionFile, DebugJson.encode(mapOf(
                "pid" to ProcessHandle.current().pid(), "port" to server.address.port,
                "token" to token, "workspace" to Path.of("").toAbsolutePath().normalize().toString(),
            )))
            Logger.info("Local debug control ready: {}", sessionFile)
        } catch (exception: Exception) {
            server.stop(0)
            executor.shutdownNow()
            throw exception
        }
    }

    // All game mutations are executed by the game thread, never the HTTP threads.
    fun pump() {
        if (stepping != null) return
        repeat(16) {
            val request = requests.poll() ?: return
            if (request.response.isDone) return@repeat
            try {
                val command = request.command
                when (command.name) {
                    "status" -> Unit
                    "pause" -> paused = true
                    "resume" -> paused = false
                    "step" -> {
                        check(paused) { "Pause the game before stepping" }
                        stepsRemaining = command.values[0].toInt()
                        stepping = request
                        return
                    }
                    "screenshot" -> {
                        screenshots.request(command.label).whenComplete { result, failure ->
                            if (failure != null) request.response.complete(DebugJson.encode(mapOf("ok" to false, "error" to failure.message)))
                            else request.response.complete(DebugJson.encode(mapOf("ok" to true,
                                "path" to result.path.toString(), "width" to result.width, "height" to result.height)))
                        }
                        return
                    }
                    "teleport", "move", "look" -> {
                        val current = level() ?: error("No active game level")
                        val values = command.values
                        if (command.name == "look") {
                            val yaw = Math.toRadians(values[0].toDouble())
                            val pitch = Math.toRadians(values[1].toDouble())
                            current.player.camera.setFront(Vector3f(
                                (kotlin.math.cos(yaw) * kotlin.math.cos(pitch)).toFloat(),
                                kotlin.math.sin(pitch).toFloat(),
                                (kotlin.math.sin(yaw) * kotlin.math.cos(pitch)).toFloat()))
                        } else {
                            val position = Vector3f(values[0], values[1], values[2])
                            current.player.setFeetPosition(if (command.name == "move") {
                                current.player.worldTransform.position.add(position)
                            } else position)
                        }
                    }
                }
                request.response.complete(DebugJson.encode(snapshot()))
            } catch (exception: Exception) {
                request.response.complete(DebugJson.encode(mapOf("ok" to false, "error" to exception.message)))
            }
        }
    }

    fun shouldUpdate(): Boolean = !paused || stepsRemaining > 0

    fun didUpdate() {
        simulationTicks++
        if (stepsRemaining > 0) stepsRemaining--
    }

    fun afterFrame() {
        renderedFrames++
        if (stepsRemaining == 0) {
            stepping?.response?.complete(DebugJson.encode(snapshot()))
            stepping = null
        }
    }

    fun snapshot(): Map<String, Any?> {
        val current = level()
        fun vector(value: Vector3f?) = value?.let { listOf(it.x, it.y, it.z) }
        return linkedMapOf("ok" to true, "pid" to ProcessHandle.current().pid(),
            "frame" to renderedFrames, "simulationTicks" to simulationTicks, "paused" to paused,
            "level" to current?.javaClass?.simpleName,
            "feet" to vector(current?.player?.worldTransform?.position),
            "camera" to vector(current?.scene?.world?.camera?.position),
            "front" to vector(current?.scene?.world?.camera?.getFront()),
            "lightDirection" to vector(current?.scene?.environment?.celestialLight?.direction))
    }

    override fun close() {
        stepping?.response?.completeExceptionally(IllegalStateException("Game closed"))
        while (true) {
            val request = requests.poll() ?: break
            request.response.completeExceptionally(IllegalStateException("Game closed"))
        }
        server.stop(0)
        executor.shutdownNow()
        Files.deleteIfExists(sessionFile)
    }
}
