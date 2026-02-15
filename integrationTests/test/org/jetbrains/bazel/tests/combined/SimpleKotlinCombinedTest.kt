package org.jetbrains.bazel.tests.combined

import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertSyncSucceeded
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.tests.hotswap.hotswap
import org.jetbrains.bazel.tests.reopen.reopenWithoutResync
import org.jetbrains.bazel.tests.sync.recoverDotBazelBsp
import org.jetbrains.bazel.tests.ui.runLineMarkerPersistence
import org.jetbrains.bazel.tests.ui.testResultsTree
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
class SimpleKotlinCombinedTest : IdeStarterBaseProjectTest() {
  private lateinit var bgRun: BackgroundRun
  private lateinit var ctx: IDETestContext

  @BeforeAll
  fun startIdeAndSync() {
    ctx = createContext("simpleKotlinCombined", IdeaBazelCases.SimpleKotlinCombined)
    bgRun = ctx.runIdeWithDriver(runTimeout = timeout) { withScreenRecording() }
    withDriver(bgRun) {
      ideFrame {
        syncBazelProject()
        waitForIndicators(5.minutes)
        assertSyncSucceeded()
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
  fun `test results tree should display passed tests correctly`() = testResultsTree(bgRun)

  @Test @Order(50)
  fun `reopening project should not trigger resync`() = reopenWithoutResync(bgRun)

  @Test @Order(51)
  fun `run line markers should persist after project reopen`() = runLineMarkerPersistence(bgRun)

  @Test @Order(100)
  fun `resync should recover after deleting bazelbsp directory`() = recoverDotBazelBsp(bgRun, ctx)

  @Test @Order(101)
  fun `hotswap should reload modified code during debug session`() = hotswap(bgRun)

  @AfterAll
  fun closeIde() {
    if (::bgRun.isInitialized) bgRun.closeIdeAndWait()
    if (::ctx.isInitialized) checkIdeaLogForExceptions(ctx)
  }
}
