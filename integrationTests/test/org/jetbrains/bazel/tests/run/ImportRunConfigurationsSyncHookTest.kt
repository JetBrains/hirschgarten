package org.jetbrains.bazel.tests.run

import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
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

  @Test
  fun openBazelProject() {
    createContext("importRunConfigurationsSyncHook", IdeaBazelCases.ImportRunConfigurationsSyncHook).runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
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
      }
    }
  }
}
