package org.jetbrains.bazel.tests.sync

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.wait
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.types.shouldBeInstanceOf
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
          withClue("Build view contains sync text") {
            buildView
              .getAllTexts()
              .any { it.text.contains(BazelPluginBundle.message("console.task.sync.in.progress")) }
              .shouldBeFalse()
          }
          wait(1.seconds)
        }
        catch (e: Exception) {
          withClue("Unknown exception: ${e.message}") {
            e.shouldBeInstanceOf<WaitForException>()
          }
        }
      }
    }
    takeScreenshot("afterReopeningProject")
  }
}
