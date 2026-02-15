package org.jetbrains.bazel.tests.server

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.execute
import com.intellij.tools.ide.performanceTesting.commands.gotoLine
import com.intellij.tools.ide.performanceTesting.commands.replaceText
import com.intellij.tools.ide.performanceTesting.commands.saveDocumentsAndSettings
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.assertSyncSucceeded
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.openFile
import kotlin.time.Duration.Companion.minutes

fun bazelVersionUpdate(bgRun: BackgroundRun) {
  bgRun.driver.withContext {
    ideFrame {
      step("Update bazel version") {
        openFile(".bazelversion")
        execute {
          it
            .gotoLine(1)
            .replaceText(0, 5, "8.3.1")
            .saveDocumentsAndSettings()
            .takeScreenshot("afterUpdateBazelVersion")
        }
      }

      step("Resync project and check if the sync is successful") {
        execute {
          it
            .buildAndSync()
            .waitForSmartMode()
            .takeScreenshot("afterResync")
        }
        assertSyncSucceeded()
      }
    }
  }
}
