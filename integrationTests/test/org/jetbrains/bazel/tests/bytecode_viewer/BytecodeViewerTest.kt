package org.jetbrains.bazel.tests.bytecode_viewer

import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.build
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.reloadFiles
import com.intellij.tools.ide.performanceTesting.commands.sleep
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/bytecode_viewer --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */

class BytecodeViewerTest : IdeStarterBaseProjectTest() {
  @Test
  fun `bytecode viewer should display compiled class bytecode`() {
    createContext("bytecode_viewer", IdeaBazelCases.BytecodeViewer)
      .applyVMOptionsPatch {
        this.addSystemProperty("expose.ui.hierarchy.url", "true")
      }
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          execute { buildAndSync() }
          waitForIndicators(10.minutes)

          step("Resync project and check if the sync is successful") {
            execute { reloadFiles() }
            execute { build() }
            execute { waitForIndicators(2.minutes) }
            execute { openFile("SimpleTest.java") }
            execute { sleep(5_000) }
            execute { build(listOf("SimpleJavaTest")) }
            execute { waitForIndicators(2.minutes) }
            execute { goto(5, 17) }
            execute { sleep(5_000) }
            invokeAction("BytecodeViewer")
            wait(5.seconds)
            val buildView = x("//div[@class='BytecodeToolWindowPanel']")
            val text = buildView.getAllTexts().joinToString { it.text }

            // we just want any text resembling bytecode to show
            val bytecodeKeywords = setOf("ICONST_3", "ICONST_2", "INVOKESTATIC")
            assert(bytecodeKeywords.all { text.contains(it) })
          }
        }
      }
  }
}
