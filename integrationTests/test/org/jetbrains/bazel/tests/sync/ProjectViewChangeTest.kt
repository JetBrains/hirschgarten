package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.assertSyncedTargets
import org.jetbrains.bazel.base.buildAndSync
import org.jetbrains.bazel.base.checkIdeaLogForExceptions
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.openFile
import org.jetbrains.bazel.base.switchProjectView
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.waitForSyncSucceeded
import org.junit.jupiter.api.Test
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ProjectViewChangeTest : IdeStarterBaseProjectTest() {

  @Test
  fun `modify project view and resync updates targets`() {
    val context = createContext("projectViewModify", IdeaBazelCases.ProjectViewChange)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)
          waitForSyncSucceeded()

          step("Verify initial targets — lib_a and lib_b only") {
            execute { assertSyncedTargets("//:lib_a", "//:lib_b") }
            takeScreenshot("afterInitialSync")
          }

          step("Modify projectview.bazelproject — add lib_c") {
            openFile("projectview.bazelproject", waitForCodeAnalysis = false)
            (context.resolvedProjectHome / "projectview.bazelproject")
              .writeText("targets:\n  //:lib_a\n  //:lib_b\n  //:lib_c\n")
            takeScreenshot("afterModifyProjectView")
            wait(3.seconds)
          }

          step("Resync and verify lib_c is now synced") {
            execute {
              buildAndSync()
              waitForSmartMode()
              takeScreenshot("afterResyncModified")
            }
            waitForSyncSucceeded()
            execute { assertSyncedTargets("//:lib_a", "//:lib_b", "//:lib_c") }
          }
        }

        step("Check IDEA log for exceptions") {
          checkIdeaLogForExceptions(context)
        }
      }
  }

  @Test
  fun `switch project view file and resync updates targets`() {
    val context = createContext("projectViewSwitch", IdeaBazelCases.ProjectViewChange)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)
          waitForSyncSucceeded()

          step("Verify initial targets — lib_a and lib_b from default view") {
            execute { assertSyncedTargets("//:lib_a", "//:lib_b") }
            takeScreenshot("afterInitialSync")
          }

          step("Switch to alternate.bazelproject") {
            execute { switchProjectView("alternate.bazelproject") }
            takeScreenshot("afterSwitchProjectView")
          }

          step("Resync and verify targets switched to lib_c, lib_d") {
            execute {
              buildAndSync()
              waitForSmartMode()
              takeScreenshot("afterResyncSwitched")
            }
            waitForSyncSucceeded()
            execute { assertSyncedTargets("//:lib_c", "//:lib_d") }
          }
        }

        step("Check IDEA log for exceptions") {
          checkIdeaLogForExceptions(context)
        }
      }
  }

}
