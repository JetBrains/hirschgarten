package org.jetbrains.bazel.tests.synthetic_targets

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.setRegistry
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.data.simpleBazelProject
import org.junit.jupiter.api.Test
import org.opentest4j.MultipleFailuresError
import kotlin.time.Duration.Companion.minutes

private val SYNTHETIC_RUN_TARGET_PROJECT = simpleBazelProject(
  revision = "5cbca8140ac85e5178ca803935fbd9e1d9400a07",
  path = "syntheticRunTargetTest",
)

class SyntheticRunTargetTest : IdeStarterBaseProjectTest() {

  @Test
  fun `synthetic run targets should execute Java and Kotlin main classes from gutter`() {
    val failures = mutableListOf<Throwable>()
    val context = createContext("syntheticRunTarget", IdeaBazelCases.withProject(SYNTHETIC_RUN_TARGET_PROJECT))
      .applyVMOptionsPatch {
        this.addSystemProperty("expose.ui.hierarchy.url", "true")
      }
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        setRegistry("bazel.run.synthetic.enable", true.toString())
        syncBazelProject()
        waitForIndicators(5.minutes)
        execute { takeScreenshot("afterSync") }

        val driver = this
        ideFrame {
          runScenario(
            driver,
            SyntheticRunScenario(
              name = "java_main1",
              file = "java_target/MyMain1.java",
              line = 4,
              targetLabel = "//java_target:my_java_lib",
              mainClass = "com.jetbrains.MyMain1",
              expectedConsoleText = "Hello from MyMain1",
            ),
            failures,
          )
          runScenario(
            driver,
            SyntheticRunScenario(
              name = "java_main2",
              file = "java_target/MyMain2.java",
              line = 4,
              targetLabel = "//java_target:my_java_lib",
              mainClass = "com.jetbrains.MyMain2",
              expectedConsoleText = "Hello from MyMain2",
            ),
            failures,
          )
          runScenario(
            driver,
            SyntheticRunScenario(
              name = "kotlin_main1",
              file = "kotlin_target/main1.kt",
              line = 3,
              targetLabel = "//kotlin_target:my_kt_lib",
              mainClass = "com.jetbrains.Main1Kt",
              expectedConsoleText = "Hello from main1",
            ),
            failures,
          )
          runScenario(
            driver,
            SyntheticRunScenario(
              name = "kotlin_main2",
              file = "kotlin_target/main2.kt",
              line = 5,
              targetLabel = "//kotlin_target:my_kt_lib",
              mainClass = "com.jetbrains.Main2Kt",
              expectedConsoleText = "Hello from main2",
            ),
            failures,
          )
        }
      }
    runCatching { checkIdeaLogForExceptions(context) }
      .onFailure { failures.add(AssertionError("IDE log contains unexpected exceptions", it)) }
    if (failures.isNotEmpty()) {
      throw MultipleFailuresError("Synthetic run target checks failed", failures)
    }
  }

  private fun IdeaFrameUI.runScenario(driver: Driver, scenario: SyntheticRunScenario, failures: MutableList<Throwable>) {
    step("Run ${scenario.name}") {
      runCatching {
        driver.execute { openFile(scenario.file) }
        driver.execute { takeScreenshot("${scenario.name}_afterOpen") }
        runFromGutter(driver, scenario)
        waitForIndicators(timeout = 2.minutes)
        driver.execute { takeScreenshot("${scenario.name}_afterRun") }
        assertOutConsoleContains(scenario.expectedConsoleText)
        driver.execute { takeScreenshot("${scenario.name}_afterConsoleCheck") }
      }.onFailure { failure ->
        runCatching { driver.execute { takeScreenshot("${scenario.name}_failure") } }
          .onFailure { screenshotFailure -> failure.addSuppressed(screenshotFailure) }
        failures.add(AssertionError("${scenario.name} failed", failure))
      }
    }
  }

  private fun IdeaFrameUI.runFromGutter(driver: Driver, scenario: SyntheticRunScenario) {
    val gutterIcons = editorTabs()
      .gutter()
      .getGutterIcons()
      .filter { it.line == scenario.line - 1 }
    check(gutterIcons.size == 1) { "Expected one gutter icon at line ${scenario.line}, got ${gutterIcons.size}" }
    gutterIcons.single().click()
    val popup = popup()
    val targetOnlyAction = "Run '${scenario.targetLabel}'"
    val targetAndMainClassAction = "Run '${scenario.targetLabel} ${scenario.mainClass}'"
    val action = popup.waitOneText(
      message = "Finding the run action for ${scenario.targetLabel} and ${scenario.mainClass}",
    ) { it.text == targetOnlyAction || it.text == targetAndMainClassAction }
    driver.execute { takeScreenshot("${scenario.name}_runPopup") }
    action.click()
  }

  private fun IdeaFrameUI.assertOutConsoleContains(text: String) {
    x("//div[@class='ConsoleViewImpl']").waitContainsText(text, timeout = 2.minutes)
  }

  private data class SyntheticRunScenario(
    val name: String,
    val file: String,
    val line: Int,
    val targetLabel: String,
    val mainClass: String,
    val expectedConsoleText: String,
  )
}
