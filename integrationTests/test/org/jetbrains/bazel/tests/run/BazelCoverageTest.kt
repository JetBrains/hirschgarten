package org.jetbrains.bazel.tests.run

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.dialogs.editRunConfigurationsDialog
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.popups.runConfigurationsPopup
import com.intellij.driver.sdk.ui.components.common.toolwindows.coverageToolWindow
import com.intellij.driver.sdk.ui.components.elements.button
import com.intellij.driver.sdk.ui.components.elements.list
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.components.elements.tree
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
      .setRunConfigRunWithBazel(runConfigRunWithBazel)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          val expectedCoverageTabText = if (runConfigRunWithBazel) {
            "Statistics, %"
          }
          else {
            "Line, %"
          }

          step("Run test with coverage") {
            execute { openFile("src/test/com/example/CalculatorTest.java") }
            runCalculatorTestWithCoverage()
            waitOneText(expectedCoverageTabText, timeout = 1.minutes)
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
  fun `bazel flags are applied properly to Bazel coverage`() {
    createContext(
      "bazelCoverage-bazelFlags",
      IdeaBazelCases.withProject(BAZEL_COVERAGE_PROJECT),
    )
      .setRunConfigRunWithBazel(true)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          step("Run test with the derived filter") {
            execute { openFile("src/test/com/example/CalculatorTest.java") }
            runCalculatorTestWithCoverage()
            waitForCoverageReport(present = listOf("Calculator.java", "OtherPackage.java"))
            takeScreenshot("afterDerivedFilterCoverage")
          }

          step("Close the coverage report") {
            closeCoverageReport()
          }

          step("Set the instrumentation filter in the Bazel flags") {
            runConfigurationsPopup {
              list().clickItem("Edit Configurations", fullMatch = false)
            }
            editRunConfigurationsDialog {
              waitFound()
              bazelFlags = "--instrumentation_filter=^//src/main/com/example"
              takeScreenshot("afterSetBazelFlags")
              button("OK").click()
            }
          }

          step("Re-run the test and verify that only Calculator is covered") {
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

private fun IdeaFrameUI.runCalculatorTestWithCoverage() {
  clickRunGutterOnLine(4)
  popup().waitOneText("Run '//src/test/com/example:calculator_test' with Coverage").click()
}

private fun IdeaFrameUI.waitForCoverageReport(present: List<String>, absent: List<String> = emptyList()) {
  waitOneText("Statistics, %", timeout = 1.minutes)
  coverageToolWindow {
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
