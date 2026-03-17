package org.jetbrains.bazel.tests.fastbuild

import com.intellij.driver.client.Remote
import com.intellij.driver.client.service
import com.intellij.driver.sdk.VirtualFile
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.GutterIcon
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.codeEditorForFile
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
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
import org.jetbrains.bazel.ideStarter.waitForBazelDebuggerUiReady
import org.jetbrains.bazel.ideStarter.withBazelFeatureFlag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/fastbuild --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
// TODO(Dan): rename to HotSwapTest
class FastBuildTest : IdeStarterBaseProjectTest() {
  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `hotswap should apply code changes during debug`(useFastBuildInsteadOfHotswap: Boolean) {
    val context = createContext("fastbuild", IdeaBazelCases.FastBuild)
      .withBazelFeatureFlag(BazelFeatureFlags.BUILD_PROJECT_ON_SYNC)
      .applyVMOptionsPatch {
        addSystemProperty(BazelFeatureFlags.FAST_BUILD_ENABLED, useFastBuildInsteadOfHotswap.toString())
      }
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          step("Set breakpoint") {
            openFile("Main.java")
            val initialBreakpointCount = editorTabs()
              .gutter()
              .getGutterIcons()
              .count { icon ->
                val iconPath = icon.getIconPath()
                iconPath.contains(GutterIcon.BREAKPOINT.path) || iconPath.contains(GutterIcon.BREAKPOINT_VALID.path)
              }
            codeEditor {
              goToPosition(7, 1)
            }
            driver.invokeAction("ToggleLineBreakpoint")
            waitFor(
              message = "Breakpoint icon should appear in the gutter after ToggleLineBreakpoint",
              timeout = 30.seconds,
              interval = 1.seconds,
            ) {
              editorTabs()
                .gutter()
                .getGutterIcons()
                .count { icon ->
                  val iconPath = icon.getIconPath()
                  iconPath.contains(GutterIcon.BREAKPOINT.path) || iconPath.contains(GutterIcon.BREAKPOINT_VALID.path)
                } > initialBreakpointCount
            }
          }

          step("Launch debug run config") {
            val runIcon = editorTabs()
              .gutter()
              .getGutterIcons()
              .firstOrNull { icon ->
                val iconPath = icon.getIconPath()
                iconPath.contains(GutterIcon.RUN.path) ||
                  iconPath.contains(GutterIcon.RUNSUCCESS.path) ||
                  iconPath.contains(GutterIcon.RUNERROR.path)
              }
              ?: error("Run gutter icon was not found in Main.java")
            runIcon.click()
            popup().waitOneText("Debug run").click()
          }

          step("Wait for debugger UI to be ready") {
            waitForBazelDebuggerUiReady(sessionTimeout = 3.minutes)
          }

          step("Change code") {
            openFile("Calculator.java")
            val editor = codeEditorForFile("Calculator.java")
            val originalMethodBody = "return a + b;"
            val updatedMethodBody = "return 1 + a + b;"
            check(editor.text.contains(originalMethodBody)) {
              "Calculator.java did not contain the expected original method body"
            }
            editor.text = editor.text.replace(originalMethodBody, updatedMethodBody)
            waitFor(
              message = "Calculator.java editor should contain the updated method body",
              timeout = 30.seconds,
              interval = 1.seconds,
            ) {
              codeEditorForFile("Calculator.java").text.contains(updatedMethodBody)
            }
            driver.invokeAction("SaveAll")
          }

          step("Apply hotswap") {
            val projectTaskManager = service<ProjectTaskManager>(singleProject())
            projectTaskManager.compile(arrayOf(checkNotNull(driver.findFile("Calculator.java"))))
          }

          if (useFastBuildInsteadOfHotswap) {
            step("Agree to the 'Reload Changed Classes' dialog") {
              val reloadButton = ui.x { byAccessibleName("Reload") }
              reloadButton.waitFound(timeout = 30.seconds)
              reloadButton.click()
            }
          }
          waitForIndicators(30.seconds)

          step("Resume program") {
            waitForBazelDebuggerUiReady().resumeButton.click()
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
