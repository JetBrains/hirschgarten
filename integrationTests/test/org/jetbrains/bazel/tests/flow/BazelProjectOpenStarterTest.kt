package org.jetbrains.bazel.tests.flow

import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.waitForProjectOpen
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.data.resolvedBazelProjectHome
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.projectRootDir
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.io.path.exists

class BazelProjectOpenStarterTest : IdeStarterBaseProjectTest() {

  @Test
  fun `open project by root directory should resolve project name`() {
    createContext("openBazelProjectByProjectRoot", IdeaBazelCases.BazelProjectOpenByRootDir)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure correct project is opened") {
          waitForProjectOpen()
          assertEquals("simpleKotlinTest", singleProject().getName())
          assertEquals("simpleKotlinTest", projectRootDir.getName())
        }
      }
  }

  @Test
  fun `open project by MODULE file should resolve project name`() {
    createContext("openBazelProjectByProjectModule", IdeaBazelCases.BazelProjectOpenByModuleFile)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Ensure correct project is opened") {
          waitForProjectOpen()
          assertEquals("simpleKotlinTest", singleProject().getName())
          assertEquals("simpleKotlinTest", projectRootDir.getName())
        }
      }
  }

  @Test
  fun `open project with custom dot idea location`() {
    createContext("openBazelProjectWithCustomDotIdeaLocation", IdeaBazelCases.bazelCustomDotIdeaLocaiton(".bazel.idea"))
      .apply {
        runIdeWithDriver(runTimeout = timeout)
        .useDriverAndCloseIde {
          step("Open project with custom .idea location") {
            waitForProjectOpen()
          }
        }
        val customIdea = this.resolvedBazelProjectHome.resolve(".bazel.idea")
        assertTrue(customIdea.exists())
        assertTrue(customIdea.resolve("workspace.xml").exists())
      }
  }
}
