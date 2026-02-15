package org.jetbrains.bazel.tests.combined

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.setRegistry
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.welcomeScreen
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitFor
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import org.jetbrains.bazel.tests.ui.expandedTree
import org.jetbrains.bazel.tests.ui.verifyTestStatus
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.DebugStepTypes
import com.intellij.tools.ide.performanceTesting.commands.Keys
import com.intellij.tools.ide.performanceTesting.commands.build
import com.intellij.tools.ide.performanceTesting.commands.debugStep
import com.intellij.tools.ide.performanceTesting.commands.delayType
import com.intellij.tools.ide.performanceTesting.commands.deleteFile
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.pressKey
import com.intellij.tools.ide.performanceTesting.commands.reloadFiles
import com.intellij.tools.ide.performanceTesting.commands.setBreakpoint
import com.intellij.tools.ide.performanceTesting.commands.sleep
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertSyncSucceeded
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.syncBazelProject
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
import kotlin.time.Duration.Companion.seconds

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
  fun `test results tree should display passed tests correctly`() = testResultsTree()

  @Test @Order(2)
  fun `test results tree should display passed tests correctly with bazel runner`() = testResultsTreeWithBazelRunner()

  @Test @Order(50)
  fun `reopening project should not trigger resync`() = reopenWithoutResync()

  @Test @Order(51)
  fun `run line markers should persist after project reopen`() = runLineMarkerPersistence()

  @Test @Order(100)
  fun `resync should recover after deleting bazelbsp directory`() = recoverDotBazelBsp()

  @Test @Order(101)
  fun `hotswap should reload modified code during debug session`() = hotswap()

  @AfterAll
  fun closeIde() {
    if (::bgRun.isInitialized) bgRun.closeIdeAndWait()
    if (::ctx.isInitialized) checkIdeaLogForExceptions(ctx)
  }

  private fun testResultsTree() {
    withDriver(bgRun) {
      ideFrame {
        step("Open SimpleKotlinTest.kt and run test") {
          execute { openFile("SimpleKotlinTest.kt") }
          execute { runSimpleKotlinTest() }
          takeScreenshot("afterRunningSimpleKotlinTest")
        }
        step("Verify test status and results tree") {
          verifyTestStatus(
            listOf("2 tests passed"),
            listOf("SimpleKotlinTest", "trivial test()", "another trivial test()"),
          )
          takeScreenshot("afterOpeningTestResultsTree")
        }

        step("Launch debug run config for SimpleKotlinTest") {
          editorTabs()
            .gutter()
            .getGutterIcons()
            .first()
            .click()
          popup().waitOneContainsText("Debug test").click()
          wait(15.seconds)
        }
        step("Verify debug test status and results tree") {
          verifyTestStatus(
            listOf("2 tests passed"),
            listOf("SimpleKotlinTest", "trivial test()", "another trivial test()"),
          )
          takeScreenshot("afterOpeningDebugTestResultsTree")
        }
      }
    }
  }

  private fun testResultsTreeWithBazelRunner() {
    expandedTree = true
    withDriver(bgRun) {
      setRegistry("bazel.run.config.run.with.bazel", true.toString())
      try {
        ideFrame {
          step("Run test with bazel runner") {
            execute { openFile("SimpleKotlinTest.kt") }
            execute { runSimpleKotlinTest() }
            takeScreenshot("afterRunningSimpleKotlinTestBazelRunner")
          }
          step("Verify test status and results tree") {
            verifyTestStatus(
              listOf("2 tests passed"),
              listOf("SimpleKotlinTest", "trivial test()", "another trivial test()"),
            )
            takeScreenshot("afterTestResultsTreeBazelRunner")
          }

          step("Run same test again to verify caching (proves Bazel runner is active)") {
            execute { openFile("SimpleKotlinTest.kt") }
            execute { runSimpleKotlinTest() }
            takeScreenshot("afterRerunSimpleKotlinTestBazelRunner")
          }
          step("Verify cached results (only Bazel runner produces cached suffix)") {
            verifyTestStatus(
              listOf("2 tests passed"),
              listOf("SimpleKotlinTest", "trivial test() (cached)", "another trivial test() (cached)"),
            )
            takeScreenshot("afterCachedTestResultsTreeBazelRunner")
          }
        }
      } finally {
        setRegistry("bazel.run.config.run.with.bazel", false.toString())
      }
    }
  }

  private fun <T : CommandChain> T.runSimpleKotlinTest(): T {
    addCommand(CMD_PREFIX + "runSimpleKotlinTest")
    return this
  }

  private fun reopenWithoutResync() {
    withDriver(bgRun) {
      step("Close project") {
        invokeAction("CloseProject")
        takeScreenshot("afterClosingProject")
      }

      step("Reopen project from welcome screen") {
        welcomeScreen { clickRecentProject("simpleKotlinTest") }
        takeScreenshot("afterClickingRecentProject")
      }

      step("Verify no sync happens on reopen") {
        ideFrame {
          wait(20.seconds)
          try {
            val buildView = x { byType("com.intellij.build.BuildView") }
            assert(
              !buildView.getAllTexts().any {
                it.text.contains(BazelPluginBundle.message("console.task.sync.in.progress"))
              },
            ) { "Build view contains sync text" }
          } catch (e: Exception) {
            assert(e is WaitForException) { "Unknown exception: ${e.message}" }
          }
        }
        takeScreenshot("afterReopeningProject")
      }
    }
  }

  private fun runLineMarkerPersistence() {
    val fileName = "SimpleKotlinTest.kt"
    withDriver(bgRun) {
      verifyKotlinRunLineMarkerText(fileName)
      takeScreenshot("afterClickingOnRunLineMarker1")
      invokeAction("CloseProject")
      welcomeScreen { clickRecentProject("simpleKotlinTest") }
      verifyKotlinRunLineMarkerText(fileName)
      takeScreenshot("afterClickingOnRunLineMarker2")
    }
  }

  private fun Driver.verifyKotlinRunLineMarkerText(fileName: String) {
    ideFrame {
      syncBazelProject()
      waitForIndicators(5.minutes)

      openFile(fileName)
      val gutterIcons = editorTabs().gutter().getGutterIcons()
      val selectedGutterIcon = gutterIcons.first()
      selectedGutterIcon.click()
      val heavyWeightWindow = popup(xQuery { byClass("HeavyWeightWindow") })
      val texts = heavyWeightWindow.getAllTexts()
      assert(texts.any { it.text.contains("Test") })
    }
  }

  private fun recoverDotBazelBsp() {
    withDriver(bgRun) {
      step("Delete .bazelbsp directory") {
        execute { deleteFile(ctx.resolvedProjectHome.toString(), ".bazelbsp") }
      }

      step("Resync") {
        execute { buildAndSync() }
        execute { waitForSmartMode() }
        takeScreenshot("afterResync")
      }

      step("Check that the sync finishes successfully") {
        ideFrame {
          try {
            assertSyncSucceeded()
          } catch (e: Exception) {
            assert(e is WaitForException) { "Unknown exception: ${e.message}" }
          }
        }
      }
    }
  }

  private fun hotswap() {
    withDriver(bgRun) {
      ideFrame {
        step("Set breakpoints and start debug") {
          execute { openFile("SimpleKotlinTest.kt") }
          execute { setBreakpoint(line = 7, "SimpleKotlinTest.kt") }
          execute { setBreakpoint(line = 11, "SimpleKotlinTest.kt") }

          wait(10.seconds)
          editorTabs()
            .gutter()
            .getGutterIcons()
            .first { it.getIconPath().contains("run") }
            .click()
          popup().waitOneContainsText("Debug test").click()

          wait(30.seconds)

          takeScreenshot("afterSetBreakpointsAndStartDebugSession")
        }

        step("Modify code during debug session") {
          execute { openFile("SimpleKotlinTest.kt") }
          codeEditor().click()
          execute { goto(10, 35) }
          execute { pressKey(Keys.ENTER) }
          execute { delayType(delayMs = 150, text = "val a = 10") }
          execute { pressKey(Keys.ENTER) }
          execute { delayType(delayMs = 150, text = "val b = 20") }
          execute { pressKey(Keys.ENTER) }
          execute { delayType(delayMs = 150, text = "print(a + b)") }
          takeScreenshot("afterModifyCodeDuringDebugSession")
        }

        step("Apply hotswap and continue debugging") {
          execute { reloadFiles() }
          execute { build(listOf("SimpleKotlinTest")) }
          execute { sleep(5000) }
          execute { takeScreenshot("finishBuildAction") }
          execute { debugStep(DebugStepTypes.OVER) }
          execute { debugStep(DebugStepTypes.OVER) }
          execute { sleep(2000) }
          execute { takeScreenshot("afterHotSwapDebugStep") }
        }
      }
    }
  }
}
