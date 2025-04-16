package org.jetbrains.bazel.compatibility

import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.exitApp
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Test

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:DisabledKotlinPluginTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class DisabledKotlinPluginTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "18020888ba1564927d7cfe672f8a3f7ca24c1e23",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("simpleMultiLanguageTest") },
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
        .checkImportedModules()
        .checkTargetsInTargetWidget()
        .exitApp()

    createContext()
      .withDisabledPlugins(setOf("org.jetbrains.kotlin"))
      .runIDE(commands = commands, runTimeout = timeout)
  }

  private fun <T : CommandChain> T.checkImportedModules(): T = also { addCommand("${CMD_PREFIX}checkImportedModules") }

  private fun <T : CommandChain> T.checkTargetsInTargetWidget(): T = also { addCommand("${CMD_PREFIX}checkTargetsInTargetWidget") }

  private fun IDETestContext.withDisabledPlugins(pluginIds: Set<String>): IDETestContext =
    also { pluginConfigurator.disablePlugins(pluginIds) }
}
