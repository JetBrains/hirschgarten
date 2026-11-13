package org.jetbrains.bazel.jvm.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.run.task.MockBazelProcessHandler
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.test.assertEquals

@BazelTestApplication
class RunJvmCommandLineExitCodeTest {
  private val project by projectFixture()

  @Test
  fun `returns the exit code of the process`() = timeoutRunBlocking {
    val java = Path(System.getProperty("java.home"), "bin", "java").toString()
    val commandLine = GeneralCommandLine(java, "--unknown-bazel-plugin-test-option")

    val exitCode = runJvmCommandLine(commandLine, CompletableDeferred(), MockBazelProcessHandler(project)) {}

    assertEquals(1, exitCode)
  }
}
