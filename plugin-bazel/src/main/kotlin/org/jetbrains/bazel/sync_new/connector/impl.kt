package org.jetbrains.bazel.sync_new.connector

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.logger.BspClientLogger
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.collections.joinToString
import kotlin.io.path.absolutePathString

// legacy - using bsp client for logging output to console
class LegacyBazelConnectorImpl(
  private val project: Project,
  private val bspClient: BspClientLogger,
  private val originId: String? = null,
) : BazelConnector {
  private val logger = logger<LegacyBazelConnectorImpl>()
  private val executor = Executors.newCachedThreadPool()

  override fun query(
    startup: StartupOptions.() -> Unit,
    args: QueryArgs.() -> Unit,
  ): BazelResult<QueryResult> {

  }

  private fun spawn(builder: CmdBuilder.() -> Unit, logInvocation: Boolean = true) {
    val cmd = CmdBuilder().apply(builder).cmd
    if (logInvocation) {
      logInvocation(cmd)
    }
    val process = GeneralCommandLine(cmd)
      .withWorkingDirectory(project.rootDir.toNioPath())
      .createProcess()

  }

  private fun logInvocation(cmd: List<String>) {
    val invokeCommand = cmd.joinToString("' '", "'", "'")
    bspClient.message("Invoking: $invokeCommand")
    logger.info("Invoking: $invokeCommand")
  }

}

private open class CmdBuilder(
  val cmd: MutableList<String> = mutableListOf()
) : StartupOptions, Args {
  fun push(value: String) {
    cmd.add(value)
  }

  fun push(name: String, value: Value) {
    when (value) {
      is Value.VBool -> {
        if (value.value) {
          push("--$name")
        } else {
          push("--no$name")
        }
      }
      is Value.VFile -> {
        push("--$name=\"${value.path.absolutePathString()}\"")
      }
      is Value.VFloat -> {
        push("--$name=${value.number}")
      }
      is Value.VInt -> {
        push("--$name=${value.number}")
      }
      is Value.VText -> {
        push("--$name=\"${value.text}\"")
      }
    }
  }

  override fun add(name: String, value: Value) = push(name, value)
}
