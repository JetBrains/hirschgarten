package org.jetbrains.bazel.tests.combined

import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.PyCharmBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProjectCloseDialog
import org.jetbrains.bazel.tests.python.pyCharmImportStatements
import org.jetbrains.bazel.tests.python.pyCharmRunLineMarkers
import org.jetbrains.bazel.tests.python.pyCharmTestLineMarkers
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
class PyCharmCombinedTest : IdeStarterBaseProjectTest() {
  private lateinit var bgRun: BackgroundRun
  private lateinit var ctx: IDETestContext

  @BeforeAll
  fun startIdeAndSync() {
    ctx = createContext("pyCharmCombined", PyCharmBazelCases.PyCharm)
    bgRun = ctx.runIdeWithDriver(runTimeout = timeout) { withScreenRecording() }
    withDriver(bgRun) {
      ideFrame {
        syncBazelProjectCloseDialog()
        waitForIndicators(10.minutes)
        execute { waitForSmartMode() }
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
  fun `Python run line markers should be available after sync`() = pyCharmRunLineMarkers(bgRun)

  @Test @Order(2)
  fun `Python test line markers should be available for test files`() = pyCharmTestLineMarkers(bgRun)

  @Test @Order(3)
  fun `Python import statements should resolve correctly`() = pyCharmImportStatements(bgRun)

  @AfterAll
  fun closeIde() {
    if (::bgRun.isInitialized) bgRun.closeIdeAndWait()
    if (::ctx.isInitialized) checkIdeaLogForExceptions(ctx)
  }
}
