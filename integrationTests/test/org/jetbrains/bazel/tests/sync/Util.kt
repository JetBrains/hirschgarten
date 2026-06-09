package org.jetbrains.bazel.tests.sync

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.wait
import org.jetbrains.bazel.config.BazelPluginBundle
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

fun Driver.verifyNoSyncOnReopen() {
  step("Verify no sync happens on reopen") {
    ideFrame {
      val start = TimeSource.Monotonic.markNow()
      while (TimeSource.Monotonic.markNow() - start < 20.seconds) {
        try {
          val buildView = x { byType("com.intellij.build.BuildView") }
          assert(
            !buildView.getAllTexts().any {
              it.text.contains(BazelPluginBundle.message("console.task.sync.in.progress"))
            },
          ) { "Build view contains sync text" }
          wait(1.seconds)
        }
        catch (e: Exception) {
          assert(e is WaitForException) { "Unknown exception: ${e.message}" }
        }
      }
    }
    takeScreenshot("afterReopeningProject")
  }
}
