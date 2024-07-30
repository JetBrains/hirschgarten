package org.jetbrains.bsp.bazel.bazelrunner

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
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

val mockContext = WorkspaceContext(
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

val contextProvider = object : WorkspaceContextProvider {
  override fun currentWorkspaceContext(): WorkspaceContext = mockContext
}

val bazelRunner = BazelRunner(contextProvider, null, Path("workspaceRoot"))

class BazelRunnerBuilderTest {
  @Test
  fun blah() {
    val builder = bazelRunner.commandBuilder()

    builder.build().withUseBuildFlags(false)

  }
}
