package org.jetbrains.bsp.bazel.bazelrunner

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelArgumentsUtils
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec

class BazelRunnerBuildBuilder(bazelRunner: BazelRunner, bazelBuildCommand: String) :
  BazelRunnerBuilder(bazelRunner, bazelBuildCommand) {
  override fun withTargets(bazelTargets: List<String>): BazelRunnerBuilder = withProgramArguments(bazelTargets)

  override fun withTargets(targetsSpec: TargetsSpec): BazelRunnerBuilder = withTargets(targetsSpec.values, targetsSpec.excludedValues)

  override fun withTargets(
    includedTargets: List<BuildTargetIdentifier>,
    excludedTargets: List<BuildTargetIdentifier>,
  ): BazelRunnerBuilder {
    val arguments = mutableListOf("--")
    arguments.addAll(BazelArgumentsUtils.toRawUris(includedTargets))
    arguments.addAll(
      BazelArgumentsUtils.calculateExcludedTargetsWithExcludedPrefix(excludedTargets),
    )
    return withProgramArguments(arguments)
  }

  companion object {
    private const val ExcludableTargetsListPrefix = "--"
  }
}
