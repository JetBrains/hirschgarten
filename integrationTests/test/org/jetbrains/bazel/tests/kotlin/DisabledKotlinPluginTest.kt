package org.jetbrains.bazel.tests.kotlin

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:DisabledKotlinPluginTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class DisabledKotlinPluginTest : IdeStarterBaseProjectTest() {
  @Test
  fun `sync should work with Kotlin plugin disabled`() {
    createContext("disabledKotlinPlugin", IdeaBazelCases.DisabledKotlinPlugin)
      .withDisabledPlugins(setOf("org.jetbrains.kotlin"))
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          step("Check imported modules") {
            execute { checkImportedModules() }
            takeScreenshot("afterCheckImportedModules")
          }

          step("Check targets in target widget") {
            execute { checkTargetsInTargetWidget() }
            takeScreenshot("afterCheckTargetsInTargetWidget")
          }
        }
      }
  }

  private fun <T : CommandChain> T.checkImportedModules(): T = also { addCommand("${CMD_PREFIX}checkImportedModules") }

  private fun <T : CommandChain> T.checkTargetsInTargetWidget(): T = also { addCommand("${CMD_PREFIX}checkTargetsInTargetWidget") }

  private fun IDETestContext.withDisabledPlugins(pluginIds: Set<String>): IDETestContext =
    also { pluginConfigurator.disablePlugins(pluginIds) }
}
