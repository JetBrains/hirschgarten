package org.jetbrains.bazel.tests.flow

import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.waitForProjectOpen
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.projectRootDir
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BazelProjectOpenStarterTest : IdeStarterBaseProjectTest() {

  @Test
  fun `test open Bazel project by project root`() {
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
  fun `test open Bazel project by MODULE file`() {
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
}
