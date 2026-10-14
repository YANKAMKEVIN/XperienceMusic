package com.xperiencemusic.shared.data.remote

import com.xperiencemusic.shared.domain.repository.SongPageSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/** Reads the song page the way a browser gets it, one page per import. */
class AccordsAppPageSource(private val http: HttpClient) : SongPageSource {
    override suspend fun chordPro(pageUrl: String): String? {
        val response = http.get(pageUrl)
        if (!response.status.isSuccess()) return null
        return extractNuxtChordPro(response.bodyAsText())
    }
}

/**
 * The page is rendered by Nuxt, which embeds its data as
 * `window.__NUXT__=(function(a,b,…){return {…chordpro:d…}}("…",false,…))`: the field is either a
 * string literal or one of the function's parameters, filled by the matching argument.
 */
internal fun extractNuxtChordPro(html: String): String? {
    val start = html.indexOf("__NUXT__=(function(")
    if (start < 0) return null
    val paramsStart = start + "__NUXT__=(function(".length
    val paramsEnd = html.indexOf(')', paramsStart).takeIf { it > 0 } ?: return null
    val params = html.substring(paramsStart, paramsEnd).split(',').map { it.trim() }

    val bodyStart = html.indexOf('{', paramsEnd).takeIf { it > 0 } ?: return null
    val bodyEnd = JsScanner(html, bodyStart).skipBlock() ?: return null
    val body = html.substring(bodyStart, bodyEnd)

    val field = body.indexOf("chordpro:").takeIf { it >= 0 } ?: return null
    val scanner = JsScanner(body, field + "chordpro:".length)
    if (scanner.peek() == '"') return scanner.readString()
    val name = scanner.readIdentifier()
    val index = params.indexOf(name).takeIf { it >= 0 } ?: return null

    if (html.getOrNull(bodyEnd) != '(') return null
    val arguments = JsScanner(html, bodyEnd + 1)
    repeat(index) { if (!arguments.skipValue()) return null }
    return if (arguments.peek() == '"') arguments.readString() else null
}

/** Just enough of a JavaScript reader to walk Nuxt's payload: strings, blocks and plain values. */
private class JsScanner(private val text: String, private var position: Int) {
    fun peek(): Char? = text.getOrNull(position)

    fun readIdentifier(): String {
        val from = position
        while (peek()?.let { it.isLetterOrDigit() || it == '_' || it == '$' } == true) position++
        return text.substring(from, position)
    }

    /** From an opening brace or bracket, the index just after its match. */
    fun skipBlock(): Int? {
        var depth = 0
        while (position < text.length) {
            when (text[position]) {
                '"', '\'' -> {
                    readString() ?: return null
                    continue
                }
                '{', '[', '(' -> depth++
                '}', ']', ')' -> {
                    depth--
                    if (depth == 0) return position + 1
                }
            }
            position++
        }
        return null
    }

    /** Skips one argument and the comma after it; false at the end of the list. */
    fun skipValue(): Boolean {
        when (peek()) {
            '"', '\'' -> readString() ?: return false
            '{', '[' -> position = skipBlock() ?: return false
            else -> while (peek()?.let { it != ',' && it != ')' } == true) position++
        }
        if (peek() != ',') return false
        position++
        return true
    }

    fun readString(): String? {
        val quote = text[position++]
        val out = StringBuilder()
        while (position < text.length) {
            val c = text[position++]
            when {
                c == quote -> return out.toString()
                c != '\\' -> out.append(c)
                else -> {
                    val escaped = text.getOrNull(position++) ?: return null
                    when (escaped) {
                        'n' -> out.append('\n')
                        't' -> out.append('\t')
                        'r' -> out.append('\r')
                        'b' -> out.append('\b')
                        'u' -> {
                            val hex = text.substring(position, minOf(position + 4, text.length))
                            out.append(hex.toIntOrNull(16)?.toChar() ?: return null)
                            position += 4
                        }
                        else -> out.append(escaped)
                    }
                }
            }
        }
        return null
    }
}
