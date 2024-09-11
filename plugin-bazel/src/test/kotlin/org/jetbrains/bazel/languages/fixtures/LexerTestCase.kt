package org.jetbrains.bazel.languages.fixtures

import com.intellij.lexer.Lexer
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.PlatformLiteFixture
import io.kotest.assertions.withClue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.string.shouldMatch
import org.jetbrains.kotlin.backend.common.push
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
abstract class LexerTestCase : PlatformLiteFixture() {
  override fun setUp() {
    super.setUp()
    initApplication()
  }

  open fun doLexerTest(
    code: String,
    lexer: Lexer,
    expectedTokens: List<String>,
  ) {
    lexer.start(code)

    val lexemes: MutableList<Triple<IElementType?, Int, Int>> = ArrayList(expectedTokens.size)

    while (lexer.tokenType != null) {
      lexemes.push(Triple(lexer.tokenType, lexer.tokenStart, lexer.tokenEnd))
      lexer.advance()
    }

    var idx = 0
    var tokenPos = 0
    withClue("$lexemes vs $expectedTokens") {
      while (idx < lexemes.size) {
        if (idx >= expectedTokens.size) {
          val remainingTokens = StringBuilder()
          while (idx < lexemes.size) {
            remainingTokens.append("\"${lexemes[idx].first}\", ")
            idx++
          }
          withClue("Too many tokens. Following tokens: $remainingTokens") {
            remainingTokens shouldMatch ""
          }
        }

        withClue("Token offset mismatch at position $idx") {
          tokenPos shouldBeEqual lexemes[idx].second
        }
        withClue("Token mismatch at position $idx") {
          expectedTokens[idx] shouldBeEqual lexemes[idx].first.toString()
        }

        tokenPos = lexemes[idx].third
        idx++
      }

      if (idx < expectedTokens.size) fail("Not enough tokens")
    }
  }
}
