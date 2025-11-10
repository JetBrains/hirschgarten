package org.jetbrains.bazel.tests.synthetic_targets

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class SyntheticRunTargetTest : IdeStarterBaseProjectTest() {

  @Test
  fun test() {
    createContext("runAllTestsAction", IdeaBazelCases.SyntheticRunTarget)
      .applyVMOptionsPatch {
        this.addSystemProperty("expose.ui.hierarchy.url", "true")
        this.addSystemProperty("ide.registry.bazel.run.synthetic.enable", "true")
      }
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        syncBazelProject()
        waitForIndicators(5.minutes)

        ideFrame {
          step("Test java") {
            execute { openFile("java_target/MyMain1.java") }
            runFromGutter(4, "Run")
            waitForIndicators(timeout = 2.minutes)
            assertOutConsoleContains("Hello from MyMain1")

            execute { openFile("java_target/MyMain2.java") }
            runFromGutter(4, "Run")
            waitForIndicators(timeout = 2.minutes)
            assertOutConsoleContains("Hello from MyMain2")
          }

          step("Test kotlin") {
            execute { openFile("kotlin_target/main1.kt") }
            runFromGutter(3, "Run")
            waitForIndicators(timeout = 2.minutes)
            assertOutConsoleContains("Hello from main1")

            execute { openFile("kotlin_target/main2.kt") }
            runFromGutter(5, "Run")
            waitForIndicators(timeout = 2.minutes)
            assertOutConsoleContains("Hello from main2")
          }
        }
      }
  }

  private fun IdeaFrameUI.runFromGutter(line: Int, text: String) {
    editorTabs()
      .gutter()
      .getGutterIcons()
      .first { it.line == line - 1 }
      .click()
    val gutters = popup().waitAnyTextsContains(text)
    gutters.first().click()
  }

  private fun IdeaFrameUI.assertOutConsoleContains(text: String) {
    val contains = x("//div[@class='ConsoleViewImpl']")
      .getAllTexts { true }
      .any { it.text.contains(text) }
    assert(contains) { "Console doesn't contain $text" }
  }

}
