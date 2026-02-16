package org.jetbrains.bazel.redcodes

import com.intellij.codeInspection.i18n.InvalidPropertyKeyInspection
import com.intellij.mock.MockDocument
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class SharedSourcesTest  {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun `test green`() = runBlocking(Dispatchers.Default) {
    fixture.enableInspections(InvalidPropertyKeyInspection())
    fixture.copyBazelTestProject("redcodes/shared_sources_green")
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("ClassA.java")
      fixture.checkHighlighting("ClassB.java")

      fixture.checkHighlighting("TestUtil.java", "a")
      fixture.checkHighlighting("TestUtil.java", "b")
      fixture.checkHighlighting("TestUtil.java", "util")
    }
  }

  @Test
  fun `test red`() = runBlocking(Dispatchers.Default) {
    fixture.enableInspections(InvalidPropertyKeyInspection())
    fixture.copyBazelTestProject("redcodes/shared_sources_red")
    fixture.performBazelSync()
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
