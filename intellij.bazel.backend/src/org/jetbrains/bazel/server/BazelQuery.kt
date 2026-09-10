package org.jetbrains.bazel.server

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
interface BazelQueryOutput<T> {
  val flag: String?

  fun parse(stdout: ByteArray): T

  // --output=label
  object Labels : BazelQueryOutput<List<Label>> {
    override val flag: String = BazelFlag.OutputFormat.label()

    override fun parse(stdout: ByteArray): List<Label> =
      stdout
        .decodeToString()
        .lineSequence()
        .filter { it.isNotBlank() }
        .map { Label.parse(it.trim()) }
        .toList()
  }

  // --output=streamed_proto
  object Targets : BazelQueryOutput<List<Build.Target>> {
    override val flag: String = BazelFlag.OutputFormat.streamed_proto()

    override fun parse(stdout: ByteArray): List<Build.Target> {
      val stream = stdout.inputStream()
      return generateSequence { Build.Target.parseDelimitedFrom(stream) }.toList()
    }
  }

  // --output=proto
  object Proto : BazelQueryOutput<Build.QueryResult> {
    override val flag: String = BazelFlag.OutputFormat.proto()

    override fun parse(stdout: ByteArray): Build.QueryResult = Build.QueryResult.parseFrom(stdout)
  }

  data class Raw(val format: String? = null) : BazelQueryOutput<String> {
    override val flag: String? = format?.let { "--output=$it" }

    override fun parse(stdout: ByteArray): String = stdout.decodeToString()
  }
}

@ApiStatus.Internal
data class BazelQueryParams<T>(
  val expression: String,
  val output: BazelQueryOutput<T>,
  val taskId: TaskId? = null,
  val keepGoing: Boolean = true,
  val flags: List<String> = emptyList(),
)

@ApiStatus.Internal
data class BazelQueryResult<T>(
  val result: T,
  val status: BazelStatus,
  val stderrLines: List<String>,
)

internal suspend fun <T> runBazelQuery(
  bazelRunner: BazelRunner,
  projectView: ProjectView,
  params: BazelQueryParams<T>,
): BazelQueryResult<T> {
  val command =
    bazelRunner.buildBazelCommand(projectView) {
      queryExpression(params.expression) {
        params.output.flag?.let { options.add(it) }
        if (params.keepGoing) {
          options.add(BazelFlag.keepGoing())
        }
        options.addAll(params.flags)
      }
    }
  val processResult =
    bazelRunner
      .runBazelCommand(command, taskId = params.taskId, logProcessOutput = false)
      .waitAndGetResult()
  return BazelQueryResult(
    result = params.output.parse(processResult.stdout),
    status = processResult.bazelStatus,
    stderrLines = processResult.stderrLines,
  )
}
