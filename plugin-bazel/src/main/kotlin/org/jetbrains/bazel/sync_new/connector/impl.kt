package org.jetbrains.bazel.sync_new.connector

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.logger.BspClientLogger
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.collections.joinToString
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

    builder.add(argValueOf(executable))
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

    return BazelResult.Success(Unit)
  }

  override suspend fun query(
    startup: StartupOptions.() -> Unit,
    args: QueryArgs.() -> Unit,
  ): BazelResult<QueryResult> {
    val builder = object : CmdBuilder(), QueryArgs {}

    builder.add(argValueOf(executable))
    startup(builder)
    builder.add(argValueOf("query"))
    args(builder)

    val queryFile = createTmpFile()
    val query = builder.popValue("query")?.require<Value.VText>() ?: error("query must be specified")
    withContext(Dispatchers.IO) { Files.writeString(queryFile, query.text) }
    builder.add("query_file", argValueOf(queryFile))

    val outputFile = createTmpFile()
    builder.add("output_file", argValueOf(outputFile))

    val output = builder.popValue("output")?.require<Value.VUnsafe>() ?: error("output must be specified")
    return when (output.obj as QueryOutput) {
      QueryOutput.PROTO -> {
        // TODO: handle bazel failure
        builder.add("output", argValueOf("proto"))
        val (_, exitCode) = spawn(builder = builder, capture = true)
        withContext(Dispatchers.IO) { Files.deleteIfExists(queryFile) }
        outputFile.inputStream().buffered().use { stream ->
          val result = Build.QueryResult.parseFrom(stream)
          Files.deleteIfExists(outputFile)
          BazelResult.Success(QueryResult.Proto(result))
        }
      }

      QueryOutput.STREAMED_PROTO -> {
        builder.add("output", argValueOf("streamed_proto"))
        val outputFileStream = withContext(Dispatchers.IO) { outputFile.inputStream().buffered() }
        // TODO: handle bazel failure
        val (_, exitCode) = spawn(builder = builder, capture = true)
        withContext(Dispatchers.IO) { Files.deleteIfExists(queryFile) }
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
        BazelResult.Success(QueryResult.StreamedProto(flow))
      }
    }
  }

  private suspend fun spawn(
    builder: CmdBuilder,
    capture: Boolean = true,
    logInvocation: Boolean = true,
  ): Pair<Process, Int> {
    val cmd = builder.build()
    if (logInvocation) {
      logInvocation(cmd)
    }
    val process = GeneralCommandLine(cmd)
      .withWorkingDirectory(project.rootDir.toNioPath())
      .createProcess()

    val exitChannel = Channel<Unit>()

    if (capture) {
      coroutineScope.launch { capture(process.inputStream, exitChannel) }
      coroutineScope.launch { capture(process.errorStream, exitChannel) }
    }

    val code = try {
      val code = process.awaitExit()
      exitChannel.trySend(Unit)
      code
    } catch (e: CancellationException) {
      OSProcessUtil.killProcessTree(process)
      OSProcessUtil.killProcess(process)
      throw e
    }

    return Pair(process, code)
  }

  private suspend fun CoroutineScope.capture(stream: InputStream, exit: Channel<Unit>) {
    val reader = stream.reader(charset = Charsets.UTF_8).buffered()
    reader.use { reader ->
      while (isActive) {
        val line = reader.readLine() ?: break
        if (exit.tryReceive().isSuccess) {
          break
        }
        bspClient.message(line)
        logger.info(line)
      }
    }
  }

  private fun logInvocation(cmd: List<String>) {
    val invokeCommand = cmd.joinToString("' '", "'", "'")
    bspClient.message("Invoking: $invokeCommand")
    logger.info("Invoking: $invokeCommand")
  }

}

private open class CmdBuilder : StartupOptions, Args {
  val args: MutableList<Arg> = mutableListOf()

  override fun add(arg: Arg) {
    args.add(arg)
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

  fun build(): List<String> = args.map {
    when (it) {
      is Arg.Named -> it.value.toArgValue(it.name)
      is Arg.Positional -> it.value.toStringValue()
    }
  }

}

private suspend fun createTmpFile(): Path = withContext(Dispatchers.IO) { Files.createTempFile("bazel", "tmp") }
