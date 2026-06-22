package org.jetbrains.bazel.tests.golang

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.welcomeScreen
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.execute
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.TestCase
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.data.GoLandBazelCases
import org.jetbrains.bazel.data.GoPluginBazelCases
import org.jetbrains.bazel.ideStarter.bazelClean
import org.jetbrains.bazel.ideStarter.navigateToFile
import org.jetbrains.bazel.ideStarter.withBazelFeatureFlag
import org.jetbrains.bazel.tests.combined.IdeStarterCombinedBaseTest
import org.jetbrains.bazel.tests.sync.verifyNoSyncOnReopen
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

private val FILES_TO_CHECK_FOR_RED_CODE =
  listOf(
    "testa/testa.go",
    "testa/src.go",
    "testb/src.go",
    "testb/testb.go",
    "testb/testb_test.go",
  )

private const val GO_LINTER_PLUGIN_ID = "com.ypwang.plugin.go-linter"

@Suppress("JUnitTestCaseWithNoTests")
class GolandSyncTest {
  @Nested
  inner class GoLand : GolandSyncBaseTest("golandSync", GoLandBazelCases.GolandSync)

  @Nested
  inner class GoPlugin : GolandSyncBaseTest("goPluginSync", GoPluginBazelCases.GoPluginSync)
}

abstract class GolandSyncBaseTest(private val projectName: String, private val case: TestCase<*>) : IdeStarterCombinedBaseTest() {
  override fun createContext(): IDETestContext =
    createContext(projectName, case)
      .withDisabledPlugins(setOf(GO_LINTER_PLUGIN_ID))
      .withBazelFeatureFlag(BazelFeatureFlags.BUILD_PROJECT_ON_SYNC)
      .withBazelFeatureFlag(BazelFeatureFlags.GO_SUPPORT)

  @Test
  @Order(1)
  fun `reopen without resync`() {
    withDriver(bgRun) {
      step("Close project") {
        invokeAction("CloseProject")
        takeScreenshot("afterClosingProject")
      }

      step("Reopen project from welcome screen") {
        if (case === GoLandBazelCases.GolandSync) {
          ideFrame { waitOneText("with_go_source").click() }
        }
        else {
          welcomeScreen { clickRecentProject("with_go_source") }
        }
        takeScreenshot("afterClickingRecentProject")
      }

      verifyNoSyncOnReopen()
    }
  }

  @Test
  @Order(Int.MAX_VALUE)  // because of bazel clean
  fun `check basic Go support functionality`() {
    withDriver(bgRun) {
      ideFrame {
        step("Open a source file and navigate from a trivial code reference to another source file in the workspace") {
          execute { it.openFile("testa/testa.go") }
          execute { it.navigateToFile(4, 11, "src.go", 7, 6) }
          takeScreenshot("navigateFromCodeReferenceToAnotherSourceFileInWorkspace")
        }

        step("Open a source file and navigate from a code reference to a generated source file outside the workspace") {
          execute { it.openFile("testa/testa.go") }
          execute { it.navigateToFile(5, 11, "gen.go", 7, 6) }
          takeScreenshot("navigateFromCodeReferenceToGeneratedSourceFileOutsideWorkspace")
        }

        step("Open a source file and navigate from the import reference to a BUILD file in the workspace") {
          execute { it.openFile("testa/src.go") }
          execute { it.navigateToFile(4, 64, "BUILD.bazel", 6, 12) }
          takeScreenshot("navigateFromImportReferenceToBuildFileInWorkspace")
        }

        step("Open a source file and navigate from the import reference to a BUILD file outside of the workspace") {
          execute { it.openFile("testb/testb_test.go") }
          execute { it.navigateToFile(6, 36, "BUILD.bazel", 4, 12) }
          takeScreenshot("navigateFromImportReferenceToBuildFileOutsideWorkspace")
        }

        step("Open a source file and navigate from the import reference to a source file outside of the workspace") {
          execute { it.openFile("testb/testb_test.go") }
          execute { it.navigateToFile(18, 10, "assertions.go", 813, 6) }
          takeScreenshot("navigateFromImportReferenceToBuildFileOutsideWorkspace")
        }

        fun checkRedCode() {
          FILES_TO_CHECK_FOR_RED_CODE.forEach {
            step("Check for red code in file $it") {
              checkForRedCodeInFile(it)
              wait(1.seconds)
              val goSupportDisabledTexts = getAllTexts().map { uiText -> uiText.text }.filter { "Go support is disabled" in it }
              goSupportDisabledTexts shouldBe emptyList()
            }
          }
        }
        checkRedCode()
        bazelClean()
        checkRedCode()
      }
    }
  }

  fun Driver.checkForRedCodeInFile(relativePath: String) =
    execute {
      it
        .openFile(relativePath)
        .takeScreenshot("fromFile_${relativePath.replace("/", "").replace(".", "")}")
        .checkOnRedCode()
    }

  private fun IDETestContext.withDisabledPlugins(pluginIds: Set<String>): IDETestContext =
    also { pluginConfigurator.disablePlugins(pluginIds) }
}
