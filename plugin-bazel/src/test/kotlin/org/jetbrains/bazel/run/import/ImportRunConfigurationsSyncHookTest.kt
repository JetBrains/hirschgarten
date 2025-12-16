package org.jetbrains.bazel.run.import

import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/run/import:ImportRunConfigurationsSyncHookTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class ImportRunConfigurationsSyncHookTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "682aed9b15c007bbb9bed2246f54ff4c44ec1bf7",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("importRunConfigurations") },
        isReusable = false,
        configureProjectBeforeUse = { context -> configureProjectBeforeUseWithoutBazelClean(context, createProjectView = false) },
      )

  @Test
  fun openBazelProject() {
    createContext().runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
      ideFrame {
        syncBazelProject()
        waitForIndicators(5.minutes)

        step("See which run configurations there are") { x { byVisibleText("Remote JVM") }.click() }
        Thread.sleep(1000)
        step("Select another imported run configuration") { keyboard { key(KeyEvent.VK_ENTER) } }
        step("Execute the run configuration") { x { byAccessibleName("Run 'Bazel run :main'") }.click() }
        val consoleView = x { byClass("ConsoleViewImpl") }
        step("Wait for run config to finish") {
          consoleView.shouldBe { present() }
          consoleView.waitContainsText("2 + 2 = 4", timeout = 3.minutes)
        }

        step("Check that \$PROJECT_DIR$ macro is expanded correctly") {
          val moduleBazelPath = checkNotNull(singleProject().getBasePath()) + "/MODULE.bazel"
          consoleView.waitContainsText("MODULE.bazel from envs: $moduleBazelPath", timeout = 5.seconds)
          consoleView.waitContainsText("Args: [-moduleBazelLocation=$moduleBazelPath", timeout = 5.seconds)
        }
        step("Check that parent environment variables are passed to run targets (BAZEL-2761)") {
          val homeValue = System.getProperty("user.home")
          checkNotNull(homeValue) { "user.home system property is not set" }

          val homeFound =
            runCatching { consoleView.waitContainsText("PARENT_ENV_HOME=$homeValue", timeout = 5.seconds) }.isSuccess
          val userProfileFound =
            runCatching { consoleView.waitContainsText("PARENT_ENV_USERPROFILE=$homeValue", timeout = 5.seconds) }.isSuccess

          check(homeFound || userProfileFound) {
            "Parent environment variables not passed: neither HOME nor USERPROFILE found in output (expected: $homeValue)"
          }
        }
      }
    }
  }
}
