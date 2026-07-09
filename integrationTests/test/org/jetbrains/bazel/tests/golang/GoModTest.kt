package org.jetbrains.bazel.tests.golang

import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.data.GoLandBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.buildAndSync
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal class GoModTest : IdeStarterBaseProjectTest() {
  @Test
  fun `should not download deps via go list and go mod download`() {
    createContext("goModTest", GoLandBazelCases.GoModTest)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          execute { buildAndSync() }
          waitForIndicators(10.minutes)

          step("Close project") {
            invokeAction("CloseProject")
            takeScreenshot("afterClosingProject")
          }

          step("Reopen project from welcome screen") {
            waitOneText("goModTest").click()
          }

          val start = TimeSource.Monotonic.markNow()
          while (TimeSource.Monotonic.markNow() - start < 20.seconds) {
            val unexpectedTexts = getAllTexts().filter {
              it.text.contains("Executing 'go mod download") ||
              it.text.contains("Executing 'go list")
            }
            unexpectedTexts shouldBe emptyList()
            wait(0.1.seconds)
          }
        }
      }
  }
}
