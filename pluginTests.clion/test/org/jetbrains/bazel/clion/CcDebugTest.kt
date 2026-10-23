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
import com.intellij.xdebugger.XDebuggerAssertions
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGdbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionLldbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.GdbDebugProfileState
import com.jetbrains.cidr.cpp.execution.debugger.backend.LldbDebugProfileState
import com.jetbrains.cidr.execution.CidrExecutionFixture
import com.jetbrains.cidr.execution.debugger.CidrDebuggingFixture.DebuggerState
import com.jetbrains.cidr.execution.debugger.toggleBreakpoint
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.assertions.findTarget
import org.jetbrains.bazel.clion.run.CcDebugRunner
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.fixtures.CcTestApplication
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

// the sum of the values that the library functions return
private const val EXPECTED_EXIT_CODE = 60 + 61 + 62 + 64

@ExtendWith(CidrCloseDebugSessionsExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class CcDebugTest(debugProfileTypeId: String, debugProfileSettings: BaseState) {

  @EnabledOnOs(OS.LINUX)
  @CcTestApplication
  class Gdb : CcDebugTest(CLionGdbDebugProfileType.ID, GdbDebugProfileState())

  @EnabledOnOs(OS.LINUX)
  @CcTestApplication
  class Lldb : CcDebugTest(CLionLldbDebugProfileType.ID, LldbDebugProfileState())

  private val projectFixture = clionBazelProjectFixture("clion/debug", buildProject = true, bazelVersion = BazelVersions.BAZEL_9) {
    addBuildFlags("--compilation_mode=dbg")
  }

  @Suppress("unused")
  private val debugProfile = debugProfileFixture(projectFixture, debugProfileTypeId, debugProfileSettings)

  private val project by projectFixture

  @Test
  fun testRunsToCompletion(): Unit = timeoutRunBlocking {
    val context = startCcDebugSession("//main:main")

    assertTerminates(context.process.processHandler)
  }

  @Test
  fun testBreakpointInDefaultLibrary(): Unit = timeoutRunBlocking {
    assertStopsAt("lib/default/impl.cc", "return DEFAULT_LIB_VALUE;")
  }

  @Test
  fun testBreakpointInHeaderOnlyLibrary(): Unit = timeoutRunBlocking {
    assertStopsAt("lib/hdronly/hdr.h", "return HDRONLY_LIB_VALUE;")
  }

  @Test
  fun testBreakpointInExternalLibrary(): Unit = timeoutRunBlocking {
    assertStopsAt("lib/external/impl.cc", "return EXTERNAL_LIB_VALUE;")
  }

  private suspend fun assertStopsAt(relativePath: String, lineText: String) {
    val file = project.rootDir.findFileByRelativePath(relativePath).assertNotNull()

    val line = file.readText().lines().indexOfFirst { it.contains(lineText) }
    assertThat(line).isGreaterThanOrEqualTo(0)

    toggleBreakpoint(project, file, line).assertNotNull()

    val context = startCcDebugSession("//main:main")
    context.waitFor(DebuggerState.PAUSED)

    XDebuggerAssertions.assertCurrentPosition(context.process.session, file, line)

    context.process.session.resume()
    assertTerminates(context.process.processHandler)
  }

  private fun assertTerminates(handler: ProcessHandler) {
    assertThat(CidrExecutionFixture.waitFor(handler)).isTrue()
    assertThat(handler.exitCode).isEqualTo(EXPECTED_EXIT_CODE)
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
