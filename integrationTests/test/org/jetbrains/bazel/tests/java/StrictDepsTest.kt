package org.jetbrains.bazel.tests.java

import com.intellij.driver.sdk.getHighlights
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.Keys
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.pressKey
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.openFile
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

internal class StrictDepsTest : IdeStarterBaseProjectTest() {
  @Test
  // https://youtrack.jetbrains.com/issue/BAZEL-2695
  fun `test protobuf strict deps for Java`() {
    // TODO
    // Add test for scala in this project

    createContext("strictDepsTest", IdeaBazelCases.ProtobufStrictDepsTest)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject(buildAndSync = true)
          execute { waitForSmartMode() }
          waitForIndicators(5.minutes)

          step("Open Main.java and check there is red code") {
            openFile("appj/Main.java")
            takeScreenshot("main-java")
            codeEditor {
              val errors = getHighlights(editor.getDocument())
                .filter { it.getSeverity().getName() == "ERROR" }
                .isNotEmpty()
              errors shouldBeEqual true
            }
          }

          step("Uncomment dependency") {
            openFile("appj/BUILD.bazel")
            execute { goto(10, 2) }
            execute { pressKey(Keys.BACKSPACE) }
          }

          syncBazelProject(buildAndSync = true)
          step("Open Main.java and check no red code") {
            openFile("appj/Main.java")
            takeScreenshot("main-java-2")
            execute { checkOnRedCode() }
          }
        }
      }
  }
}
