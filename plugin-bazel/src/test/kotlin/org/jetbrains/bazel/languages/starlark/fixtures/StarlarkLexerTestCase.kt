package org.jetbrains.bazel.languages.starlark.fixtures

import com.intellij.lexer.Lexer
import com.intellij.testFramework.PlatformLiteFixture
import io.kotest.assertions.withClue
import io.kotest.matchers.equals.shouldBeEqual
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
abstract class StarlarkLexerTestCase : PlatformLiteFixture() {
  override fun setUp() {
    super.setUp()
    initApplication()
  }

  fun doLexerTest(code: String, lexer: Lexer, expectedTokens: List<String>) {
    lexer.start(code)
    var idx = 0
    var tokenPos = 0
    while (lexer.tokenType != null) {
      if (idx >= expectedTokens.size) {
        val remainingTokens = StringBuilder()
        while (lexer.tokenType != null) {
          remainingTokens.append("\"${lexer.tokenType}\", ")
          lexer.advance()
        }
        fail("Too many tokens. Following tokens: $remainingTokens")
      }

      withClue("Token offset mismatch at position $idx") {
        tokenPos shouldBeEqual lexer.tokenStart
      }
      withClue("Token mismatch at position $idx") {
        expectedTokens[idx] shouldBeEqual lexer.tokenType.toString()
      }

      idx++
      tokenPos = lexer.tokenEnd
      lexer.advance()
    }
    if (idx < expectedTokens.size) fail("Not enough tokens")
  }
}
