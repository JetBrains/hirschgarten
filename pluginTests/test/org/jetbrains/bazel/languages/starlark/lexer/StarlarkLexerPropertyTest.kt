package org.jetbrains.bazel.languages.starlark.lexer

import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.propertyBased.MadTestingUtil
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.lexer.generated._StarlarkLexer
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.IntDistribution
import org.jetbrains.jetCheck.PropertyChecker
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkLexerPropertyTest {
  @Test
  fun `tokens stay consistent after random edits`() {
    val root = BazelPathManager.testDataRoot.resolve("starlark")
    val files = MadTestingUtil.randomFiles(root.toString()) { it.isDirectory || it.name.endsWith(".bzl") }
    PropertyChecker
      .customized()
      .withIterationCount(500)
      .checkScenarios {
        ImperativeCommand { env ->
          val file = env.generateValue(files, null)
          env.logMessage("Edit ${root.relativize(file.toPath())}")
          val text = StringBuilder(StringUtil.convertLineSeparators(file.readText()))
          checkTokens(text)
          env.executeCommands(IntDistribution.uniform(1, 10), Generator.sampledFrom(InsertText(text), DeleteText(text)))
        }
      }
  }

  private class InsertText(private val text: StringBuilder) : ImperativeCommand {
    override fun performCommand(env: ImperativeCommand.Environment) {
      val offset = env.generateValue(Generator.integers(0, text.length).noShrink(), null)
      val inserted =
        env.generateValue(
          Generator.frequency(4, Generator.sampledFrom(FRAGMENTS), 1, Generator.stringsOf(Generator.asciiPrintableChars())),
          null,
        )
      env.logMessage("Insert '${StringUtil.escapeStringCharacters(inserted)}' at $offset")
      text.insert(offset, inserted)
      checkTokens(text)
    }
  }

  private class DeleteText(private val text: StringBuilder) : ImperativeCommand {
    override fun performCommand(env: ImperativeCommand.Environment) {
      val start = env.generateValue(Generator.integers(0, text.length).noShrink(), null)
      val end = minOf(text.length, start + env.generateValue(Generator.integers(IntDistribution.geometric(10)).noShrink(), null))
      env.logMessage("Delete $start-$end '${StringUtil.escapeStringCharacters(text.substring(start, end))}'")
      text.delete(start, end)
      checkTokens(text)
    }
  }
}

private val FRAGMENTS = listOf(
  "(", ")", "[", "]", "{", "}", ":", ",", ";", "=", "#", "\"", "'''", "\"\"\"", "\\", "\\\n",
  " ", "  ", "    ", "\t", "\n", "\n\n", "\n  ", "\n    ", "\n\t",
  "\n# c", "\n  # c", "\n    # c",
  "if a:", "elif b:", "else:", "while x:", "for x in y:", "def f(", "lambda x:", "return", "pass",
)

private val SYNTHETIC_TOKENS = TokenSet.create(
  StarlarkTokenTypes.INDENT,
  StarlarkTokenTypes.DEDENT,
  StarlarkTokenTypes.INCONSISTENT_DEDENT,
  StarlarkTokenTypes.STATEMENT_BREAK,
)

private val LINE_END_WHITESPACE = TokenSet.orSet(StarlarkTokenSets.SPACES_AND_TABS, TokenSet.create(StarlarkTokenTypes.LINE_BREAK))

private data class Token(val type: IElementType, val start: Int, val end: Int)

private fun checkTokens(text: CharSequence) {
  fun ensure(value: Boolean, message: () -> String) =
    check(value) { "${message()} in '${StringUtil.escapeStringCharacters(text.toString())}'" }

  val lexer = StarlarkIndentingLexer()
  val tokens = tokens(lexer, text)
  ensure(tokens(lexer, text) == tokens) { "The tokens differ when the lexer starts again" }
  val half = text.length / 2
  ensure(tokens(StarlarkIndentingLexer(), text, half) == tokens(StarlarkIndentingLexer(), text.subSequence(0, half))) {
    "The tokens up to the end offset $half differ from the tokens of the cut text"
  }

  val realTokens = tokens.filter { it.type !in SYNTHETIC_TOKENS }
  fun realTokenBefore(token: Token) = realTokens.lastOrNull { it.end <= token.start && it.end > it.start }
  fun realTokenAfter(token: Token) = realTokens.firstOrNull { it.start >= token.end && it.end > it.start }
  fun isLineEndOrTextEnd(token: Token?) =
    token == null || token.type === StarlarkTokenTypes.LINE_BREAK ||
    (token.type in StarlarkTokenSets.WHITESPACE && token.end == text.length)

  var offset = 0
  var openBlocks = 0
  var codeSinceStatementBreak = false
  for (token in tokens) {
    ensure(token.start == offset) { "$token does not start at the end of the previous token, $offset" }
    ensure(token.end >= token.start) { "$token ends before its start" }
    ensure((token.end == token.start) == (token.type in SYNTHETIC_TOKENS)) { "$token has a wrong length" }
    if (token.type in StarlarkTokenSets.WHITESPACE) {
      ensure(text.subSequence(token.start, token.end).all { it in " \t\n\\" }) { "$token holds text that is not whitespace" }
    }
    when (token.type) {
      StarlarkTokenTypes.LINE_BREAK -> ensure('\n' in text.subSequence(token.start, token.end)) { "$token holds no line break" }
      in StarlarkTokenSets.SPACES_AND_TABS -> ensure('\n' !in text.subSequence(token.start, token.end)) { "$token holds a line break" }
      StarlarkTokenTypes.STATEMENT_BREAK -> {
        ensure(codeSinceStatementBreak) { "$token ends a statement without code" }
        ensure(realTokenBefore(token)?.type !in LINE_END_WHITESPACE) { "$token comes after whitespace of the line end" }
        ensure(isLineEndOrTextEnd(realTokenAfter(token))) { "$token is not at a line end" }
        codeSinceStatementBreak = false
      }
      StarlarkTokenTypes.INDENT -> {
        openBlocks++
        val before = realTokenBefore(token)
        val isAfterLineStart = before == null || before.type === StarlarkTokenTypes.LINE_BREAK ||
                               (before.start == 0 && before.type in StarlarkTokenSets.WHITESPACE)
        ensure(isAfterLineStart) { "$token does not come after a line break or the text start" }
        val after = realTokenAfter(token)?.type
        ensure(after != null && after !in StarlarkTokenSets.WHITESPACE) { "$token is not before code or a comment" }
      }
      StarlarkTokenTypes.DEDENT, StarlarkTokenTypes.INCONSISTENT_DEDENT -> {
        if (token.type === StarlarkTokenTypes.DEDENT) ensure(--openBlocks >= 0) { "$token closes a block that is not open" }
        ensure(isLineEndOrTextEnd(realTokenAfter(token))) { "$token is not at a line end" }
      }
      !in StarlarkTokenSets.WHITESPACE_AND_COMMENTS -> codeSinceStatementBreak = true
    }
    offset = token.end
  }
  ensure(offset == text.length) { "The tokens end at $offset, not at the text end ${text.length}" }
  ensure(openBlocks == 0) { "$openBlocks blocks are open at the text end" }
  ensure(!codeSinceStatementBreak) { "The last statement has no statement break" }

  val baseTokens = tokens(FlexAdapter(_StarlarkLexer(null)), text).filter { it.type !in StarlarkTokenSets.WHITESPACE }
  val codeTokens = realTokens.filter { it.type !in StarlarkTokenSets.WHITESPACE }
  ensure(codeTokens == baseTokens) {
    val index = codeTokens.indices.firstOrNull { it >= baseTokens.size || codeTokens[it] != baseTokens[it] } ?: codeTokens.size
    "The code tokens differ from the tokens of the JFlex lexer at $index: ${codeTokens.getOrNull(index)} and ${baseTokens.getOrNull(index)}"
  }
}

private fun tokens(lexer: Lexer, text: CharSequence, end: Int = text.length): List<Token> {
  lexer.start(text, 0, end, 0)
  val result = ArrayList<Token>()
  val limit = 4 * end + 16
  while (true) {
    val type = lexer.tokenType ?: return result
    check(result.size < limit) { "The lexer gives more than $limit tokens, so it does not advance" }
    result.add(Token(type, lexer.tokenStart, lexer.tokenEnd))
    lexer.advance()
  }
}
