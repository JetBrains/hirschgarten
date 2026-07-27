package org.jetbrains.bazel.tests.scala

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.assertSyncedTargets
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.waitForSyncSucceeded
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

private val BASIC_SCALA_PROJECT = simpleBazelProject(
  // TODO: temporary pin to SBPFT branch bazel/dan/e2e-os-bazel-matrix; repoint to main once the fixture upstreaming lands there
  revision = "e974ca77b97e65a329f03492f9b556e44f47f648",
  path = "simpleScalaTest",
)

internal class BasicScalaIdeStarterTest : IdeStarterBaseProjectTest() {

  @Test
  fun `pure scala projects should sync with bazel`() {
    createContext("basicScalaTest", IdeaBazelCases.withProject(BASIC_SCALA_PROJECT))
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
