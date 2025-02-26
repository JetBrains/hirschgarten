package org.jetbrains.bazel.bazelrunner

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.AllowManualTargetsSyncSpec
import org.jetbrains.bazel.workspacecontext.AndroidMinSdkSpec
import org.jetbrains.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bazel.workspacecontext.BuildFlagsSpec
import org.jetbrains.bazel.workspacecontext.DEFAULT_TARGET_SHARD_SIZE
import org.jetbrains.bazel.workspacecontext.DirectoriesSpec
import org.jetbrains.bazel.workspacecontext.DotBazelBspDirPathSpec
import org.jetbrains.bazel.workspacecontext.EnableNativeAndroidRules
import org.jetbrains.bazel.workspacecontext.EnabledRulesSpec
import org.jetbrains.bazel.workspacecontext.ExperimentalAddTransitiveCompileTimeJars
import org.jetbrains.bazel.workspacecontext.IdeJavaHomeOverrideSpec
import org.jetbrains.bazel.workspacecontext.ImportDepthSpec
import org.jetbrains.bazel.workspacecontext.NoPruneTransitiveCompileTimeJarsPatternsSpec
import org.jetbrains.bazel.workspacecontext.ShardSyncSpec
import org.jetbrains.bazel.workspacecontext.ShardingApproachSpec
import org.jetbrains.bazel.workspacecontext.SyncFlagsSpec
import org.jetbrains.bazel.workspacecontext.TargetShardSizeSpec
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.TransitiveCompileTimeJarsTargetKindsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.WorkspaceContextProvider
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

fun String.label() = Label.parse(this)

val mockContext =
  WorkspaceContext(
    targets = TargetsSpec(listOf("in1".label(), "in2".label()), listOf("ex1".label(), "ex2".label())),
    directories = DirectoriesSpec(listOf(Path("in1dir"), Path("in2dir")), listOf(Path("ex1dir"), Path("ex2dir"))),
    buildFlags = BuildFlagsSpec(listOf("flag1", "flag2")),
    syncFlags = SyncFlagsSpec(listOf("flag1", "flag2")),
    bazelBinary = BazelBinarySpec(Path("bazel")),
    allowManualTargetsSync = AllowManualTargetsSyncSpec(true),
    dotBazelBspDirPath = DotBazelBspDirPathSpec(Path(".bazelbsp")),
    importDepth = ImportDepthSpec(2),
    enabledRules = EnabledRulesSpec(listOf("rule1", "rule2")),
    ideJavaHomeOverrideSpec = IdeJavaHomeOverrideSpec(Path("java_home")),
    experimentalAddTransitiveCompileTimeJars = ExperimentalAddTransitiveCompileTimeJars(true),
    experimentalTransitiveCompileTimeJarsTargetKinds = TransitiveCompileTimeJarsTargetKindsSpec(emptyList()),
    experimentalNoPruneTransitiveCompileTimeJarsPatterns = NoPruneTransitiveCompileTimeJarsPatternsSpec(emptyList()),
    enableNativeAndroidRules = EnableNativeAndroidRules(false),
    androidMinSdkSpec = AndroidMinSdkSpec(null),
    shardSync = ShardSyncSpec(false),
    targetShardSize = TargetShardSizeSpec(DEFAULT_TARGET_SHARD_SIZE),
    shardingApproachSpec = ShardingApproachSpec(null),
  )

val mockBazelInfo =
  BazelInfo(
    execRoot = "execRoot",
    outputBase = Path("outputBase"),
    workspaceRoot = Path("workspaceRoot"),
    release = BazelRelease(7),
    isBzlModEnabled = true,
    isWorkspaceEnabled = true,
  )

val contextProvider =
  object : WorkspaceContextProvider {
    override fun currentWorkspaceContext(): WorkspaceContext = mockContext
  }

val bazelRunner = BazelRunner(contextProvider, null, mockBazelInfo.workspaceRoot)
val bazelRunnerWithBazelInfo = BazelRunner(contextProvider, null, mockBazelInfo.workspaceRoot, mockBazelInfo)

class BazelRunnerBuilderTest {
  @Test
  fun `most bare bones build without targets (even though it's not correct)`() {
    val command =
      bazelRunner.buildBazelCommand(inheritProjectviewOptionsOverride = false) {
        build()
      }

    command.buildExecutionDescriptor().command shouldContainExactly
      listOf(
        "bazel",
        "build",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
        "--",
      )
  }

  @Test
  fun `build with targets from spec without bazel info (legacy flow)`() {
    val command =
      bazelRunner.buildBazelCommand {
        build {
          addTargetsFromSpec(mockContext.targets)
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
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
        "--",
        "in1",
        "in2",
        "-ex1",
        "-ex2",
      )
  }

  @Test
  fun `build with targets from spec (new flow with target pattern file)`() {
    val command =
      bazelRunnerWithBazelInfo.buildBazelCommand {
        build {
          addTargetsFromSpec(mockContext.targets)
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
      bazelRunner.buildBazelCommand {
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
      bazelRunner.buildBazelCommand {
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
      bazelRunner.buildBazelCommand {
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
      bazelRunner.buildBazelCommand {
        build {
          targets.add("in1".label())
          environment["key"] = "value"
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
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
        "--",
        "in1",
      )
  }

  @Test
  fun `test sets environment using --test_env`() {
    val command =
      bazelRunner.buildBazelCommand {
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
      bazelRunner.buildBazelCommand {
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
      bazelRunner.buildBazelCommand {
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
      bazelRunner.buildBazelCommand(inheritProjectviewOptionsOverride = null) {
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
      bazelRunner.buildBazelCommand(inheritProjectviewOptionsOverride = null) {
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
      bazelRunner.buildBazelCommand(inheritProjectviewOptionsOverride = null) {
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
      bazelRunner.buildBazelCommand {
        build {
          targets.add("in1".label())
          useBes(Path("/dev/null"))
        }
      }

    command.buildExecutionDescriptor().command shouldContainExactly
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
        "--",
        "in1",
      )
  }
}
