package org.jetbrains.bsp.bazel.bazelrunner

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bsp.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bsp.bazel.workspacecontext.BuildFlagsSpec
import org.jetbrains.bsp.bazel.workspacecontext.BuildManualTargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.DirectoriesSpec
import org.jetbrains.bsp.bazel.workspacecontext.DotBazelBspDirPathSpec
import org.jetbrains.bsp.bazel.workspacecontext.EnabledRulesSpec
import org.jetbrains.bsp.bazel.workspacecontext.ExperimentalAddTransitiveCompileTimeJars
import org.jetbrains.bsp.bazel.workspacecontext.ExperimentalUseLibOverModSpec
import org.jetbrains.bsp.bazel.workspacecontext.IdeJavaHomeOverrideSpec
import org.jetbrains.bsp.bazel.workspacecontext.ImportDepthSpec
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

fun String.bsp() = BuildTargetIdentifier(this)

val mockContext =
  WorkspaceContext(
    targets = TargetsSpec(listOf("in1".bsp(), "in2".bsp()), listOf("ex1".bsp(), "ex2".bsp())),
    directories = DirectoriesSpec(listOf(Path("in1dir"), Path("in2dir")), listOf(Path("ex1dir"), Path("ex2dir"))),
    buildFlags = BuildFlagsSpec(listOf("flag1", "flag2")),
    bazelBinary = BazelBinarySpec(Path("bazel")),
    buildManualTargets = BuildManualTargetsSpec(true),
    dotBazelBspDirPath = DotBazelBspDirPathSpec(Path(".bazelbsp")),
    importDepth = ImportDepthSpec(2),
    enabledRules = EnabledRulesSpec(listOf("rule1", "rule2")),
    ideJavaHomeOverrideSpec = IdeJavaHomeOverrideSpec(Path("java_home")),
    experimentalUseLibOverModSection = ExperimentalUseLibOverModSpec(true),
    experimentalAddTransitiveCompileTimeJars = ExperimentalAddTransitiveCompileTimeJars(true),
  )

val contextProvider =
  object : WorkspaceContextProvider {
    override fun currentWorkspaceContext(): WorkspaceContext = mockContext
  }

val bazelRunner = BazelRunner(contextProvider, null, Path("workspaceRoot"))

class BazelRunnerBuilderTest {
  @Test
  fun `most bare bones build without targets (even though it's not correct)`() {
    val command =
      bazelRunner.buildBazelCommand(inheritProjectviewOptionsOverride = false) {
        build()
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "build",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--",
      )
  }

  @Test
  fun `build with targets from spec`() {
    val command =
      bazelRunner.buildBazelCommand {
        build {
          addTargetsFromSpec(mockContext.targets)
        }
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "build",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
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
  fun `run without program arguments`() {
    val command =
      bazelRunner.buildBazelCommand {
        run("in1".bsp())
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "run",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "flag1",
        "flag2",
        "in1",
      )
  }

  @Test
  fun `run with program arguments`() {
    val command =
      bazelRunner.buildBazelCommand {
        run("in1".bsp()) {
          programArguments.addAll(listOf("hello", "world"))
        }
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "run",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
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
        run("in1".bsp()) {
          environment["key"] = "value"
        }
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "run",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
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
          targets.add("in1".bsp())
          environment["key"] = "value"
        }
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "build",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
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
          targets.add("in1".bsp())
          environment["key"] = "value"
        }
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "test",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
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
          targets.add("in1".bsp())
          programArguments.addAll(listOf("hello", "world"))
        }
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "test",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
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
          targets.add("in1".bsp())
          programArguments.addAll(listOf("hello", "world"))
          environment["key"] = "value"
        }
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "coverage",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
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
          targets.add("in1".bsp())
        }
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "query",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "--",
        "in1",
      )
  }

  @Test
  fun `cquery does inherit projectview options`() {
    val command =
      bazelRunner.buildBazelCommand(inheritProjectviewOptionsOverride = null) {
        cquery {
          targets.add("in1".bsp())
        }
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "cquery",
        BazelFlag.toolTag(),
        "--override_repository=bazelbsp_aspect=.bazelbsp",
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
          targets.add("in1".bsp())
          useBes(Path("/dev/null"))
        }
      }

    command.makeCommandLine() shouldContainExactly
      listOf(
        "bazel",
        "build",
        BazelFlag.toolTag(),
        "--build_event_binary_file=/dev/null",
        "--bes_outerr_buffer_size=10",
        "--isatty=true",
        "--override_repository=bazelbsp_aspect=.bazelbsp",
        "flag1",
        "flag2",
        "--",
        "in1",
      )
  }
}
