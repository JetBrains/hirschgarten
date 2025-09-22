package org.jetbrains.bazel.tests.golang

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.driver.execute
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.data.GoLandBazelCases
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.navigateToFile
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.jetbrains.bazel.ideStarter.withBazelFeatureFlag
import org.junit.jupiter.api.Test
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.time.Duration.Companion.seconds

private val FILES_TO_CHECK_FOR_RED_CODE =
  listOf(
    "testa/testa.go",
    "testa/src.go",
    "testb/src.go",
    "testb/testb.go",
    "testb/testb_test.go",
  )

/**
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/golang/resolve/golandSync --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 */
class GolandSync : IdeStarterBaseProjectTest() {
  @Test
  fun `check basic Go support functionality`() {
    createContext("golandSync", GoLandBazelCases.GolandSync)
      .withBazelFeatureFlag(BazelFeatureFlags.BUILD_PROJECT_ON_SYNC)
      .withBazelFeatureFlag(BazelFeatureFlags.GO_SUPPORT)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          step("Sync project") {
            execute { it.waitForBazelSync().waitForSmartMode() }
          }

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
            execute { it.navigateToFile(4, 64, "BUILD.bazel", 5, 1) }
            takeScreenshot("navigateFromImportReferenceToBuildFileInWorkspace")
          }

          step("Open a source file and navigate from the import reference to a BUILD file outside of the workspace") {
            execute { it.openFile("testb/testb_test.go") }
            execute { it.navigateToFile(6, 36, "BUILD.bazel", 3, 1) }
            takeScreenshot("navigateFromImportReferenceToBuildFileOutsideWorkspace")
          }

          FILES_TO_CHECK_FOR_RED_CODE.forEach {
            step("Check for red code in file $it") {
              checkForRedCodeInFile(it)
              wait(1.seconds)
            }
          }
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
}
