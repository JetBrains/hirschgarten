package org.jetbrains.bazel.bazelrunner

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.commons.FileUtil
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.startup.FileUtilIntellij
import org.jetbrains.bazel.startup.IntellijSystemInfoProvider
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.readText

fun String.label() = Label.parse(this)

val mockContext =
  WorkspaceContext(
    targets =
      listOf(
        ExcludableValue.included("in1".label()),
        ExcludableValue.included("in2".label()),
        ExcludableValue.excluded("ex1".label()),
        ExcludableValue.excluded("ex2".label()),
      ),
    directories =
      listOf(
        ExcludableValue.included(Path("in1dir")),
        ExcludableValue.included(Path("in2dir")),
        ExcludableValue.excluded(Path("ex1dir")),
        ExcludableValue.excluded(Path("ex2dir")),
      ),
    buildFlags = listOf("flag1", "flag2"),
    syncFlags = listOf("flag1", "flag2"),
    debugFlags = emptyList(),
    bazelBinary = Path("bazel"),
    allowManualTargetsSync = true,
    dotBazelBspDirPath = Path(".bazelbsp"),
    importDepth = 2,
    enabledRules = listOf("rule1", "rule2"),
    ideJavaHomeOverride = Path("java_home"),
    shardSync = false,
    targetShardSize = 1000,
    shardingApproach = null,
    importRunConfigurations = emptyList(),
    gazelleTarget = null,
    indexAllFilesInDirectories = false,
    pythonCodeGeneratorRuleNames = emptyList(),
    importIjars = false,
    deriveInstrumentationFilterFromTargets = false,
    indexAdditionalFilesInDirectories = emptyList(),
  )

val mockBazelInfo =
  BazelInfo(
    execRoot = Path("execRoot"),
    outputBase = Path("outputBase"),
    workspaceRoot = Path("workspaceRoot"),
    bazelBin = Path("bazel-bin"),
    release = BazelRelease(7),
    isBzlModEnabled = true,
    isWorkspaceEnabled = true,
    externalAutoloads = emptyList(),
  )

val bazelRunner = BazelRunner(null, mockBazelInfo.workspaceRoot)
val bazelRunnerWithBazelInfo = BazelRunner(null, mockBazelInfo.workspaceRoot, mockBazelInfo)

fun splitOfTargetPattern(cmds : List<String>) : Pair<List<String>, List<String>> {
  cmds.indexOf("--") shouldBe -1
  val targetPatternFiles = cmds.filter { s -> s.startsWith("--target_pattern_file=")}
  val targetPatternFileArgument = targetPatternFiles.singleOrNull() ?: fail("Expected precisely one target pattern file argument")
  val patterns = Path(targetPatternFileArgument.substringAfter("--target_pattern_file=")).readLines()
  return Pair(patterns, cmds.filter { s -> !s.startsWith("--target_pattern_file=")})
}

class BazelRunnerBuilderTest {
  @BeforeEach
  fun beforeEach() {
    // Initialize providers for tests
    SystemInfoProvider.provideSystemInfoProvider(IntellijSystemInfoProvider)
    FileUtil.provideFileUtil(FileUtilIntellij)
  }

  @Test
  fun `most bare bones build without targets`() {
    val command =
      bazelRunner.buildBazelCommand(workspaceContext = mockContext, inheritProjectviewOptionsOverride = false) {
        build()
      }

    val (targets, cmds) = splitOfTargetPattern(command.buildExecutionDescriptor().command)
    cmds shouldContainExactly
      listOf(
        "bazel",
        "build",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
      )
    targets shouldBe emptyList()
  }

  @Test
  fun `build with targets from spec`() {
    val command =
      bazelRunnerWithBazelInfo.buildBazelCommand(mockContext) {
        build {
          addTargetsFromExcludableList(mockContext.targets)
        }
      }
    val executionDescriptor = command.buildExecutionDescriptor()
    val commandLine = executionDescriptor.command
    commandLine.dropLast(1) shouldContainExactly
      listOf(
        "bazel",
        "build",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "flag1",
        "flag2",
      )
    val targetPatternFileFlag = "--target_pattern_file="
    commandLine.last().startsWith(targetPatternFileFlag)
    val targetPatternFile = Paths.get(commandLine.last().removePrefix(targetPatternFileFlag))
    targetPatternFile.readText() shouldBe
      """
      in1
      in2
      -ex1
      -ex2

      """.trimIndent()
    executionDescriptor.finishCallback()
    targetPatternFile.exists() shouldBe false
  }

  @Test
  fun `run without program arguments`() {
    val command =
      bazelRunner.buildBazelCommand(mockContext) {
        run("in1".label())
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "run",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "flag1",
        "flag2",
        "in1",
      )
  }

  @Test
  fun `run with program arguments`() {
    val command =
      bazelRunner.buildBazelCommand(mockContext) {
        run("in1".label()) {
          programArguments.addAll(listOf("hello", "world"))
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "run",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "flag1",
        "flag2",
        "in1",
        "--",
        "hello",
        "world",
      )
  }

  @Test
  fun `run doesn't set environment using arguments`() {
    val command =
      bazelRunner.buildBazelCommand(mockContext) {
        run("in1".label()) {
          environment["key"] = "value"
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "run",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "flag1",
        "flag2",
        "in1",
      )
  }

  @Test
  fun `build sets environment using --action_env`() {
    val command =
      bazelRunner.buildBazelCommand(mockContext) {
        build {
          targets.add("in1".label())
          environment["key"] = "value"
        }
      }
    val (targets, cmds) = splitOfTargetPattern(command.buildExecutionDescriptor().command)

    cmds shouldContainExactly
      listOf(
        "bazel",
        "build",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "flag1",
        "flag2",
        "--action_env=key=value",
      )
    targets shouldBe listOf("in1")
  }

  @Test
  fun `test sets environment using --test_env`() {
    val command =
      bazelRunner.buildBazelCommand(mockContext) {
        test {
          targets.add("in1".label())
          environment["key"] = "value"
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "test",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "flag1",
        "flag2",
        "--test_env=key=value",
        "--",
        "in1",
      )
  }

  @Test
  fun `test sets arguments using --test_arg`() {
    val command =
      bazelRunner.buildBazelCommand(mockContext) {
        test {
          targets.add("in1".label())
          programArguments.addAll(listOf("hello", "world"))
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "test",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "flag1",
        "flag2",
        "--test_arg=hello",
        "--test_arg=world",
        "--",
        "in1",
      )
  }

  @Test
  fun `coverage uses the same way of settings arguments and env as test`() {
    val command =
      bazelRunner.buildBazelCommand(mockContext) {
        coverage {
          targets.add("in1".label())
          programArguments.addAll(listOf("hello", "world"))
          environment["key"] = "value"
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "coverage",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "flag1",
        "flag2",
        "--test_env=key=value",
        "--test_arg=hello",
        "--test_arg=world",
        "--",
        "in1",
      )
  }

  @Test
  fun `query does not inherit projectview options`() {
    val command =
      bazelRunner.buildBazelCommand(workspaceContext = mockContext, inheritProjectviewOptionsOverride = null) {
        query {
          targets.add("in1".label())
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "query",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "in1",
      )
  }

  @Test
  fun `query correctly handles excluded values`() {
    val command =
      bazelRunner.buildBazelCommand(workspaceContext = mockContext, inheritProjectviewOptionsOverride = null) {
        query {
          targets.add("in1".label())
          targets.add("in2".label())
          excludedTargets.add("ex1".label())
          excludedTargets.add("ex2".label())
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "query",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "in1 + in2 - ex1 - ex2",
      )
  }

  @Test
  fun `cquery does inherit projectview options`() {
    val command =
      bazelRunner.buildBazelCommand(workspaceContext = mockContext, inheritProjectviewOptionsOverride = null) {
        cquery {
          targets.add("in1".label())
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "cquery",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "flag1",
        "flag2",
        "--",
        "in1",
      )
  }

  @Test
  fun `bes arguments are handled properly`() {
    val command =
      bazelRunner.buildBazelCommand(mockContext) {
        build {
          targets.add("in1".label())
          useBes(Path("/dev/null"))
        }
      }

    val (targets, cmds) = splitOfTargetPattern(command.buildExecutionDescriptor().command)
    cmds shouldContainExactly
      listOf(
        "bazel",
        "build",
        BazelFlag.toolTag(),
        "--build_event_binary_file=/dev/null",
        "--bes_outerr_buffer_size=10",
        "--build_event_publish_all_actions",
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "flag1",
        "flag2",
      )
    targets shouldBe listOf("in1")
  }
}
