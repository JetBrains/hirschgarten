package org.jetbrains.bsp.bazel.bazelrunner

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.google.common.collect.ImmutableList
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelQueryKindParameters
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelArgumentsUtils
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import java.nio.file.Path

open class BazelRunnerBuilder internal constructor(private val bazelRunner: BazelRunner, private val bazelCommand: String) {
  private val globalFlags = listOf<String>(BazelFlag.toolTag())
  private val flags = globalFlags.toMutableList()
  private val programArguments = mutableListOf<String>()
  private val environmentVariables = mutableMapOf<String, String>()
  private var useBuildFlags = false

  fun withUseBuildFlags(useBuildFlags: Boolean = true): BazelRunnerBuilder {
    this.useBuildFlags = useBuildFlags
    return this
  }

  fun withBazelArgument(bazelFlag: String): BazelRunnerBuilder {
    flags.add(bazelFlag)
    return this
  }

  fun withBazelArguments(bazelFlags: List<String>?): BazelRunnerBuilder {
    flags.addAll(bazelFlags.orEmpty())
    return this
  }

  fun withProgramArgument(bazelArgument: String): BazelRunnerBuilder {
    programArguments.add(bazelArgument)
    return this
  }

  fun withProgramArguments(bazelArguments: List<String>?): BazelRunnerBuilder {
    programArguments.addAll(bazelArguments.orEmpty())
    return this
  }

  open fun withTargets(bazelTargets: List<String>): BazelRunnerBuilder {
    val joinedTargets = BazelArgumentsUtils.getJoinedBazelTargets(bazelTargets)
    programArguments.add(joinedTargets)
    return this
  }

  open fun withTargets(targetsSpec: TargetsSpec): BazelRunnerBuilder {
    val joinedTargets = BazelArgumentsUtils.joinBazelTargets(targetsSpec.values, targetsSpec.excludedValues)
    programArguments.add(joinedTargets)
    return this
  }

  open fun withTargets(includedTargets: List<BuildTargetIdentifier>, excludedTargets: List<BuildTargetIdentifier>): BazelRunnerBuilder? {
    val joinedTargets = BazelArgumentsUtils.joinBazelTargets(includedTargets, excludedTargets)
    programArguments.add(joinedTargets)
    return this
  }

  fun withMnemonic(bazelTargets: List<String>, languageIds: List<String>): BazelRunnerBuilder {
    val argument = BazelArgumentsUtils.getMnemonicWithJoinedTargets(bazelTargets, languageIds)
    programArguments.add(argument)
    return this
  }

  fun withKind(bazelParameter: BazelQueryKindParameters): BazelRunnerBuilder = withKinds(ImmutableList.of(bazelParameter))

  fun withKinds(bazelParameters: List<BazelQueryKindParameters>): BazelRunnerBuilder {
    val argument = BazelArgumentsUtils.getQueryKindForPatternsAndExpressions(bazelParameters)
    programArguments.add(argument)
    return this
  }

  fun withKindsAndExcept(parameters: BazelQueryKindParameters, exception: String): BazelRunnerBuilder {
    val argument =
      BazelArgumentsUtils.getQueryKindForPatternsAndExpressionsWithException(
        listOf(parameters),
        exception,
      )
    programArguments.add(argument)
    return this
  }

  fun withEnvironment(environment: List<Pair<String, String>>): BazelRunnerBuilder {
    environmentVariables.putAll(environment)
    return this
  }

  fun dump(): List<String> = bazelRunner.prepareBazelCommand(bazelCommand, flags, programArguments, useBuildFlags)

  fun executeBazelCommand(originId: String? = null, parseProcessOutput: Boolean = true): BazelProcess =
    bazelRunner.runBazelCommand(
      bazelCommand,
      flags,
      programArguments,
      environmentVariables,
      originId,
      parseProcessOutput,
      useBuildFlags,
    )

  fun executeBazelBesCommand(originId: String? = null, buildEventFile: Path): BazelProcess =
    bazelRunner.runBazelCommandBes(
      bazelCommand,
      flags,
      programArguments,
      environmentVariables,
      originId,
      buildEventFile.toAbsolutePath(),
    )
}
