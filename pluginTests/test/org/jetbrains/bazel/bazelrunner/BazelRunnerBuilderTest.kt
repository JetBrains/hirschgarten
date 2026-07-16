package org.jetbrains.bazel.bazelrunner

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ALLOW_MANUAL_TARGETS_SYNC_KEY
import org.jetbrains.bazel.languages.projectview.BAZEL_BINARY_KEY
import org.jetbrains.bazel.languages.projectview.BUILD_FLAGS_KEY
import org.jetbrains.bazel.languages.projectview.DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.IDE_JAVA_HOME_OVERRIDE_KEY
import org.jetbrains.bazel.languages.projectview.IMPORT_DEPTH_KEY
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.SHARD_SYNC_KEY
import org.jetbrains.bazel.languages.projectview.SYNC_FLAGS_KEY
import org.jetbrains.bazel.languages.projectview.TARGETS_KEY
import org.jetbrains.bazel.languages.projectview.TARGET_SHARD_SIZE_KEY
import org.jetbrains.bazel.languages.projectview.targets
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.readText

fun String.label() = Label.parse(this)

private val mockProjectView: ProjectView = ProjectView(
  sections = mapOf(
    TARGETS_KEY to listOf(
      ExcludableValue.included("in1".label()),
      ExcludableValue.included("in2".label()),
      ExcludableValue.excluded("ex1".label()),
      ExcludableValue.excluded("ex2".label()),
    ),
    DIRECTORIES_KEY to listOf(
      ExcludableValue.included(Path("in1dir")),
      ExcludableValue.included(Path("in2dir")),
      ExcludableValue.excluded(Path("ex1dir")),
      ExcludableValue.excluded(Path("ex2dir")),
    ),
    BUILD_FLAGS_KEY to listOf("flag1", "flag2"),
    SYNC_FLAGS_KEY to listOf("flag1", "flag2"),
    BAZEL_BINARY_KEY to Path("bazel"),
    ALLOW_MANUAL_TARGETS_SYNC_KEY to true,
    IMPORT_DEPTH_KEY to 2,
    IDE_JAVA_HOME_OVERRIDE_KEY to Path("java_home"),
    SHARD_SYNC_KEY to false,
    TARGET_SHARD_SIZE_KEY to 1000,
  ),
  imports = emptyList(),
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

val mockBazelProcessLauncher = object : BazelProcessLauncher {
  override fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process {
    throw NotImplementedError()
  }
}

val bazelRunner = BazelRunner(null, mockBazelInfo.workspaceRoot, mockBazelProcessLauncher, Path("bazel"))
val bazelRunnerWithBazelInfo = BazelRunner(null, mockBazelInfo.workspaceRoot, mockBazelProcessLauncher, Path("bazel"))

fun splitOfTargetPattern(cmds : List<String>) : Pair<List<String>, List<String>> {
  cmds.indexOf("--") shouldBe -1
  val targetPatternFiles = cmds.filter { s -> s.startsWith("--target_pattern_file=")}
  val targetPatternFileArgument = targetPatternFiles.singleOrNull() ?: fail("Expected precisely one target pattern file argument")
  val patterns = Path(targetPatternFileArgument.substringAfter("--target_pattern_file=")).readLines()
  return Pair(patterns, cmds.filter { s -> !s.startsWith("--target_pattern_file=")})
}

class BazelRunnerBuilderTest {
  @Test
  fun `most bare bones build without targets`() {
    val command =
      bazelRunner.buildBazelCommand(projectView = mockProjectView, inheritProjectviewOptionsOverride = false) {
        build()
      }

    val (targets, cmds) = splitOfTargetPattern(command.buildExecutionDescriptor().command)
    cmds shouldContainExactly
      listOf(
        "bazel",
        "build",
        BazelFlag.toolTag(),
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
      )
    targets shouldBe emptyList()
  }

  @Test
  fun `build with targets from spec`() {
    val command =
      bazelRunnerWithBazelInfo.buildBazelCommand(mockProjectView) {
        build {
          addTargetsFromExcludableList(mockProjectView.targets)
        }
      }
    val executionDescriptor = command.buildExecutionDescriptor()
    val commandLine = executionDescriptor.command
    commandLine.dropLast(1) shouldContainExactly
      listOf(
        "bazel",
        "build",
        BazelFlag.toolTag(),
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
      bazelRunner.buildBazelCommand(mockProjectView) {
        run("in1".label())
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "run",
        BazelFlag.toolTag(),
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
      bazelRunner.buildBazelCommand(mockProjectView) {
        run("in1".label()) {
          programArguments.addAll(listOf("hello", "world"))
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "run",
        BazelFlag.toolTag(),
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
      bazelRunner.buildBazelCommand(mockProjectView) {
        run("in1".label()) {
          environment["key"] = "value"
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "run",
        BazelFlag.toolTag(),
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
      bazelRunner.buildBazelCommand(mockProjectView) {
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
      bazelRunner.buildBazelCommand(mockProjectView) {
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
      bazelRunner.buildBazelCommand(mockProjectView) {
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
      bazelRunner.buildBazelCommand(mockProjectView) {
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
      bazelRunner.buildBazelCommand(projectView = mockProjectView, inheritProjectviewOptionsOverride = null) {
        query {
          targets.add("in1".label())
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "query",
        BazelFlag.toolTag(),
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "in1",
      )
  }

  @Test
  fun `query correctly handles excluded values`() {
    val command =
      bazelRunner.buildBazelCommand(projectView = mockProjectView, inheritProjectviewOptionsOverride = null) {
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
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "in1 + in2 - ex1 - ex2",
      )
  }

  @Test
  fun `cquery does inherit projectview options`() {
    val command =
      bazelRunner.buildBazelCommand(projectView = mockProjectView, inheritProjectviewOptionsOverride = null) {
        cquery {
          targets.add("in1".label())
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "cquery",
        BazelFlag.toolTag(),
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
      bazelRunner.buildBazelCommand(mockProjectView) {
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
        "--build_event_binary_file_upload_mode=wait_for_upload_complete",
        "--bes_outerr_buffer_size=10",
        "--build_event_publish_all_actions",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "flag1",
        "flag2",
      )
    targets shouldBe listOf("in1")
  }
}
