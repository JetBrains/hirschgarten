package org.jetbrains.plugins.bsp.server.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import ch.epfl.scala.bsp4j.BuildClient
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.project.stateStore
import com.intellij.util.concurrency.AppExecutorUtil
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.magicmetamodel.impl.ConvertableToState
import org.jetbrains.plugins.bsp.server.client.BspClient
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetailsParser
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

public data class BspFileConnectionState(
  public var connectionFilePath: String? = null
)

public class BspFileConnection(
  private val project: Project,
  private val locatedConnectionFile: LocatedBspConnectionDetails
) : BspConnection, ConvertableToState<BspFileConnectionState> {

  public override var server: BspServer? = null
    private set

  private var bspProcess: Process? = null
  private var disconnectActions: MutableList<() -> Unit> = mutableListOf()

  public override fun connect(taskId: Any) {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole

    bspSyncConsole.startSubtask(taskId, connectSubtaskId, "Connecting to the server...")

    bspSyncConsole.addMessage(connectSubtaskId, "Establishing connection...")
    val client = createBspClient()
    val process = createAndStartProcessAndAddDisconnectActions(locatedConnectionFile.bspConnectionDetails)

    bspProcess = process
    server = startServerAndAddDisconnectActions(process, client)
    bspSyncConsole.addMessage(connectSubtaskId, "Establishing connection done!")

    bspSyncConsole.addMessage(connectSubtaskId, "Initializing server...")
    client.onConnectWithServer(server)
    bspSyncConsole.addMessage(connectSubtaskId, "Server initialized! Server is ready to use.")

    bspSyncConsole.finishSubtask(connectSubtaskId, "Connecting to the server done!")
  }

  private fun createAndStartProcessAndAddDisconnectActions(bspConnectionDetails: BspConnectionDetails): Process {
    val process = createAndStartProcess(bspConnectionDetails)

    disconnectActions.add { server?.buildShutdown() }
    disconnectActions.add { server?.onBuildExit() }

    disconnectActions.add { process.waitFor(3, TimeUnit.SECONDS) }
    disconnectActions.add { process.destroy() }

    return process
  }

  private fun createAndStartProcess(bspConnectionDetails: BspConnectionDetails): Process =
    ProcessBuilder(bspConnectionDetails.argv)
      .directory(project.stateStore.projectBasePath.toFile())
      .start()

  private fun createBspClient(): BspClient {
    val bspConsoleService = BspConsoleService.getInstance(project)

    return BspClient(
      bspConsoleService.bspSyncConsole,
      bspConsoleService.bspBuildConsole,
      bspConsoleService.bspRunConsole,
      bspConsoleService.bspTestConsole,
    )
  }

  private fun startServerAndAddDisconnectActions(process: Process, client: BuildClient): BspServer {
    val bspIn = process.inputStream
    disconnectActions.add { bspIn.close() }

    val bspOut = process.outputStream
    disconnectActions.add { bspOut.close() }

    val launcher = createLauncher(bspIn, bspOut, client)
    val listening = launcher.startListening()
    disconnectActions.add { listening.cancel(true) }

    return launcher.remoteProxy
  }

  private fun createLauncher(bspIn: InputStream, bspOut: OutputStream, client: BuildClient): Launcher<BspServer> =
    Launcher.Builder<BspServer>()
      .setRemoteInterface(BspServer::class.java)
      .setExecutorService(AppExecutorUtil.getAppExecutorService())
      .setInput(bspIn)
      .setOutput(bspOut)
      .setLocalService(client)
      .create()

  public override fun disconnect() {
    val exceptions = executeDisconnectActionsAndCollectExceptions(disconnectActions)
    disconnectActions.clear()
    throwExceptionWithSuppressedIfOccurred(exceptions)

    server = null
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
