package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.deleteFile
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertSyncSucceeded
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class RecoverDotBazelBspTest : IdeStarterBaseProjectTest() {

  @Test
  fun `resync should recover after deleting bazelbsp directory`() {
    val context = createContext("recoverDotBazelBsp", IdeaBazelCases.RecoverDotBazelBsp)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        syncBazelProject()
        waitForIndicators(10.minutes)

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
              assertSyncSucceeded()
            } catch (e: Exception) {
              assert(e is WaitForException) { "Unknown exception: ${e.message}" }
            }
          }
        }
      }
  }
}
