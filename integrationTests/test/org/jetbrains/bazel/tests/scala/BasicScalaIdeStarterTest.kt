package org.jetbrains.bazel.tests.scala

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.assertSyncedTargets
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.waitForSyncSucceeded
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

internal class BasicScalaIdeStarterTest : IdeStarterBaseProjectTest() {

  @Test
  fun `pure scala projects should sync with bazel`() {
    createContext("basicScalaTest", IdeaBazelCases.SimpleScalaTest)
      .runIdeWithDriver(runTimeout = timeout) { withScreenRecording() }
      .useDriverAndCloseIde {
        ideFrame {
          step("Sync pure scala project") {
            syncBazelProject()
            waitForIndicators(5.minutes)
            waitForSyncSucceeded()
            takeScreenshot("afterSync")
            execute { assertSyncedTargets("//src/main/com/example/foo:example-lib", "//src/test/com/example/foo:test") }
          }
        }
      }
  }
}
