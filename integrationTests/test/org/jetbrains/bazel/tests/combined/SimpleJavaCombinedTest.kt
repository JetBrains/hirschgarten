package org.jetbrains.bazel.tests.combined

import com.intellij.driver.client.Remote
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.UiText.Companion.asString
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.tools.ide.performanceTesting.commands.Keys
import com.intellij.tools.ide.performanceTesting.commands.build
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.createJavaFile
import com.intellij.tools.ide.performanceTesting.commands.delayType
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.gotoLine
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.pressKey
import com.intellij.tools.ide.performanceTesting.commands.reloadFiles
import com.intellij.tools.ide.performanceTesting.commands.replaceText
import com.intellij.tools.ide.performanceTesting.commands.saveDocumentsAndSettings
import com.intellij.tools.ide.performanceTesting.commands.sleep
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.assertFileKind
import org.jetbrains.bazel.ideStarter.bazelClean
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.waitForSyncSucceeded
import org.jetbrains.bazel.performanceImpl.FileKindCheck
import org.jetbrains.bazel.tests.ui.verifyAvailableRunGutterActions
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import com.intellij.ide.starter.driver.execute as sdkExecute

class SimpleJavaCombinedTest : IdeStarterCombinedBaseTest() {
  override fun createContext(): IDETestContext =
    createContext("simpleJavaCombined", IdeaBazelCases.SimpleJavaCombined)
      .applyVMOptionsPatch {
        addSystemProperty("expose.ui.hierarchy.url", "true")
      }

  @Test @Order(1)
  fun `project view files should open automatically after sync`() = projectViewOpen()

  @Test @Order(2)
  fun `bytecode viewer should display compiled class bytecode`() = bytecodeViewer()

  @Test @Order(3)
  fun `run gutter for test suite should contain several targets`() = runGutterTestSuite()

  @Test @Order(4)
  fun `build and sync should succeed`() = buildAndSyncTest()

  @Test @Order(5)
  fun `add new file should show it as unsynced`() = addUnsyncedFileTest()

  @Test @Order(100)
  fun `update bazel version should not cause server to break`() = bazelVersionUpdate()

  @Test @Order(101)
  fun `shard sync works`() = enableShardSync()

  @Test
  @Order(Integer.MAX_VALUE)
  fun `bazel clean shouldn't cause red code`() = bazelCleanTest()

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

  private fun buildAndSyncTest() {
    withDriver(bgRun) {
      ideFrame {
        step("Build and sync") {
          sdkExecute {
            it
              .buildAndSync()
              .waitForSmartMode()
              .takeScreenshot("afterBuildAndSync")
          }
        }
        waitForSyncSucceeded()
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
          waitForSyncSucceeded()
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
          verifyAvailableRunGutterActions(
            listOf(
              "Build Target",
              "Run 'Tests in '//:SimpleJavaTest''",
              "Run 'Tests in '//:SimpleJavaTest'' with Coverage",
              "Run '//:Simple2Test'",
              "Debug '//:Simple2Test'",
              "Run '//:Simple2Test' with Coverage",
              "Profile '//:Simple2Test' with 'IntelliJ Profiler'",
              "Run '//:SimpleTest'",
              "Debug '//:SimpleTest'",
              "Run '//:SimpleTest' with Coverage",
              "Profile '//:SimpleTest' with 'IntelliJ Profiler'",
            ),
          )
          popup().close()
        }
      }
    }
  }

  private fun addUnsyncedFileTest() {
    withDriver(bgRun) {
      ideFrame {
        execute { buildAndSync() }
        waitForIndicators(10.minutes)

        step("Create new java file Simple3Test.java and check if it is unsynced") {
          execute { createJavaFile("Simple3Test", "", "class") }
          Thread.sleep(5.seconds.inWholeMilliseconds) // Make BazelFileEventsListener do its job
          execute { assertFileKind("Simple3Test.java", FileKindCheck.SHOW_AS_UNSYNCED) }
        }

        step("Resync project and check if the file is synced") {
          sdkExecute {
            it
              .buildAndSync()
              .waitForSmartMode()
              .takeScreenshot("afterResync")
          }
          execute { assertFileKind("Simple3Test.java", FileKindCheck.SHOW_AS_SYNCED) }
        }
      }
    }
  }

  private fun bazelCleanTest() {
    withDriver(bgRun) {
      ideFrame {
        bazelClean()
        step("Open SimpleTest.java") {
          openFile("SimpleTest.java")
        }
        step("Assert no red code") {
          execute {
            checkOnRedCode()
          }
        }
      }
    }
  }

  private fun enableShardSync() {
    withDriver(bgRun) {
      ideFrame {
        step("Change projectview") {
          execute {openFile("projectview.bazelproject")}
          execute { gotoLine(3) }
          execute { delayType(150, "shard_sync: true") }
          execute { pressKey(Keys.ENTER) }
          execute { delayType(150, "target_shard_size: 1") }
          execute { pressKey(Keys.ENTER) }
          takeScreenshot("afterProjectViewChange")
          execute { buildAndSync() }
          waitForIndicators(10.minutes)
          takeScreenshot("afterShardSync")

          // Verify there are no reports about failure reading target info files
          val buildView = x { byType("com.intellij.build.BuildView") }
          waitFor(
            message = "Build console shouldn't indicate error reading targets",
            timeout = 10.seconds,
            getter = { buildView.getAllTexts().asString() },
            checker = { text: String -> "Could not read target info" !in text },
          )
        }
        step("sanity check after shard sync") {
          openFile("SimpleTest.java")
          takeScreenshot("sanityCheckAfterShardSync")
          execute { checkOnRedCode() }
        }
      }
    }
  }

}

@Remote("com.intellij.openapi.vfs.VirtualFileManager")
interface VirtualFileManager {
  fun asyncRefresh(): Long
}
