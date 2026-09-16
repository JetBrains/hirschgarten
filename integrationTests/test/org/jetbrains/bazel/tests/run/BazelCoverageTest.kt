package org.jetbrains.bazel.tests.run

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UIComponentsList.Companion.waitAny
import com.intellij.driver.sdk.ui.components.UIComponentsList.Companion.waitNotFound
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.dialogs.editRunConfigurationsDialog
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.popups.runConfigurationsPopup
import com.intellij.driver.sdk.ui.components.common.toolwindows.coverageToolWindow
import com.intellij.driver.sdk.ui.components.elements.button
import com.intellij.driver.sdk.ui.components.elements.checkBoxWithName
import com.intellij.driver.sdk.ui.components.elements.dialog
import com.intellij.driver.sdk.ui.components.elements.list
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.components.elements.tree
import com.intellij.driver.sdk.ui.components.elements.tryToScrollDown
import com.intellij.driver.sdk.ui.should
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.delay
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.tests.ui.clickRunGutterOnLine
import org.jetbrains.bazel.tests.ui.setRunConfigRunWithBazel
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.minutes

private val BAZEL_COVERAGE_PROJECT = simpleBazelProject(
  path = "coverageTest",
)

class BazelCoverageTest : IdeStarterBaseProjectTest() {

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `run test with coverage and verify results`(runConfigRunWithBazel: Boolean) {
    createContext(
      "bazelCoverage-${if (runConfigRunWithBazel) "withBazel" else "withoutBazel"}",
      IdeaBazelCases.withProject(BAZEL_COVERAGE_PROJECT),
    )
      .also { it.pluginConfigurator.disablePlugins("com.intellij.ml.llm") }
      .setRunConfigRunWithBazel(runConfigRunWithBazel)
      .runIdeWithDriver(runTimeout = timeout, pauseOnIndicators = 5.minutes)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject(true)

          step("Run test with coverage") {
            execute { openFile("src/test/com/example/CalculatorTest.java") }
            runCalculatorTestWithCoverage()
            coverageToolWindow().waitFound(1.minutes).reportTable.waitFound()
            if (!runConfigRunWithBazel) {
              // When running with default IDEA coverage, respect the default include filter for packages
              waitNoTexts("org.other_package")
            }
            takeScreenshot("afterRunTestWithCoverage")
          }

          val expectedCoverageText = if (runConfigRunWithBazel) {
            "50% lines covered"
          }
          else {
            "40% methods, 40% lines covered"  // More detailed native IDEA coverage with a Java agent
          }

          step("Verify coverage results") {
            execute { openFile("src/main/com/example/Calculator.java") }
            execute { assertCoverage(expectedCoverageText) }
            execute { delay(1000) }
            takeScreenshot("afterAssertCoverage")
          }
        }
      }
  }

  @Test
  fun `the Run with Bazel check box swaps the coverage settings`() {
    createContext(
      "bazelCoverage-settingsSwap",
      IdeaBazelCases.withProject(BAZEL_COVERAGE_PROJECT),
    )
      .also { it.pluginConfigurator.disablePlugins("com.intellij.ml.llm") }
      .setRunConfigRunWithBazel(true)
      .runIdeWithDriver(runTimeout = timeout, pauseOnIndicators = 5.minutes)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject(true)

          step("Open the run configuration of the test") {
            // "Modify Run Configuration…" opens the editor without a run, so this test runs no test.
            execute { openFile("src/test/com/example/CalculatorTest.java") }
            clickRunGutterOnLine(4)
            popup().waitOneText("Modify Run Configuration…").click()
          }

          step("Verify that the editor starts with the Bazel coverage settings") {
            // Without this check, the step below passes even if the field never shows at all.
            dialog {
              instrumentationFilterField.waitFound()
              waitNotFound(NO_IDEA_COVERAGE_OPTIONS) { byClass(COVERAGE_FILTER_EDITOR) }
            }
          }

          step("Clear Run with Bazel and verify that the IDEA coverage options replace the filter") {
            dialog {
              checkBoxWithName(RUN_WITH_BAZEL).uncheck()
              instrumentationFilterField.waitNotFound()
              waitAny(IDEA_COVERAGE_OPTIONS) { byClass(COVERAGE_FILTER_EDITOR) }
              takeScreenshot("afterUncheckRunWithBazel")
            }
          }

          step("Set Run with Bazel and verify that the filter replaces the IDEA coverage options") {
            dialog {
              checkBoxWithName(RUN_WITH_BAZEL).check()
              instrumentationFilterField.waitFound()
              waitNotFound(NO_IDEA_COVERAGE_OPTIONS) { byClass(COVERAGE_FILTER_EDITOR) }
              takeScreenshot("afterCheckRunWithBazel")
              cancelButton.click()
            }
          }
        }
      }
  }

  @Test
  fun `the instrumentation filter and the bazel flags are applied properly to Bazel coverage`() {
    createContext(
      "bazelCoverage-instrumentationFilter",
      IdeaBazelCases.withProject(BAZEL_COVERAGE_PROJECT),
    )
      .setRunConfigRunWithBazel(true)
      .runIdeWithDriver(runTimeout = timeout, pauseOnIndicators = 5.minutes)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject(true)

          step("Run the test with the filter of the .bazelrc file") {
            // The run configuration holds no filter, so the coverage scope comes from the .bazelrc file.
            // That file sets ^//, so the report holds both packages.
            execute { openFile("src/test/com/example/CalculatorTest.java") }
            runCalculatorTestWithCoverage()
            waitForCoverageReport(present = listOf("Calculator.java", "OtherPackage.java"))
            takeScreenshot("afterBazelrcFilterCoverage")
          }

          step("Close the coverage report") {
            closeCoverageReport()
          }

          step("Set the instrumentation filter in the Bazel flags") {
            openRunConfigurationEditor()
            editRunConfigurationsDialog {
              bazelFlags = "--instrumentation_filter=^//src/main/org/other_package[/:]"
              takeScreenshot("afterSetBazelFlags")
              button("OK").click()
            }
          }

          step("Re-run the test and verify that the Bazel flags beat the .bazelrc file") {
            execute { openFile("src/test/com/example/CalculatorTest.java") }
            runCalculatorTestWithCoverage()
            waitForCoverageReport(present = listOf("OtherPackage.java"), absent = listOf("Calculator.java"))
            takeScreenshot("afterBazelFlagsCoverage")
          }

          step("Close the coverage report") {
            closeCoverageReport()
          }

          step("Set the instrumentation filter of the run configuration") {
            openRunConfigurationEditor()
            editRunConfigurationsDialog {
              instrumentationFilter = "^//src/main/com/example[/:]"
              takeScreenshot("afterSetInstrumentationFilter")
              button("OK").click()
            }
          }

          step("Re-run the test and verify that the filter of the run configuration wins") {
            execute { openFile("src/test/com/example/CalculatorTest.java") }
            runCalculatorTestWithCoverage()
            waitForCoverageReport(present = listOf("Calculator.java"), absent = listOf("OtherPackage.java"))
            execute { openFile("src/main/com/example/Calculator.java") }
            execute { assertCoverage("50% lines covered") }
            takeScreenshot("afterFilteredCoverage")
          }
        }
      }
  }
}

private const val RUN_WITH_BAZEL = "Run with Bazel"

private const val COVERAGE_FILTER_EDITOR = "CoverageClassFilterEditor"
private const val IDEA_COVERAGE_OPTIONS = "The IDEA coverage options show"
private const val NO_IDEA_COVERAGE_OPTIONS = "The IDEA coverage options hide"

private fun IdeaFrameUI.openRunConfigurationEditor() {
  runConfigurationsPopup {
    list().clickItem("Edit Configurations", fullMatch = false)
  }
  editRunConfigurationsDialog {
    waitFound()
  }
}

private fun IdeaFrameUI.runCalculatorTestWithCoverage() {
  clickRunGutterOnLine(4)
  popup().waitOneText("Run '//src/test/com/example:calculator_test' with Coverage").click()
}

private fun IdeaFrameUI.waitForCoverageReport(present: List<String>, absent: List<String> = emptyList()) {
  coverageToolWindow {
    waitFound(1.minutes)
    reportTable.waitFound()
    val tree = tree()
    tree.expandAll()
    tree.should(
      message = "The coverage report holds $present and none of $absent",
      timeout = 3.minutes,
      errorMessage = { "The coverage report holds: ${tree.collectExpandedPathsAsStrings()}" },
    ) {
      val reportPaths = collectExpandedPathsAsStrings()
      present.all { expected -> reportPaths.any { expected in it } } &&
        absent.none { unexpected -> reportPaths.any { unexpected in it } }
    }
  }
}

fun <T : CommandChain> T.assertCoverage(coverageInformationString: String): T {
  addCommand(CMD_PREFIX + "assertCoverage $coverageInformationString")
  return this
}
