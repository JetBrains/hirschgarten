package org.jetbrains.bazel.golang.resolve.golandSync

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.driver.execute
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
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
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/xuansontrinh/bazel-test-projects-by-languages.git",
        commitHash = "59aa72ad42c212b079633e658fdb51fbe82c5e70",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("go/with_go_source") },
        isReusable = true,
        configureProjectBeforeUse = { context -> configureProjectBeforeUse(context, createProjectView = false) },
      )

  override fun createContext(): IDETestContext =
    super.createContext().applyVMOptionsPatch {
      addSystemProperty(BazelFeatureFlags.BUILD_PROJECT_ON_SYNC, "true")
      addSystemProperty(BazelFeatureFlags.GO_SUPPORT, "true")
    }

  @Test
  fun checkNoRedCode() {
    createContext()
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)
          step("Check for red code in files") {
            FILES_TO_CHECK_FOR_RED_CODE.forEach {
              checkForRedCodeInFile(it)
              wait(4.seconds)
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
