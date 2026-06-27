package qorrnsmj.smf.game.level

internal object GlbJson {
    fun parse(text: String): Any? {
        return Parser(text).parse()
    }

    private class Parser(private val text: String) {
        private var index = 0

        fun parse(): Any? {
            skipWhitespace()
            val value = parseValue()
            skipWhitespace()
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
