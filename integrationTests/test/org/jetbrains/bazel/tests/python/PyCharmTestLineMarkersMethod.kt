package org.jetbrains.bazel.tests.python

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.execute
import kotlin.time.Duration.Companion.seconds

fun pyCharmTestLineMarkers(bgRun: BackgroundRun) {
  bgRun.driver.withContext {
    ideFrame {
      step("Open test file") {
        execute { openFile("python/test.py") }
        wait(5.seconds)
      }

      step("Verify run line marker text") {
        verifyRunLineMarkerText(listOf("Test", "Debug test", "Run with Coverage"))
      }
    }
  }
}
