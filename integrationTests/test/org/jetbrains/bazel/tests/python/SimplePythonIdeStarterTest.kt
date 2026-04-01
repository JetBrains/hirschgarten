package org.jetbrains.bazel.tests.python

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.goToDeclaration
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.data.PyCharmBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProjectCloseDialog
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:SimplePythonTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class SimplePythonIdeStarterTest : IdeStarterBaseProjectTest() {

  @Test
  fun `Python files should have no red code after sync`() {
    createContext("simplePython", PyCharmBazelCases.SimplePython)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProjectCloseDialog()
          waitForIndicators(10.minutes)

          step("check no red node in main/main.py") {
            execute { openFile("main/main.py") }
            execute { checkOnRedCode() }
            takeScreenshot("main.py")
          }
          step("check no red node in lib/libA/src/bkng/lib/aaa/hello.py") {
            execute { openFile("lib/libA/src/bkng/lib/aaa/hello.py") }
            execute { checkOnRedCode() }
            takeScreenshot("main.py")
          }
          step("Check withImports/main.py") {
            execute { openFile("withImports/main.py") }
            takeScreenshot("withImports")
            execute { checkOnRedCode() }

            execute { goto(4,6)}
            takeScreenshot("beforeBuildNavigation")
            execute { goToDeclaration() }
            takeScreenshot("afterBuildNavigation")
            execute { assertCurrentFileDirectory("foo/foo.py")}
          }
        }
      }
  }

  private fun <T : CommandChain> T.assertCurrentFileDirectory(qualifiedName: String): T {
    addCommand("${CMD_PREFIX}assertCurrentFileDirectory $qualifiedName")
    return this
  }

}
