package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import org.eclipse.lsp4j.jsonrpc.Launcher
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Executors


/**
 * A session is a "physical" connection to a BSP server. It must be closed when it is no longer
 * needed. The user is responsible for maintaining the correct BSP life-cycle.
 */
class Session(val workspacePath: Path, val coroutineScope: CoroutineScope) : AutoCloseable {
  private val workspaceFile = workspacePath.toFile()
  private val connectionDetails = readBspConnectionDetails(workspaceFile)

  public val process: Process = ProcessBuilder(connectionDetails.argv)
    .directory(workspaceFile)
    .start()

  val client: MockClient = MockClient()

  private val executor = Executors.newCachedThreadPool()
  private val launcher = Launcher.Builder<MockServer>()
    .setRemoteInterface(MockServer::class.java)
    .setExecutorService(executor)
    .setInput(process.inputStream)
    .setOutput(process.outputStream)
    .setLocalService(client)
    .create()

  init {
    launcher.startListening()
  }

  val server: MockServer = launcher.remoteProxy

  val serverClosed: Deferred<SessionResult> = process.onExit().thenApply {
    SessionResult(process.exitValue(), process.errorStream.bufferedReader().readText())
  }.asDeferred()

  override fun close() {
    executor.shutdown()
    process.inputStream.close()
    process.outputStream.close()
    process.errorStream.close()
    process.destroy()
  }

  companion object {
    const val bspVersion = "2.0.0"
    private const val BspWorkspaceConfigDirName = ".bsp"
    private const val BspWorkspaceConfigFileExtension = ".json"
    private val gson = Gson()

    private fun readBspConnectionDetails(workspace: File): BspConnectionDetails {
      val bspDir = File(workspace, BspWorkspaceConfigDirName)
      require(bspDir.exists() && bspDir.isDirectory)

      val configFiles = bspDir.listFiles { _, name -> name.endsWith(BspWorkspaceConfigFileExtension) }
      require(configFiles?.size == 1)

      val configFile = configFiles.first()
      require(configFile.isFile && configFile.canRead())

      return gson.fromJson(configFile.bufferedReader(), BspConnectionDetails::class.java)
    }
  }
}

data class SessionResult(val exitCode: Int, val stderr: String)
