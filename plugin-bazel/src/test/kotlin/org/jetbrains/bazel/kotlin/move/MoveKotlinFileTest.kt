package org.jetbrains.bazel.kotlin.move

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.dialog
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.saveDocumentsAndSettings
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.assertFileContentsEqual
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/kotlin/move:moveKotlinFileTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class MoveKotlinFileTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
        commitHash = "7beddfa764a06bd5fcd4211e00c744847b7a0da9",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("moveFilesTest") },
        isReusable = true,
        configureProjectBeforeUse = { context -> configureProjectBeforeUseWithoutBazelClean(context) },
      )

  override fun createContext(): IDETestContext =
    super.createContext().applyVMOptionsPatch {
      skipRefactoringDialogs()
    }

  @Test
  fun openBazelProject() {
    val context = createContext()

    context.runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
      syncBazelProject()
      step("Create a new subpackage") {
        execute { createDirectory("subpackage") }
      }

      step("Move Class2.kt to subpackage") {
        execute { moveClass("Class2.kt", "subpackage") }
      }

      step("Close Add file to Git popup") {
        ideFrame {
          dialog {
            closeDialog()
          }
        }
      }

      execute { saveDocumentsAndSettings() }

      step("Check that Class2.kt is correct after move") {
        execute { assertFileContentsEqual("expected/Class2.kt", "subpackage/Class2.kt") }
      }

      step("Check that Class1.java is correct after move") {
        execute { assertFileContentsEqual("expected/Class1.java", "Class1.java") }
      }

      Thread.sleep(30000)
    }
  }
}

fun <T : CommandChain> T.createDirectory(directoryPath: String): T {
  addCommand(CMD_PREFIX + "createDirectory $directoryPath")
  return this
}

fun <T : CommandChain> T.moveClass(sourceFile: String, destinationDirectory: String): T {
  addCommand(CMD_PREFIX + "moveClass $sourceFile $destinationDirectory")
  return this
}
