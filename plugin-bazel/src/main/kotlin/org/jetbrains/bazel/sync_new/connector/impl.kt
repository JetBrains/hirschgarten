package org.jetbrains.bazel.sync_new.connector

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import com.intellij.execution.configurations.GeneralCommandLine
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
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.logger.BspClientLogger
import java.io.InputStream
import java.nio.file.Files
import kotlin.collections.joinToString
import kotlin.io.path.absolutePathString

// legacy - using bsp client for logging output to console
class LegacyBazelConnectorImpl(
  private val project: Project,
  private val executable: String,
  private val bspClient: BspClientLogger,
  private val originId: String? = null,
  private val coroutineScope: CoroutineScope,
) : BazelConnector {
  companion object {
    private val logger = logger<LegacyBazelConnectorImpl>()
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

    val queryFile = withContext(Dispatchers.IO) { Files.createTempFile("bazel", "query") }
    val query = builder.popValue("query")?.require<Value.VText>() ?: error("query must be specified")
    withContext(Dispatchers.IO) { Files.writeString(queryFile, query.text) }
    builder.add("query_file", argValueOf(queryFile))

    val output = builder.getValue("output")?.require<Value.VText>() ?: error("output must be specified")
    return when (output.text) {
      "proto" -> {
        val (process, _) = spawn(builder = builder, capture = false)
        val result = Build.QueryResult.parseFrom(process.inputStream)
        BazelResult.Success(QueryResult.Proto(result))
      }

      "streamed_proto" -> {
        val outputFile = withContext(Dispatchers.IO) { Files.createTempFile("bazel", "out") }
        val outputFileStream = withContext(Dispatchers.IO) { Files.newInputStream(outputFile) }
        builder.add("--output_file", argValueOf(outputFile))
        val (process, _) = spawn(builder = builder, capture = false)
        val flow = flow { emit(Build.Target.parseDelimitedFrom(outputFileStream)) }
          .takeWhile { it != null }
          .onCompletion { outputFileStream.close() }
          .flowOn(Dispatchers.IO)
        BazelResult.Success(QueryResult.StreamedProto(flow))
      }

      else -> error("unsupported output type")
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
    val outChannel = Channel<String?>()

    if (capture) {
      coroutineScope.launch { capture(process.inputStream, outChannel, exitChannel) }
      coroutineScope.launch { capture(process.errorStream, outChannel, exitChannel) }
      coroutineScope.launch { consume(outChannel) }
    }

    val code = process.awaitExit()
    exitChannel.send(Unit)

    return Pair(process, code)
  }

  private suspend fun CoroutineScope.capture(stream: InputStream, out: Channel<String?>, exit: Channel<Unit>) {
    val reader = stream.reader(charset = Charsets.UTF_8).buffered()
    reader.use { reader ->
      while (isActive) {
        val line = reader.readLine() ?: break
        if (exit.tryReceive().isSuccess) {
          out.send(null)
          break
        }
        out.send(line)
      }
    }
  }

  private suspend fun CoroutineScope.consume(out: Channel<String?>) {
    while (isActive) {
      val line = out.receive() ?: return
      bspClient.message(line)
      logger.info(line)
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
        "--$arg=\"${this.path.absolutePathString()}\""
      }

      is Value.VFloat -> {
        "--$arg=${this.number}"
      }

      is Value.VInt -> {
        "--$arg=${this.number}"
      }

      is Value.VText -> {
        "--$arg=\"${this.text}\""
      }
    }
  }

  private fun Value.toStringValue(): String {
    return when (this) {
      is Value.VBool -> this.value.toString()
      is Value.VFile -> this.path.absolutePathString()
      is Value.VFloat -> this.number.toString()
      is Value.VInt -> this.number.toString()
      is Value.VText -> this.text
    }
  }

  fun build(): List<String> = args.map {
    when (it) {
      is Arg.Named -> it.value.toArgValue(it.name)
      is Arg.Positional -> it.value.toStringValue()
    }
  }

}
