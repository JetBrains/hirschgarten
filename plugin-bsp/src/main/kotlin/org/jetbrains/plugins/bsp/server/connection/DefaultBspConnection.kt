package org.jetbrains.plugins.bsp.server.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.SourceItem
import com.google.gson.JsonObject
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.execution.process.OSProcessUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.platform.diagnostic.telemetry.impl.getOtlpEndPoint
import com.intellij.project.stateStore
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.bsp.protocol.BSP_CLIENT_NAME
import org.jetbrains.bsp.protocol.BSP_CLIENT_VERSION
import org.jetbrains.bsp.protocol.BSP_VERSION
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.CLIENT_CAPABILITIES
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.utils.BazelBuildServerCapabilitiesTypeAdapter
import org.jetbrains.bsp.protocol.utils.EnhancedSourceItemTypeAdapter
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.BspServerProvider
import org.jetbrains.plugins.bsp.extension.points.GenericConnection
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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

private const val OK_EXIT_CODE = 0
private const val TERMINATED_EXIT_CODE = 130
private val TERMINATION_TIMEOUT = 10.seconds

private class CancelableInvocationHandlerWithTimeout(
  private val remoteProxy: JoinedBuildServer,
  private val cancelOnFuture: CompletableFuture<Void>,
  private val timeoutHandler: TimeoutHandler,
) : InvocationHandler {
  override fun invoke(
    proxy: Any,
    method: Method,
    args: Array<out Any>?,
  ): Any? {
    val startTime = currentTimeMillis()
    log.debug("Running BSP request '${method.name}'")

    return when (val result = invokeMethod(method, args)) {
      is CompletableFuture<*> -> addTimeoutAndHandler(result, startTime, method.name)
      else -> result
    }
  }

  private fun invokeMethod(method: Method, args: Array<out Any>?): Any? = method.invoke(remoteProxy, *args ?: emptyArray())

  private fun addTimeoutAndHandler(
    result: CompletableFuture<*>,
    startTime: Long,
    methodName: String,
  ): CompletableFuture<Any?> =
    CancellableFuture
      .from(result)
      .reactToExceptionIn(cancelOnFuture)
      .reactToExceptionIn(timeoutHandler.getUnfinishedTimeoutFuture())
      .handle { value, error -> doHandle(value, error, startTime, methodName) }

  private fun doHandle(
    value: Any?,
    error: Throwable?,
    startTime: Long,
    methodName: String,
  ): Any? {
    val elapsedTime = calculateElapsedTime(startTime)
    log.debug(
      "BSP method '$methodName' call took ${elapsedTime}ms. " +
        "Result: ${if (error == null) "SUCCESS" else "FAILURE"}",
    )

    return when (error) {
      null -> value
      else -> handleError(error, methodName, elapsedTime)
    }
  }

  private fun calculateElapsedTime(startTime: Long): Long = currentTimeMillis() - startTime

  private fun handleError(
    error: Throwable,
    methodName: String,
    elapsedTime: Long,
  ): Nothing {
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
private const val CONNECT_SUBTASK_ID = "bsp-file-connection-connect"

internal class DefaultBspConnection(
  private val project: Project,
  private val connectionDetailsProviderExtension: ConnectionDetailsProviderExtension,
) : BspConnection {
  private var connectionDetails: BspConnectionDetails? = null

  private var server: JoinedBuildServer? = null
  private var capabilities: BazelBuildServerCapabilities? = null

  private var bspProcess: Process? = null
  private var disconnectActions: MutableList<() -> Unit> = mutableListOf()
  private val timeoutHandler = TimeoutHandler { Registry.intValue("bsp.request.timeout.seconds").seconds }

  override suspend fun connect(taskId: Any) {
    try {
      if (!isConnected()) {
        doConnect(taskId)
      }
    } catch (e: Exception) {
      bspProcess?.also {
        if (it.isAlive) it.destroyForcibly()
      }
      throw e
    }
  }

  private suspend fun doConnect(taskId: Any) {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole

    bspSyncConsole.startSubtask(
      parentTaskId = taskId,
      subtaskId = CONNECT_SUBTASK_ID,
      message = BspPluginBundle.message("console.subtask.connect.in.progress"),
    )

    try {
      connectOrThrowIfFailed(bspSyncConsole, taskId)
    } catch (e: Exception) {
      if (e is CancellationException) {
        bspSyncConsole.finishSubtask(
          subtaskId = CONNECT_SUBTASK_ID,
          message = BspPluginBundle.message("console.subtask.connect.cancelled"),
          result = FailureResultImpl(),
        )
      } else {
        bspSyncConsole.finishSubtask(
          subtaskId = CONNECT_SUBTASK_ID,
          message = BspPluginBundle.message("console.subtask.connect.failed"),
          result = FailureResultImpl(e),
        )
        throw e
      }
    }
  }

  private suspend fun connectOrThrowIfFailed(bspSyncConsole: TaskConsole, taskId: Any) {
    val bspClient = createBspClient()
    bspSyncConsole.addMessage(
      taskId = CONNECT_SUBTASK_ID,
      message = BspPluginBundle.message("console.task.connect.message.in.progress"),
    )
    val inMemoryConnection =
      BspServerProvider.getBspServer()?.getConnection(
        project,
        null,
        bspClient,
      )
    if (inMemoryConnection != null) {
      val console = BspConsoleService.getInstance(project).bspSyncConsole
      connectBuiltIn(console, inMemoryConnection)
    } else {
      val newConnectionDetails = connectionDetailsProviderExtension.provideNewConnectionDetails(project, null)

      if (newConnectionDetails != null) {
        this.connectionDetails = newConnectionDetails
        newConnectionDetails.connect(bspSyncConsole, taskId, bspClient)
      } else {
        error("Cannot connect. Please check your connection file.")
      }
    }
  }

  private suspend fun BspConnectionDetails.connect(
    bspSyncConsole: TaskConsole,
    taskId: Any,
    bspClient: BspClient,
  ) {
    log.info("Connecting to server with connection details: $this")
    val process = createAndStartProcessAndAddDisconnectActions(this)

    process.handleErrorOnExit(bspSyncConsole, taskId)

    bspProcess = process
    bspSyncConsole.addMessage(CONNECT_SUBTASK_ID, BspPluginBundle.message("console.task.connect.message.success"))

    initializeServer(process, bspClient, bspSyncConsole)
  }

  private fun connectBuiltIn(bspSyncConsole: TaskConsole, connection: GenericConnection) {
    disconnectActions.add { connection.shutdown() }
    bspSyncConsole.addMessage(CONNECT_SUBTASK_ID, BspPluginBundle.message("console.task.connect.message.success"))
    bspSyncConsole.addMessage(
      CONNECT_SUBTASK_ID,
      BspPluginBundle.message("console.message.initialize.server.in.progress"),
    )
    server = connection.server.wrapInChunkingServerIfRequired()
    capabilities = server?.initializeAndObtainCapabilities()
    bspSyncConsole.addMessage(
      CONNECT_SUBTASK_ID,
      BspPluginBundle.message("console.message.initialize.server.success"),
    )
    bspSyncConsole.finishSubtask(CONNECT_SUBTASK_ID, BspPluginBundle.message("console.subtask.connect.success"))
    bspSyncConsole.finishTask(
      taskId = "bsp-autoconnect",
      message = BspPluginBundle.message("console.task.auto.connect.success"),
    )
  }

  private fun createBspClient(): BspClient {
    val bspConsoleService = BspConsoleService.getInstance(project)

    return BspClient(
      bspConsoleService.bspSyncConsole,
      bspConsoleService.bspBuildConsole,
      timeoutHandler,
      project,
    )
  }

  private suspend fun createAndStartProcessAndAddDisconnectActions(bspConnectionDetails: BspConnectionDetails): Process {
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

  private suspend fun BspConnectionDetails.createAndStartProcess(): Process =
    withContext(Dispatchers.IO) {
      ProcessBuilder(argv)
        .directory(project.stateStore.projectBasePath.toFile())
        .withRealEnvs()
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    }

  private fun ProcessHandle.terminateGracefully() {
    try {
      OSProcessUtil.terminateProcessGracefully(this.pid().toInt())
    } catch (e: Exception) {
      log.debug("OSProcessUtil.terminateProcessGracefully not supported! Fallback to '.destroy()'", e)
      this.destroy()
    }
    this.onExit().get(TERMINATION_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS)
  }

  private fun Process.terminateDescendants() = this.descendants().forEach { it.terminateGracefully() }

  private fun Process.checkExitValueAndThrowIfError() {
    val exitValue = this.exitValue()
    if (exitValue != OK_EXIT_CODE && exitValue != TERMINATED_EXIT_CODE) {
      error("Server exited with exit value: $exitValue")
    }
  }

  private fun Process.handleErrorOnExit(bspSyncConsole: TaskConsole, taskId: Any) =
    this.onExit().whenComplete { completedProcess, _ ->
      val exitValue = completedProcess.exitValue()
      if (exitValue != OK_EXIT_CODE && exitValue != TERMINATED_EXIT_CODE) {
        val errorMessage = BspPluginBundle.message("console.server.exited", exitValue)
        bspSyncConsole.finishTask(taskId, errorMessage, FailureResultImpl(errorMessage))
      }
    }

  private fun initializeServer(
    process: Process,
    client: BspClient,
    bspSyncConsole: TaskConsole,
  ) {
    bspSyncConsole.addMessage(
      CONNECT_SUBTASK_ID,
      BspPluginBundle.message("console.message.initialize.server.in.progress"),
    )

    val newServer = startServerAndAddDisconnectActions(process, client)
    server = newServer.wrapInChunkingServerIfRequired()
    capabilities = server?.initializeAndObtainCapabilities()

    bspSyncConsole.addMessage(CONNECT_SUBTASK_ID, BspPluginBundle.message("console.message.initialize.server.success"))
    bspSyncConsole.finishSubtask(CONNECT_SUBTASK_ID, BspPluginBundle.message("console.subtask.connect.success"))
  }

  private fun startServerAndAddDisconnectActions(process: Process, client: BuildClient): JoinedBuildServer {
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
      arrayOf(JoinedBuildServer::class.java),
      CancelableInvocationHandlerWithTimeout(remoteProxy, cancelOnFuture, timeoutHandler),
    ) as JoinedBuildServer
  }

  private fun createLauncher(
    bspIn: InputStream,
    bspOut: OutputStream,
    client: BuildClient,
  ): Launcher<JoinedBuildServer> =
    TelemetryContextPropagatingLauncherBuilder<JoinedBuildServer>()
      .setRemoteInterface(JoinedBuildServer::class.java)
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
        builder.registerTypeAdapter(
          SourceItem::class.java,
          EnhancedSourceItemTypeAdapter(),
        )
      }.create()

  private fun JoinedBuildServer.wrapInChunkingServerIfRequired(): JoinedBuildServer =
    if (Registry.`is`("bsp.request.chunking.enable")) {
      val minChunkSize = Registry.intValue("bsp.request.chunking.size.min")
      ChunkingBuildServer(this, minChunkSize)
    } else {
      this
    }

  private fun JoinedBuildServer.initializeAndObtainCapabilities(): BazelBuildServerCapabilities {
    val buildInitializeResults = buildInitialize(createInitializeBuildParams()).get()
    onBuildInitialized()
    // cast is safe because we registered a custom type adapter
    return buildInitializeResults.capabilities as BazelBuildServerCapabilities
  }

  private fun createInitializeBuildParams(): InitializeBuildParams {
    val projectBaseDir = project.rootDir
    val params =
      InitializeBuildParams(
        BSP_CLIENT_NAME,
        BSP_CLIENT_VERSION,
        BSP_VERSION,
        projectBaseDir.toString(),
        CLIENT_CAPABILITIES,
      )
    val dataJson = JsonObject()
    dataJson.addProperty("clientClassesRootDir", "$projectBaseDir/out")
    getOtlpEndPoint()?.let {
      dataJson.addProperty("openTelemetryEndpoint", it)
    }
    params.data = dataJson

    return params
  }

  override suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities) -> T): T {
    val bspClient = createBspClient()
    val inMemoryConnection =
      BspServerProvider.getBspServer()?.getConnection(
        project,
        null,
        bspClient,
      )

    if (inMemoryConnection != null) {
      val console = BspConsoleService.getInstance(project).bspSyncConsole
      connectBuiltIn(console, inMemoryConnection)
    } else {
      val currentConnectionDetails =
        connectionDetailsProviderExtension.provideNewConnectionDetails(project, connectionDetails)
      if (currentConnectionDetails != null) {
        currentConnectionDetails.autoConnect(bspClient)
      } else if (!isConnected()) {
        connectionDetails?.autoConnect(bspClient)
      }
    }

    val server = server ?: error("Cannot execute the task. Server not available.")
    val capabilities = capabilities ?: error("Cannot execute the task. Capabilities not available.")

    return task(server, capabilities)
  }

  private suspend fun BspConnectionDetails.autoConnect(bspClient: BspClient) {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    bspSyncConsole.startTask(
      taskId = "bsp-autoconnect",
      title = BspPluginBundle.message("console.task.auto.connect.title"),
      message = BspPluginBundle.message("console.task.auto.connect.in.progress"),
    )
    try {
      disconnect()
      this.connect(bspSyncConsole, "bsp-autoconnect", bspClient)
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

  override fun isConnected(): Boolean = bspProcess?.isAlive == true
}

private fun Process.logErrorOutputs(project: Project) {
  if (!Registry.`is`("bsp.log.error.outputs")) return
  val bspConsoleService = BspConsoleService.getInstance(project)
  BspCoroutineService.getInstance(project).start {
    val bufferedReader = this.errorReader()
    bufferedReader.forEachLine { doLogErrorOutputLine(it, bspConsoleService) }
  }
}

private fun doLogErrorOutputLine(line: String, bspConsoleService: BspConsoleService) {
  val taskConsole = bspConsoleService.getActiveConsole()
  taskConsole?.addMessage(line)
}

private fun BspConsoleService.getActiveConsole(): TaskConsole? =
  if (this.bspBuildConsole.hasTasksInProgress()) {
    this.bspBuildConsole
  } else if (this.bspSyncConsole.hasTasksInProgress()) {
    this.bspSyncConsole
  } else {
    null
  }
