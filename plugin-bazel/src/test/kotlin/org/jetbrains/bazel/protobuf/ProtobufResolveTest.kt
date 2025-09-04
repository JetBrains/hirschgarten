package org.jetbrains.bazel.protobuf

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.tools.ide.performanceTesting.commands.checkOnRedCode
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/protobuf --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class ProtobufResolveTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/Krystian20857/simpleBazelProjectsForTesting.git",
        commitHash = "148ddeddd07cfc678db20e50d9c955d4d9bbbafb",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("protobufTest") },
        isReusable = true,
        configureProjectBeforeUse = ::configureProjectBeforeUseWithoutBazelClean,
      )

  private val context by lazy { createContext() }

  @Test
  fun testProfobufResolve() {
    context.pluginConfigurator.installPluginFromPluginManager("idea.plugin.protoeditor", context.ide)
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          execute { buildAndSync() }
          waitForIndicators(10.minutes)

          step("Check pure") {
            execute { openFile("pure/libA/lib_a.proto") }
            takeScreenshot("afterPureLibAProtoOpenFile")
            execute { checkOnRedCode() }

            execute { openFile("pure/libB/lib_b.proto") }
            takeScreenshot("afterPureLibBProtoOpenFile")
            execute { checkOnRedCode() }

            execute { openFile("pure/consumerJava/Main.java") }
            takeScreenshot("afterPureLibAConsumerJavaOpenFile")
            execute { checkOnRedCode() }
          }

          step("Check prefix") {
            execute { openFile("prefix/libA/lib_a.proto") }
            takeScreenshot("afterPrefixLibAProtoOpenFile")
            execute { checkOnRedCode() }

            execute { openFile("prefix/libB/lib_b.proto") }
            takeScreenshot("afterPrefixLibBProtoOpenFile")
            execute { checkOnRedCode() }

            execute { openFile("prefix/consumerJava/Main.java") }
            takeScreenshot("afterPrefixLibAConsumerJavaOpenFile")
            execute { checkOnRedCode() }
          }

          step("Check stripped1") {
            execute { openFile("stripped1/libA/src/package/lib_a.proto") }
            takeScreenshot("afterStripped1LibAProtoOpenFile")
            execute { checkOnRedCode() }

            execute { openFile("stripped1/libB/lib_b.proto") }
            takeScreenshot("afterStripped1LibBProtoOpenFile")
            execute { checkOnRedCode() }

            execute { openFile("stripped1/consumerJava/Main.java") }
            takeScreenshot("afterStripped1LibAConsumerJavaOpenFile")
            execute { checkOnRedCode() }
          }

          step("Check stripped2") {
            execute { openFile("stripped2/libA/src/package/lib_a.proto") }
            takeScreenshot("afterStripped2LibAProtoOpenFile")
            execute { checkOnRedCode() }

            execute { openFile("stripped2/libB/lib_b.proto") }
            takeScreenshot("afterStripped2LibBProtoOpenFile")
            execute { checkOnRedCode() }

            execute { openFile("stripped2/consumerJava/Main.java") }
            takeScreenshot("afterStripped2LibAConsumerJavaOpenFile")
            execute { checkOnRedCode() }
          }
        }
      }
  }
}
