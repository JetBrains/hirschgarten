package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertSyncedTargets
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.data.simpleBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

private val BROKEN_DEPS_PROJECT = simpleBazelProject(
  revision = "41a8dc6b668681d114d6760e7755de0daa25ab12",
  path = "broken",
)

class BrokenDepsTest : IdeStarterBaseProjectTest() {

  @Test
  fun `Sync succeeds in the presence of broken dependencies`() {
    createContext("brokeDeps", IdeaBazelCases.withProject(BROKEN_DEPS_PROJECT))
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          step("Initial sync") {
            syncBazelProject()
            waitForIndicators(5.minutes)
            takeScreenshot("afterSync")
            execute { assertSyncedTargets("//:hello") }
          }
        }
      }
  }
}
