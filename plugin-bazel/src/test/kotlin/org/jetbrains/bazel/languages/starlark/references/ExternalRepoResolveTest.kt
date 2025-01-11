package org.jetbrains.bazel.languages.starlark.references

import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.assertCaretPosition
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

class ExternalRepoResolveTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "c88d3cb84fae0c8a42cd8ddd78306a40595ff764",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("starlarkResolveTest") },
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
        .openFile("src/BUILD")
        // load(":junit_test.bzl", "kt_<caret>test") -> def <caret>kt_test
        .navigateToFile(1, 29, expectedFilename = "junit_test.bzl", 3, 5)
        // load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm<caret>_test") -> <caret>kt_jvm_test
        .navigateToFile(1, 46, expectedFilename = "jvm.bzl", 17, 1)
        // "//kotlin/internal:opts.<caret>bzl" -> <caret>kt_javac_options
        .navigateToFile(2, 29, expectedFilename = "opts.bzl", 1, 1)
        .openFile("src/BUILD")
        // srcs = ["nested<caret>_src/Hello.java"],
        .navigateToFile(12, 20, expectedFilename = "Hello.java", 1, 1)
        .exitApp()
    createContext().runIDE(commands = commands, runTimeout = timeout)
  }

  private fun CommandChain.navigateToFile(
    caretLine: Int,
    caretColumn: Int,
    expectedFilename: String,
    expectedCaretLine: Int,
    expectedCaretColumn: Int,
  ): CommandChain =
    this
      .goto(caretLine, caretColumn)
      .delay(500)
      .takeScreenshot("Before navigating to $expectedFilename")
      .goToDeclaration()
      .delay(500)
      .assertCurrentFile(expectedFilename)
      .assertCaretPosition(expectedCaretLine, expectedCaretColumn)
}
