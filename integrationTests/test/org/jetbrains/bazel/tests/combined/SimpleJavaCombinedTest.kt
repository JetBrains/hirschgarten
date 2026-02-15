package org.jetbrains.bazel.tests.combined

import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.tests.bytecode_viewer.bytecodeViewer
import org.jetbrains.bazel.tests.server.bazelVersionUpdate
import org.jetbrains.bazel.tests.settings.projectViewOpen
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SimpleJavaCombinedTest : IdeStarterBaseProjectTest() {
  private lateinit var bgRun: BackgroundRun
  private lateinit var ctx: IDETestContext

  @BeforeAll
  fun startIdeAndSync() {
    ctx = createContext("simpleJavaCombined", IdeaBazelCases.SimpleJavaCombined)
      .applyVMOptionsPatch {
        addSystemProperty("expose.ui.hierarchy.url", "true")
      }
    bgRun = ctx.runIdeWithDriver(runTimeout = timeout) { withScreenRecording() }
    withDriver(bgRun) {
      ideFrame {
        syncBazelProject()
        waitForIndicators(5.minutes)
      }
    }
  }

  @BeforeEach
  fun skipIfCriticalFailed() = Assumptions.assumeFalse(criticalProblemOccurred)

  @AfterEach
  fun checkIdeState() {
    if (!criticalProblemOccurred && ::bgRun.isInitialized && !bgRun.driver.isConnected) {
      criticalProblemOccurred = true
    }
  }

  @Test @Order(1)
  fun `project view files should open automatically after sync`() = projectViewOpen(bgRun)

  @Test @Order(2)
  fun `bytecode viewer should display compiled class bytecode`() = bytecodeViewer(bgRun)

  @Test @Order(100)
  fun `update bazel version should not cause server to break`() = bazelVersionUpdate(bgRun)

  @AfterAll
  fun closeIde() {
    if (::bgRun.isInitialized) bgRun.closeIdeAndWait()
    if (::ctx.isInitialized) checkIdeaLogForExceptions(ctx)
  }
}
