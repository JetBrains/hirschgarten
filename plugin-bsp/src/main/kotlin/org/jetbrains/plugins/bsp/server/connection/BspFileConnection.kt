package org.jetbrains.plugins.bsp.server.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildClientCapabilities
import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.InitializeBuildParams
import com.google.gson.JsonObject
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.execution.process.OSProcessUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.project.stateStore
import com.intellij.util.concurrency.AppExecutorUtil
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.magicmetamodel.impl.ConvertableToState
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetailsParser
import org.jetbrains.plugins.bsp.protocol.connection.logErrorOutputs
import org.jetbrains.plugins.bsp.server.client.BspClient
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
import kotlin.io.path.Path
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
    log.debug("BSP method '$methodName' call took ${elapsedTime}ms. " +
      "Result: ${if (error == null) "SUCCESS" else "FAILURE"}")

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

public data class BspFileConnectionState(
  public var connectionFilePath: String? = null,
)

public class BspFileConnection(
  private val project: Project,
  locatedConnectionFile: LocatedBspConnectionDetails,
) : BspConnection, ConvertableToState<BspFileConnectionState> {
  public override val buildToolId: String? = locatedConnectionFile.bspConnectionDetails?.name

  public override var server: BspServer? = null
    private set

  public var locatedConnectionFile: LocatedBspConnectionDetails = locatedConnectionFile
    private set

  public override var capabilities: BuildServerCapabilities? = null
    private set

  private var bspProcess: Process? = null
  private var disconnectActions: MutableList<() -> Unit> = mutableListOf()
  private val timeoutHandler = TimeoutHandler { Registry.intValue("bsp.request.timeout.seconds").seconds }
  private val log = logger<BspFileConnection>()

  public override fun connect(taskId: Any, errorCallback: () -> Unit) {
    if (!isConnected()) {
      doConnect(taskId, errorCallback)
    }
  }

  private fun doConnect(taskId: Any, errorCallback: () -> Unit = {}) {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole

    bspSyncConsole.startSubtask(taskId, connectSubtaskId, "Connecting to the server...")
    bspSyncConsole.addMessage(connectSubtaskId, "Establishing connection...")

    try {
      doConnectOrThrowIfFailed(bspSyncConsole, taskId, errorCallback)
    } catch (e: Exception) {
      bspSyncConsole.finishTask(taskId, "Establishing connection has failed!", FailureResultImpl(e))
    }
  }

  private fun doConnectOrThrowIfFailed(bspSyncConsole: TaskConsole, taskId: Any, errorCallback: () -> Unit) {
    val connectionDetails = locatedConnectionFile.bspConnectionDetails

    if (connectionDetails != null) {
      val client = createBspClient()
      val process = createAndStartProcessAndAddDisconnectActions(connectionDetails)

      process.handleErrorOnExit(bspSyncConsole, taskId, errorCallback)

      bspProcess = process
      bspSyncConsole.addMessage(connectSubtaskId, "Establishing connection done!")

      initializeServer(process, client, bspSyncConsole)
    } else {
      error("Parsing connection file '${locatedConnectionFile.connectionFileLocation}' failed!")
    }
  }

  private fun createAndStartProcessAndAddDisconnectActions(bspConnectionDetails: BspConnectionDetails): Process {
    val process = createAndStartProcess(bspConnectionDetails)
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

  private fun Process.terminateDescendants() =
    this.descendants().forEach { it.terminateGracefully() }

  private fun ProcessHandle.terminateGracefully() {
    try {
      OSProcessUtil.terminateProcessGracefully(this.pid().toInt())
    } catch (e: Exception) {
      log.debug("OSProcessUtil.terminateProcessGracefully not supported! Fallback to '.destroy()'", e)
      this.destroy()
    }
    this.onExit().get(TERMINATION_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS)
  }

  private fun Process.checkExitValueAndThrowIfError() {
    val exitValue = this.exitValue()
    if (exitValue != OK_EXIT_CODE && exitValue != TERMINATED_EXIT_CODE) {
      error("Server exited with exit value: $exitValue")
    }
  }

  private fun createAndStartProcess(bspConnectionDetails: BspConnectionDetails): Process =
    ProcessBuilder(bspConnectionDetails.argv)
      .directory(project.stateStore.projectBasePath.toFile())
      .withRealEnvs()
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .start()

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

  private fun Process.handleErrorOnExit(bspSyncConsole: TaskConsole, taskId: Any, errorCallback: () -> Unit) =
    this.onExit().whenComplete { completedProcess, _ ->
      val exitValue = completedProcess.exitValue()
      if (exitValue != OK_EXIT_CODE && exitValue != TERMINATED_EXIT_CODE) {
        val errorMessage = "Server exited with exit value $exitValue"
        bspSyncConsole.finishTask(taskId, errorMessage, FailureResultImpl(errorMessage))
        errorCallback()
      }
    }

  private fun initializeServer(
    process: Process,
    client: BspClient,
    bspSyncConsole: TaskConsole,
  ) {
    bspSyncConsole.addMessage(connectSubtaskId, "Initializing server...")

    server = startServerAndAddDisconnectActions(process, client)
    capabilities = server?.initializeAndObtainCapabilities()

    bspSyncConsole.addMessage(connectSubtaskId, "Server initialized! Server is ready to use.")
    bspSyncConsole.finishSubtask(connectSubtaskId, "Connecting to the server done!")
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
      .create()

  private fun BspServer.initializeAndObtainCapabilities(): BuildServerCapabilities {
    val buildInitializeResults = buildInitialize(createInitializeBuildParams()).get()
    onBuildInitialized()
    return buildInitializeResults.capabilities
  }

  private fun createInitializeBuildParams(): InitializeBuildParams {
    val projectBaseDir = project.rootDir
    val params = InitializeBuildParams(
      "IntelliJ-BSP",
      "0.0.1",
      "2.1.0-M5",
      projectBaseDir.toString(),
      BuildClientCapabilities(listOf("java")),
    )
    val dataJson = JsonObject()
    dataJson.addProperty("clientClassesRootDir", "$projectBaseDir/out")
    params.data = dataJson

    return params
  }

  public override fun disconnect() {
    val exceptions = executeDisconnectActionsAndCollectExceptions(disconnectActions)
    disconnectActions.clear()
    throwExceptionWithSuppressedIfOccurred(exceptions)

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

  override fun reload() {
    locatedConnectionFile =
      LocatedBspConnectionDetailsParser.parseFromFile(locatedConnectionFile.connectionFileLocation)
  }

  override fun isConnected(): Boolean =
    bspProcess?.isAlive == true

  // TODO test
  override fun toState(): BspFileConnectionState =
    BspFileConnectionState(locatedConnectionFile.connectionFileLocation.canonicalPath)

  public companion object {
    private const val connectSubtaskId = "bsp-file-connection-connect"

    public fun fromState(project: Project, state: BspFileConnectionState): BspFileConnection? =
      state.connectionFilePath
        ?.let { toVirtualFile(it) }
        ?.let { LocatedBspConnectionDetailsParser.parseFromFile(it) }
        ?.let { BspFileConnection(project, it) }

    private fun toVirtualFile(path: String): VirtualFile? =
      VirtualFileManager.getInstance().findFileByNioPath(Path(path))
  }
}
