package org.jetbrains.plugins.bsp.services

import ch.epfl.scala.bsp4j.BspConnectionDetails
import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildClientCapabilities
import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.DidChangeBuildTarget
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.LogMessageParams
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.ShowMessageParams
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartParams
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.build.AbstractViewManager
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.SyncViewManager
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FinishBuildEventImpl
import com.intellij.build.events.impl.MessageEventImpl
import com.intellij.build.events.impl.ProgressBuildEventImpl
import com.intellij.build.events.impl.StartBuildEventImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.util.concurrency.AppExecutorUtil
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.protocol.connection.LocatedBspConnectionDetails
import java.nio.file.Path

public interface BspServer : BuildServer

public class BspConnectionService(private val project: Project) {

  public var server: BspServer? = null

  public var bspResolver: VeryTemporaryBspResolver? = null

  public fun connect(connectionFile: LocatedBspConnectionDetails) {
    val process = createAndStartProcess(connectionFile.bspConnectionDetails)
    // TODO
    val buildView = project.getService(SyncViewManager::class.java)

    val client = BspClient(buildView, "xd2", "xd")

    val launcher = createLauncher(process, client)

    launcher.startListening()
    server = launcher.remoteProxy
    client.onConnectWithServer(server)

    bspResolver = VeryTemporaryBspResolver(project.stateStore.projectBasePath, server!!, project, buildView)
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


public class VeryTemporaryBspResolver(private val projectBaseDir: Path, private val server: BspServer, private val project: Project, private val buildViewManager: AbstractViewManager) {

  public fun collectModel(): ProjectDetails {
    val buildId = "xd"
    val title = "Title 2"
    val basePath = project.basePath!!
    val buildDescriptor = DefaultBuildDescriptor(buildId, title, basePath, System.currentTimeMillis())


    var workspaceBuildTargetsResult: WorkspaceBuildTargetsResult? = null
    var allTargetsIds: List<BuildTargetIdentifier>? = null
    var sourcesResult: SourcesResult? = null
    var resourcesResult: ResourcesResult? = null
    var dependencySourcesResult: DependencySourcesResult? = null

//    val task = object : Task.Backgroundable(project, "Loading changes 1 ", true) {
//
//      override fun run(indicator: ProgressIndicator) {

        val startEvent = StartBuildEventImpl(buildDescriptor, "message")
        buildViewManager.onEvent(buildId, startEvent)

//        indicator.text = "Loading changes 2"
//        indicator.isIndeterminate = true
//        indicator.fraction = 0.0
        val progressEvent = ProgressBuildEventImpl(
          "xd2", buildId, System.currentTimeMillis(), "Tmport", -1,
          -1, "kłykcie"
        )
        buildViewManager.onEvent(buildId, progressEvent)

        println("buildInitialize")
        server.buildInitialize(createInitializeBuildParams()).get()

        println("onBuildInitialized")
        server.onBuildInitialized()

        println("workspaceBuildTargets")
        workspaceBuildTargetsResult = server.workspaceBuildTargets().get()
        allTargetsIds = workspaceBuildTargetsResult!!.targets.map(BuildTarget::getId)

        println("buildTargetSources")
        sourcesResult = server.buildTargetSources(SourcesParams(allTargetsIds)).get()

        println("buildTargetResources")
        resourcesResult = server.buildTargetResources(ResourcesParams(allTargetsIds)).get()

        println("buildTargetDependencySources")
        dependencySourcesResult = server.buildTargetDependencySources(DependencySourcesParams(allTargetsIds)).get()

        println("done!")
        val xd = FinishBuildEventImpl(
          "xd2", null, System.currentTimeMillis(), "Tmport 2", SuccessResultImpl()
        )
        buildViewManager.onEvent(buildId, xd)
        println("done!")
        val xd2 = FinishBuildEventImpl(
          buildId, null, System.currentTimeMillis(), "Tmport 233", SuccessResultImpl()
        )
        buildViewManager.onEvent(buildId, xd2)
//        buildViewManager.dispose()
//      }
//    }
//    task.queue()
//    ProgressManager.getInstance().run(task)


    println("done done!")
    return ProjectDetails(
      targetsId = allTargetsIds!!,
      targets = workspaceBuildTargetsResult!!.targets.toSet(),
      sources = sourcesResult!!.items,
      resources = resourcesResult!!.items,
      dependenciesSources = dependencySourcesResult!!.items,
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

private class BspClient(private val buildViewManager: AbstractViewManager, private val randomId: String, private val buildId: String) : BuildClient {
  override fun onBuildShowMessage(params: ShowMessageParams?) {
    println("onBuildShowMessage")
    println(params)

    val event = MessageEventImpl(randomId, MessageEvent.Kind.SIMPLE, null, "", params?.message)
    buildViewManager.onEvent(buildId, event)
  }

  override fun onBuildLogMessage(params: LogMessageParams?) {
    println("onBuildLogMessage")
    println(params)

    val event = MessageEventImpl(randomId, MessageEvent.Kind.SIMPLE, null, "", params?.message)
    buildViewManager.onEvent(buildId, event)
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
