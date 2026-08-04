package org.jetbrains.bazel.tests.run

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.minutes

private val BAZEL_COVERAGE_PROJECT = simpleBazelProject(
  revision = "cb47ad3072495dd9811da4ae2f346566f8b0c7cf",
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
            clickRunGutterOnLine(4)
            popup().waitOneText("Run '//src/test/com/example:calculator_test' with Coverage").click()
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
}

fun <T : CommandChain> T.assertCoverage(coverageInformationString: String): T {
  addCommand(CMD_PREFIX + "assertCoverage $coverageInformationString")
  return this
}
