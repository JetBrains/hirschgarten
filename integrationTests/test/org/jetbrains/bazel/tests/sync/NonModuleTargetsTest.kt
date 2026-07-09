package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugins/bazel/integrationTests:integrationTests_test --test_env=JB_TEST_FILTER=org.jetbrains.bazel.tests.sync.NonModuleTargetsTest --test_output=errors --nocache_test_results
 * ```
 */
class NonModuleTargetsTest : IdeStarterBaseProjectTest() {

  @Test
  fun `non-module targets should be synced and visible in project`() {
    createContext("nonModuleTargets", IdeaBazelCases.NonModuleTargets)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          step("Check non-module targets") {
            execute { checkNonModuleTargets() }
            takeScreenshot("afterCheckNonModuleTargets")
          }
        }
      }
  }

  private fun <T : CommandChain> T.checkNonModuleTargets(): T {
    addCommand("${CMD_PREFIX}checkNonModuleTargets")
    return this
  }
}
