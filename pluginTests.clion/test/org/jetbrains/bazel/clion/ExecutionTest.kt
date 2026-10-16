package org.jetbrains.bazel.clion

import com.intellij.clion.testFramework.nolang.junit5.debugger.CidrCloseDebugSessionsExtension
import com.intellij.clion.testFramework.nolang.junit5.debugger.debugProfileFixture
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.util.system.LowLevelLocalMachineAccess
import com.intellij.util.system.OS
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGdbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.GdbDebugProfileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.fixtures.BazelExecutionResult
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.fixtures.createBazelConfiguration
import org.jetbrains.bazel.fixtures.executeBazelConfiguration
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.state.HasProgramArguments
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

private const val ECHO_OUTPUT_MARKER = "ECHO_OUTPUT_FILE: "

@OptIn(LowLevelLocalMachineAccess::class)
@CcTestApplication
@ExtendWith(CidrCloseDebugSessionsExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
class ExecutionTest {

  private val projectFixture = clionBazelProjectFixture("clion/execution", buildProject = true, bazelVersion = BazelVersions.BAZEL_9) {
    if (OS.CURRENT == OS.Windows) {
      addBuildFlags("--cxxopt=/std:c++17")
    }
    else {
      addBuildFlags("--cxxopt=-std=c++17")
    }

    addBuildFlags("--compilation_mode=dbg")
    addBuildFlags("--test_output=streamed")
  }

  @Suppress("unused")
  private val debugProfile = debugProfileFixture(projectFixture, CLionGdbDebugProfileType.ID, GdbDebugProfileState())

  private val project by projectFixture

  @Test
  fun doTest(): Unit = timeoutRunBlocking(30.minutes) {
    checkRun(DefaultRunExecutor.EXECUTOR_ID)
    checkArgs(DefaultRunExecutor.EXECUTOR_ID)
    checkTest(DefaultRunExecutor.EXECUTOR_ID)

    //
    if (OS.CURRENT != OS.macOS) {
      checkRun(DefaultDebugExecutor.EXECUTOR_ID)
      checkArgs(DefaultDebugExecutor.EXECUTOR_ID)
      checkTest(DefaultDebugExecutor.EXECUTOR_ID)
    }
  }

  private suspend fun checkRun(executorId: String) {
    val echo = execute(Label.parse("//main:echo0"), executorId)
    echo.assertSuccess()

    val gtest = execute(Label.parse("//main:gtest"), executorId)
    gtest.assertSuccess()

    val catch2 = execute(Label.parse("//main:catch"), executorId)
    catch2.assertSuccess()
  }

  private suspend fun checkArgs(executorId: String) {
    assertThat(executeEcho("echo0", executorId, "'one argument with spaces'")).containsExactly(
      "one argument with spaces",
    )

    assertThat(executeEcho("echo0", executorId, "'one argument' 'another argument'")).containsExactly(
      "one argument",
      "another argument",
    )

    assertThat(executeEcho("echo1", executorId, "CONFIG_ARGUMENT")).containsExactly(
      "BUILD_FILE_STRING",
      "CONFIG_ARGUMENT",
    )

    assertThat(executeEcho("echo2", executorId, "CONFIG_ARGUMENT")).containsExactly(
      "main/echo.cc",
      "CONFIG_ARGUMENT",
    )
  }

  private suspend fun checkTest(executorId: String) {
    // filter gtest by suite name
    val gtestFiltered = execute(
      Label.parse("//main:gtest"), executorId,
      testFilter = "FilterSuite.*",
    )
    gtestFiltered.assertSuccess()
    assertThat(gtestFiltered.output).contains("FilterSuite.FilteredTest")
    assertThat(gtestFiltered.output).contains("FilterSuite.SkippedTest")
    assertThat(gtestFiltered.output).doesNotContain("SampleSuite.SampleTest")
    assertThat(gtestFiltered.output).doesNotContain("SampleSuite.AnotherTest")

    // filter gtest by specific test
    val gtestSingleTest = execute(
      Label.parse("//main:gtest"), executorId,
      testFilter = "FilterSuite.FilteredTest",
    )
    gtestSingleTest.assertSuccess()
    assertThat(gtestSingleTest.output).contains("FilterSuite.FilteredTest")
    assertThat(gtestSingleTest.output).doesNotContain("FilterSuite.SkippedTest")

    // filter catch2 tests
    val catchFiltered = execute(
      Label.parse("//main:catch"), executorId,
      testFilter = "FilteredTest",
    )
    catchFiltered.assertSuccess()
    assertThat(catchFiltered.output).contains("FilteredTest")
    assertThat(catchFiltered.output).doesNotContain("SkippedTest")
    assertThat(catchFiltered.output).doesNotContain("Test0")

    // arguments for the test binary must be passed as --test_arg
    val gtestArgs = execute(
      Label.parse("//main:gtest"), executorId,
      args = listOf("--gtest_filter=FilterSuite.*"),
    )
    gtestArgs.assertSuccess()
    assertThat(gtestArgs.output).contains("FilterSuite.FilteredTest")
    assertThat(gtestArgs.output).doesNotContain("SampleSuite.SampleTest")
  }

  private suspend fun executeEcho(target: String, executorId: String, args: String): List<String> {
    val result = execute(Label.parse("//main:$target"), executorId, args = listOf(args))
    result.assertSuccess()

    val line = result.output.lines().firstOrNull { it.startsWith(ECHO_OUTPUT_MARKER) }
    requireNotNull(line)

    val path = Path.of(line.substring(ECHO_OUTPUT_MARKER.length))
    assertThat(Files.exists(path)).isTrue()

    return withContext(Dispatchers.IO) { Files.readAllLines(path) }
  }

  private suspend fun execute(
    label: Label,
    executorId: String,
    testFilter: String? = null,
    args: List<String> = emptyList(),
  ): BazelExecutionResult {
    val executor = ExecutorRegistry.getInstance().getExecutorById(executorId)
    requireNotNull(executor) { "executor not found: $executorId" }

    val configuration = project.createBazelConfiguration(label)
    val state = configuration.handler.assertNotNull().state

    if (testFilter != null) {
      (state as HasTestFilter).testFilter = testFilter
    }
    if (args.isNotEmpty()) {
      (state as HasProgramArguments).programArguments = args.joinToString(" ")
    }

    return project.executeBazelConfiguration(configuration, executor)
  }
}
