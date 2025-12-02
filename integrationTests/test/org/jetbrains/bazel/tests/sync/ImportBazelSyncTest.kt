package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

class ImportBazelSyncTest : IdeStarterBaseProjectTest() {

  private fun getProjectInfoFromSystemProperties(): ProjectInfoSpec {
    val localProjectPath = System.getProperty("bazel.ide.starter.test.project.path")
    if (localProjectPath != null) {
      return LocalProjectInfo(
        projectDir = Path.of(localProjectPath),
        isReusable = true,
        configureProjectBeforeUse = BazelProjectConfigurer::configureProjectBeforeUse,
      )
    }
    val projectUrl = System.getProperty("bazel.ide.starter.test.project.url") ?: "https://github.com/JetBrains/hirschgarten.git"
    val commitHash = System.getProperty("bazel.ide.starter.test.commit.hash").orEmpty()
    val branchName = System.getProperty("bazel.ide.starter.test.branch.name") ?: "252"
    val projectHomeRelativePath: String? = System.getProperty("bazel.ide.starter.test.project.home.relative.path")

    return GitProjectInfo(
      repositoryUrl = projectUrl,
      commitHash = commitHash,
      branchName = branchName,
      projectHomeRelativePath = { if (projectHomeRelativePath != null) it.resolve(projectHomeRelativePath) else it },
      isReusable = true,
      configureProjectBeforeUse = BazelProjectConfigurer::configureProjectBeforeUse,
    )
  }

  @Test
  fun importBazelProject() {
    val context = createContext("bazel-sync", IdeaBazelCases.withProject(getProjectInfoFromSystemProperties()))
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(10.minutes)
        }
      }
  }
}
