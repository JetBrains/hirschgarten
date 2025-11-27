package org.jetbrains.bazel.sync_new.connector

import com.android.tools.idea.util.buffered
import com.google.devtools.build.lib.query2.proto.proto2api.Build
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.sync_new.connector.BazelResult.Failure
import org.jetbrains.bazel.sync_new.connector.BazelResult.Success
import org.jetbrains.bazel.sync_new.connector.QueryResult.Labels
import org.jetbrains.bazel.sync_new.connector.QueryResult.Proto
import org.jetbrains.bazel.sync_new.connector.QueryResult.StreamedProto
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream

// legacy - using bsp client for logging output to console
class LegacyBazelConnectorImpl(
  private val project: Project,
  private val executable: String,
  private val bspClient: BspClientLogger,
  private val coroutineScope: CoroutineScope,
) : BazelConnector {
  companion object {
    private val logger = logger<LegacyBazelConnectorImpl>()
  }

  override suspend fun build(
    startup: StartupOptions.() -> Unit,
    args: BuildArgs.() -> Unit,
  ): BazelResult<Unit> {
    val builder = object : CmdBuilder(), BuildArgs {}

    builder.executable = executable
    startup(builder)
    builder.add(argValueOf("build"))
    args(builder)

    var targetPatternFile: Path? = null
    builder.popValue("target_patterns")?.require<Value.VText>()?.let {
      targetPatternFile = createTmpFile()
      withContext(Dispatchers.IO) { Files.writeString(targetPatternFile, it.text) }
      builder.add("target_pattern_file", argValueOf(targetPatternFile))
    }

    // TODO: handle bazel failure
    spawn(builder = builder)

    targetPatternFile?.let { withContext(Dispatchers.IO) { Files.deleteIfExists(it) } }

    return Success(Unit)
  }

  override suspend fun query(
    startup: StartupOptions.() -> Unit,
    args: QueryArgs.() -> Unit,
  ): BazelResult<QueryResult> {
    val builder = object : CmdBuilder(), QueryArgs {}

    builder.executable = executable
    startup(builder)
    builder.add(argValueOf("query"))
    args(builder)

    val query = builder.popValue("query")?.require<Value.VText>() ?: error("query must be specified")
    if (query.text.length > 10000) {
      val queryFile = createTmpFile()
      withContext(Dispatchers.IO) { Files.writeString(queryFile, query.text) }
      builder.add("query_file", argValueOf(queryFile))
      builder.onExit { withContext(Dispatchers.IO) { Files.deleteIfExists(queryFile) } }
    } else {
      builder.add(Arg.Positional(value = argValueOf(query.text), last = true))
    }

    val output = builder.popValue("output")?.require<Value.VUnsafe>() ?: error("output must be specified")
    return when (output.obj as QueryOutput) {
      QueryOutput.PROTO -> {
        val outputFile = createTmpFile()
        builder.add("output_file", argValueOf(outputFile))
        builder.add("output", argValueOf("proto"))
        val (_, failure) = spawn(builder = builder, capture = true)
        if (!failure.isRecoverable) {
          withContext(Dispatchers.IO) { Files.deleteIfExists(outputFile) }
          Failure(failure)
        } else {
          val result = outputFile.inputStream()
            .buffered()
            .use { stream -> Build.QueryResult.parseFrom(stream) }
          withContext(Dispatchers.IO) { Files.deleteIfExists(outputFile) }
          Success(Proto(result))
        }
      }

      QueryOutput.STREAMED_PROTO -> {
        val outputFile = createTmpFile()
        builder.add("output_file", argValueOf(outputFile))
        builder.add("output", argValueOf("streamed_proto"))
        val outputFileStream = withContext(Dispatchers.IO) { outputFile.inputStream().buffered() }
        val (_, failure) = spawn(builder = builder, capture = true)
        if (!failure.isRecoverable) {
          withContext(Dispatchers.IO) { Files.deleteIfExists(outputFile) }
          Failure(failure)
        } else {
          val flow = flow {
            while (true) {
              emit(Build.Target.parseDelimitedFrom(outputFileStream))
            }
          }
            .takeWhile { it != null }
            .onCompletion {
              outputFileStream.close()
              withContext(Dispatchers.IO) { Files.deleteIfExists(outputFile) }
            }
            .flowOn(Dispatchers.IO)
          Success(StreamedProto(flow))
        }
      }

      QueryOutput.LABEL -> {
        val outputFile = createTmpFile()
        builder.add("output_file", argValueOf(outputFile))
        builder.add("output", argValueOf("label"))
        val (process, failure) = spawn(builder = builder, capture = true)
        if (!failure.isRecoverable) {
          withContext(Dispatchers.IO) { Files.deleteIfExists(outputFile) }
          Failure(failure)
        } else {
          val labels = withContext(Dispatchers.IO) { Files.readAllLines(outputFile) }
            .map { Label.parse(it) }
          withContext(Dispatchers.IO) { Files.deleteIfExists(outputFile) }
          //val labels = process.inputStream.reader()
          //  .buffered()
          //  .use { it.readLines().map { Label.parse(it) } }
          Success(Labels(labels))
        }
      }
    }
  }

  private suspend fun spawn(
    builder: CmdBuilder,
    capture: Boolean = true,
    logInvocation: Boolean = true,
  ): Pair<Process, BazelFailureReason> {
    val cmd = builder.build()
    if (logInvocation) {
      logInvocation(cmd)
    }
    val process = GeneralCommandLine(cmd)
      .withWorkingDirectory(project.rootDir.toNioPath())
      .createProcess()

    val jobs = mutableListOf<Job>()
    if (capture) {
      jobs += coroutineScope.launch { capture(process.inputStream) }
      jobs += coroutineScope.launch { capture(process.errorStream) }
    }

    val code = try {
      process.awaitExit()
    } catch (e: CancellationException) {
      OSProcessUtil.killProcessTree(process)
      OSProcessUtil.killProcess(process)
      throw e
    }

    builder.onExit.forEach { it() }

    jobs.joinAll()

    return Pair(process, BazelFailureReason.fromExitCode(code))
  }

  private suspend fun capture(stream: InputStream) {
    withContext(Dispatchers.IO) {
      stream.reader(charset = Charsets.UTF_8)
        .buffered()
        .use { reader ->
          while (coroutineScope.isActive) {
            val line = reader.readLine() ?: break
            bspClient.message(line)
            logger.info(line)
          }
        }
    }
  }

  private fun logInvocation(cmd: List<String>) {
    val invokeCommand = cmd.joinToString(" ", "'", "'")
    bspClient.message("Invoking: $invokeCommand")
    logger.info("Invoking: $invokeCommand")
  }

}

private open class CmdBuilder : StartupOptions, Args {
  lateinit var executable: String
  val args: MutableList<Arg> = mutableListOf()
  val onExit: MutableList<suspend () -> Unit> = mutableListOf()

  override fun add(arg: Arg) {
    args.add(arg)
  }

  fun onExit(callback: suspend () -> Unit) {
    onExit.add(callback)
  }

  fun getValue(name: String): Value? = args.firstNotNullOfOrNull {
    when (it) {
      is Arg.Named -> if (it.name == name) {
        it.value
      } else {
        null
      }

      is Arg.Positional -> null
    }
  }

  fun popValue(name: String): Value? {
    val value = getValue(name)
    args.removeAll { it is Arg.Named && it.name == name }
    return value
  }

  private fun Value.toArgValue(arg: String): String {
    return when (this) {
      is Value.VBool ->
        if (this.value) {
          "--$arg"
        } else {
          "--no$arg"
        }

      is Value.VFile -> {
        "--$arg=${this.path.absolutePathString()}"
      }

      is Value.VFloat -> {
        "--$arg=${this.number}"
      }

      is Value.VInt -> {
        "--$arg=${this.number}"
      }

      is Value.VText -> {
        "--$arg=${this.text}"
      }

      else -> error("unsafe value leaked")
    }
  }

  private fun Value.toStringValue(): String {
    return when (this) {
      is Value.VBool -> this.value.toString()
      is Value.VFile -> this.path.absolutePathString()
      is Value.VFloat -> this.number.toString()
      is Value.VInt -> this.number.toString()
      is Value.VText -> this.text
      else -> error("unsafe value leaked")
    }
  }

  fun build(): List<String> {
    val args1 = args.mapNotNull {
      when (it) {
        is Arg.Named -> it.value.toArgValue(it.name)
        is Arg.Positional if !it. last -> it.value.toStringValue()
        else -> null
      }
    }
    val lastArgs = args.mapNotNull {
      when (it) {
        is Arg.Positional if it.last -> it.value.toStringValue()
        else -> null
      }
    }
    return listOf(executable) + args1 + lastArgs
  }

}

private suspend fun createTmpFile(): Path = withContext(Dispatchers.IO) { Files.createTempFile("bazel", "tmp") }
