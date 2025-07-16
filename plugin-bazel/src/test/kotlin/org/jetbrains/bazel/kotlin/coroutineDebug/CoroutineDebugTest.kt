package org.jetbrains.bazel.kotlin.coroutineDebug

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.components.elements.verticalScrollBar
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.driver.execute
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.setBreakpoint
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/kotlin/coroutineDebug --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 */
class CoroutineDebugTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/xuansontrinh/bazel-test-projects-by-languages.git",
        commitHash = "59aa72ad42c212b079633e658fdb51fbe82c5e70",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("kotlin/coroutineDebug") },
        isReusable = true,
        configureProjectBeforeUse = ::configureProjectBeforeUseWithoutBazelClean,
      )

  override fun createContext(): IDETestContext =
    super.createContext().applyVMOptionsPatch {
      addSystemProperty(BazelFeatureFlags.BUILD_PROJECT_ON_SYNC, "true")
    }

  @Test
  fun testCoroutineDebug() {
    createContext()
      .runIdeWithDriver(runTimeout = timeout)
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
            waitOneContainsText("secondLevel:30")
            x("//div[@class='Splitter']").verticalScrollBar { scrollBlockDown(6) }
            val text = x("//div[@class='XDebuggerFramesList']").getAllTexts()
            var nAsyncStackTraceText = 0
            text.forEach {
              if (it.text.contains("Async stack trace")) ++nAsyncStackTraceText
            }
            takeScreenshot("asyncStackTraceText")
            assert(nAsyncStackTraceText == 2) {
              "Async stack trace is not correctly displayed, number of async stack trace text found is $nAsyncStackTraceText"
            }
          }
        }
      }
  }
}

private fun <T : CommandChain> T.enableKotlinCoroutineDebug(): T {
  addCommand(CMD_PREFIX + "enableKotlinCoroutineDebug")
  return this
}
