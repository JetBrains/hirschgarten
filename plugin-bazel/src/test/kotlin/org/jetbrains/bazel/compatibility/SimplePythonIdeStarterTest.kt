package org.jetbrains.bazel.compatibility

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:SimplePythonTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class SimplePythonIdeStarterTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "1fe1e07dcf5d50868e10f3e6e87f2c4e95b4c290",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("simplePythonTest") },
        isReusable = true,
        configureProjectBeforeUse = ::configureProjectBeforeUseWithoutBazelClean,
      )

  @Test
  fun checkImportStatements() {
    createContext()
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(10.minutes)

          step("check no red node in main/main.py") {
            execute { openFile("main/main.py") }
            execute { checkOnRedCode() }
            takeScreenshot("main.py")
          }
          step("check no red node in lib/libA/src/bkng/lib/aaa/hello.py") {
            execute { openFile("lib/libA/src/bkng/lib/aaa/hello.py") }
            execute { checkOnRedCode() }
            takeScreenshot("main.py")
          }
        }
      }
  }
}
