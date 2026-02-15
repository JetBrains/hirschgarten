package org.jetbrains.bazel.tests.reopen

import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.welcomeScreen
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.driver.engine.BackgroundRun
import org.jetbrains.bazel.config.BazelPluginBundle
import kotlin.time.Duration.Companion.seconds

fun reopenWithoutResync(bgRun: BackgroundRun) {
  bgRun.driver.withContext {
    step("Close project") {
      invokeAction("CloseProject")
      takeScreenshot("afterClosingProject")
    }

    step("Reopen project from welcome screen") {
      welcomeScreen { clickRecentProject("simpleKotlinTest") }
      takeScreenshot("afterClickingRecentProject")
    }

    step("Verify no sync happens on reopen") {
      ideFrame {
        wait(20.seconds)
        try {
          val buildView = x { byType("com.intellij.build.BuildView") }
          assert(
            !buildView.getAllTexts().any {
              it.text.contains(BazelPluginBundle.message("console.task.sync.in.progress"))
            },
          ) { "Build view contains sync text" }
        } catch (e: Exception) {
          assert(e is WaitForException) { "Unknown exception: ${e.message}" }
        }
      }
      takeScreenshot("afterReopeningProject")
    }
  }
}
