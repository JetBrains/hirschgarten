package org.jetbrains.bazel.server.sync

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import org.jetbrains.bazel.bazelrunner.BazelCommand
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import java.nio.file.Path
import kotlin.io.path.relativeTo

object InverseSourcesQuery {
  suspend fun inverseSourcesQuery(
    params: InverseSourcesParams,
    workspaceRoot: Path,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
  ): InverseSourcesResult {
    val relativePaths = params.files.map { it.relativeTo(workspaceRoot) }
    val fileLabels = relativePaths.associateWithLabels(bazelRunner, workspaceContext, params.originId)
    val result =
      if (fileLabels.isNotEmpty()) {
        val sourcesByRule =
          getSourcesByRule(fileLabels.values, bazelRunner, workspaceContext, params.originId)
        val rulesBySource = reverseMap(sourcesByRule, fileLabels.values.toSet())
        params.files.associateWith { rulesBySource[fileLabels[it]] ?: emptyList() }
      } else {
        emptyMap()
      }
    return InverseSourcesResult(result)
  }

  private suspend fun List<Path>.associateWithLabels(
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
    originId: String?,
  ): Map<Path, Label> =
    prepareFileLabelQuery(this, bazelRunner, workspaceContext).runAndParse(bazelRunner, originId).getSourcesPathLabelMap()

  private suspend fun getSourcesByRule(
    fileLabels: Collection<Label>,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
    originId: String?,
  ): Map<Label, List<Label>> {
    val command = prepareInverseSourcesQuery(fileLabels, bazelRunner, workspaceContext)
    return command.runAndParse(bazelRunner, originId).sourceLabelsByRuleNames()
  }

  private fun prepareFileLabelQuery(files: List<Path>, bazelRunner: BazelRunner, workspaceContext: WorkspaceContext): BazelCommand =
    bazelRunner.buildBazelCommand(workspaceContext) {
      fileQuery(files) {
        options.add(BazelFlag.OutputFormat.streamed_proto())
      }
    }

  @Suppress("SameParameterValue")
  private fun prepareInverseSourcesQuery(
    fileLabels: Collection<Label>,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
  ): BazelCommand {
    val universe = generateUniverse(fileLabels)
    val targets = fileLabels.joinToString(separator = " + ")
    val expression = "allrdeps($targets, 1)"
    return bazelRunner.buildBazelCommand(workspaceContext) {
      queryExpression(expression) {
        options.add(BazelFlag.OutputFormat.streamed_proto())
        options.add(BazelFlag.orderOutput(false))
        options.add(BazelFlag.universeScope(universe))
      }
    }
  }

  private fun generateUniverse(fileLabels: Collection<Label>): String =
    fileLabels
      .map { it.toString().substringBefore(':') }
      .distinct()
      .joinToString(separator = ",") { "$it:all" }

  private suspend fun BazelCommand.runAndParse(bazelRunner: BazelRunner, originId: String?): List<Build.Target> {
    val bazelProcess = bazelRunner.runBazelCommand(this, serverPidFuture = null, logProcessOutput = true, originId = originId)
    bazelProcess.waitAndGetResult(false, shouldReadStdout = false)
    val inputStream = bazelProcess.process.inputStream
    val processOutput = generateSequence { Build.Target.parseDelimitedFrom(inputStream) }
    return processOutput.toList()
  }

  private fun Collection<Build.Target>.getSourcesPathLabelMap(): Map<Path, Label> =
    this
      .filter { it.type == Build.Target.Discriminator.SOURCE_FILE }
      .mapNotNull { it.sourceFile }
      .filter { it.hasLocation() }
      .associate { it.location to Label.parse(it.name) }
      .mapKeys { Path.of(it.key.dropLineAndColumn()) }

  private fun Collection<Build.Target>.sourceLabelsByRuleNames(): Map<Label, List<Label>> =
    this
      .filter { it.type == Build.Target.Discriminator.RULE }
      .mapNotNull { it.rule }
      .associate { Label.parse(it.name) to it.attributeList }
      .mapValues { getSrcs(it.value) }

  private fun getSrcs(attributes: List<Build.Attribute>): List<Label> =
    attributes
      .firstOrNull { it.name == "srcs" }
      ?.stringListValueList
      ?.map { Label.parse(it) }
      ?: emptyList()

  private fun String.dropLineAndColumn(): String =
    substringBeforeLast(':').substringBeforeLast(':')
}

private fun <A, B> reverseMap(
  original: Map<A, List<B>>,
  filter: Set<B>
): Map<B, List<A>> {
  val result = mutableMapOf<B, MutableList<A>>()

  for ((a, bList) in original) {
    for (b in bList) {
      if (b in filter) {
        result.getOrPut(b) { mutableListOf() }.add(a)
      }
    }
  }

  return result
}
