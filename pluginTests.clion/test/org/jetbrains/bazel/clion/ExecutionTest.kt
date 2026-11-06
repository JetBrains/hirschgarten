package org.jetbrains.bazel.clion

import com.intellij.clion.testFramework.nolang.junit5.debugger.debugProfileFixture
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.testFixture
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionLldbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.LldbDebugProfileState
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.fixtures.BazelExecutionResult
import org.jetbrains.bazel.fixtures.CcDebugTestApplication
import org.jetbrains.bazel.fixtures.EXECUTION_TIMEOUT
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.fixtures.createBazelConfiguration
import org.jetbrains.bazel.fixtures.executeBazelConfiguration
import org.jetbrains.bazel.isWindows
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.state.HasProgramArguments
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import java.nio.file.Files
import java.nio.file.Path

private const val ECHO_OUTPUT_MARKER = "ECHO_OUTPUT_FILE: "

@CcDebugTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ExecutionTest(private val executorId: String) {

  protected val projectFixture = clionBazelProjectFixture("clion/execution", buildProject = true, bazelVersion = BazelVersions.BAZEL_9) {
    if (isWindows()) {
      addBuildFlags("--cxxopt=/std:c++17")
    }
    else {
      addBuildFlags("--cxxopt=-std=c++17")
    }

    addBuildFlags("--compilation_mode=dbg")
    addBuildFlags("--test_output=streamed")
  }


  private val project by projectFixture

  class Default : ExecutionTest(DefaultRunExecutor.EXECUTOR_ID)

  @DisabledOnOs(OS.MAC, OS.WINDOWS)
  class Debug : ExecutionTest(DefaultDebugExecutor.EXECUTOR_ID) {

    @Suppress("unused")
    private val debugProfile = debugProfileFixture(projectFixture, CLionLldbDebugProfileType.ID, LldbDebugProfileState())

    @Suppress("unused")
    private val bundledLldb = testFixture {
      assertThat(LLDBDriverConfiguration.hasBundledLLDB()).withFailMessage("bundled LLDB not found").isTrue()
      initialized(Unit) {}
    }
  }

  @Test
  fun runEcho0Test() = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    execute(Label.parse("//main:echo0")).assertSuccess()
  }

  @Test
  fun runGtestTest() = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    execute(Label.parse("//main:gtest")).assertSuccess()
  }

  @Test
  fun runCatchTest() = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    execute(Label.parse("//main:catch")).assertSuccess()
  }

  @Test
  fun argsWithSpacesTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val result = executeEcho("echo0", "'one argument with spaces'")
    assertThat(result).containsExactly("one argument with spaces", )
  }

  @Test
  fun argsMultipleTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val result = executeEcho("echo0", "'one argument' 'another argument'")
    assertThat(result).containsExactly("one argument", "another argument")
  }

  @Test
  fun argsFromBuildFileTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val result = executeEcho("echo1", "CONFIG_ARGUMENT")
    assertThat(result).containsExactly("BUILD_FILE_STRING", "CONFIG_ARGUMENT")
  }

  @Test
  fun argsWithLocationExpansionTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val result = executeEcho("echo2", "CONFIG_ARGUMENT")
    assertThat(result).containsExactly("main/echo.cc", "CONFIG_ARGUMENT")
  }

  @Test
  fun testFilterGtestSuiteTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val result = execute(Label.parse("//main:gtest"), testFilter = "FilterSuite.*")
    result.assertSuccess()
    assertThat(result.output).contains("FilterSuite.FilteredTest")
    assertThat(result.output).contains("FilterSuite.SkippedTest")
    assertThat(result.output).doesNotContain("SampleSuite.SampleTest")
    assertThat(result.output).doesNotContain("SampleSuite.AnotherTest")
  }

  @Test
  fun testFilterGtestSingleTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val result = execute(Label.parse("//main:gtest"), testFilter = "FilterSuite.FilteredTest")
    result.assertSuccess()
    assertThat(result.output).contains("FilterSuite.FilteredTest")
    assertThat(result.output).doesNotContain("FilterSuite.SkippedTest")
  }

  @Test
  fun testFilterCatchTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val result = execute(Label.parse("//main:catch"), testFilter = "FilteredTest")
    result.assertSuccess()
    assertThat(result.output).contains("FilteredTest")
    assertThat(result.output).doesNotContain("SkippedTest")
    assertThat(result.output).doesNotContain("Test0")
  }

  @Test
  fun testArgsGtestTest(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    // arguments for the test binary must be passed as --test_arg
    val result = execute(Label.parse("//main:gtest"), args = listOf("--gtest_filter=FilterSuite.*"))
    result.assertSuccess()
    assertThat(result.output).contains("FilterSuite.FilteredTest")
    assertThat(result.output).doesNotContain("SampleSuite.SampleTest")
  }

  private suspend fun executeEcho(target: String, args: String): List<String> {
    val result = execute(Label.parse("//main:$target"), args = listOf(args))
    result.assertSuccess()

    val line = result.output.lines().firstOrNull { it.startsWith(ECHO_OUTPUT_MARKER) }
    requireNotNull(line)

    val path = Path.of(line.substring(ECHO_OUTPUT_MARKER.length))
    assertThat(Files.exists(path)).isTrue()

    return withContext(Dispatchers.IO) { Files.readAllLines(path) }
  }

  private suspend fun execute(label: Label, testFilter: String? = null, args: List<String> = emptyList()): BazelExecutionResult {
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
