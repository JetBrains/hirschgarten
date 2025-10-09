package org.jetbrains.bazel.tests.server

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.driver.execute
import com.intellij.tools.ide.performanceTesting.commands.gotoLine
import com.intellij.tools.ide.performanceTesting.commands.replaceText
import com.intellij.tools.ide.performanceTesting.commands.saveDocumentsAndSettings
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Test

/**
 * Test that the server is reset properly after a bazel version update.
 */
class BazelVersionUpdateTest : IdeStarterBaseProjectTest() {

  @Test
  fun `update bazel version should not cause server to break`() {
    createContext("bazelVersionUpdate", IdeaBazelCases.BazelVersionUpdate).runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
      step("Import Bazel project") {
        execute {
          it
            .waitForBazelSync()
            .waitForSmartMode()
            .takeScreenshot("afterImport")
        }
      }

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
        ideFrame {
          val buildView = x { byType("com.intellij.build.BuildView") }
          assert(
            buildView.getAllTexts().any {
              it.text.contains(BazelPluginBundle.message("console.task.sync.success"))
            },
          ) { "Build view does not contain success sync text" }
        }
      }
    }
  }
}
