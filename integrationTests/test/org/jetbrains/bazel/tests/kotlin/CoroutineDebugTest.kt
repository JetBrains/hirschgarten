package org.jetbrains.bazel.tests.kotlin

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.components.elements.verticalScrollBar
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.driver.execute
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.setBreakpoint
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.ideStarter.withBazelFeatureFlag
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/kotlin/coroutineDebug --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 */
class CoroutineDebugTest : IdeStarterBaseProjectTest() {

  @Test
  fun `coroutine debugger should show async stack traces`() {
    createContext("coroutineDebug", IdeaBazelCases.CoroutineDebug)
      .withBazelFeatureFlag(BazelFeatureFlags.BUILD_PROJECT_ON_SYNC)
      .runIdeWithDriver(runTimeout = timeout) { withScreenRecording() }
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          step("Enable Kotlin Coroutine Debug") {
            execute { it.enableKotlinCoroutineDebug() }
          }

          step("Open TestCoroutine.kt and set breakpoint") {
            execute {
              it
                .openFile("src/test/org/example/TestCoroutine.kt")
                .setBreakpoint(line = 30)
            }
          }

          step("Launch debug run config") {
            wait(10.seconds)
            editorTabs()
              .gutter()
              .getGutterIcons()
              .first { it.getIconPath().contains("run") }
              .click()
            popup().waitOneContainsText("Debug test").click()
          }

          step("Check if async stack trace is displayed") {
            waitOneContainsText("secondLevel:30", timeout = 1.minutes)
            waitFor(message = "Async stack traces to appear", timeout = 30.seconds, interval = 2.seconds) {
              x("//div[@class='Splitter']").verticalScrollBar { scrollBlockDown(6) }
              val text = x("//div[@class='XDebuggerFramesList']").getAllTexts()
              text.count { it.text.contains("Async stack trace") } >= 2
            }
            takeScreenshot("asyncStackTraceText")
          }

          step("Check thread dump for coroutine thread") {
            x("//div[@class='JBRunnerTabs']//div[@tooltiptext='More']").click()
            popup().waitOneContainsText("Get Thread Dump").click()
            waitFor(message = "Thread dump to contain coroutine:2", timeout = 30.seconds, interval = 2.seconds) {
              x("//div[@class='ThreadDumpPanel']").getAllTexts().any { it.text == "coroutine:2" }
            }
            takeScreenshot("threadDumpPanel")
          }
        }
      }
  }
}

private fun <T : CommandChain> T.enableKotlinCoroutineDebug(): T {
  addCommand(CMD_PREFIX + "enableKotlinCoroutineDebug")
  return this
}
