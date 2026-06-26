package org.jetbrains.bazel.tests.golang

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.ide.IDETestContext
import org.jetbrains.bazel.data.GoLandBazelCases
import org.jetbrains.bazel.tests.combined.IdeStarterCombinedBaseTest
import org.jetbrains.bazel.tests.run.selectRunConfiguration
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class GoRunConfigurationsTest : IdeStarterCombinedBaseTest() {
  override fun createContext(): IDETestContext =
    createContext("goRunConfigurationsTest", GoLandBazelCases.GoRunConfigurationsTest)

  @Test
  @Order(1)
  fun `debugging main_binary has correct program arguments and env vars`() {
    withDriver(bgRun) {
      ideFrame {
        step("Select //:main_binary configuration") {
          selectRunConfiguration(targetText = "//:main_binary")
        }

        step("Run with debug") { x { byAccessibleName("Debug '//:main_binary'") }.click() }

        val consoleView = x { byClass("ConsoleViewImpl") }
        step("Wait for run config to finish") {
          consoleView.waitFound(timeout = 3.minutes)
          consoleView.waitContainsText("Execution finished", timeout = 3.minutes)
        }

        step("Check that env vars and program arguments are passed") {
          consoleView.waitContainsText("Program arguments: [example_arg=example_value another_arg=another_value]", timeout = 5.seconds)
          consoleView.waitContainsText("Environment variable EXAMPLE_ENV: EXAMPLE_VALUE", timeout = 5.seconds)
        }
      }
    }
  }
}
