package org.jetbrains.bazel.golang.debug

import com.goide.dlv.location.DlvPositionConverter
import com.goide.dlv.location.DlvPositionConverterFactory
import com.goide.sdk.GoSdkService.getInstance
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.golang.sync.GoLanguagePlugin
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import java.nio.file.Path

private val logger: Logger = logger<BspDlvPositionConverter>()

internal class BazelDlvPosConverterFactory : DlvPositionConverterFactory {
  override fun createPositionConverter(
    project: Project,
    module: Module?,
    remotePaths: Set<String>,
  ): DlvPositionConverter? {
    if (!project.isBazelProject)
      return null
    return BspDlvPositionConverter(project, remotePaths, getInstance(project).getSdk(module).homePath)
  }
}

private class BspDlvPositionConverter(
  private val project: Project,
  private val remotePaths: Set<String>,
  private val goRoot: String,
) : DlvPositionConverter {

  private val localToRemoteCache = mutableMapOf<String, String>()
  private val remoteToLocalCache = mutableMapOf<String, VirtualFile>()

  init {
    if (remotePaths.isNotEmpty()) {
      val resolvedMap = resolveRemoteToLocalOnServer(remotePaths.toList())
      resolvedMap.forEach { (remote, localPath) ->
        val vf = VirtualFileManager.getInstance().findFileByNioPath(localPath)
        if (vf != null && vf.isValid) {
          remoteToLocalCache[remote] = vf
        }
      }
    }
  }

  /**
   * Converts a local file path (IDE) to a remote debugger path.
   * Checks cache first or makes a server call if not found.
   * Saves the result in the cache.
   */
  override fun toRemotePath(localFile: VirtualFile): String {
    val localPath = localFile.path

    localToRemoteCache[localPath]?.let { cached ->
      return cached
    }

    val resolvedMap = resolveLocalToRemoteOnServer(listOf(localPath))

    val remotePath =
      resolvedMap[localPath]
      ?: run {
        logger.warn("Server could not resolve local path '$localPath' to remote path. Using as-is.")
        localPath
      }

    // Cache the result
    localToRemoteCache[localPath] = remotePath
    return remotePath
  }

  /**
   * Converts a remote debugger path to a local VirtualFile.
   * Checks cache first or makes a server call if not found.
   * Saves the result in the cache.
   */
  override fun toLocalFile(remotePath: String): VirtualFile? {
    remoteToLocalCache[remotePath]?.let { cachedVf ->
      if (cachedVf.isValid) return cachedVf
    }

    val resolvedMap = resolveRemoteToLocalOnServer(listOf(remotePath))

    val localAbsolute = resolvedMap[remotePath]
    if (localAbsolute == null) {
      logger.warn("Server could not resolve remote path '$remotePath' to local path.")
      return null
    }

    val vf = VirtualFileManager.getInstance().findFileByNioPath(localAbsolute)
    if (vf != null && vf.isValid) {
      remoteToLocalCache[remotePath] = vf
      return vf
    }
    return null
  }

  private fun resolveLocalToRemoteOnServer(localPaths: List<String>): Map<String, String> {
    val params =
      BazelResolveLocalToRemoteParams(
        localPaths = localPaths,
      )

    val bazelInfo = project.service<WorkspaceSnapshotService>().snapshot.value.bazelInfo
    val bazelPathsResolver = BazelPathsResolver(bazelInfo)
    return GoLanguagePlugin
      .resolveLocalToRemote(bazelPathsResolver, params)
      .resolvedPaths
  }

  private fun resolveRemoteToLocalOnServer(remotePaths: List<String>): Map<String, Path> {
    val params =
      BazelResolveRemoteToLocalParams(
        remotePaths = remotePaths,
        goRoot = goRoot,
      )

    val bazelInfo = project.service<WorkspaceSnapshotService>().snapshot.value.bazelInfo
    val bazelPathsResolver = BazelPathsResolver(bazelInfo)
    return GoLanguagePlugin
      .resolveRemoteToLocal(bazelPathsResolver, params)
      .resolvedPaths
  }
}

