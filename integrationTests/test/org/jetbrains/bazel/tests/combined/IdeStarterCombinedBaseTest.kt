package org.jetbrains.bazel.tests.combined

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertSyncSucceeded
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
abstract class IdeStarterCombinedBaseTest : IdeStarterBaseProjectTest() {
  protected lateinit var bgRun: BackgroundRun
  protected lateinit var ctx: IDETestContext

  abstract fun createContext(): IDETestContext

  protected open fun Driver.syncBazelProject() {
    syncBazelProject(buildAndSync = false)
  }

  @BeforeAll
  protected fun startIdeAndSync() {
    ctx = createContext()
    bgRun = ctx.runIdeWithDriver(runTimeout = timeout)
    withDriver(bgRun) {
      ideFrame {
        syncBazelProject()
        waitForIndicators(5.minutes)
        assertSyncSucceeded()
      }
    }
  }

  @BeforeEach
  protected fun skipIfCriticalFailed() = Assumptions.assumeFalse(criticalProblemOccurred)

  @AfterEach
  protected fun checkIdeState() {
    if (!criticalProblemOccurred && ::bgRun.isInitialized && !bgRun.driver.isConnected) {
      criticalProblemOccurred = true
    }
  }

  @AfterAll
  protected fun closeIde() {
    if (::bgRun.isInitialized) bgRun.closeIdeAndWait()
    if (::ctx.isInitialized) checkIdeaLogForExceptions(ctx)
  }
}
