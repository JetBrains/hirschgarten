package org.jetbrains.bsp.bazel.languages.starlark

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.util.containers.IntStack

// TODO: how can i test it?
object StarlarkParserUtils : GeneratedParserUtilBase() {

    private var indents: ThreadLocal<IntStack> = ThreadLocal.withInitial {
        val stack = IntStack()
        stack.push(0)
        stack
    }

    @JvmStatic
    fun noNewLineOrLineBreak(builder: PsiBuilder, level: Int): Boolean {
        val previousWhitespace = getPreviousRawTokenText(builder)

        return !previousWhitespace.contains("\n") || previousWhitespace.contains("\\\n")
    }

    @JvmStatic
    fun newLine(builder: PsiBuilder, level: Int): Boolean =
        getPreviousRawTokenText(builder).contains("\n")

    @JvmStatic
    fun checkIfIndentIncreasesAndSaveIt(builder: PsiBuilder, level: Int): Boolean {
        val stack = indents.get()
        val previousIndent = stack.peek()
        val currentIndent = inferIndent(builder)

        return if (currentIndent > previousIndent) {
            stack.push(currentIndent)
            true
        } else {
            false
        }
    }

    @JvmStatic
    fun checkIndent(builder: PsiBuilder, level: Int): Boolean {
        val currentIndent = inferIndent(builder)

        return currentIndent == indents.get().peek()
    }

    @JvmStatic
    fun finishBlock(builder: PsiBuilder, level: Int): Boolean {
        val stack = indents.get()

        val previousIndent = stack.peek()
        val currentIndent = inferIndent(builder)

        return if (previousIndent > currentIndent) {
            stack.pop()
            true
        } else {
            false
        }
    }

    private fun inferIndent(builder: PsiBuilder): Int {
        val previousWhitespace = getPreviousRawTokenText(builder)
        val lastNewLineIndex = previousWhitespace.lastIndexOf('\n')

        return if (lastNewLineIndex >= 0) previousWhitespace.length - lastNewLineIndex - 1 else -1
    }

    private fun getPreviousRawTokenText(builder: PsiBuilder): CharSequence {
        val currentTokenStartIndex = builder.rawTokenTypeStart(0)
        val previousTokenStartIndex = builder.rawTokenTypeStart(-1)

        return builder.originalText.subSequence(previousTokenStartIndex, currentTokenStartIndex)
    }
}
