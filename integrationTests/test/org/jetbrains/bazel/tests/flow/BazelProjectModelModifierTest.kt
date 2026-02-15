package org.jetbrains.bazel.tests.flow

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.delay
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertEitherFileContentIsEqual
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.navigateToFile
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/flow/modify:BazelProjectModelModifierTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class BazelProjectModelModifierTest : IdeStarterBaseProjectTest() {
  @Test
  fun `quick fix should add module and library dependencies to BUILD file`() {
    val context = createContext("bazelProjectModelModifier", IdeaBazelCases.BazelProjectModelModifier)
    context
      .runIdeWithDriver(runTimeout = timeout) { withScreenRecording() }
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          step("Add module dependency for UsesDependency1") {
            execute { openFile("UsesDependency1.java") }
            execute { goto(2, 10) }
            execute { applyOrderEntryFixAndCheckRedCode(hint = "Add dependency on module") }
          }

          step("Add module dependency for UsesDependency2") {
            execute { openFile("UsesDependency2.java") }
            execute { goto(2, 10) }
            execute { applyOrderEntryFixAndCheckRedCode(hint = "Add dependency on module") }
          }

          step("Add library dependency for UsesDependency3") {
            execute { openFile("UsesDependency3.java") }
            execute { goto(5, 14) }
            execute { applyOrderEntryFixAndCheckRedCode(hint = "Add library 'junit' to classpath") }
          }

          step("Check that the added dependency to JUnit is navigatable") {
            execute { openFile("BUILD") }
            execute { navigateToFile(33, 17, "BUILD", 934, 9) }
          }

          step("Assert files are equal and exit app") {
            execute { assertEitherFileContentIsEqual("BUILD", "BUILD.expected", "BUILD.expected2") }
          }
        }
      }
    checkIdeaLogForExceptions(context)
  }

  private fun <T : CommandChain> T.applyOrderEntryFixAndCheckRedCode(hint: String) =
    this
      .applyOrderEntryQuickFix(hint)
      .waitForSmartMode()
      .delay(3000)
      .checkOnRedCode()

  private fun <T : CommandChain> T.applyOrderEntryQuickFix(hint: String): T {
    addCommand(CMD_PREFIX + "applyOrderEntryQuickFix $hint")
    return this
  }
}
