package org.jetbrains.bazel.server.sync

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.bazelrunner.BazelCommand
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import java.nio.file.Path
import kotlin.io.path.relativeTo
import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

object NewInverseSourcesQuery/*(
  private val workspaceRoot: Path,
  private val bazelRunner: BazelRunner,
  private val workspaceContext: WorkspaceContext,
  private val bazelInfo: BazelInfo,
  private val bspClientLogger: BspClientLogger,
)*/ {
  //suspend fun performBenchmarks(filesetName: String, files: List<String>): List<AbuBenchmark> {
  //  println("\n--- Fileset: $filesetName ---")
  //  val relatives = files.map { Path.of(it).relativeTo(workspaceRoot) }
  //  val step1 = benchmark("Files to Labels") { relatives.associateWithLabels() }
  //  val step2 = SCENARIOS.map { benchmark(it.name) { getSourcesByRule(step1.value.values, it.universeGenerator, it.useAllDeps) } }
  //  val oneResult = step2.firstOrNull()?.value ?: emptyMap()
  //  val step3 = benchmark("Reverse map") { reverseMap(oneResult, step1.value.values.toSet()) }
  //  val results: List<TypedBenchmark<*>> = step2 + listOf(step1, step3)
  //  return results.map { it.benchmark }
  //}
  //
  //private suspend fun <T> benchmark(name: String, action: () -> T): TypedBenchmark<T> {
  //  println("Benchmark started: $name")
  //  val cleanCommand = bazelRunner.buildBazelCommand(workspaceContext) { clean() }
  //  bazelRunner.runBazelCommand(cleanCommand, serverPidFuture = null, logProcessOutput = false).waitAndGetResult()
  //  val timedResult = measureTimedValue(action)
  //  return TypedBenchmark(AbuBenchmark(name, timedResult.duration.inWholeMilliseconds / 1000.0, timedResult.value), timedResult.value)
  //}
  //
  //fun doStuff(vararg file: String): Any {
  //  val relatives = file.toList().map { Path.of(it).relativeTo(workspaceRoot) }
  //  val step1 = relatives.associateWithLabels()
  //  val step2v1f = getSourcesByRule(step1.values, UniverseGenerator.Global, false)
  //  val step2v2f = getSourcesByRule(step1.values, UniverseGenerator.EveryAll(" + "), false)
  //  val step2v3f = getSourcesByRule(step1.values, UniverseGenerator.CommonPrefix, false)
  //  val step2v1t = getSourcesByRule(step1.values, UniverseGenerator.Global, true)
  //  val step2v2t = getSourcesByRule(step1.values, UniverseGenerator.EveryAll(","), true)
  //  val step2v3t = getSourcesByRule(step1.values, UniverseGenerator.CommonPrefix, true)
  //  //val step3 = reverseMap(step2, step1.values.toSet())
  //  return mapOf(
  //    "step1" to step1,
  //    "step2v1f" to step2v1f,
  //    "step2v2f" to step2v2f,
  //    "step2v3f" to step2v3f,
  //    "step2v1t" to step2v1t,
  //    "step2v2t" to step2v2t,
  //    "step2v3t" to step2v3t,
  //  )
  //}

  suspend fun inverseSourcesQuery(
    params: InverseSourcesParams,
    workspaceRoot: Path,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
  ): InverseSourcesResult {
    val relativePaths = params.files.map { it.relativeTo(workspaceRoot) }
    val fileLabels = relativePaths.associateWithLabels(bazelRunner, workspaceContext)
    val sourcesByRule =
      getSourcesByRule(fileLabels.values, UniverseGenerator.EveryAll(","), bazelRunner, workspaceContext)
    val rulesBySource = reverseMap(sourcesByRule, fileLabels.values.toSet())
    val result = params.files.associateWith { rulesBySource[fileLabels[it]] ?: emptyList() }
    return InverseSourcesResult(result)
  }

  private suspend fun List<Path>.associateWithLabels(bazelRunner: BazelRunner, workspaceContext: WorkspaceContext): Map<Path, Label> =
    prepareFileLabelQuery(this, bazelRunner, workspaceContext).runAndParse(bazelRunner).getSourcesPathLabelMap()

  private suspend fun getSourcesByRule(
    fileLabels: Collection<Label>,
    universeGenerator: UniverseGenerator,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
  ): Map<Label, List<Label>> {
    val command = prepareInverseSourcesQuery(fileLabels, universeGenerator, bazelRunner, workspaceContext)
    return command.runAndParse(bazelRunner).sourceLabelsByRuleNames()
  }

  private fun prepareFileLabelQuery(files: List<Path>, bazelRunner: BazelRunner, workspaceContext: WorkspaceContext): BazelCommand =
    bazelRunner.buildBazelCommand(workspaceContext) {
      fileQuery(files) {
        options.addAll(commonQueryFlags)
      }
    }

  @Suppress("SameParameterValue")
  private fun prepareInverseSourcesQuery(
    fileLabels: Collection<Label>,
    universeGenerator: UniverseGenerator,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
  ): BazelCommand {
    val useAllDeps = true // ABU - inline (remove false branches)
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

  private suspend fun BazelCommand.runAndParse(bazelRunner: BazelRunner): List<Build.Target> {
    val commandString = this.buildExecutionDescriptor().command.joinToString(" ")
    val bazelProcess = bazelRunner.runBazelCommand(this, serverPidFuture = null, logProcessOutput = false) // ABU - what if log?
    val inputStream = bazelProcess.process.inputStream
    val processOutput = generateSequence { Build.Target.parseDelimitedFrom(inputStream) }
    bazelProcess.process.awaitExit() // ABU - will this break things?
    //bazelProcess.process.waitFor() // this DOES break things
    return processOutput.toList()//.also { println("...Bazel finished") }
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
    substringBeforeLast(':').substringBeforeLast(':') // ABU - will path:123:456 always look like that?
}

private val commonQueryFlags = listOf(BazelFlag.OutputFormat.streamed_proto(), BazelFlag.keepGoing())

private fun <A, B> reverseMap(
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

private data class Scenario(
  val name: String,
  val universeGenerator: UniverseGenerator,
  val useAllDeps: Boolean,
)

private val SCENARIOS = listOf(
  Scenario("Global", UniverseGenerator.Global, false),
  Scenario("EveryAll", UniverseGenerator.EveryAll(" + "), false),
  Scenario("CommonPrefix", UniverseGenerator.CommonPrefix, false),
  Scenario("Global [Sky]", UniverseGenerator.Global, true),
  Scenario("EveryAll [Sky]", UniverseGenerator.EveryAll(","), true),
  Scenario("CommonPrefix [Sky]", UniverseGenerator.CommonPrefix, true),
)

private data class TypedBenchmark<T>(
  val benchmark: AbuBenchmark,
  val value: T,
)


