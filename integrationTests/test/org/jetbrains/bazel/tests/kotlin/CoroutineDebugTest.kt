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
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.setBreakpoint
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.withBazelFeatureFlag
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * bazel test //plugins/bazel/integrationTests:integrationTests_test --test_env=JB_TEST_FILTER=org.jetbrains.bazel.tests.kotlin.CoroutineDebugTest --test_output=errors --nocache_test_results
 */
class CoroutineDebugTest : IdeStarterBaseProjectTest() {

  @Test
  fun `coroutine debugger should show async stack traces`() {
    createContext("coroutineDebug", IdeaBazelCases.CoroutineDebug)
      .withBazelFeatureFlag(BazelFeatureFlags.BUILD_PROJECT_ON_SYNC)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

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
            popup().waitOneContainsText("Debug '//src/test/org/example:TestCoroutine'").click()
          }

          step("Check if async stack trace is displayed") {
            waitOneContainsText("secondLevel:30", timeout = 1.minutes)
            waitFor(message = "Async stack traces to appear", timeout = 30.seconds, interval = 2.seconds) {
              runCatching {
                x("//div[@class='Splitter']").verticalScrollBar { scrollBlockDown(6) }
              }
              val text = x("//div[@class='XDebuggerFramesList']").getAllTexts()
              text.count { it.text.contains("Async stack trace") } >= 2
            }
            takeScreenshot("asyncStackTraceText")
          }

          step("Check thread dump for coroutine thread") {
            x("//div[@class='JBRunnerTabs']//div[@tooltiptext='More']").click()
            popup().waitOneContainsText("Get Thread Dump").click()
            val threadDumpPanel = x("//div[@class='ThreadDumpPanel']")
            threadDumpPanel.waitOneContainsText("Dumped Coroutines").click()
            waitFor(message = "Thread dump to contain coroutine #2", timeout = 30.seconds, interval = 2.seconds) {
              threadDumpPanel.getAllTexts().any {
                it.text.contains("coroutine:2") || it.text.contains("coroutine#2")
              }
            }
            takeScreenshot("threadDumpPanel")
          }
        }
      }
  }
}
