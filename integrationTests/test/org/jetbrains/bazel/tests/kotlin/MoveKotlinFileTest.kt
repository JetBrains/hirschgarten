package org.jetbrains.bazel.tests.kotlin

import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.dialog
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.saveDocumentsAndSettings
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.assertFileContentsEqual
import org.jetbrains.bazel.base.assertFileKind
import org.jetbrains.bazel.base.checkIdeaLogForExceptions
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.withBazelFeatureFlag
import org.jetbrains.bazel.performanceImpl.FileKindCheck
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * ```sh
 * bazel test //plugins/bazel/integrationTests:integrationTests_test --test_env=JB_TEST_FILTER=org.jetbrains.bazel.tests.kotlin.MoveKotlinFileTest --test_output=errors --nocache_test_results
 * ```
 */
class MoveKotlinFileTest : IdeStarterBaseProjectTest() {

  @Test
  fun `move Kotlin file to subpackage should update imports`() {
    val context = createContext("MoveFilesTest", IdeaBazelCases.MoveKotlinFile)
      .applyVMOptionsPatch {
        skipRefactoringDialogs()
      }
      .withBazelFeatureFlag(BazelFeatureFlags.MERGE_SOURCE_ROOTS, false)
    context
      .runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(10.minutes)
          step("Create a new subpackage") {
            execute { createDirectory("subpackage") }
          }

          // BazelFileEventListener has a processing delay on 250 milliseconds (PROCESSING_DELAY), but wait longer to be sure
          wait(5.seconds)

          step("Move Class2.kt to subpackage") {
            execute { moveClass("Class2.kt", "subpackage") }
          }

          step("Close Add file to Git popup if it appears") {
            try {
              dialog().waitFound(timeout = 10.seconds)
              dialog {
                closeDialog()
              }
            }
            catch (_: WaitForException) {
            }
          }

          execute { saveDocumentsAndSettings() }

          step("Check that Class2.kt is correct after move") {
            execute {
              assertFileContentsEqual("expected/Class2.kt", "subpackage/Class2.kt")
              assertFileKind("subpackage/Class2.kt", FileKindCheck.SHOW_AS_UNSYNCED)
            }
          }

          step("Check that Class1.java is correct after move") {
            execute {
              assertFileContentsEqual("expected/Class1.java", "Class1.java")
              assertFileKind("Class1.java", FileKindCheck.IN_WSM, FileKindCheck.IN_TARGETS)
            }
          }
        }
      }
    checkIdeaLogForExceptions(context)
  }
}

fun <T : CommandChain> T.createDirectory(directoryPath: String): T {
  addCommand(CMD_PREFIX + "createDirectory $directoryPath")
  return this
}

fun <T : CommandChain> T.moveClass(sourceFile: String, destinationDirectory: String): T {
  addCommand(CMD_PREFIX + "moveKtClass $sourceFile $destinationDirectory")
  return this
}
