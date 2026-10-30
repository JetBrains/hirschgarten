package org.jetbrains.bazel.redcodes

import com.intellij.mock.MockDocument
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class DropNoIdeTest {

  // private val project by bazelProjectFixture("redcodes/noidedeps", projectView = ".bazelproject-drop")
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun `red code disappears after no longer dropping no-ide`() : Unit =
    timeoutRunBlocking(timeout = 10.minutes, context = Dispatchers.Default) {
      fixture.copyBazelTestProject("redcodes/noidedeps")
      fixture.setProjectView(".bazelproject-drop")
      fixture.performBazelSync()

      withContext(Dispatchers.EDT) {
        // expect missing symbol Heavy
        fixture.checkHighlighting("Main.java",
        expected = ExpectedHighlightingData(
          MockDocument().apply {
            replaceText(
              """
              package com.example.project;
              
              import com.example.project.<error descr="Cannot resolve symbol 'Heavy'">Heavy</error>;
              
              public class Main {
                  public static void main(String[] args) {
                      System.out.println(<error descr="Cannot resolve symbol 'Heavy'">Heavy</error>.notWorthInspecting("Hello World"));
                  }
              }
              """.trimIndent(),
              1,
            )
          },
        ).also { it.init() },
        )
      }

      // set `drop_no_ide` to false
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()
      withContext(Dispatchers.EDT) {
        // expect everything resolves
        fixture.checkHighlighting("Main.java")
      }
    }
}
