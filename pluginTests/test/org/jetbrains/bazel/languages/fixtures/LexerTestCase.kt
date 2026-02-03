package org.jetbrains.bazel.languages.fixtures

import com.intellij.lexer.Lexer
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.junit5.TestApplication
import io.kotest.assertions.withClue
import io.kotest.matchers.string.shouldHaveLength
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.jupiter.api.Assertions.fail

@TestApplication
abstract class LexerTestCase {

  open fun doLexerTest(
    code: String,
    lexer: Lexer,
    expectedTokens: List<String>,
  ) {
    lexer.start(code)

    val lexemes: MutableList<Triple<IElementType?, Int, Int>> = ArrayList(expectedTokens.size)

    while (lexer.tokenType != null) {
      lexemes.add(Triple(lexer.tokenType, lexer.tokenStart, lexer.tokenEnd))
      lexer.advance()
    }

    var idx = 0
    var tokenPos = 0
    withClue("\nLexed tokens: ${lexemes.map { Pair(it.first, code.substring(it.second, it.third)) }}") {
      withClue("Expected tokens: $expectedTokens") {
        withClue("lexed tokens assert: listOf(\n${lexemes.joinToString(",\n") { """"${it.first}"""" }}\n)") {
          while (idx < lexemes.size) {
            if (idx >= expectedTokens.size) {
              val remainingTokens = StringBuilder()
              while (idx < lexemes.size) {
                remainingTokens.append("\"${lexemes[idx].first}\", ")
                idx++
              }
              withClue("Too many tokens. Following tokens: $remainingTokens") {
                remainingTokens shouldHaveLength 0
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

          if (idx < expectedTokens.size) fail<Unit>("Not enough tokens")
        }
      }
    }
  }
}
