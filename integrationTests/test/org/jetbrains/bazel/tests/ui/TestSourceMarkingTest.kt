package org.jetbrains.bazel.tests.ui

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter.Companion.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertSyncSucceeded
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

internal class TestSourceMarkingTest : IdeStarterBaseProjectTest() {
  @Test
  fun test() {
    val context = createContext("testSourcesMarking", IdeaBazelCases.TestSourcesMarking)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)
          assertSyncSucceeded()

          step("Check sources marked as tests") {
            // a library used only in a test target in the same package
            shouldBeMarkedAsTestSource("java/case1/TestLibrary.java")
            // all sources in this folder are tests, so the folder shoul be also marked
            shouldBeMarkedAsTestSource("java/case1")


            // a library used only in a test target in the same package
            shouldBeMarkedAsTestSource("java/case2/TestLibrary.java")
            // belongs directly to a test target
            shouldBeMarkedAsTestSource("java/case2/JavaTest.java")
            // a library used in both a test and binary target
            shouldNotBeMarkedAsTestSource("java/case2/NormalLibrary1.java")
            // a library used in a test target that has its own sources
            shouldNotBeMarkedAsTestSource("java/case2/NormalLibrary2.java")
            // both test and not test - do not mark
            shouldNotBeMarkedAsTestSource("java/case2/MixedSource.java")
            shouldNotBeMarkedAsTestSource("java/case2")

            // a library marked as test-only
            shouldBeMarkedAsTestSource("java/case3/TestOnlyLibrary.java")
            // a library used in a test target in another package
            shouldNotBeMarkedAsTestSource("java/case3/TestLibrary.java")
            shouldNotBeMarkedAsTestSource("java/case3")
          }
        }
      }
    checkIdeaLogForExceptions(context)
  }
}

private fun Driver.shouldBeMarkedAsTestSource(relativePath: String) {
  execute { testSourceCheck(relativePath, true) }
}

private fun Driver.shouldNotBeMarkedAsTestSource(relativePath: String) {
  execute { testSourceCheck(relativePath, false) }
}

private fun <T : CommandChain> T.testSourceCheck(relativePath: String, shouldBeMarkedAsTest: Boolean): T {
  addCommand("${CMD_PREFIX}testSourceCheck $shouldBeMarkedAsTest $relativePath")
  return this
}
