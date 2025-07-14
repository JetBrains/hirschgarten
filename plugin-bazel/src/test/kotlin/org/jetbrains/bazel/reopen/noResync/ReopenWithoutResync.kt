package org.jetbrains.bazel.reopen.noResync

import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.welcomeScreen
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The current state of the test only prevents the regression error on a cleanly imported project.
 *
 * However, this issue may also happen when migrating the workspace data (in `.idea/workspace.xml`) from one version of the plugin to another.
 * Refer to [BAZEL-1967](https://youtrack.jetbrains.com/issue/BAZEL-1967) for more details.
 * A more complex version of this test should be introduced to capture this case. e.g., importing the project with an older plugin version,
 * and reopening such a project with the current plugin version.
 */
class ReopenWithoutResync : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      com.intellij.ide.starter.project.GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "c88d3cb84fae0c8a42cd8ddd78306a40595ff764",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("simpleKotlinTest") },
        isReusable = false,
        configureProjectBeforeUse = ::configureProjectBeforeUse,
      )

  @Test
  fun openProject() {
    val commands =
      CommandChain()
        .takeScreenshot("startSync")

    createContext()
      .runIdeWithDriver(runTimeout = timeout, commands = commands)
      .useDriverAndCloseIde {
        step("Verify sync is in progress") {
          ideFrame {
            wait(20.seconds)
            val buildView = x { byType("com.intellij.build.BuildView") }
            assert(
              buildView.getAllTexts().any {
                it.text.contains(BazelPluginBundle.message("console.task.sync.in.progress"))
              },
            ) { "Build view does not contain sync text" }
          }
          takeScreenshot("whileSyncing")
        }

        step("Wait for sync to complete and close project") {
          waitForIndicators(10.minutes)
          invokeAction("CloseProject")
          takeScreenshot("afterClosingProject")
        }

        step("Reopen project from welcome screen") {
          // simulating reopening project
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
}
