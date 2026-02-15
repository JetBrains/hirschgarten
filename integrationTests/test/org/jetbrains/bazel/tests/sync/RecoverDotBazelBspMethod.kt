package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.tools.ide.performanceTesting.commands.deleteFile
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.assertSyncSucceeded
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.execute

fun recoverDotBazelBsp(bgRun: BackgroundRun, ctx: IDETestContext) {
  bgRun.driver.withContext {
    step("Delete .bazelbsp directory") {
      execute { deleteFile(ctx.resolvedProjectHome.toString(), ".bazelbsp") }
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
