package org.jetbrains.bazel.clion

import com.intellij.clion.testFramework.nolang.junit5.debugger.CidrCloseDebugSessionsExtension
import com.intellij.clion.testFramework.nolang.junit5.debugger.CidrDebugSessionContext
import com.intellij.clion.testFramework.nolang.junit5.debugger.debugProfileFixture
import com.intellij.clion.testFramework.nolang.junit5.debugger.startDebugSession
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.vfs.readText
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.testFixture
import com.intellij.xdebugger.XDebuggerAssertions
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGdbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionLldbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.GdbDebugProfileState
import com.jetbrains.cidr.cpp.execution.debugger.backend.LldbDebugProfileState
import com.jetbrains.cidr.execution.CidrExecutionFixture
import com.jetbrains.cidr.execution.debugger.CidrDebuggerPathManager
import com.jetbrains.cidr.execution.debugger.CidrDebuggingFixture.DebuggerState
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import com.jetbrains.cidr.execution.debugger.toggleBreakpoint
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.assertions.findTarget
import org.jetbrains.bazel.clion.run.CcDebugRunner
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.EXECUTION_TIMEOUT
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.bazelRunConfigurationFactory
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtendWith

// the sum of the values that the library functions return in //main:main
private const val EXPECTED_EXIT_CODE = 60 + 61 + 62 + 64

@CcTestApplication
@ExtendWith(CidrCloseDebugSessionsExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class CcDebugTest(debugProfileTypeId: String, debugProfileSettings: BaseState, bazelVersion: String) {
  abstract class Gdb(bazelVersion: String) : CcDebugTest(CLionGdbDebugProfileType.ID, GdbDebugProfileState(), bazelVersion) {
    @EnabledOnOs(OS.LINUX)
    class Bazel7 : Gdb(BazelVersions.BAZEL_7)

    @EnabledOnOs(OS.LINUX)
    class Bazel8 : Gdb(BazelVersions.BAZEL_8)

    @EnabledOnOs(OS.LINUX)
    class Bazel9 : Gdb(BazelVersions.BAZEL_9)

    @Suppress("unused")
    private val bundledGdb = testFixture {
      assertThat(CidrDebuggerPathManager.bundledGDBBinary).withFailMessage("bundled GDB not found").exists()
      initialized(Unit) {}
    }
  }

  abstract class Lldb(bazelVersion: String) : CcDebugTest(CLionLldbDebugProfileType.ID, LldbDebugProfileState(), bazelVersion) {
    @EnabledOnOs(OS.LINUX)
    class Bazel7 : Lldb(BazelVersions.BAZEL_7)

    @EnabledOnOs(OS.LINUX)
    class Bazel8 : Lldb(BazelVersions.BAZEL_8)

    @EnabledOnOs(OS.LINUX)
    class Bazel9 : Lldb(BazelVersions.BAZEL_9)

    @Suppress("unused")
    private val bundledLldb = testFixture {
      assertThat(LLDBDriverConfiguration.hasBundledLLDB()).withFailMessage("bundled LLDB not found").isTrue()
      initialized(Unit) {}
    }
  }

  private val projectFixture = clionBazelProjectFixture("clion/debug", buildProject = true, bazelVersion = bazelVersion) {
    addBuildFlags("--compilation_mode=dbg")
  }

  @Suppress("unused")
  private val debugProfile = debugProfileFixture(projectFixture, debugProfileTypeId, debugProfileSettings)

  private val project by projectFixture

  @Test
  fun testRunsToCompletion(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val context = startCcDebugSession("//main:main")

    assertTerminates(context.process.processHandler, EXPECTED_EXIT_CODE)
  }

  @Test
  fun testBreakpointInDefaultLibrary(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    assertStopsAt("lib/default/impl.cc", "return DEFAULT_LIB_VALUE;", "//main:main", EXPECTED_EXIT_CODE)
  }

  @Test
  fun testBreakpointInHeaderOnlyLibrary(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    assertStopsAt("lib/hdronly/hdr.h", "return HDRONLY_LIB_VALUE;", "//main:main", EXPECTED_EXIT_CODE)
  }

  @Test
  fun testBreakpointInExternalLibrary(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    assertStopsAt("lib/external/impl.cc", "return EXTERNAL_LIB_VALUE;", "//main:main", EXPECTED_EXIT_CODE)
  }

  @Test
  fun testTestRunsToCompletion(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val context = startCcDebugSession("//main:test")

    assertTerminates(context.process.processHandler, 0)
  }

  @Test
  fun testBreakpointInTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    assertStopsAt("main/test.cc", "REQUIRE(1 == 1);", "//main:test", 0)
  }

  private suspend fun assertStopsAt(relativePath: String, lineText: String, label: String, expectedExitCode: Int) {
    val file = project.rootDir.findFileByRelativePath(relativePath).assertNotNull()

    val line = file.readText().lines().indexOfFirst { it.contains(lineText) }
    assertThat(line).isGreaterThanOrEqualTo(0)

    toggleBreakpoint(project, file, line).assertNotNull()

    val context = startCcDebugSession(label)
    context.waitFor(DebuggerState.PAUSED)

    XDebuggerAssertions.assertCurrentPosition(context.process.session, file, line)

    context.process.session.resume()
    assertTerminates(context.process.processHandler, expectedExitCode)
  }

  private fun assertTerminates(handler: ProcessHandler, expectedExitCode: Int) {
    assertThat(CidrExecutionFixture.waitFor(handler)).isTrue()
    assertThat(handler.exitCode).isEqualTo(expectedExitCode)
  }

  private suspend fun startCcDebugSession(label: String): CidrDebugSessionContext {
    val labels = listOf(Label.parse(label))
    val targets = listOf(project.findTarget(label))

    val settings = RunManager.getInstance(project).createConfiguration(label, bazelRunConfigurationFactory)
    val executor = DefaultDebugExecutor.getDebugExecutorInstance()

    val configuration = settings.configuration as BazelRunConfiguration
    configuration.updateTargets(labels, RunHandlerProvider.getRunHandlerProvider(project, targets))

    val runner = ProgramRunner.getRunner(executor.id, configuration).assertNotNull()
    assertThat(runner).isInstanceOf(CcDebugRunner::class.java)

    val environment = ExecutionEnvironmentBuilder.create(executor, settings).runner(runner).build()
    return startDebugSession(project, environment, runner)
  }
}
