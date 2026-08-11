package org.jetbrains.bazel.tests.golang

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.debugToolWindow
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.setBreakpoint
import org.jetbrains.bazel.data.GoLandBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.ideStarter.waitForSyncSucceeded
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.tests.ui.clickRunGutterOnLine
import org.jetbrains.bazel.tests.ui.verifyTestStatus
import org.jetbrains.bazel.tests.ui.waitForDebuggerPausedAt
import org.junit.jupiter.api.Test
import kotlin.io.path.appendText
import kotlin.io.path.div

private val GO_RUN_CONFIGURATIONS_SYMLINK_PROJECT = simpleBazelProject(
  revision = "f01c28ffa2adfadee12673e44e78fa63d7cd8b13",
  path = "goRunConfigurationsTest",
  configureProject = { context ->
    BazelProjectConfigurer.configureProjectBeforeUse(
      context,
      createProjectView = false,
    )
  },
)

internal class GoRunConfigurationsSymlinkPrefixTest : IdeStarterBaseProjectTest() {

  /**
   * BAZEL-3326
   */
  @Test
  fun `test debug run configurations when the symlinks are not in the default location`() {
    val context = createContext("goRunConfigurationsTest", GoLandBazelCases.withProject(GO_RUN_CONFIGURATIONS_SYMLINK_PROJECT))
    val bazelrc = context.resolvedProjectHome / ".bazelrc"
    bazelrc.appendText("common --symlink_prefix=out/bazel-")
    context
      .runIdeWithDriver()
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForSyncSucceeded()
          step("Open lib/lib_test.go") {
            execute { openFile("lib/lib_test.go") }
          }
          step("Set all breaking points") {
            execute { setBreakpoint(line = 6, relativePath = "lib/lib_test.go") }
            execute { setBreakpoint(line = 12, relativePath = "lib/lib_test.go") }
          }
          step("Debug tests in package via its run gutter") {
            clickRunGutterOnLine(0)
            popup().waitOneContainsText("Debug '//lib:lib_test'").click()
          }
          step("Debugger stops at all breakpoints") {
            repeat(2) { i ->
              waitForDebuggerPausedAt("TestAdd", "TestSubtract")
              takeScreenshot("goDebugPausedAt$i")
              debugToolWindow().resumeButton.click()
            }
          }
          step("Verify results") {
            verifyTestStatus(
              expectedStatus = listOf("2 tests passed"),
              expectedTree = listOf("TestAdd", "TestSubtract")
            )
          }
        }
      }
  }
}
