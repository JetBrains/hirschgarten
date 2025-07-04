package org.jetbrains.bazel.run.coverage

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.delay
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/run/coverage:BazelCoverageTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class BazelCoverageTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "2603adc47076c07404310ab113023e01495e04ee",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("coverageTest") },
        isReusable = false,
        configureProjectBeforeUse = ::configureProjectBeforeUse,
      )

  @Test
  fun openBazelProject() {
    val commands =
      CommandChain()
        .takeScreenshot("startSync")
        .waitForBazelSync()
        .buildAndSync()
        .waitForSmartMode()

    createContext()
      .runIdeWithDriver(commands = commands, runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          waitForIndicators(5.minutes)

          step("Run test with coverage") {
            execute { openFile("src/com/example/CalculatorTest.java") }
            execute { runTestWithCoverage() }
            takeScreenshot("afterRunTestWithCoverage")
          }

          step("Verify coverage results") {
            execute { openFile("src/com/example/Calculator.java") }
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
