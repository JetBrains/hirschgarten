package org.jetbrains.bazel.clion

import com.intellij.clion.testFramework.nolang.junit5.debugger.CidrCloseDebugSessionsExtension
import com.intellij.clion.testFramework.nolang.junit5.debugger.CidrDebugSessionContext
import com.intellij.clion.testFramework.nolang.junit5.debugger.debugProfileFixture
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.vfs.readText
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.common.waitUntil
import com.intellij.testFramework.junit5.fixture.testFixture
import com.intellij.xdebugger.XDebuggerAssertions
import com.intellij.xdebugger.impl.XDebugSessionImpl
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
import org.jetbrains.bazel.clion.run.debug.CcDebugInfoChoice
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.EXECUTION_TIMEOUT
import org.jetbrains.bazel.fixtures.ccDebugInfoDialogFixture
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.fixtures.startCcDebugSession
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

  private val debugInfoDialogFixture = ccDebugInfoDialogFixture(projectFixture, choice = CcDebugInfoChoice.DEBUG_ANYWAY)

  private val project by projectFixture
  private val debugInfoDialog by debugInfoDialogFixture

  @Test
  fun testRunsToCompletion(): Unit = timeoutRunBlocking {
    val context = startCcDebugSession("//main:main")

    assertTerminates(context.process.processHandler, EXPECTED_EXIT_CODE)
  }

  @Test
  fun testBreakpointInDefaultLibrary(): Unit = timeoutRunBlocking {
    assertStopsAt("lib/default/impl.cc", "return DEFAULT_LIB_VALUE;", "//main:main", EXPECTED_EXIT_CODE)
  }

  @Test
  fun testBreakpointInHeaderOnlyLibrary(): Unit = timeoutRunBlocking {
    assertStopsAt("lib/hdronly/hdr.h", "return HDRONLY_LIB_VALUE;", "//main:main", EXPECTED_EXIT_CODE)
  }

  @Test
  fun testBreakpointInExternalLibrary(): Unit = timeoutRunBlocking {
    assertStopsAt("lib/external/impl.cc", "return EXTERNAL_LIB_VALUE;", "//main:main", EXPECTED_EXIT_CODE)
  }

  @Test
  fun testTestRunsToCompletion(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val context = startCcDebugSession("//main:test")

    assertTerminates(context.process.processHandler, 0)

    val testTree = (context.process.session.consoleView as SMTRunnerConsoleView).resultsViewer.testsRootNode
    waitUntil("the test tree is complete") { testTree.children.isNotEmpty() && !testTree.isInProgress }
    assertThat(testTree.allTests.filter { it.isLeaf }.map { it.name }).containsExactly("Test")
    assertThat((context.process.session as XDebugSessionImpl).restartActions).isNotEmpty()
  }

  @Test
  fun testBreakpointInTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    assertStopsAt("main/test.cc", "int debug = 1;", "//main:test", 0)
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
    val context = project.startCcDebugSession(label)

    // the project builds with dbg, thus every target has debug info
    assertThat(debugInfoDialog.takeRequests()).describedAs("the debug info dialog requests").isEmpty()

    return context
  }
}
