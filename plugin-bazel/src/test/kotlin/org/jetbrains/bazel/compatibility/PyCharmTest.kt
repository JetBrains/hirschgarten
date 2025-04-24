package org.jetbrains.bazel.compatibility

import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.exitApp
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.junit.jupiter.api.Test

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:PyCharmTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class PyCharmTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/abrams27/simpleBazelProjectsForTesting.git",
        commitHash = "febe0d80ab883e49e14bad76aaa9e8e857ae14e1",
        branchName = "abrams/python-to-multi-lang",
        projectHomeRelativePath = { it.resolve("simpleMultiLanguageTest") },
        isReusable = false,
        configureProjectBeforeUse = ::configureProjectBeforeUse,
      )

  override val ideInfo = IdeProductProvider.PY

  @Test
  fun openBazelProject() {
    val commands =
      CommandChain()
        .takeScreenshot("startSync")
        // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1910
        // .waitForBazelSync()
        .waitForSmartMode()
        .checkImportedModules()
        .exitApp()

    createContext().runIDE(commands = commands, runTimeout = timeout)
  }

  private fun <T : CommandChain> T.checkImportedModules(): T = also { addCommand("${CMD_PREFIX}PyCharmCheckImportedModules") }
}
