package org.jetbrains.bazel.languages.bazelversion.service

import io.kotest.matchers.equals.shouldBeEqual
import org.junit.jupiter.api.Test

class BazeliskrcParserTest {
  @Test
  fun `test simple case2`() {
    val properties =
      BazeliskrcParser.parse(
        """
        # comment
        KEY1=VALUE1
        KEY2= VALUE2 
        """.trimIndent(),
      )

    properties shouldBeEqual
      mapOf(
        "KEY1" to "VALUE1",
        "KEY2" to "VALUE2",
      )
  }

  @Test
  fun `test comment after`() {
    val properties =
      BazeliskrcParser.parse(
        """
        # comment
        KEY1=VALUE1
        KEY2= VALUE2 # THIS IS NOT COMMENT
        """.trimIndent(),
      )

    properties shouldBeEqual
      mapOf(
        "KEY1" to "VALUE1",
        "KEY2" to "VALUE2 # THIS IS NOT COMMENT",
      )
  }
}
