package org.jetbrains.bazel.compatibility

import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
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
        repositoryUrl = "https://github.com/abrams27/simpleBazelProjectsForTesting.git",
        commitHash = "ed13bd5586f6b3a484a13c7ee0ffd959e0092ff0",
        branchName = "abrams/multi-lang-project",
        projectHomeRelativePath = { it.resolve("simpleMultiLanguageTest") },
        isReusable = false,
        configureProjectBeforeUse = ::configureProjectBeforeUse,
      )

  @Test
  fun openBazelProject() {
    val commands =
      CommandChain()
        .takeScreenshot("startSync")
//        .waitForBazelSync()
        .waitForSmartMode()
//        .exitApp()
    createContext()
      .withDisabledPlugins(setOf("org.jetbrains.kotlin"))
      .runIDE(commands = commands, runTimeout = timeout)
  }

  private fun IDETestContext.withDisabledPlugins(pluginIds: Set<String>): IDETestContext {
    pluginConfigurator.disablePlugins(pluginIds)
    return this
  }
}
