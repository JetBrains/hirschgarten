package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.bsp.testkit.client.Session.readBspConnectionDetails

import java.io.File
import java.nio.file.Path
import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.jdk.FutureConverters.CompletionStageOps

/** A session is a "physical" connection to a BSP server. It must be closed when it is no longer
 * needed. The user is responsible for maintaining the correct BSP life-cycle.
 *
 * @param workspacePath
 * the path to the workspace
 */
class Session(val workspacePath: Path) extends AutoCloseable {
  private val workspaceFile = workspacePath.toFile
  private val connectionDetails = readBspConnectionDetails(workspaceFile)

  private val process =
    new ProcessBuilder(connectionDetails.getArgv)
      .directory(workspaceFile)
      .start() // side-effect: starts the process

  val client: MockClient = new MockClient()

  private val executor: ExecutorService = Executors.newCachedThreadPool()

  private val launcher = new Launcher.Builder[MockServer]()
    .setRemoteInterface(classOf[MockServer])
    .setExecutorService(executor)
    .setInput(process.getInputStream)
    .setOutput(process.getOutputStream)
    .setLocalService(client)
    .create()

  launcher.startListening() // side-effect: server starts listening for messages

  val server: MockServer = launcher.getRemoteProxy

  def serverClosed: Future[SessionResult] = process
    .onExit()
    .asScala
    .map(process =>
      SessionResult(process.exitValue(), Source.fromInputStream(process.getErrorStream).mkString)
    )(ExecutionContext.global)

  def close(): Unit = {
    executor.shutdown()
    process.getInputStream.close()
    process.getOutputStream.close()
    process.getErrorStream.close()
    process.destroy()
  }
}

case class SessionResult(exitCode: Int, stderr: String)

object Session {
  val bspVersion = "2.0.0"

  private val BspWorkspaceConfigDirName = ".bsp"
  private val BspWorkspaceConfigFileExtension = ".json"
  private val gson = new Gson()

  private def readBspConnectionDetails(workspace: File): BspConnectionDetails = {
    val bspDir = new File(workspace, BspWorkspaceConfigDirName)
    assert(bspDir.exists, s"$workspace is not a BSP workspace")
    assert(bspDir.isDirectory, s"$workspace is not a BSP workspace")

    val configFiles =
      bspDir.listFiles(file => file.getName.endsWith(BspWorkspaceConfigFileExtension))
    assert(
      configFiles.size == 1,
      s"$workspace should have exactly one BSP workspace configuration file"
    )

    val configFile = configFiles.head
    assert(
      configFile.isFile,
      s"$workspace should have exactly one BSP workspace configuration file"
    )
    assert(
      configFile.canRead,
      s"$workspace should have exactly one BSP workspace configuration file"
    )

    gson.fromJson(Source.fromFile(configFile).bufferedReader(), classOf[BspConnectionDetails])
  }
}
