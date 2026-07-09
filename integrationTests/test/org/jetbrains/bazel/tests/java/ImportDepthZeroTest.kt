package org.jetbrains.bazel.tests.java

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.Keys
import com.intellij.tools.ide.performanceTesting.commands.acceptDecompileNotice
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.pressKey
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.bazelClean
import org.jetbrains.bazel.base.buildAndSync
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.navigateToFile
import org.jetbrains.bazel.base.openFile
import org.jetbrains.bazel.base.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

internal class ImportDepthZeroTest : IdeStarterBaseProjectTest() {
  @Test
  fun `should import the first level of dependencies and jdeps`() {
    createContext("importDepthZeroTest", IdeaBazelCases.ImportDepthZeroTest)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject(buildAndSync = true)
          execute { waitForSmartMode() }
          waitForIndicators(5.minutes)

          step("Open A.java and check for red code") {
            openFile("src/main/com/example/a/A.java")
            execute { checkOnRedCode() }
          }

          step("Uncomment line with a reference to B (first-level dependency should already be imported)") {
            execute { goto(6, 11) }
            execute { pressKey(Keys.BACKSPACE) }
            execute { pressKey(Keys.BACKSPACE) }
            execute { checkOnRedCode() }
          }

          step("Check that first-level dependency to B navigates to a jar") {
            execute { acceptDecompileNotice() }
            execute { navigateToFile(6, 23, expectedFilename = "B.class", 10, 14) }
            openFile("src/main/com/example/a/A.java")
          }

          step("Uncomment line with reference to C (jdeps dependency, transitive)") {
            execute { goto(7, 11) }
            execute { pressKey(Keys.BACKSPACE) }
            execute { pressKey(Keys.BACKSPACE) }
          }

          step("Check that C is not imported (2nd level dependency)") {
            execute { navigateToFile(7, 9, expectedFilename = "A.java", 7, 9) }
          }

          step("Build and sync to import updated jdeps libraries") {
            execute { buildAndSync() }
            execute { waitForSmartMode() }
          }

          step("Check C is now resolved to a jdeps jar") {
            execute { navigateToFile(7, 9, expectedFilename = "C.class", 8, 14) }
            openFile("src/main/com/example/a/A.java")
          }

          step("bazel clean to check jdeps materialization (BAZEL-3117) then build&resync") {
            bazelClean()
            execute { buildAndSync() }
            execute { waitForSmartMode() }
          }

          step("Check C is still resolved to a jdeps jar") {
            execute { navigateToFile(7, 9, expectedFilename = "C.class", 8, 14) }
            openFile("src/main/com/example/a/A.java")
          }
        }
      }
  }
}
