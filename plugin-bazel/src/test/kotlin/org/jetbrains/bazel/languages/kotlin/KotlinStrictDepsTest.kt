package org.jetbrains.bazel.languages.kotlin

import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.assertCurrentFile
import com.intellij.tools.ide.performanceTesting.commands.delay
import com.intellij.tools.ide.performanceTesting.commands.exitApp
import com.intellij.tools.ide.performanceTesting.commands.goToDeclaration
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Test

// https://youtrack.jetbrains.com/issue/BAZEL-1423/Green-code-in-IDEA-red-in-build
class KotlinStrictDepsTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "24a6c74e96dc557a3cbeec072c870eb4ff4684b7",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("kotlinStrictDepsTest") },
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
        .openFile("Main.kt")
        // The first import should not be resolved because it's a non-exported transitive dependency
        .navigateToFile(2, 46, expectedFilename = "Main.kt")
        // The second import should be resolved properly
        .navigateToFile(5, 64, expectedFilename = "WorkerProtocol.java")
        .exitApp()
    createContext().runIDE(commands = commands, runTimeout = timeout)
  }

  private fun CommandChain.navigateToFile(
    caretLine: Int,
    caretColumn: Int,
    expectedFilename: String,
  ): CommandChain =
    this
      .goto(caretLine, caretColumn)
      .delay(500)
      .takeScreenshot("Before navigating to $expectedFilename")
      .goToDeclaration()
      .delay(500)
      .assertCurrentFile(expectedFilename)
}
