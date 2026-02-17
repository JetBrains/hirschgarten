package org.jetbrains.bazel.tests.java

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.delayType
import com.intellij.tools.ide.performanceTesting.commands.goToDeclaration
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/java:CompiledSourceCodeInsideJarExcludeTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class CompiledSourceCodeInsideJarExcludeTest : IdeStarterBaseProjectTest() {
  @Test
  fun `navigation should resolve to source file not compiled jar`() {
    createContext("compiledSourceCodeInsideJarExclude", IdeaBazelCases.CompiledSourceCodeInsideJarExclude)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          execute { buildAndSync() }
          execute { waitForSmartMode() }
          waitForIndicators(5.minutes)

          step("Open Main.kt and check for red code") {
            execute { openFile("Main.kt") }
            execute { checkOnRedCode() }
            takeScreenshot("afterOpenMainKt")
          }

          step("Modify my_addition.kt function signature") {
            execute { openFile("my_addition.kt") }
            execute { goto(1, 36) }
            // Change the signature of a top-level Kotlin function
            execute { delayType(delayMs = 150, text = ", c: Int") }
            takeScreenshot("afterModifyMyAdditionKt")
          }

          step("Navigate to function and verify it's not inside jar") {
            execute { openFile("Main.kt") }
            // Navigate to that top-level function
            execute { goto(5, 5) }
            execute { goToDeclaration() }
            execute { checkOpenedFileNotInsideJar() }
            takeScreenshot("afterGoToDeclaration")
          }
        }
      }
  }
}

fun <T : CommandChain> T.checkOpenedFileNotInsideJar(): T {
  addCommand(CMD_PREFIX + "checkOpenedFileNotInsideJar")
  return this
}
