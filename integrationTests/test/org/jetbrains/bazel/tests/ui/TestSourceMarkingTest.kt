package org.jetbrains.bazel.tests.ui

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter.Companion.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.ideStarter.waitForSyncSucceeded
import org.jetbrains.bazel.data.simpleBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

private val TEST_SOURCE_MARKING_PROJECT = simpleBazelProject(
  revision = "ce48e24a5cbb8315737a8b42b274ff4a99ad7a3e",
  path = "testSourcesMarking",
)

internal class TestSourceMarkingTest : IdeStarterBaseProjectTest() {
  @Test
  fun test() {
    val context = createContext("testSourcesMarking", IdeaBazelCases.withProject(TEST_SOURCE_MARKING_PROJECT))
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)
          waitForSyncSucceeded()

          step("Check sources and source roots marked as tests") {
            // testRunner: a single java_test owns src/, so the whole source root is test
            shouldBeMarkedAsTestSource("java/testRunner/src/JavaTest.java")
            shouldBeMarkedAsTestSource("java/testRunner/src/TestHelper.java")
            shouldBeMarkedAsTestSource("java/testRunner/src")

            // testLibrary: a single testonly library owns src/, so the whole source root is test
            shouldBeMarkedAsTestSource("java/testLibrary/src/TestFixtures.java")
            shouldBeMarkedAsTestSource("java/testLibrary/src/TestHelpers.java")
            shouldBeMarkedAsTestSource("java/testLibrary/src")

            // projectViewTestSources: a plain library marked as test only by the project view
            // test_sources glob (see the workspace-root .bazelproject)
            shouldBeMarkedAsTestSource("java/projectViewTestSources/src/FirstSource.java")
            shouldBeMarkedAsTestSource("java/projectViewTestSources/src/SecondSource.java")
            shouldBeMarkedAsTestSource("java/projectViewTestSources/src")

            // mixedSources: several targets share src/ and mix test and production sources,
            // so the src/ source root is not test
            shouldBeMarkedAsTestSource("java/mixedSources/src/JavaTest.java")
            shouldNotBeMarkedAsTestSource("java/mixedSources/src/SharedSource.java")
            shouldNotBeMarkedAsTestSource("java/mixedSources/src/ProductionLibrary.java")
            shouldNotBeMarkedAsTestSource("java/mixedSources/src/Main.java")
            shouldNotBeMarkedAsTestSource("java/mixedSources/src")
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
