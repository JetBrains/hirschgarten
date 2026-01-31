package org.jetbrains.bazel.tests.run

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.delay
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/run/coverage:BazelCoverageTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class BazelCoverageTest : IdeStarterBaseProjectTest() {

  @Test
  fun `run test with coverage and verify results`() {
    createContext("bazelCoverage", IdeaBazelCases.BazelCoverage)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          step("Run test with coverage") {
            execute { openFile("src/test/com/example/CalculatorTest.java") }
            execute { runTestWithCoverage() }
            takeScreenshot("afterRunTestWithCoverage")
          }

          step("Verify coverage results") {
            execute { openFile("src/main/com/example/Calculator.java") }
            execute { assertCoverage("50% lines covered") }
            execute { delay(1000) }
            takeScreenshot("afterAssertCoverage")
          }
        }
      }
  }
}

fun <T : CommandChain> T.runTestWithCoverage(): T {
  addCommand(CMD_PREFIX + "runTestWithCoverage")
  return this
}

fun <T : CommandChain> T.assertCoverage(coverageInformationString: String): T {
  addCommand(CMD_PREFIX + "assertCoverage $coverageInformationString")
  return this
}
