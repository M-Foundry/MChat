package com.mroldl001.mimochat.ui.chat.components

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.textmate.grammar.Grammar
import dev.textmate.grammar.raw.GrammarReader
import dev.textmate.grammar.raw.RawGrammar
import dev.textmate.grammar.tokenize.StateStack
import dev.textmate.regex.JoniOnigLib
import dev.textmate.registry.GrammarSource
import dev.textmate.registry.Registry
import dev.textmate.theme.ResolvedStyle
import dev.textmate.theme.Theme
import dev.textmate.theme.ThemeReader
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val GRAMMAR_ASSET_DIR = "grammars"
private const val MAX_CACHED_GRAMMARS = 24
private const val MAX_CACHED_RAW = 48
private const val MAX_CACHED_THEMES = 4

internal data class CodeToken(val start: Int, val end: Int, val scopes: List<String>)

private object TextMateEngine {
    private val lock = ReentrantLock()
    private val onigLib = JoniOnigLib()
    private val rawCache = LinkedHashMap<String, RawGrammar?>(16, 0.75f, true)
    private val grammars = LinkedHashMap<String, Grammar?>(16, 0.75f, true)

    fun grammar(context: Context, language: String): Grammar? = lock.withLock {
        grammars[language]?.let { return@withLock it }
        val raw = rawGrammarLocked(context.applicationContext, language)
        val scope = raw?.scopeName ?: "source.$language"
        val loaded = if (raw == null) {
            null
        } else {
            try {
                Registry(singleGrammarSource(raw), onigLib).loadGrammar(scope)
            } catch (error: Throwable) {
                null
            }
        }
        grammars[language] = loaded
        while (grammars.size > MAX_CACHED_GRAMMARS) {
            val oldest = grammars.keys.firstOrNull() ?: break
            grammars.remove(oldest)
        }
        loaded
    }

    fun tokenize(grammar: Grammar, lines: List<String>): List<List<CodeToken>> = lock.withLock {
        val perLine = ArrayList<List<CodeToken>>(lines.size)
        var state: StateStack? = null
        lines.forEach { line ->
            val result = grammar.tokenizeLine(line, state)
            state = result.ruleStack
            perLine += result.tokens.map { token ->
                CodeToken(token.startIndex, token.endIndex, token.scopes)
            }
        }
        perLine
    }

    private fun singleGrammarSource(raw: RawGrammar) = object : GrammarSource {
        override fun loadRawGrammar(scopeName: String): RawGrammar? =
            if (scopeName == raw.scopeName) raw else null
    }

    private fun rawGrammarLocked(context: Context, language: String): RawGrammar? {
        rawCache[language]?.let { return it }
        val parsed = try {
            context.assets.open("$GRAMMAR_ASSET_DIR/$language.tmLanguage.json").use { stream ->
                GrammarReader.readGrammar(stream)
            }
        } catch (error: Throwable) {
            null
        }
        rawCache[language] = parsed
        while (rawCache.size > MAX_CACHED_RAW) {
            val oldest = rawCache.keys.firstOrNull() ?: break
            rawCache.remove(oldest)
        }
        return parsed
    }
}

private object ThemeCache {
    private val lock = ReentrantLock()
    private val cache = LinkedHashMap<String, Theme>(4, 0.75f, true)

    fun get(darkMode: Boolean, palette: CodeBlockPalette): Theme = lock.withLock {
        val key = (if (darkMode) "d" else "l") + palette.hashCode()
        cache[key]?.let { return@withLock it }
        val built = try {
            buildTheme(if (darkMode) "MIMO_DARK" else "MIMO_LIGHT", palette)
        } catch (error: Throwable) {
            null
        } ?: return@withLock fallbackTheme(palette)
        cache[key] = built
        while (cache.size > MAX_CACHED_THEMES) {
            val oldest = cache.keys.firstOrNull() ?: break
            cache.remove(oldest)
        }
        built
    }

    private fun fallbackTheme(palette: CodeBlockPalette): Theme {
        val fg = String.format("%06X", 0xFFFFFF and palette.text.toArgb())
        val bg = String.format("%06X", 0xFFFFFF and palette.background.toArgb())
        val json = """{"name":"MIMO_FALLBACK","type":"dark",""" +
            """"colors":{"editor.foreground":"#$fg","editor.background":"#$bg"},""" +
            """"tokenColors":[]}"""
        return ThemeReader.readTheme(
            ByteArrayInputStream(json.toByteArray(StandardCharsets.UTF_8))
        )
    }

    private fun buildTheme(key: String, palette: CodeBlockPalette): Theme {
        val rules = ArrayList<String>()
        fun rule(scope: String, color: Color, italic: Boolean) {
            val hex = String.format("%06X", 0xFFFFFF and color.toArgb())
            val style = if (italic) {
                ""","settings":{"foreground":"#$hex","fontStyle":"italic"}"""
            } else {
                ""","settings":{"foreground":"#$hex"}"""
            }
            rules += """{"scope":"$scope"$style}"""
        }
        rule("comment", palette.comment, italic = true)
        rule("string", palette.string, italic = false)
        rule("constant.numeric", palette.number, italic = false)
        rule("constant.language", palette.keyword, italic = false)
        rule("keyword", palette.keyword, italic = false)
        rule("keyword.control", palette.keyword, italic = false)
        rule("keyword.operator", palette.operator, italic = false)
        rule("storage", palette.type, italic = false)
        rule("storage.type", palette.type, italic = false)
        rule("storage.modifier", palette.keyword, italic = false)
        rule("entity.name.function", palette.function, italic = false)
        rule("entity.name.type", palette.type, italic = false)
        rule("entity.name.class", palette.type, italic = false)
        rule("entity.name.tag", palette.function, italic = false)
        rule("entity.other.attribute-name", palette.annotation, italic = false)
        rule("support.function", palette.function, italic = false)
        rule("support.type", palette.type, italic = false)
        rule("support.class", palette.type, italic = false)
        rule("variable", palette.text, italic = false)
        rule("variable.language", palette.keyword, italic = false)
        rule("meta.annotation", palette.annotation, italic = false)
        rule("punctuation", palette.operator, italic = false)
        rule("invalid", palette.annotation, italic = false)
        val fg = String.format("%06X", 0xFFFFFF and palette.text.toArgb())
        val bg = String.format("%06X", 0xFFFFFF and palette.background.toArgb())
        val json = """{"name":"$key","type":"dark",""" +
            """"colors":{"editor.foreground":"#$fg","editor.background":"#$bg"},""" +
            """"tokenColors":[${rules.joinToString(",")}]}"""
        val bytes = ByteArrayInputStream(json.toByteArray(StandardCharsets.UTF_8))
        return ThemeReader.readTheme(bytes)
    }
}

internal fun codeGrammar(context: Context, language: String): Grammar? =
    TextMateEngine.grammar(context, language)

internal fun codeTokens(grammar: Grammar, lines: List<String>): List<List<CodeToken>> =
    TextMateEngine.tokenize(grammar, lines)

internal fun codeTheme(darkMode: Boolean, palette: CodeBlockPalette): Theme =
    ThemeCache.get(darkMode, palette)

internal fun hasGrammar(context: Context, language: String): Boolean {
    if (language == "text") return false
    return try {
        context.assets.open("$GRAMMAR_ASSET_DIR/$language.tmLanguage.json").close()
        true
    } catch (error: Throwable) {
        false
    }
}

internal fun resolveStyle(theme: Theme, scopes: List<String>): ResolvedStyle? = theme.match(scopes)

internal fun textMateForegroundColor(foreground: Long): Int =
    (0xFF000000L or (foreground and 0xFFFFFFL)).toInt()