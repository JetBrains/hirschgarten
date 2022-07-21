package org.jetbrains.plugins.bsp.services

import ch.epfl.scala.bsp4j.BspConnectionDetails
import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildClientCapabilities
import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DidChangeBuildTarget
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.LogMessageParams
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ShowMessageParams
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartParams
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.util.concurrency.AppExecutorUtil
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.ui.console.BspSyncConsole
import org.jetbrains.protocol.connection.LocatedBspConnectionDetails
import java.nio.file.Path

public interface BspServer : BuildServer

public class BspConnectionService(private val project: Project) {

  public var server: BspServer? = null

  public fun connect(connectionFile: LocatedBspConnectionDetails) {
    val process = createAndStartProcess(connectionFile.bspConnectionDetails)
    val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)

    val client = BspClient(bspSyncConsoleService.bspSyncConsole)

    val launcher = createLauncher(process, client)

    launcher.startListening()
    server = launcher.remoteProxy
    client.onConnectWithServer(server)

  }

  private fun createAndStartProcess(bspConnectionDetails: BspConnectionDetails): Process =
    ProcessBuilder(bspConnectionDetails.argv)
      .directory(project.stateStore.projectBasePath.toFile())
      .start()

  private fun createLauncher(process: Process, client: BuildClient): Launcher<BspServer> =
    Launcher.Builder<BspServer>()
      .setRemoteInterface(BspServer::class.java)
      .setExecutorService(AppExecutorUtil.getAppExecutorService())
      .setInput(process.inputStream)
      .setOutput(process.outputStream)
      .setLocalService(client)
      .create()

  public fun disconnect() {
  }

  public companion object {
    public fun getInstance(project: Project): BspConnectionService =
      project.getService(BspConnectionService::class.java)
  }
}


public class VeryTemporaryBspResolver(
  private val projectBaseDir: Path,
  private val server: BspServer,
  private val bspSyncConsole: BspSyncConsole
) {

  public fun collectModel(): ProjectDetails {
    bspSyncConsole.startImport("bsp-import", "BSP: Import", "Importing...")

    println("buildInitialize")
    server.buildInitialize(createInitializeBuildParams()).get()

    println("onBuildInitialized")
    server.onBuildInitialized()

    println("workspaceBuildTargets")
    val workspaceBuildTargetsResult = server.workspaceBuildTargets().get()
    val allTargetsIds = workspaceBuildTargetsResult!!.targets.map(BuildTarget::getId)

    println("buildTargetSources")
    val sourcesResult = server.buildTargetSources(SourcesParams(allTargetsIds)).get()

    println("buildTargetResources")
    val resourcesResult = server.buildTargetResources(ResourcesParams(allTargetsIds)).get()

    println("buildTargetDependencySources")
    val dependencySourcesResult = server.buildTargetDependencySources(DependencySourcesParams(allTargetsIds)).get()

    bspSyncConsole.finishImport("Import done!")

    println("done done!")
    return ProjectDetails(
      targetsId = allTargetsIds,
      targets = workspaceBuildTargetsResult.targets.toSet(),
      sources = sourcesResult.items,
      resources = resourcesResult.items,
      dependenciesSources = dependencySourcesResult.items,
    )
  }

  private fun createInitializeBuildParams(): InitializeBuildParams {
    val params = InitializeBuildParams(
      "IntelliJ-BSP",
      "1.0.0",
      "2.0.0",
      projectBaseDir.toString(),
      BuildClientCapabilities(listOf("java"))
    )
    val dataJson = JsonObject()
    dataJson.addProperty("clientClassesRootDir", "$projectBaseDir/out")
    dataJson.add("supportedScalaVersions", JsonArray())
    params.data = dataJson

    return params
  }
}

private class BspClient(private val bspSyncConsole: BspSyncConsole) : BuildClient {

  override fun onBuildShowMessage(params: ShowMessageParams) {
    println("onBuildShowMessage")
    println(params)

    bspSyncConsole.addMessage(params.task?.id, params.message)
  }

  override fun onBuildLogMessage(params: LogMessageParams) {
    println("onBuildLogMessage")
    println(params)

    bspSyncConsole.addMessage(params.task?.id, params.message)
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
