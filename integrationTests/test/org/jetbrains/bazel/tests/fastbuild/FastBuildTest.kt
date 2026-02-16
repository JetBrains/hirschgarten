package org.jetbrains.bazel.tests.fastbuild

import com.intellij.driver.client.Remote
import com.intellij.driver.client.service
import com.intellij.driver.model.OnDispatcher
import com.intellij.driver.sdk.VirtualFile
import com.intellij.driver.sdk.XDebuggerUtil
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.pasteText
import com.intellij.driver.sdk.ui.ui
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.findFile
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.ideStarter.withBazelFeatureFlag
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/fastbuild --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class FastBuildTest : IdeStarterBaseProjectTest() {
  @Test
  fun `fast build hotswap should apply code changes during debug`() {
    val context = createContext("fastbuild", IdeaBazelCases.FastBuild)
      .withBazelFeatureFlag(BazelFeatureFlags.BUILD_PROJECT_ON_SYNC)
      .withBazelFeatureFlag(BazelFeatureFlags.FAST_BUILD_ENABLED)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          step("Set breakpoint") {
            val file = openFile("Main.java")
            withContext(OnDispatcher.EDT) {
              val debuggerUtil = service<XDebuggerUtil>()
              debuggerUtil.toggleLineBreakpoint(singleProject(), file, 6, false)
            }
          }

          step("Launch debug run config") {
            editorTabs {
              gutter().icons.first().click()
            }
            popup().waitOneText("Debug run").click()
          }

          val resumeProgram = x { byAccessibleName("Resume Program") }
          step("Wait for breakpoint to be hit") {
            waitFor(
              timeout = 3.minutes,
              getter = {},
              checker = { resumeProgram.present() && resumeProgram.isEnabled() },
            )
          }

          step("Change code") {
            openFile("Calculator.java")
            codeEditor {
              goToPosition(5, 16)
              // int add(int a, int b) {
              //     return 1 + a + b;
              ui.pasteText("1 + ")
            }
          }

          step("Apply hotswap") {
            val projectTaskManager = service<ProjectTaskManager>(singleProject())
            projectTaskManager.compile(arrayOf(checkNotNull(driver.findFile("Calculator.java"))))
          }

          step("Agree to the 'Reload Changed Classes' dialog") {
            val reloadButton = ui.x { byAccessibleName("Reload") }
            reloadButton.waitFound(timeout = 30.seconds)
            reloadButton.click()
            Thread.sleep(5000)
          }

          step("Resume program") {
            resumeProgram.click()
          }

          val consoleView = x { byClass("ConsoleViewImpl") }
          step("Wait for program to finish running after hotswap") {
            consoleView.waitContainsText("2 + 2 = 5", timeout = 30.seconds)
          }
        }
      }
    checkIdeaLogForExceptions(context)
  }
}

@Remote(value = "com.intellij.task.ProjectTaskManager")
interface ProjectTaskManager {
  fun compile(files: Array<VirtualFile>): Promise
}

@Remote(value = "org.jetbrains.concurrency.Promise")
interface Promise
