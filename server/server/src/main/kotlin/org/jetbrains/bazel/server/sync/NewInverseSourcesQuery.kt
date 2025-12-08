package org.jetbrains.bazel.server.sync

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import org.jetbrains.bazel.bazelrunner.BazelCommand
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path
import kotlin.io.path.relativeTo

class NewInverseSourcesQuery(
  private val workspaceRoot: Path,
  private val bazelRunner: BazelRunner,
  private val workspaceContext: WorkspaceContext,
  //private val bazelInfo: BazelInfo,
  //private val bspClientLogger: BspClientLogger,
) {
  fun doStuff(vararg file: String): Any {
    val relatives = file.toList().map { Path.of(it).relativeTo(workspaceRoot) }
    val step1 = relatives.associateWithLabels()
    val step2v1f = getSourcesByRule(step1.values, UniverseGenerator.Global, false)
    val step2v2f = getSourcesByRule(step1.values, UniverseGenerator.EveryAll(" + "), false)
    val step2v3f = getSourcesByRule(step1.values, UniverseGenerator.CommonPrefix, false)
    val step2v1t = getSourcesByRule(step1.values, UniverseGenerator.Global, true)
    val step2v2t = getSourcesByRule(step1.values, UniverseGenerator.EveryAll(","), true)
    val step2v3t = getSourcesByRule(step1.values, UniverseGenerator.CommonPrefix, true)
    //val step3 = reverseMap(step2, step1.values.toSet())
    return mapOf(
      "step1" to step1,
      "step2v1f" to step2v1f,
      "step2v2f" to step2v2f,
      "step2v3f" to step2v3f,
      "step2v1t" to step2v1t,
      "step2v2t" to step2v2t,
      "step2v3t" to step2v3t,
    )
  }

  private fun List<Path>.associateWithLabels(): Map<Path, Label> =
    prepareFileLabelQuery(this).runAndParse().getSourcesPathLabelMap()

  private fun getSourcesByRule(
    fileLabels: Collection<Label>,
    universeGenerator: UniverseGenerator,
    useAllDeps: Boolean,
  ): Map<Label, List<Label>> {
    val command = prepareInverseSourcesQuery(fileLabels, useAllDeps, universeGenerator)
    return command.runAndParse().sourceLabelsByRuleNames()
  }

  private fun prepareFileLabelQuery(files: List<Path>): BazelCommand =
    bazelRunner.buildBazelCommand(workspaceContext) {
      fileQuery(files) {
        options.addAll(commonQueryFlags)
      }
    }

  @Suppress("SameParameterValue")
  private fun prepareInverseSourcesQuery(
    fileLabels: Collection<Label>,
    useAllDeps: Boolean,
    universeGenerator: UniverseGenerator,
  ): BazelCommand {
    // ABU - set universe and stuff?
    val universe = universeGenerator.generateUniverse(fileLabels)
    val targets = fileLabels.joinToString(separator = " + ")
    val expression = when (useAllDeps) {
      true -> "allrdeps($targets, 1)"
      false -> "rdeps($universe, $targets, 1)"
    }
    return bazelRunner.buildBazelCommand(workspaceContext) {
      queryExpression(expression) {
        options.addAll(commonQueryFlags)
        if (useAllDeps) {
          options.add("--universe_scope=$universe")
          options.add("--order_output=no")
        }
      }
    }
  }

  private fun BazelCommand.runAndParse(): Sequence<Build.Target> {
    val bazelProcess = bazelRunner.runBazelCommand(this, serverPidFuture = null, logProcessOutput = false) // ABU - what if log?
    val inputStream = bazelProcess.process.inputStream
    return generateSequence { Build.Target.parseDelimitedFrom(inputStream) }
  }

  private fun Sequence<Build.Target>.getSourcesPathLabelMap(): Map<Path, Label> =
    this
      .filter { it.type == Build.Target.Discriminator.SOURCE_FILE }
      .mapNotNull { it.sourceFile }
      .filter { it.hasLocation() }
      .associate { it.location to Label.parse(it.name) }
      .mapKeys { Path.of(it.key.dropLineAndColumn()) }

  private fun Sequence<Build.Target>.sourceLabelsByRuleNames(): Map<Label, List<Label>> =
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
    substringBeforeLast(':').substringBeforeLast(':') // ABU - will path:123:456 always look like that?
}

private val commonQueryFlags = listOf(BazelFlag.OutputFormat.streamed_proto(), BazelFlag.keepGoing())

fun <A, B> reverseMap(
  original: Map<A, List<B>>,
  filter: Set<B>
): Map<B, List<A>> {
  val result = mutableMapOf<B, MutableList<A>>()

  for ((a, bList) in original) {
    for (b in bList) {
      // Only process if 'b' is one of our targets
      if (b in filter) {
        result.getOrPut(b) { mutableListOf() }.add(a)
      }
    }
  }

  return result
}

private sealed class UniverseGenerator {
  abstract fun generateUniverse(fileLabels: Collection<Label>): String

  object Global : UniverseGenerator() {
    override fun generateUniverse(fileLabels: Collection<Label>): String = "//..."
  }

  class EveryAll(private val separator: String) : UniverseGenerator() {
    override fun generateUniverse(fileLabels: Collection<Label>): String =
      fileLabels
        .map { it.toString().substringBefore(':') }
        .distinct()
        .joinToString(separator = separator) { "$it:all" }
  }

  object CommonPrefix : UniverseGenerator() {
    // ABU - hacky, just for benchmarking
    override fun generateUniverse(fileLabels: Collection<Label>): String {
      val commonPrefix =
        fileLabels
          .map { it.toString().substringBefore(':') + "/" }
          .reduceOrNull { acc, s -> acc.commonPrefixWith(s) } ?: return "//..."
          .dropLastWhile { it != '/' }
      return "$commonPrefix..."
    }
  }
}
