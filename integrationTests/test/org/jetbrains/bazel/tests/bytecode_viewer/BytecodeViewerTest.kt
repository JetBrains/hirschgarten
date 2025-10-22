package org.jetbrains.bazel.tests.bytecode_viewer

import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.driver.execute
import com.intellij.tools.ide.performanceTesting.commands.build
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.reloadFiles
import com.intellij.tools.ide.performanceTesting.commands.sleep
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/bytecode_viewer --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class BytecodeViewerTest : IdeStarterBaseProjectTest() {
  @Test
  fun testBytecodeViewer() {
    createContext("bytecode_viewer", IdeaBazelCases.BytecodeViewer)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Import Bazel project") {
          execute {
            it.buildAndSync()
              .waitForBazelSync()
              .waitForSmartMode()
              .takeScreenshot("afterImport")
          }
        }

        step("Resync project and check if the sync is successful") {
          execute { it.reloadFiles() }
          execute { it.build() }
          execute { it.openFile("SimpleTest.java") }
          execute { it.sleep(5_000) }
          execute { it.build(listOf("SimpleJavaTest")) }
          execute { it.goto(5, 17) }
          execute { it.sleep(5_000) }
          ideFrame {
            invokeAction("BytecodeViewer")
            wait(5.seconds)
            val buildView = x { byType("com.intellij.byteCodeViewer.BytecodeToolWindowPanel") }
            val text = buildView.getAllTexts().joinToString { it.text }

            // we just want any text resembling bytecode to show
            val bytecodeKeywords = setOf("ICONST_3", "ICONST_2", "INVOKESTATIC")
            assert(bytecodeKeywords.all { text.contains(it) })
          }
        }
      }
  }
}
