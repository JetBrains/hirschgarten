package org.jetbrains.bazel.ui.testResultsTree

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.requireProject
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testResultsTree:runAllTestsActionTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class RunAllTestsActionTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "448f723b7b32c31821908a81fbe70fcf43aa48d4",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("runAllTests") },
        isReusable = false,
        configureProjectBeforeUse = ::configureProjectBeforeUse,
      )

  @Test
  fun openProject() {
    createContext().runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
      ideFrame {
        syncBazelProject()
        waitForIndicators(5.minutes)

        step("Right-click the root project directory") {
          // the root folder is presented "ProjectName ProjectPath" now in UI
          // not sure if it's intended but the test will work with it now
          val rootFolderName = "${requireProject().getName()} ${requireProject().getBasePath()}"

          projectView().projectViewTree.rightClickPath(rootFolderName)
          popupMenu().waitFound()
          takeScreenshot("afterRightClickingProjectRoot")
        }

        step("Click on Run all tests") {
          popupMenu().findMenuItemByText("Run all tests").click()
          waitForIndicators(5.minutes)
        }

        verifyTestStatus(
          listOf("2 tests passed", "2 tests total"),
          listOf("AdditionTest", "MultiplicationTest"),
        )
      }
    }
  }
}
