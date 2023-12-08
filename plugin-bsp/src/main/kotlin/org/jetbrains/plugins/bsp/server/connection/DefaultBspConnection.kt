package org.jetbrains.plugins.bsp.server.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.InitializeBuildParams
import com.google.gson.JsonObject
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.execution.process.OSProcessUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.project.stateStore
import com.intellij.util.concurrency.AppExecutorUtil
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.bsp.BSP_CLIENT_NAME
import org.jetbrains.bsp.BSP_VERSION
import org.jetbrains.bsp.BazelBuildServerCapabilities
import org.jetbrains.bsp.CLIENT_CAPABILITIES
import org.jetbrains.bsp.utils.BazelBuildServerCapabilitiesTypeAdapter
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.server.ChunkingBuildServer
import org.jetbrains.plugins.bsp.server.client.BspClient
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import org.jetbrains.plugins.bsp.utils.withRealEnvs
import java.io.InputStream
import java.io.OutputStream
import java.lang.System.currentTimeMillis
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.seconds

private const val OK_EXIT_CODE = 0
private const val TERMINATED_EXIT_CODE = 130
private val TERMINATION_TIMEOUT = 10.seconds

private class CancelableInvocationHandlerWithTimeout(
  private val remoteProxy: BspServer,
  private val cancelOnFuture: CompletableFuture<Void>,
  private val timeoutHandler: TimeoutHandler,
) : InvocationHandler {
  override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
    val startTime = currentTimeMillis()
    log.debug("Running BSP request '${method.name}'")

    return when (val result = invokeMethod(method, args)) {
      is CompletableFuture<*> -> addTimeoutAndHandler(result, startTime, method.name)
      else -> result
    }
  }

  private fun invokeMethod(method: Method, args: Array<out Any>?): Any? =
    method.invoke(remoteProxy, *args ?: emptyArray())

  private fun addTimeoutAndHandler(
    result: CompletableFuture<*>,
    startTime: Long,
    methodName: String,
  ): CompletableFuture<Any?> =
    CancellableFuture.from(result)
      .reactToExceptionIn(cancelOnFuture)
      .reactToExceptionIn(timeoutHandler.getUnfinishedTimeoutFuture())
      .handle { value, error -> doHandle(value, error, startTime, methodName) }

  private fun doHandle(value: Any?, error: Throwable?, startTime: Long, methodName: String): Any? {
    val elapsedTime = calculateElapsedTime(startTime)
    log.debug(
      "BSP method '$methodName' call took ${elapsedTime}ms. " +
        "Result: ${if (error == null) "SUCCESS" else "FAILURE"}"
    )

    return when (error) {
      null -> value
      else -> handleError(error, methodName, elapsedTime)
    }
  }

  private fun calculateElapsedTime(startTime: Long): Long =
    currentTimeMillis() - startTime

  private fun handleError(error: Throwable, methodName: String, elapsedTime: Long): Nothing {
    when (error) {
      is TimeoutException -> log.error("BSP request '$methodName' timed out after ${elapsedTime}ms", error)
    }
    throw error
  }

  private companion object {
    private var log = logger<CancelableInvocationHandlerWithTimeout>()
  }
}

private val log = logger<DefaultBspConnection>()
private const val connectSubtaskId = "bsp-file-connection-connect"

internal class DefaultBspConnection(
  private val project: Project,
  private val connectionDetailsProviderExtension: ConnectionDetailsProviderExtension,
) : BspConnection {
  private var connectionDetails: BspConnectionDetails? = null

  private var server: BspServer? = null
  private var capabilities: BazelBuildServerCapabilities? = null

  private var bspProcess: Process? = null
  private var disconnectActions: MutableList<() -> Unit> = mutableListOf()
  private val timeoutHandler = TimeoutHandler { Registry.intValue("bsp.request.timeout.seconds").seconds }

  override fun connect(taskId: Any, errorCallback: () -> Unit) {
    if (!isConnected()) {
      doConnect(taskId, errorCallback)
    }
  }

  private fun doConnect(taskId: Any, errorCallback: () -> Unit) {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole

    bspSyncConsole.startSubtask(
      parentTaskId = taskId,
      subtaskId = connectSubtaskId,
      message = BspPluginBundle.message("console.subtask.connect.in.progress"),
    )

    try {
      connectOrThrowIfFailed(bspSyncConsole, taskId, errorCallback)
    } catch (e: Exception) {
      bspSyncConsole.finishTask(
        taskId = taskId,
        message = BspPluginBundle.message("console.task.connect.message.failed"),
        result = FailureResultImpl(e),
      )
    }
  }

  private fun connectOrThrowIfFailed(bspSyncConsole: TaskConsole, taskId: Any, errorCallback: () -> Unit) {
    bspSyncConsole.addMessage(
      taskId = connectSubtaskId,
      message = BspPluginBundle.message("console.task.connect.message.in.progress"),
    )

    val newConnectionDetails = connectionDetailsProviderExtension.provideNewConnectionDetails(project, null)

    if (newConnectionDetails != null) {
      this.connectionDetails = newConnectionDetails
      newConnectionDetails.connect(bspSyncConsole, taskId, errorCallback)
    } else {
      error("Cannot connect. Please check your connection file.")
    }
  }

  private fun BspConnectionDetails.connect(bspSyncConsole: TaskConsole, taskId: Any, errorCallback: () -> Unit) {
    val client = createBspClient()
    val process = createAndStartProcessAndAddDisconnectActions(this)

    process.handleErrorOnExit(bspSyncConsole, taskId, errorCallback)

    bspProcess = process
    bspSyncConsole.addMessage(connectSubtaskId, BspPluginBundle.message("console.task.connect.message.success"))

    initializeServer(process, client, bspSyncConsole)
  }

  private fun createBspClient(): BspClient {
    val bspConsoleService = BspConsoleService.getInstance(project)

    return BspClient(
      bspConsoleService.bspSyncConsole,
      bspConsoleService.bspBuildConsole,
      bspConsoleService.bspRunConsole,
      bspConsoleService.bspTestConsole,
      timeoutHandler,
    )
  }

  private fun createAndStartProcessAndAddDisconnectActions(bspConnectionDetails: BspConnectionDetails): Process {
    val process = bspConnectionDetails.createAndStartProcess()
    process.logErrorOutputs(project)
    disconnectActions.add { server?.buildShutdown()?.get(TERMINATION_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS) }
    disconnectActions.add { server?.onBuildExit() }

    disconnectActions.add { process.terminateDescendants() }
    disconnectActions.add {
      process.toHandle().terminateGracefully()
      process.checkExitValueAndThrowIfError()
    }

    return process
  }

  private fun BspConnectionDetails.createAndStartProcess(): Process =
    ProcessBuilder(argv)
      .directory(project.stateStore.projectBasePath.toFile())
      .withRealEnvs()
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .start()

  private fun ProcessHandle.terminateGracefully() {
    try {
      OSProcessUtil.terminateProcessGracefully(this.pid().toInt())
    } catch (e: Exception) {
      log.debug("OSProcessUtil.terminateProcessGracefully not supported! Fallback to '.destroy()'", e)
      this.destroy()
    }
    this.onExit().get(TERMINATION_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS)
  }

  private fun Process.terminateDescendants() =
    this.descendants().forEach { it.terminateGracefully() }

  private fun Process.checkExitValueAndThrowIfError() {
    val exitValue = this.exitValue()
    if (exitValue != OK_EXIT_CODE && exitValue != TERMINATED_EXIT_CODE) {
      error("Server exited with exit value: $exitValue")
    }
  }

  private fun Process.handleErrorOnExit(bspSyncConsole: TaskConsole, taskId: Any, errorCallback: () -> Unit) =
    this.onExit().whenComplete { completedProcess, _ ->
      val exitValue = completedProcess.exitValue()
      if (exitValue != OK_EXIT_CODE && exitValue != TERMINATED_EXIT_CODE) {
        val errorMessage = BspPluginBundle.message("console.server.exited", exitValue)
        bspSyncConsole.finishTask(taskId, errorMessage, FailureResultImpl(errorMessage))
        errorCallback()
      }
    }

  private fun initializeServer(
    process: Process,
    client: BspClient,
    bspSyncConsole: TaskConsole,
  ) {
    bspSyncConsole.addMessage(
      connectSubtaskId,
      BspPluginBundle.message("console.message.initialize.server.in.progress")
    )

    val newServer = startServerAndAddDisconnectActions(process, client)
    server = newServer.wrapInChunkingServerIfRequired()
    capabilities = server?.initializeAndObtainCapabilities()

    bspSyncConsole.addMessage(connectSubtaskId, BspPluginBundle.message("console.message.initialize.server.success"))
    bspSyncConsole.finishSubtask(connectSubtaskId, BspPluginBundle.message("console.subtask.connect.success"))
  }

  private fun startServerAndAddDisconnectActions(process: Process, client: BuildClient): BspServer {
    val bspIn = process.inputStream
    disconnectActions.add { bspIn.close() }

    val bspOut = process.outputStream
    disconnectActions.add { bspOut.close() }

    val launcher = createLauncher(bspIn, bspOut, client)
    val listening = launcher.startListening()
    disconnectActions.add { listening.cancel(true) }

    val cancelOnFuture = CompletableFuture<Void>()
    disconnectActions.add { cancelOnFuture.cancel(true) }

    val remoteProxy = launcher.remoteProxy
    return Proxy.newProxyInstance(
      javaClass.classLoader,
      arrayOf(BspServer::class.java),
      CancelableInvocationHandlerWithTimeout(remoteProxy, cancelOnFuture, timeoutHandler),
    ) as BspServer
  }

  private fun createLauncher(bspIn: InputStream, bspOut: OutputStream, client: BuildClient): Launcher<BspServer> =
    Launcher.Builder<BspServer>()
      .setRemoteInterface(BspServer::class.java)
      .setExecutorService(AppExecutorUtil.getAppExecutorService())
      .setInput(bspIn)
      .setOutput(bspOut)
      .setLocalService(client)
      // Allows us to deserialize our custom capabilities
      .configureGson { builder ->
        builder.registerTypeAdapter(
          BuildServerCapabilities::class.java,
          BazelBuildServerCapabilitiesTypeAdapter(),
        )
      }
      .create()

  private fun BspServer.wrapInChunkingServerIfRequired(): BspServer =
    if (Registry.`is`("bsp.request.chunking.enable")) {
      val minChunkSize = Registry.intValue("bsp.request.chunking.size.min")
      ChunkingBuildServer(this, minChunkSize)
    } else this

  private fun BspServer.initializeAndObtainCapabilities(): BazelBuildServerCapabilities {
    val buildInitializeResults = buildInitialize(createInitializeBuildParams()).get()
    onBuildInitialized()
    // cast is safe because we registered a custom type adapter
    return buildInitializeResults.capabilities as BazelBuildServerCapabilities
  }

  private fun createInitializeBuildParams(): InitializeBuildParams {
    val projectBaseDir = project.rootDir
    val params = InitializeBuildParams(
      BSP_CLIENT_NAME,
      "2023.3-EAP",
      BSP_VERSION,
      projectBaseDir.toString(),
      CLIENT_CAPABILITIES,
    )
    val dataJson = JsonObject()
    dataJson.addProperty("clientClassesRootDir", "$projectBaseDir/out")
    params.data = dataJson

    return params
  }

  override fun <T> runWithServer(task: (server: BspServer, capabilities: BazelBuildServerCapabilities) -> T?): T? {
    val currentConnectionDetails =
      connectionDetailsProviderExtension.provideNewConnectionDetails(project, connectionDetails)

    if (currentConnectionDetails != null) {
      currentConnectionDetails.autoConnect()
    } else if (!isConnected()) {
      connectionDetails?.autoConnect()
    }

    if (server != null && capabilities != null) {
      return task(server!!, capabilities!!)
    } else {
      error("Cannot execute the task. Server not available.")
    }
  }

  private fun BspConnectionDetails.autoConnect() {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    bspSyncConsole.startTask(
      taskId = "bsp-autoconnect",
      title = BspPluginBundle.message("console.task.auto.connect.title"),
      message = BspPluginBundle.message("console.task.auto.connect.in.progress"),
    )

    try {
      disconnect()
      this.connect(bspSyncConsole, "bsp-autoconnect") {}
      bspSyncConsole.finishTask(
        taskId = "bsp-autoconnect",
        message = BspPluginBundle.message("console.task.auto.connect.success"),
      )
    } catch (e: Exception) {
      bspSyncConsole.finishTask(
        taskId = "bsp-autoconnect",
        message = BspPluginBundle.message("console.task.auto.connect.failed"),
        result = FailureResultImpl(e),
      )
      error("Auto connect has failed.")
    }
  }

  override fun disconnect() {
    if (isConnected()) {
      val exceptions = executeDisconnectActionsAndCollectExceptions(disconnectActions)
      throwExceptionWithSuppressedIfOccurred(exceptions)
    }

    disconnectActions.clear()
    server = null
    capabilities = null
  }

  private fun executeDisconnectActionsAndCollectExceptions(disconnectActions: List<() -> Unit>): List<Throwable> =
    disconnectActions.mapNotNull { executeDisconnectActionAndReturnThrowableIfFailed(it) }

  private fun executeDisconnectActionAndReturnThrowableIfFailed(disconnectAction: () -> Unit): Throwable? =
    try {
      disconnectAction()
      null
    } catch (e: Exception) {
      e
    }

  private fun throwExceptionWithSuppressedIfOccurred(exceptions: List<Throwable>) {
    val firstException = exceptions.firstOrNull()

    if (firstException != null) {
      exceptions
        .drop(1)
        .forEach { firstException.addSuppressed(it) }

      throw firstException
    }
  }

  override fun isConnected(): Boolean =
    bspProcess?.isAlive == true
}

private fun Process.logErrorOutputs(project: Project) {
  if (!Registry.`is`("bsp.log.error.outputs")) return
  @Suppress("DeferredResultUnused")
  val bspConsoleService = BspConsoleService.getInstance(project)
  BspCoroutineService.getInstance(project).startAsync {
    val bufferedReader = this.errorReader()
    bufferedReader.forEachLine { doLogErrorOutputLine(it, bspConsoleService) }
  }
}

private fun doLogErrorOutputLine(line: String, bspConsoleService: BspConsoleService) {
  val taskConsole = bspConsoleService.getActiveConsole()
  taskConsole?.addMessage(line)
}

private fun BspConsoleService.getActiveConsole(): TaskConsole? =
  if (this.bspBuildConsole.hasTasksInProgress()) this.bspBuildConsole
  else if (this.bspSyncConsole.hasTasksInProgress()) this.bspSyncConsole
  else null
