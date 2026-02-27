package org.jetbrains.bazel.tests.combined

import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.tools.ide.performanceTesting.commands.build
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.gotoLine
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.reloadFiles
import com.intellij.tools.ide.performanceTesting.commands.replaceText
import com.intellij.tools.ide.performanceTesting.commands.saveDocumentsAndSettings
import com.intellij.tools.ide.performanceTesting.commands.sleep
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
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
import com.intellij.ide.starter.driver.execute as sdkExecute

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
    bgRun = ctx.runIdeWithDriver(runTimeout = timeout)
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
  fun `project view files should open automatically after sync`() = projectViewOpen()

  @Test @Order(2)
  fun `bytecode viewer should display compiled class bytecode`() = bytecodeViewer()

  @Test @Order(3)
  fun `run gutter for test suite should contain several targets`() = runGutterTestSuite()

  @Test @Order(100)
  fun `update bazel version should not cause server to break`() = bazelVersionUpdate()

  @AfterAll
  fun closeIde() {
    if (::bgRun.isInitialized) bgRun.closeIdeAndWait()
    if (::ctx.isInitialized) checkIdeaLogForExceptions(ctx)
  }

  private fun projectViewOpen() {
    withDriver(bgRun) {
      ideFrame {
        shouldBe("README.md should be opened") {
          editorTabs().isTabOpened("README.md")
        }
        shouldBe("projectview.bazelproject should be opened") {
          editorTabs().isTabOpened("projectview.bazelproject")
        }
      }
    }
  }

  private fun bytecodeViewer() {
    withDriver(bgRun) {
      ideFrame {
        execute { buildAndSync() }
        waitForIndicators(10.minutes)

        step("Build and view bytecode") {
          execute { reloadFiles() }
          execute { build() }
          execute { waitForIndicators(2.minutes) }
          execute { openFile("SimpleTest.java") }
          execute { sleep(5_000) }
          execute { build(listOf("SimpleJavaTest")) }
          execute { waitForIndicators(2.minutes) }
          execute { goto(5, 17) }
          execute { sleep(5_000) }
          invokeAction("BytecodeViewer")
          val buildView = x("//div[@class='BytecodeToolWindowPanel']")
          val bytecodeKeywords = setOf("ICONST_3", "ICONST_2", "INVOKESTATIC")
          waitFor(message = "Bytecode viewer to display expected keywords", timeout = 30.seconds, interval = 2.seconds) {
            val text = buildView.getAllTexts().joinToString { it.text }
            bytecodeKeywords.all { text.contains(it) }
          }
        }
      }
    }
  }

  private fun bazelVersionUpdate() {
    withDriver(bgRun) {
      ideFrame {
        step("Update bazel version") {
          openFile(".bazelversion")
          sdkExecute {
            it
              .gotoLine(1)
              .replaceText(0, 5, "8.3.1")
              .saveDocumentsAndSettings()
              .takeScreenshot("afterUpdateBazelVersion")
          }
        }

        step("Resync project and check if the sync is successful") {
          sdkExecute {
            it
              .buildAndSync()
              .waitForSmartMode()
              .takeScreenshot("afterResync")
          }
          assertSyncSucceeded()
        }
      }
    }
  }

  private fun runGutterTestSuite() {
    withDriver(bgRun) {
      ideFrame {
        step("Open BUILD file") {
          openFile("BUILD")
        }
        step("Check run gutters for test suite") {
          editorTabs()
            .gutter()
            .getGutterIcons()
            .first()
            .click()
          val expectedTexts = listOf(
            "Run all tests",
            "Test //:SimpleTest",
            "Test //:Simple2Test",
          )
          for (text in expectedTexts) {
            step("Checking that run gutter contains run config with name: $text") {
              popup().waitAnyTextsContains(text)
            }
          }
          popup().close()
        }
      }
    }
  }
}
