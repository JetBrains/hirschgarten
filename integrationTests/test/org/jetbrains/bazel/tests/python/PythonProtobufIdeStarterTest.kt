package org.jetbrains.bazel.tests.python

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.idea.TestFor
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.data.PyCharmBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.syncBazelProjectCloseDialog
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugins/bazel/integrationTests:integrationTests_test --test_env=JB_TEST_FILTER=org.jetbrains.bazel.tests.python.PythonProtobufIdeStarterTest --test_output=errors --nocache_test_results
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
