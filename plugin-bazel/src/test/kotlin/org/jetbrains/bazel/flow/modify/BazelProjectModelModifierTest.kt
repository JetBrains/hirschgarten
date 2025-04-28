package org.jetbrains.bazel.flow.modify

import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.delay
import com.intellij.tools.ide.performanceTesting.commands.exitApp
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertFileContentsEqual
import org.jetbrains.bazel.ideStarter.navigateToFile
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Test

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/flow/modify:BazelProjectModelModifierTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class BazelProjectModelModifierTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "dfdeecb49806f57818c74bf13b3b671349568865",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("bazelProjectModelModifierTest") },
        isReusable = false,
        configureProjectBeforeUse = ::configureProjectBeforeUse,
      )

  @Test
  fun openBazelProject() {
    val commands =
      CommandChain()
        .takeScreenshot("startSync")
        .waitForBazelSync()
        .waitForSmartMode()
        // Add module dependency
        .openFile("UsesDependency1.java")
        .goto(2, 10)
        .applyOrderEntryFixAndCheckRedCode(hint = "Add dependency on module")
        // Add module dependency
        .openFile("UsesDependency2.java")
        .goto(2, 10)
        .applyOrderEntryFixAndCheckRedCode(hint = "Add dependency on module")
        // Add library dependency
        .openFile("UsesDependency3.java")
        .goto(5, 14)
        .applyOrderEntryFixAndCheckRedCode(hint = "Add library 'junit' to classpath")
        // Check that the dependencies were added properly
        .openFile("BUILD")
        .assertFileContentsEqual("BUILD.expected", "BUILD")
        // Check that the added dependency to JUnit is navigatable
        .navigateToFile(33, 17, "BUILD", 934, 9)
        .exitApp()
    createContext().runIDE(commands = commands, runTimeout = timeout)
  }
}

private fun <T : CommandChain> T.applyOrderEntryFixAndCheckRedCode(hint: String) =
  this
    .applyOrderEntryQuickFix(hint)
    .waitForSmartMode()
    .delay(3000)
    .checkOnRedCode()

private fun <T : CommandChain> T.applyOrderEntryQuickFix(hint: String): T {
  addCommand(CMD_PREFIX + "applyOrderEntryQuickFix $hint")
  return this
}
