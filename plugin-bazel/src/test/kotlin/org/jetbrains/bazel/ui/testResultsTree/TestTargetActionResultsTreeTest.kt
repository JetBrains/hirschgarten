package org.jetbrains.bazel.ui.testResultsTree

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testResultsTree --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class TestTargetActionResultsTreeTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "c88d3cb84fae0c8a42cd8ddd78306a40595ff764",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("simpleKotlinTest") },
        isReusable = false,
        configureProjectBeforeUse = ::configureProjectBeforeUse,
      )

  @Test
  fun testTestResultsTree() {
    val fileName = "SimpleKotlinTest.kt"

    createContext()
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          step("Sync & set up project") {
            syncBazelProject()
            waitForIndicators(5.minutes)
            takeScreenshot("afterSync")
          }

          step("open SimpleKotlinTest.kt and run test") {
            execute { openFile(fileName) }
            execute { runSimpleKotlinTest() }
            takeScreenshot("afterRuningSimpleKotlinTest")
          }
          step("Verify test status and results tree") {
            verifyTestStatus(
              listOf("2 tests passed", "2 tests total"),
              listOf("SimpleKotlinTest", "trivial test()", "another trivial test()"),
            )
            takeScreenshot("afterOpeningTestResultsTree")
          }

          step("Launch debug run config for SimpleKotlinTest") {
            // revert the click from the previous `verifyTestStatus` step
            x("//div[@accessiblename='Show Passed']").click()

            editorTabs()
              .gutter()
              .getGutterIcons()
              .first()
              .click()
            popup().waitOneContainsText("Debug test").click()
            wait(15.seconds)
          }
          step("Verify debug test status and results tree") {
            // should be the same as for the above test results tree
            verifyTestStatus(
              listOf("2 tests passed", "2 tests total"),
              listOf("SimpleKotlinTest", "trivial test()", "another trivial test()"),
            )
            takeScreenshot("afterOpeningDebugTestResultsTree")
          }
        }
      }
  }

  private fun <T : CommandChain> T.runSimpleKotlinTest(): T {
    addCommand(CMD_PREFIX + "runSimpleKotlinTest")
    return this
  }
}
