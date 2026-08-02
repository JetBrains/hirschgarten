package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.data.simpleBazelProject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.minutes

private val RUN_ALL_TESTS_PROJECT = simpleBazelProject(
  // TODO: temporary pin to SBPFT branch bazel/dan/e2e-os-bazel-matrix; repoint to main once the fixture upstreaming lands there
  revision = "e974ca77b97e65a329f03492f9b556e44f47f648",
  path = "runAllTests",
)

/**
 * ```sh
 * bazel test //plugins/bazel/integrationTests:integrationTests_test --test_env=JB_TEST_FILTER=org.jetbrains.bazel.tests.ui.RunAllTestsActionTest --test_output=errors --nocache_test_results
 * ```
 */
class RunAllTestsActionTest : IdeStarterBaseProjectTest() {

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `run all tests action should execute and show results`(runConfigRunWithBazel: Boolean) {
    createContext("runAllTestsAction-${if (runConfigRunWithBazel) "withBazel" else "withoutBazel"}", IdeaBazelCases.withProject(RUN_ALL_TESTS_PROJECT))
      .setRunConfigRunWithBazel(runConfigRunWithBazel)
      .runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
      ideFrame {
        syncBazelProject()
        waitForIndicators(5.minutes)

        step("Right-click the root project directory") {
          projectView().projectViewTree.rightClickRow(0)
          popupMenu().waitFound()
          takeScreenshot("afterRightClickingProjectRoot")
        }

        step("Click on Run all tests") {
          popupMenu().findMenuItemByText("Run all tests").click()
          waitForIndicators(5.minutes)
        }

        verifyTestStatus(
          listOf("2 tests passed"),
          setOf("AdditionTest", "testAddition", "MultiplicationTest", "testMultiplication"),
        )
      }
    }
  }
}
