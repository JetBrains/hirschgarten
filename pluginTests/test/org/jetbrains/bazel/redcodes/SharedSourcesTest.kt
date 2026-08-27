package org.jetbrains.bazel.redcodes

import com.intellij.codeInspection.i18n.InvalidPropertyKeyInspection
import com.intellij.mock.MockDocument
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.ExpectedHighlightingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SharedSourcesTest {

  @Nested
  @BazelTestApplication
  inner class Green {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/shared_sources_green",
      configure = { it.enableInspections(InvalidPropertyKeyInspection()) },
    )

    @Test
    fun `test green`() = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("ClassA.java")
        fixture.checkHighlighting("ClassB.java")

        fixture.checkHighlighting("TestUtil.java", "a")
        fixture.checkHighlighting("TestUtil.java", "b")
        fixture.checkHighlighting("TestUtil.java", "util")
      }
    }
  }

  @Nested
  @BazelTestApplication
  inner class Red {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/shared_sources_red",
      configure = { it.enableInspections(InvalidPropertyKeyInspection()) },
    )

    @Test
    fun `test red`() = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("Main.java", "a")
        fixture.checkHighlighting("Main.java", "b",
          expected = ExpectedHighlightingData(MockDocument().apply {
            replaceText("""
              package org.example;

              public class Main {
                  public static Object x = new <error descr="Cannot resolve symbol 'ClassA'">ClassA</error>();
              }
            """.trimIndent(), 1)
          }).also { it.init() }
        )
      }
    }
  }
}
