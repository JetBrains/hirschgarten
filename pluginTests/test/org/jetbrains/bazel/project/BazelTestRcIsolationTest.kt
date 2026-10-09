package org.jetbrains.bazel.project

import com.intellij.openapi.Disposable
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.bazelrunner.BazelCommandExecutionDescriptor
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncher
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncherProvider
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.BazelTestCaches
import org.jetbrains.bazel.test.framework.installHostRcIsolation
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.nio.file.Path
import kotlin.io.path.readLines
import kotlin.io.path.writeText

@BazelTestApplication
internal class BazelTestRcIsolationTest {
  private val projectRoot by tempPathFixture()

  @Test
  fun `test settings come after the bazelrc of the test project`() {
    val bazelrc = projectRoot.resolve(".bazelrc")
    bazelrc.writeText("build --@rules_go//go/config:pure\n")

    val outputBase = projectRoot.resolve("output-base")
    BazelTestCaches.configureBazelCaches(projectRoot, "redcodes/go_strict_deps", outputBase)
    BazelTestCaches.configureBazelCaches(projectRoot, "redcodes/go_strict_deps", outputBase)

    val lines = bazelrc.readLines()
    assertThat(lines.first()).isEqualTo("build --@rules_go//go/config:pure")
    assertThat(lines).contains("build --java_runtime_version=remotejdk_21")
    assertThat(lines.filter { it.startsWith("# BEGIN ") }).hasSize(1)
  }

  @Test
  fun `Bazel commands ignore the rc files of the host`(@TestDisposable disposable: Disposable) {
    val commands = mutableListOf<List<String>>()
    val recordingProvider = object : BazelProcessLauncherProvider {
      override fun createBazelProcessLauncher(workspaceRoot: Path, parentEnvironment: Map<String, String>): BazelProcessLauncher =
        object : BazelProcessLauncher {
          override fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process {
            commands.add(executionDescriptor.command)
            return mock(Process::class.java)
          }
        }
    }
    BazelProcessLauncherProvider.ep.point.registerExtension(recordingProvider, disposable)
    // Two fixtures in one test install the isolation twice.
    installHostRcIsolation(disposable)
    installHostRcIsolation(disposable)

    BazelProcessLauncherProvider.getInstance()
      .createBazelProcessLauncher(projectRoot, emptyMap())
      .launchProcess(BazelCommandExecutionDescriptor(listOf("bazel", "--output_base=/out", "info"), enablePty = false))

    assertThat(commands.single()).containsExactly("bazel", "--nohome_rc", "--nosystem_rc", "--output_base=/out", "info")
  }
}
