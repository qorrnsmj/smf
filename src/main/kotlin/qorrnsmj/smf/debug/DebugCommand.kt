package qorrnsmj.smf.debug

data class DebugCommand(val name: String, val values: List<Float> = emptyList(), val label: String = "game") {
    companion object {
        fun parse(text: String): DebugCommand {
            val parts = text.trim().split(Regex("\\s+"))
            val name = parts.first()
            val arguments = parts.drop(1)
            if (name == "screenshot") {
                require(arguments.size <= 1) { "Usage: screenshot [label]" }
                val label = arguments.firstOrNull() ?: "game"
                require(label.matches(Regex("[A-Za-z0-9_-]{1,64}"))) { "Invalid screenshot label" }
                return DebugCommand(name, label = label)
            }
            val count = when (name) {
                "status", "pause", "resume" -> 0
                "teleport", "move" -> 3
                "look" -> 2
                "step" -> 1
                else -> error("Unknown command: $name")
            }
            require(arguments.size == count) { "$name requires $count numeric arguments" }
            val values = arguments.map {
                val value = it.toFloatOrNull()
                require(value != null && value.isFinite() && kotlin.math.abs(value) <= 100000f) { "Invalid numeric argument: $it" }
                value
            }
            if (name == "look") require(values[1] in -89f..89f) { "Pitch must be between -89 and 89 degrees" }
            if (name == "step") require(values[0] in 1f..600f && values[0] == values[0].toInt().toFloat()) {
                "Step count must be an integer from 1 to 600"
            }
            return DebugCommand(name, values)
        }
    }
}

internal object DebugJson {
    fun encode(value: Any?): String = when (value) {
        null -> "null"
        is Boolean, is Number -> value.toString()
        is Map<*, *> -> value.entries.joinToString(",", "{", "}") { encode(it.key.toString()) + ":" + encode(it.value) }
        is Iterable<*> -> value.joinToString(",", "[", "]") { encode(it) }
        else -> buildString {
            append('"')
            value.toString().forEach {
                when (it) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (it < ' ') append("\\u%04x".format(it.code)) else append(it)
                }
            }
            append('"')
        }
    }
}
