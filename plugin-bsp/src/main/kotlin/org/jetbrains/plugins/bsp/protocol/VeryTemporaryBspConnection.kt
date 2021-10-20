package org.jetbrains.plugins.bsp.protocol

import ch.epfl.scala.bsp4j.BspConnectionDetails
import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.DidChangeBuildTarget
import ch.epfl.scala.bsp4j.LogMessageParams
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.ShowMessageParams
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartParams
import com.google.gson.Gson
import com.intellij.util.concurrency.AppExecutorUtil
import org.eclipse.lsp4j.jsonrpc.Launcher
import java.io.File
import java.nio.file.Path

interface BspServer : BuildServer

internal class VeryTemporaryBspConnection(projectBaseDir: Path) {

  val bspServer = connect(projectBaseDir)

  private fun connect(projectBaseDir: Path): BspServer {
    val config = getConfig(projectBaseDir)

    val client = BspClient()
    val process = ProcessBuilder(config.argv)
      .directory(projectBaseDir.toFile())
      .start()

    val launcher = Launcher.Builder<BspServer>()
      .setRemoteInterface(BspServer::class.java)
      .setExecutorService(AppExecutorUtil.getAppExecutorService())
      .setInput(process.inputStream)
      .setOutput(process.outputStream)
      .setLocalService(client)
      .create()

    launcher.startListening()
    val server = launcher.remoteProxy
    client.onConnectWithServer(server)

    return server
  }

  private fun getConfig(projectBaseDir: Path): BspConnectionDetails {
    val configFile = getConfigFile(projectBaseDir)

    return Gson().fromJson(configFile.readText(), BspConnectionDetails::class.java)
  }

  private fun getConfigFile(projectBaseDir: Path): File =
    File(projectBaseDir.toFile(), ".bsp")
      .listFiles { file -> file.name.endsWith(".json") }
      .first()
}

private class BspClient : BuildClient {
  override fun onBuildShowMessage(params: ShowMessageParams?) {
    println("onBuildShowMessage")
    println(params)
  }

  override fun onBuildLogMessage(params: LogMessageParams?) {
    println("onBuildLogMessage")
    println(params)
  }

  override fun onBuildTaskStart(params: TaskStartParams?) {
    println("onBuildTaskStart")
    println(params)
  }

  override fun onBuildTaskProgress(params: TaskProgressParams?) {
    println("onBuildTaskProgress")
    println(params)
  }

  override fun onBuildTaskFinish(params: TaskFinishParams?) {
    println("onBuildTaskFinish")
    println(params)
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams?) {
    println("onBuildPublishDiagnostics")
    println(params)
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget?) {
    println("onBuildTargetDidChange")
    println(params)
  }
}
