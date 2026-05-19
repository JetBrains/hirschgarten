package org.jetbrains.bazel.tests.flow

import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.waitFor
import com.intellij.driver.sdk.waitForProjectOpen
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.isBazelProject
import org.jetbrains.bazel.ideStarter.projectRootDir
import org.jetbrains.bazel.ideStarter.singleProjectOrNull
import org.jetbrains.bazel.tests.ui.setAutoOpenProjectIfPresent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class BazelProjectOpenStarterTest : IdeStarterBaseProjectTest() {

  @Test
  fun `open project by root directory should resolve project name`() {
    val context = createContext("openBazelProjectByProjectRoot", IdeaBazelCases.BazelProjectOpenByRootDir)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure correct project is opened") {
          waitForProjectOpen()
          assertEquals("simpleKotlinTest", singleProject().getName())
          assertEquals("simpleKotlinTest", projectRootDir.getName())
        }
        step("Ensure project was opened as a Bazel project") {
          assertTrue(isBazelProject)
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `open project by MODULE file should resolve project name`() {
    val context = createContext("openBazelProjectByProjectModule", IdeaBazelCases.BazelProjectOpenByModuleFile)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure correct project is opened") {
          waitForProjectOpen()
          assertEquals("simpleKotlinTest", singleProject().getName())
          assertEquals("simpleKotlinTest", projectRootDir.getName())
        }
        step("Ensure project was opened as a Bazel project") {
          assertTrue(isBazelProject)
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `open legacy ijwb project should reopen with MODULE bazel`() {
    val context = createContext("openLegacyProject", IdeaBazelCases.BazelLegacyPluginProject)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure legacy project reopened with MODULE.bazel") {
          waitFor(timeout = 1.minutes, message = "Project not reopened with MODULE.bazel!") {
            singleProjectOrNull()?.getPresentableUrl()?.endsWith("MODULE.bazel") == true
          }
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `should not open directory as a bazel project when it contains a dot idea directory`() {
    val context = createContext("projectWithDotIdeaDir", IdeaBazelCases.BazelProjectWithDotIdeaDirectory)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure correct project was opened") {
          waitForProjectOpen()
          assertEquals("simpleJavaTest", singleProject().getName())
        }
        step("Ensure that project is not a Bazel project") {
          assertFalse(isBazelProject)
        }
      }
    checkIdeaLogForExceptions(context)
  }

  @Test
  fun `should open directory as a bazel project when it contains a dot idea directory and registry flag is enabled`() {
    val context = createContext("projectWithDotIdeaDir", IdeaBazelCases.BazelProjectWithDotIdeaDirectory)
    context
      .setAutoOpenProjectIfPresent(true)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure correct project was opened") {
          waitForProjectOpen()
          assertEquals("simpleJavaTest", singleProject().getName())
          assertEquals("simpleJavaTest", projectRootDir.getName())
        }
        step("Ensure project was opened as a Bazel project") {
          assertTrue(isBazelProject)
        }
      }
    checkIdeaLogForExceptions(context)
  }
}
