package org.jetbrains.bsp.inmem

import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.SourceItem
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.jsonrpc.Launcher.Builder
import org.jetbrains.bsp.bazel.server.BazelBspServer
import org.jetbrains.bsp.bazel.server.benchmark.TelemetryConfig
import org.jetbrains.bsp.bazel.server.bsp.BspIntegrationData
import org.jetbrains.bsp.bazel.server.bsp.info.BspInfo
import org.jetbrains.bsp.bazel.workspacecontext.DefaultWorkspaceContextProvider
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.utils.BazelBuildServerCapabilitiesTypeAdapter
import org.jetbrains.bsp.protocol.utils.EnhancedSourceItemTypeAdapter
import org.jetbrains.plugins.bsp.impl.server.connection.TelemetryContextPropagatingLauncherBuilder
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class Connection(
  installationDirectory: Path,
  metricsFile: Path?,
  workspace: Path,
  client: BuildClient,
  propagateTelemetryContext: Boolean,
) {
  val serverOut = FixedThreadPipedOutputStream()
  val clientOut = FixedThreadPipedOutputStream()
  val serverExecutor = Executors.newFixedThreadPool(4, threadFactory("cli-server-pool-%d"))
  val telemetryConfig = TelemetryConfig(metricsFile = metricsFile)
  val serverLauncher =
    startServer(
      serverOut,
      clientOut.inputStream,
      serverExecutor,
      workspace,
      installationDirectory,
      telemetryConfig,
    )
  val serverAliveFuture = serverLauncher.startListening()

  val clientExecutor = Executors.newFixedThreadPool(4, threadFactory("cli-client-pool-%d"))
  val clientLauncher = startClient(serverOut.inputStream, clientOut, clientExecutor, client, propagateTelemetryContext)
  val clientAliveFuture = clientLauncher.startListening()

  fun stop() {
    clientExecutor.shutdown()
    serverExecutor.shutdown()

    clientOut.stop()
    serverOut.stop()

    clientAliveFuture.get()
    serverAliveFuture.get()
  }
}

/**
 * This class is required, because of limitations of java's PipedStreams.
 * Unfortunately, whenever PipedInputStream calls read ([code](https://github.com/openjdk/jdk/blob/e30e3564420c631f08ac3d613ab91c93227a00b3/src/java.base/share/classes/java/io/PipedInputStream.java#L314-L316)),
 * it checks whether the writing thread is alive. Unfortunately, in case of Bazel BSP server, there are a lot of writes
 * from different threads, that are often spawned only temporarily, from java's Executors.
 *
 * The idea how to solve it is to create a single thread, which lifetime is longer than both PipedOutputStream and
 * PipedInputStream, and it's the only thread that is allowed to write to PipedOutputStream
 */
class FixedThreadPipedOutputStream : OutputStream() {
  val inputStream = PipedInputStream()
  private val outputStream = PrintStream(PipedOutputStream(inputStream), true)
  private val queue = ArrayBlockingQueue<Int>(10000)
  private val stop = AtomicBoolean(false)
  private val thread =
    Thread {
      while (!stop.get()) {
        queue
          .poll(100, TimeUnit.MILLISECONDS)
          ?.let { outputStream.write(it) }
      }
    }.also { it.start() }

  fun stop() {
    outputStream.close()
    inputStream.close()
    stop.set(true)
    thread.join()
  }

  override fun write(b: Int) {
    queue.put(b)
  }
}

private fun threadFactory(nameFormat: String): ThreadFactory =
  ThreadFactoryBuilder()
    .setNameFormat(nameFormat)
    .setUncaughtExceptionHandler { _, e ->
      e.printStackTrace()
      exitProcess(1)
    }.build()

@Suppress("UNCHECKED_CAST")
private fun startClient(
  serverOut: PipedInputStream,
  clientIn: OutputStream,
  clientExecutor: ExecutorService?,
  buildClient: BuildClient,
  propagateTelemetryContext: Boolean,
): Launcher<JoinedBuildServer> {
  val builder: Builder<JoinedBuildClient> =
    if (propagateTelemetryContext) {
      TelemetryContextPropagatingLauncherBuilder<JoinedBuildServer>() as Builder<JoinedBuildClient>
    } else {
      Builder<JoinedBuildClient>()
    }
  return builder
    .setInput(serverOut)
    .setOutput(clientIn)
    .setRemoteInterface(JoinedBuildServer::class.java as Class<JoinedBuildClient>)
    .setExecutorService(clientExecutor)
    .setLocalService(buildClient)
    .configureGson { gsonBuilder ->
      gsonBuilder.registerTypeAdapter(
        BuildServerCapabilities::class.java,
        BazelBuildServerCapabilitiesTypeAdapter(),
      )
      gsonBuilder.registerTypeAdapter(
        SourceItem::class.java,
        EnhancedSourceItemTypeAdapter(),
      )
    }.create() as Launcher<JoinedBuildServer>
}

private fun startServer(
  serverIn: OutputStream,
  clientOut: PipedInputStream,
  serverExecutor: ExecutorService,
  workspace: Path,
  directory: Path,
  telemetryConfig: TelemetryConfig,
): Launcher<JoinedBuildClient> {
  val bspInfo = BspInfo(directory)
  val bspIntegrationData = BspIntegrationData(serverIn, clientOut, serverExecutor, null)
  val workspaceContextProvider =
    DefaultWorkspaceContextProvider(
      workspaceRoot = workspace,
      projectViewPath = directory.resolve("projectview.bazelproject"),
      dotBazelBspDirPath = bspInfo.bazelBspDir(),
    )
  val bspServer = BazelBspServer(bspInfo, workspaceContextProvider, workspace, telemetryConfig)
  return bspServer.buildServer(bspIntegrationData)
}
