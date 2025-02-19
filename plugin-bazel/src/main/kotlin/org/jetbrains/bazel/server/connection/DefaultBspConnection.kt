package org.jetbrains.bazel.server.connection

import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.SourceItem
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.diagnostic.telemetry.OtlpConfiguration
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.config.FeatureFlagsProvider
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.server.chunking.ChunkingBuildServer
import org.jetbrains.bazel.server.client.BspClient
import org.jetbrains.bazel.server.client.GenericConnection
import org.jetbrains.bazel.server.utils.CancellableFuture
import org.jetbrains.bazel.server.utils.TimeoutHandler
import org.jetbrains.bazel.server.utils.reactToExceptionIn
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.ui.console.BspConsoleService
import org.jetbrains.bazel.ui.console.ids.CONNECT_TASK_ID
import org.jetbrains.bsp.bazel.install.EnvironmentCreator
import org.jetbrains.bsp.protocol.BSP_CLIENT_NAME
import org.jetbrains.bsp.protocol.BSP_CLIENT_VERSION
import org.jetbrains.bsp.protocol.BSP_VERSION
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.CLIENT_CAPABILITIES
import org.jetbrains.bsp.protocol.InitializeBuildData
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.utils.BazelBuildServerCapabilitiesTypeAdapter
import org.jetbrains.bsp.protocol.utils.EnhancedSourceItemTypeAdapter
import java.io.InputStream
import java.io.OutputStream
import java.lang.System.currentTimeMillis
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.seconds

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

class DotBazelBspCreator(projectPath: VirtualFile) : EnvironmentCreator(projectPath.toNioPath()) {
  override fun create() {
    createDotBazelBsp()
  }
}

class DefaultBspConnection(private val project: Project) : BspConnection {
  @Volatile
  private var server: JoinedBuildServer? = null
  private var capabilities: BazelBuildServerCapabilities? = null

  @Volatile
  private var bspProcess: Process? = null
  private val disconnectActions: MutableList<() -> Unit> = mutableListOf()
  private val timeoutHandler = TimeoutHandler { Registry.intValue("bsp.request.timeout.seconds").seconds }
  private val mutex = Mutex()

  override suspend fun connect() {
    mutex.withLock {
      try {
        ensureConnected()
      } catch (connectException: Exception) {
        try {
          disconnectWithAcquiredLock()
        } catch (disconnectException: Exception) {
          // connectException is probably more informative to the user, throw it instead of disconnectException
          connectException.addSuppressed(disconnectException)
        }
        throw connectException
      }
    }
  }

  private suspend fun ensureConnected() {
    log.debug("ensuring the connection is established")
    val bspClient = createBspClient()
    val inMemoryConnection =
      object : GenericConnection {
        val installationDirectory = project.rootDir.toNioPath()
        val conn =
          Connection(
            installationDirectory,
            null,
            project.bazelProjectSettings.projectViewPath?.toAbsolutePath(),
            installationDirectory,
            bspClient,
            propagateTelemetryContext = true,
          )
        val projectPath = VfsUtil.findFile(installationDirectory, true) ?: error("Project doesn't exist")

        init {
          DotBazelBspCreator(projectPath).create()
        }

        override val server: JoinedBuildServer
          get() = conn.clientLauncher.remoteProxy

        override fun shutdown() {
          conn.stop()
        }
      }
    if (!isConnected()) {
      connectBuiltIn(inMemoryConnection)
    }
  }

  private suspend fun connectBuiltIn(connection: GenericConnection) {
    coroutineScope {
      val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
      bspSyncConsole.startTask(
        taskId = CONNECT_TASK_ID,
        title = BspPluginBundle.message("console.task.auto.connect.title"),
        message = BspPluginBundle.message("console.task.auto.connect.in.progress"),
        cancelAction = { coroutineContext.cancel() },
      )
      bspSyncConsole.addMessage(taskId = CONNECT_TASK_ID, message = BspPluginBundle.message("console.task.connect.message.in.progress"))
      disconnectActions.add { connection.shutdown() }
      bspSyncConsole.addMessage(CONNECT_TASK_ID, BspPluginBundle.message("console.task.connect.message.success"))
      bspSyncConsole.addMessage(
        CONNECT_TASK_ID,
        BspPluginBundle.message("console.message.initialize.server.in.progress"),
      )
      server = connection.server.wrapInChunkingServerIfRequired()
      capabilities = server?.initializeAndObtainCapabilities()
      bspSyncConsole.addMessage(
        CONNECT_TASK_ID,
        BspPluginBundle.message("console.message.initialize.server.success"),
      )
      bspSyncConsole.finishTask(CONNECT_TASK_ID, BspPluginBundle.message("console.task.auto.connect.success"))
    }
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

  private suspend fun JoinedBuildServer.initializeAndObtainCapabilities(): BazelBuildServerCapabilities {
    val buildInitializeResults = buildInitialize(createInitializeBuildParams()).asDeferred().await()
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
    val initializeBuildData =
      InitializeBuildData(
        clientClassesRootDir = "$projectBaseDir/out",
        openTelemetryEndpoint = getOpenTelemetryEndPoint(),
        featureFlags = FeatureFlagsProvider.accumulateFeatureFlags(),
      )
    params.data = initializeBuildData

    return params
  }

  private fun getOpenTelemetryEndPoint(): String? =
    try {
      OtlpConfiguration.getTraceEndpoint()
    } catch (_: NoSuchMethodError) {
      null
    }

  override suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities) -> T): T {
    connect()
    val server = server ?: error("Cannot execute the task. Server not available.")
    val capabilities = capabilities ?: error("Cannot execute the task. Capabilities not available.")

    return task(server, capabilities)
  }

  override suspend fun disconnect() {
    if (!isConnected()) return // Fast path to avoid waiting for mutex
    mutex.withLock {
      disconnectWithAcquiredLock()
    }
  }

  private fun disconnectWithAcquiredLock() {
    if (!isConnected()) return
    val exceptions = executeDisconnectActionsAndCollectExceptions(disconnectActions)
    bspProcess?.destroy().also { bspProcess = null }
    disconnectActions.clear()
    server = null
    capabilities = null
    throwExceptionWithSuppressedIfOccurred(exceptions)
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

  override fun isConnected(): Boolean {
    bspProcess?.let { bspProcess ->
      return bspProcess.isAlive
    }
    return server != null
  }
}
