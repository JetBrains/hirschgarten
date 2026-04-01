package org.jetbrains.bazel.tests.python

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.idea.TestFor
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.data.PyCharmBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProjectCloseDialog
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:PythonProtobufTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
@TestFor(issues = ["BAZEL-3041", "BAZEL-2309"])
class PythonProtobufIdeStarterTest : IdeStarterBaseProjectTest() {

  @Test
  fun `Python files should have no red code after sync in ProtoBuf references`() {
    createContext("pythonProtobufTest", PyCharmBazelCases.PythonProtobufTest)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProjectCloseDialog()
          waitForIndicators(10.minutes)

          step("check no red node in hirschgarten_python_test/lib.py") {
            execute { openFile("hirschgarten_python_test/lib.py") }
            execute { checkOnRedCode() }
            takeScreenshot("lib.py")
          }
        }
      }
  }
}
