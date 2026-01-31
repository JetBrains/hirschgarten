package org.jetbrains.bazel.tests.flow

import com.intellij.driver.sdk.step
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.exitApp
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.sleep
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * ```sh
 *  bazel test @//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/flow/starter:starter --jvmopt="-Dbazel.ide.starter.test.cache.directory=/private/var/tmp/_bazel_Jiaming.Tang/1f264e0d8fb7f76000aa401efbb89f09/sandbox/darwin-sandbox/2098/execroot/_main/bazel-out/darwin_arm64-fastbuild/bin/plugin-bazel/src/test/kotlin/org/jetbrains/bazel/flow/starter/starter.runfiles/_main" --sandbox_writable_path=/ --action_env=PATH --nocache_test_results
 * ```
 * For this test -Dbazel.ide.starter.test.cache.directory must NOT be hirschgarten root folder. It is because that we are testing the case that no MODULE.bazel / WORKSPACE file exists
 * If we use hirschgarten as parent folder, Bazel plugin will detect this as legal bazel repo in parent folder and open it.
 */
class BazelProjectOpenProcessorStarterTest : IdeStarterBaseProjectTest() {

  @Disabled("current project open behavior breaks this test")
  @Test
  fun `project open processor should handle Bazel project correctly`() {
    createContext("bazelProjectOpenProcessorStarter", IdeaBazelCases.BazelProjectOpenProcessorStarter)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Open file") {
          execute { openFile("BUILD") }
          execute { sleep(2000) }
        }
      }
  }
}

private fun <T : CommandChain> T.debugLocalJvmSimpleKotlinTest(): T {
  addCommand(CMD_PREFIX + "debugLocalJvmSimpleKotlinTest")
  return this
}
