package org.jetbrains.bsp.bazel.languages.starlark

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

object StarlarkSyntaxHighlighter : SyntaxHighlighterBase() {

    private val keyword = createTextAttributesKey("STARLARK_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    private val string = createTextAttributesKey("STARLARK_STRING", DefaultLanguageHighlighterColors.STRING)
    private val number = createTextAttributesKey("STARLARK_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    private val lineComment = createTextAttributesKey("STARLARK_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    private val semicolon = createTextAttributesKey("STARLARK_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
    private val comma = createTextAttributesKey("STARLARK_COMMA", DefaultLanguageHighlighterColors.COMMA)
    private val dot = createTextAttributesKey("STARLARK_DOT", DefaultLanguageHighlighterColors.DOT)
    private val parentheses =
        createTextAttributesKey("STARLARK_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
    private val brackets = createTextAttributesKey("STARLARK_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    private val identifier = createTextAttributesKey("STARLARK_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)

    private val emptyKeys = emptyArray<TextAttributesKey>()

    override fun getHighlightingLexer(): Lexer =
        StarlarkLexerAdapter

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        when (tokenType) {
            StarlarkTypes.DEF -> arrayOf(keyword)
            StarlarkTypes.LAMBDA -> arrayOf(keyword)

            StarlarkTypes.IF -> arrayOf(keyword)
            StarlarkTypes.ELIF -> arrayOf(keyword)
            StarlarkTypes.ELSE -> arrayOf(keyword)

            StarlarkTypes.FOR -> arrayOf(keyword)
            StarlarkTypes.IN -> arrayOf(keyword)

            StarlarkTypes.RETURN -> arrayOf(keyword)
            StarlarkTypes.BREAK -> arrayOf(keyword)
            StarlarkTypes.CONTINUE -> arrayOf(keyword)
            StarlarkTypes.PASS -> arrayOf(keyword)

            StarlarkTypes.LOAD -> arrayOf(keyword)

            // boolean operators
            StarlarkTypes.OR -> arrayOf(keyword)
            StarlarkTypes.AND -> arrayOf(keyword)
            StarlarkTypes.NOT -> arrayOf(keyword)

            // code organization
            StarlarkTypes.SEMICOLON -> arrayOf(semicolon)
            StarlarkTypes.COMMA -> arrayOf(comma)
            StarlarkTypes.DOT -> arrayOf(dot)

            StarlarkTypes.LEFT_PAREN -> arrayOf(parentheses)
            StarlarkTypes.RIGHT_PAREN -> arrayOf(parentheses)

            StarlarkTypes.LEFT_CURLY -> arrayOf(brackets)
            StarlarkTypes.RIGHT_CURLY -> arrayOf(brackets)

            // random
            StarlarkTypes.INT -> arrayOf(number)
            StarlarkTypes.FLOAT -> arrayOf(number)

            StarlarkTypes.IDENTIFIER -> arrayOf(identifier)

            StarlarkTypes.STRING -> arrayOf(string)
            StarlarkTypes.BYTES -> arrayOf(string)

            StarlarkTypes.COMMENT -> arrayOf(lineComment)

            else -> emptyKeys
        }
}

class StarlarkSyntaxHighlighterFactory : SyntaxHighlighterFactory() {

    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        StarlarkSyntaxHighlighter
}
