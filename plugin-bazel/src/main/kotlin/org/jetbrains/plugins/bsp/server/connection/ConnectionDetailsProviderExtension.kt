package org.jetbrains.plugins.bsp.server.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.future.await
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.WithBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolIdOrDefault
import java.util.concurrent.CompletableFuture

/**
 * Extension that provides connection details for connecting to a BSP server.
 *
 * It should provide the latest available connection details so
 * the client is always connected to the latest server.
 * For example if the connection file has changed after initial import of the project,
 * this extension allows the client to disconnect from the old server and connection to the new one.
 *
 * Implementation should take care of its state (e.g. using a dedicated service with state).
 */
interface ConnectionDetailsProviderExtension : WithBuildToolId {
  /**
   * Method called only on the first opening of the project (so initial sync).
   * It should be used to run an initial configuration, e.g. show a wizard.
   *
   * Note: UI actions should be executed under: [com.intellij.openapi.application.writeAction]
   *
   * This method is preferred over [onFirstOpeningJavaShim] if kotlin is used.
   *
   * @return [true] if all the actions have succeeded and initial sync should continue;
   * [false] if something has failed and initial sync should be terminated.
   */
  suspend fun onFirstOpening(project: Project, projectPath: VirtualFile): Boolean

  /**
   * Provides the new connection details if are available.
   *
   * The method is called before each task executed on server.
   * Client will disconnect from the old server and connect to the new one if non-null value is returned.
   *
   * [currentConnectionDetails] set to [null] means that client is not aware of any connection details
   * (like during initial sync or after reopening the project)
   *
   * @return [null] if the newest available connection details are equal to [currentConnectionDetails];
   * otherwise new [BspConnectionDetails] if available
   */
  fun provideNewConnectionDetails(project: Project, currentConnectionDetails: BspConnectionDetails?): BspConnectionDetails?

  companion object {
    val ep: ExtensionPointName<ConnectionDetailsProviderExtension> =
      ExtensionPointName.create("org.jetbrains.bsp.connectionDetailsProviderExtension")
  }
}

/**
 * Java shim of [ConnectionDetailsProviderExtension], should be used only if coroutines are not available,
 * e.g. Java or Scala are used.
 */
interface ConnectionDetailsProviderExtensionJavaShim : WithBuildToolId {
  fun onFirstOpening(project: Project, projectPath: VirtualFile): CompletableFuture<Boolean>

  fun provideNewConnectionDetails(project: Project, currentConnectionDetails: BspConnectionDetails?): BspConnectionDetails?

  companion object {
    internal val ep: ExtensionPointName<ConnectionDetailsProviderExtensionJavaShim> =
      ExtensionPointName.create("org.jetbrains.bsp.connectionDetailsProviderExtensionJavaShim")
  }
}

internal class ConnectionDetailsProviderExtensionAdapter(private val extension: ConnectionDetailsProviderExtensionJavaShim) :
  ConnectionDetailsProviderExtension {
  override val buildToolId: BuildToolId
    get() = extension.buildToolId

  override suspend fun onFirstOpening(project: Project, projectPath: VirtualFile): Boolean =
    extension.onFirstOpening(project, projectPath).await()

  override fun provideNewConnectionDetails(project: Project, currentConnectionDetails: BspConnectionDetails?): BspConnectionDetails? =
    extension.provideNewConnectionDetails(project, currentConnectionDetails)
}

val Project.connectionDetailsProvider: ConnectionDetailsProviderExtension
  get() =
    calculateJavaShimProviderInAdapter(buildToolId)
      ?: ConnectionDetailsProviderExtension.ep.withBuildToolIdOrDefault(buildToolId)

private fun calculateJavaShimProviderInAdapter(buildToolId: BuildToolId): ConnectionDetailsProviderExtension? =
  ConnectionDetailsProviderExtensionJavaShim.ep
    .withBuildToolId(buildToolId)
    ?.let { ConnectionDetailsProviderExtensionAdapter(it) }
