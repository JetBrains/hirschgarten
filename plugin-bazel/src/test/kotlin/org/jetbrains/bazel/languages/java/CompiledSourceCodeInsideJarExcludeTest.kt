package org.jetbrains.bazel.languages.java

import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.delayType
import com.intellij.tools.ide.performanceTesting.commands.exitApp
import com.intellij.tools.ide.performanceTesting.commands.goToDeclaration
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Test

class CompiledSourceCodeInsideJarExcludeTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "60e9e037ca25be0734ea6760614defe228728dcb",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("generatedCodeTest") },
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
        .openFile("Main.kt")
        .checkOnRedCode()
        .openFile("my_addition.kt")
        .goto(1, 36)
        // Change the signature of a top-level Kotlin function
        .delayType(delayMs = 150, text = ", c: Int")
        .openFile("Main.kt")
        // Navigate to that top-level function
        .goto(5, 5)
        .goToDeclaration()
        .checkOpenedFileNotInsideJar()
        .exitApp()
    createContext().runIDE(commands = commands, runTimeout = timeout)
  }
}

fun <T : CommandChain> T.checkOpenedFileNotInsideJar(): T {
  addCommand(CMD_PREFIX + "checkOpenedFileNotInsideJar")
  return this
}
