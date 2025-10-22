package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.deleteFile
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Test

class RecoverDotBazelBspTest : IdeStarterBaseProjectTest() {

  @Test
  fun openProject() {
    val context = createContext("recoverDotBazelBsp", IdeaBazelCases.RecoverDotBazelBsp)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Wait for import to finish") {
          execute { waitForBazelSync() }
          execute { waitForSmartMode() }
          takeScreenshot("afterImport")
        }

        step("Delete .bazelbsp directory") {
          execute { deleteFile(context.resolvedProjectHome.toString(), ".bazelbsp") }
        }

        step("Resync") {
          execute { buildAndSync() }
          execute { waitForSmartMode() }
          takeScreenshot("afterResync")
        }

        step("Check that the sync finishes successfully") {
          ideFrame {
            try {
              val buildView = x { byType("com.intellij.build.BuildView") }
              assert(
                buildView.getAllTexts().any {
                  it.text.contains(BazelPluginBundle.message("console.task.sync.success"))
                },
              ) { "Build view does not contain success sync text" }
            } catch (e: Exception) {
              assert(e is WaitForException) { "Unknown exception: ${e.message}" }
            }
          }
        }
      }
  }
}
