package org.jetbrains.bazel.fixtures

import com.intellij.clion.testFramework.nolang.junit5.debugger.CidrDebugSessionContext
import com.intellij.clion.testFramework.nolang.junit5.debugger.debugProfileFixture
import com.intellij.clion.testFramework.nolang.junit5.debugger.startDebugSession
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.testFixture
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGdbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionLldbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.GdbDebugProfileState
import com.jetbrains.cidr.cpp.execution.debugger.backend.LldbDebugProfileState
import com.jetbrains.cidr.execution.debugger.CidrDebuggerPathManager
import com.jetbrains.cidr.execution.debugger.backend.dap.profile.DapDebugProfileType
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.assertions.findTarget
import org.jetbrains.bazel.clion.run.debug.CcDebugInfoCheck
import org.jetbrains.bazel.clion.run.debug.CcDebugInfoDialogTestHandler
import org.jetbrains.bazel.clion.run.debug.CcDebugRunner
import org.jetbrains.bazel.clion.run.debug.setCcDebugInfoDialogHandlerInTests
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoChoice
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.bazelRunConfigurationFactory
import java.io.File

/** A bundled debugger of the IDE and its debug profile type. */
sealed interface CcDebugger {

  val debugProfileTypeId: String

  fun createDebugProfileSettings(): BaseState

  fun isBundled(): Boolean

  object GDB : CcDebugger {

    override val debugProfileTypeId: String get() = CLionGdbDebugProfileType.ID

    override fun createDebugProfileSettings(): BaseState = GdbDebugProfileState()

    override fun isBundled(): Boolean = CidrDebuggerPathManager.bundledGDBBinary.exists()
  }

  object LLDB : CcDebugger {

    override val debugProfileTypeId: String get() = CLionLldbDebugProfileType.ID

    override fun createDebugProfileSettings(): BaseState = LldbDebugProfileState()

    override fun isBundled(): Boolean = LLDBDriverConfiguration.hasBundledLLDB()
  }

  /** The bundled GDB in its DAP mode. */
  object GDB_DAP : CcDebugger {

    override val debugProfileTypeId: String get() = DapDebugProfileType.ID

    override fun createDebugProfileSettings(): BaseState = DapDebugProfileType().createDefaultState().apply {
      debuggerExecutable = CidrDebuggerPathManager.bundledGDBBinary.path
      debuggerArguments = "--interpreter=dap"
    }

    override fun isBundled(): Boolean = GDB.isBundled()
  }

  /** The bundled `lldb-dap` binary (only available on Linux). */
  object LLDB_DAP : CcDebugger {

    override val debugProfileTypeId: String get() = DapDebugProfileType.ID

    override fun createDebugProfileSettings(): BaseState = DapDebugProfileType().createDefaultState().apply {
      debuggerExecutable = lldbDapBinary().path
    }

    override fun isBundled(): Boolean = lldbDapBinary().exists()

    private fun lldbDapBinary(): File = CidrDebuggerPathManager.getLLDBBinFile("bin/lldb-dap", null, true, "lldb")
  }

  /** The xcode `lldb-dap` binary. */
  object XCODE_DAP : CcDebugger {

    override val debugProfileTypeId: String get() = DapDebugProfileType.ID

    override fun createDebugProfileSettings(): BaseState = DapDebugProfileType().createDefaultState().apply {
      debuggerExecutable = "xcrun"
      debuggerArguments = "--run lldb-dap"
    }

    override fun isBundled(): Boolean = true
  }
}

/**
 * Makes the [debugger] the current debug profile of the project. Fails when
 * the current build does not bundle the [debugger].
 */
internal fun ccDebuggerFixture(projectFixture: TestFixture<Project>, debugger: CcDebugger): TestFixture<Unit> = testFixture {
  assertThat(debugger.isBundled()).withFailMessage("bundled $debugger not found").isTrue()
  debugProfileFixture(projectFixture, debugger.debugProfileTypeId, debugger.createDebugProfileSettings()).init()
  initialized(Unit) {}
}

/** Starts a debug session for the [label] with the [CcDebugRunner]. */
internal suspend fun Project.startCcDebugSession(label: String): CidrDebugSessionContext {
  val labels = listOf(Label.parse(label))
  val targets = listOf(findTarget(label).info)

  val settings = RunManager.getInstanceAsync(this).createConfiguration(label, bazelRunConfigurationFactory)
  val executor = DefaultDebugExecutor.getDebugExecutorInstance()

  val configuration = settings.configuration as BazelRunConfiguration
  configuration.updateTargets(labels, RunHandlerProvider.getRunHandlerProvider(this, targets))

  val runner = ProgramRunner.getRunner(executor.id, configuration).assertNotNull()
  assertThat(runner).isInstanceOf(CcDebugRunner::class.java)

  val environment = ExecutionEnvironmentBuilder.create(executor, settings).runner(runner).build()
  return startDebugSession(this, environment, runner)
}

/** Records each debug info dialog of the project and answers it with the [choice]. */
internal class CcDebugInfoDialogFixture(private val choice: CcDebugInfoChoice) : CcDebugInfoDialogTestHandler {

  data class Request(val result: CcDebugInfoCheck.Result, val canUseDbg: Boolean)

  private val requests = mutableListOf<Request>()

  override fun choose(result: CcDebugInfoCheck.Result, canUseDbg: Boolean): CcDebugInfoChoice {
    synchronized(requests) { requests += Request(result, canUseDbg) }
    return choice
  }

  /** Returns the requests since the last call and clears them. */
  fun takeRequests(): List<Request> = synchronized(requests) {
    requests.toList().also { requests.clear() }
  }
}

/** Answers the debug info dialog of the project with the [choice]. */
internal fun ccDebugInfoDialogFixture(
  projectFixture: TestFixture<Project>,
  choice: CcDebugInfoChoice,
): TestFixture<CcDebugInfoDialogFixture> = testFixture {
  val project = projectFixture.init()
  val recorder = CcDebugInfoDialogFixture(choice)

  setCcDebugInfoDialogHandlerInTests(project, recorder)

  initialized(recorder) {
    setCcDebugInfoDialogHandlerInTests(project, null)
  }
}
