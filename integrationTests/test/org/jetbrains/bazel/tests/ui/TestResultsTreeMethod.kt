package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.ideStarter.execute
import kotlin.time.Duration.Companion.seconds

fun testResultsTree(bgRun: BackgroundRun) {
  bgRun.driver.withContext {
    ideFrame {
      step("Open SimpleKotlinTest.kt and run test") {
        execute { openFile("SimpleKotlinTest.kt") }
        execute { runSimpleKotlinTest() }
        takeScreenshot("afterRunningSimpleKotlinTest")
      }
      step("Verify test status and results tree") {
        verifyTestStatus(
          listOf("2 tests passed"),
          listOf("SimpleKotlinTest", "trivial test()", "another trivial test()"),
        )
        takeScreenshot("afterOpeningTestResultsTree")
      }

      step("Launch debug run config for SimpleKotlinTest") {
        editorTabs()
          .gutter()
          .getGutterIcons()
          .first()
          .click()
        popup().waitOneContainsText("Debug test").click()
        wait(15.seconds)
      }
      step("Verify debug test status and results tree") {
        verifyTestStatus(
          listOf("2 tests passed"),
          listOf("SimpleKotlinTest", "trivial test()", "another trivial test()"),
        )
        takeScreenshot("afterOpeningDebugTestResultsTree")
      }
    }
  }
}

private fun <T : CommandChain> T.runSimpleKotlinTest(): T {
  addCommand(CMD_PREFIX + "runSimpleKotlinTest")
  return this
}
