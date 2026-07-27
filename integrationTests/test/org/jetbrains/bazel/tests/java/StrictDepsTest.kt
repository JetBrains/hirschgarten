package org.jetbrains.bazel.tests.java

import com.intellij.driver.sdk.ProjectManager
import com.intellij.driver.sdk.getHighlights
import com.intellij.driver.sdk.loadPluginDynamically
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.installAndEnable
import com.intellij.tools.ide.performanceTesting.commands.Keys
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.pressKey
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.IdeStarterOs
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.jupiter.api.Test
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes

private val STRICT_DEPS_PROJECT = simpleBazelProject(
  revision = "5cbca8140ac85e5178ca803935fbd9e1d9400a07",
  path = "protobufStrictDepsTest",
  configureProject = { context ->
    BazelProjectConfigurer.configureProjectBeforeUse(
      context,
      createProjectView = false,
    )
    if (IdeStarterOs.current() == IdeStarterOs.WINDOWS) {
      // No scala toolchains are registered on Windows, so the sync scope is
      // restricted to the Java part of the fixture.
      (context.resolvedProjectHome / ".bazelproject").writeText(
        """
        directories:
          .

        derive_targets_from_directories: false

        targets:
          //appj:appj

        index_all_files_in_directories: true
        """.trimIndent() + "\n",
      )
    }
  },
)

internal class StrictDepsTest : IdeStarterBaseProjectTest() {
  @Test
  // https://youtrack.jetbrains.com/issue/BAZEL-2695
  fun `test protobuf strict deps for Java`() {
    // TODO
    // Add test for scala in this project

    createContext("strictDepsTest", IdeaBazelCases.withProject(STRICT_DEPS_PROJECT))
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
