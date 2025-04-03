package org.jetbrains.bazel.ui.testResultsTree

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.tree
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

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
  fun openProject() {
    val fileName = "SimpleKotlinTest.kt"
    val commands =
      CommandChain()
        .takeScreenshot("startSync")
        .waitForBazelSync()
        .waitForSmartMode()
        .openFile(fileName)
        .runSimpleKotlinTest()
    createContext().runIdeWithDriver(runTimeout = timeout, commands = commands).useDriverAndCloseIde {
      ideFrame {
        waitForIndicators(5.minutes)
        verifyTestStatus(
          fileName,
          listOf("2 tests passed", "2 tests total"),
          listOf("SimpleKotlinTest", "trivial test()", "another trivial test()"),
        )
        takeScreenshot("afterOpeningTestResultsTree")
      }
    }
  }

  private fun IdeaFrameUI.verifyTestStatus(
    fileName: String,
    expectedStatus: List<String>,
    expectedTree: List<String>,
  ) {
    var result = true
    step("Verify test status for '$fileName'") {
      waitContainsText("Test Results", timeout = 1.minutes)
      val actualResults = x("//div[@class='TestStatusLine']").getAllTexts()
      for (expectedItem in expectedStatus) {
        result = result && actualResults.any { it.text.contains(expectedItem) }
      }
      Assertions.assertTrue(
        result,
        "Actual status doesn't contain expected: $expectedStatus",
      )
    }
    step("Verify test results tree for '$fileName'") {
      x("//div[@accessiblename='Show Passed']").click()
      val treeResults = tree(xQuery { byClass("SMTRunnerTestTreeView") }).getAllTexts()
      for (expectedItem in expectedTree) {
        result = result && treeResults.any { it.text.contains(expectedItem) }
      }
      Assertions.assertTrue(
        result,
        "Actual tree doesn't contain expected: $expectedTree",
      )
    }
  }

  private fun <T : CommandChain> T.runSimpleKotlinTest(): T {
    addCommand(CMD_PREFIX + "runSimpleKotlinTest")
    return this
  }
}
