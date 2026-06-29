package qorrnsmj.smf.game.progress

import qorrnsmj.smf.math.Vector3f

internal object GameProgressJson {
    fun encode(progress: GameProgress): String {
        return buildString {
            append("{\n")
            append("  \"currentStageName\": \"").append(escape(progress.currentStageName)).append("\",\n")
            append("  \"player\": {\n")
            append("    \"position\": ").append(encodeVector(progress.playerPosition)).append(",\n")
            append("    \"facing\": ").append(encodeVector(progress.playerFacing)).append("\n")
            append("  },\n")
            append("  \"flags\": ").append(encodeStringArray(progress.flags.sorted())).append(",\n")
            append("  \"inventory\": ").append(encodeStringArray(progress.inventory)).append(",\n")
            append("  \"defeatedMobIds\": ").append(encodeStringArray(progress.defeatedMobIds.sorted())).append("\n")
            append("}\n")
        }
    }

    fun decode(text: String): GameProgress {
        val root = JsonParser(text).parse() as? Map<*, *>
            ?: error("Save data root must be a JSON object")
        val player = root["player"] as? Map<*, *>
            ?: error("Save data must contain player object")

        return GameProgress(
            currentStageName = root.string("currentStageName"),
            playerPosition = player.vector("position"),
            playerFacing = player.vector("facing"),
            flags = root.stringList("flags").toSet(),
            inventory = root.stringList("inventory"),
            defeatedMobIds = root.stringList("defeatedMobIds").toSet(),
        )
    }

    private fun encodeVector(vector: Vector3f): String {
        return "{\"x\": ${vector.x}, \"y\": ${vector.y}, \"z\": ${vector.z}}"
    }

    private fun encodeStringArray(values: Iterable<String>): String {
        return values.joinToString(prefix = "[", postfix = "]") { "\"${escape(it)}\"" }
    }

    private fun escape(value: String): String {
        return buildString {
            for (char in value) {
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (char.code < 0x20) {
                            append("\\u").append(char.code.toString(16).padStart(4, '0'))
                        } else {
                            append(char)
                        }
                    }
                }
            }
        }
    }

    private fun Map<*, *>.string(key: String): String {
        return this[key] as? String ?: error("Save data must contain string '$key'")
    }

    private fun Map<*, *>.stringList(key: String): List<String> {
        val values = this[key] as? List<*> ?: error("Save data must contain array '$key'")
        return values.mapIndexed { index, value ->
            value as? String ?: error("Save data '$key[$index]' must be a string")
        }
    }

    private fun Map<*, *>.vector(key: String): Vector3f {
        val values = this[key] as? Map<*, *> ?: error("Save data must contain vector '$key'")
        return Vector3f(values.float("x"), values.float("y"), values.float("z"))
    }

    private fun Map<*, *>.float(key: String): Float {
        val value = this[key] as? Number ?: error("Save data vector must contain number '$key'")
        return value.toFloat()
    }

    private class JsonParser(private val text: String) {
        private var index = 0

        fun parse(): Any? {
            skipWhitespace()
            val value = parseValue()
            skipWhitespace()
            if (index != text.length) error("Unexpected JSON content at $index")
            return value
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            return when (peek()) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> {
                    expect("true")
                    true
                }
                'f' -> {
                    expect("false")
                    false
                }
                'n' -> {
                    expect("null")
                    null
                }
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            consume('{')
            val result = linkedMapOf<String, Any?>()
            skipWhitespace()
            if (tryConsume('}')) return result

            while (true) {
                val key = parseString()
                skipWhitespace()
                consume(':')
                result[key] = parseValue()
                skipWhitespace()
                if (tryConsume('}')) break
                consume(',')
            }

            return result
        }

        private fun parseArray(): List<Any?> {
            consume('[')
            val result = mutableListOf<Any?>()
            skipWhitespace()
            if (tryConsume(']')) return result

            while (true) {
                result.add(parseValue())
                skipWhitespace()
                if (tryConsume(']')) break
                consume(',')
            }

            return result
        }

        private fun parseString(): String {
            consume('"')
            val builder = StringBuilder()
            while (index < text.length) {
                val char = text[index++]
                when (char) {
                    '"' -> return builder.toString()
                    '\\' -> builder.append(parseEscape())
                    else -> builder.append(char)
                }
            }
            error("Unterminated JSON string")
        }

        private fun parseEscape(): Char {
            val escaped = text[index++]
            return when (escaped) {
                '"', '\\', '/' -> escaped
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> {
                    val value = text.substring(index, index + 4).toInt(16)
                    index += 4
                    value.toChar()
                }
                else -> error("Unsupported JSON escape: \\$escaped")
            }
        }

        private fun parseNumber(): Double {
            val start = index
            if (peek() == '-') index++
            while (peekOrNull()?.isDigit() == true) index++
            if (peekOrNull() == '.') {
                index++
                while (peekOrNull()?.isDigit() == true) index++
            }
            if (peekOrNull() == 'e' || peekOrNull() == 'E') {
                index++
                if (peekOrNull() == '+' || peekOrNull() == '-') index++
                while (peekOrNull()?.isDigit() == true) index++
            }
            if (start == index) error("Expected JSON number at $index")
            return text.substring(start, index).toDouble()
        }

        private fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) {
                index++
            }
        }

        private fun expect(expected: String) {
            if (!text.startsWith(expected, index)) {
                error("Expected '$expected' at $index")
            }
            index += expected.length
        }

        private fun consume(expected: Char) {
            skipWhitespace()
            if (peek() != expected) {
                error("Expected '$expected' at $index")
            }
            index++
        }

        private fun tryConsume(expected: Char): Boolean {
            skipWhitespace()
            if (peekOrNull() != expected) return false
            index++
            return true
        }

        private fun peek(): Char = peekOrNull() ?: error("Unexpected end of JSON")

        private fun peekOrNull(): Char? = text.getOrNull(index)
    }
}
