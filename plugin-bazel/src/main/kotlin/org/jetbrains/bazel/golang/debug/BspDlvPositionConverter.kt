/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original Java code taken from https://github.com/bazelbuild, converted to Kotlin and modified.
 */
package org.jetbrains.bazel.golang.debug

import com.goide.dlv.location.DlvPositionConverter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import java.io.File

class BspDlvPositionConverter(
  private val project: Project,
  private val remotePaths: Set<String>,
  private val goRoot: String,
) : DlvPositionConverter {
  private val logger: Logger = logger<BspDlvPositionConverter>()

  private val localToRemoteCache = mutableMapOf<String, String>()
  private val remoteToLocalCache = mutableMapOf<String, VirtualFile>()

  init {
    if (remotePaths.isNotEmpty()) {
      runBlocking {
        val resolvedMap = resolveRemoteToLocalOnServer(remotePaths.toList())
        resolvedMap.forEach { (remote, localPath) ->
          if (localPath.isNotBlank()) {
            val vf = LocalFileSystem.getInstance().findFileByIoFile(File(localPath))
            if (vf != null && vf.isValid) {
              remoteToLocalCache[remote] = vf
            }
          }
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

    val resolvedMap =
      runBlocking {
        resolveLocalToRemoteOnServer(listOf(localPath))
      }

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

    val resolvedMap =
      runBlocking {
        resolveRemoteToLocalOnServer(listOf(remotePath))
      }

    val localAbsolute = resolvedMap[remotePath]
    if (localAbsolute.isNullOrEmpty()) {
      logger.warn("Server could not resolve remote path '$remotePath' to local path.")
      return null
    }

    val vf = LocalFileSystem.getInstance().findFileByIoFile(File(localAbsolute))
    if (vf != null && vf.isValid) {
      remoteToLocalCache[remotePath] = vf
      return vf
    }
    return null
  }

  private suspend fun resolveLocalToRemoteOnServer(localPaths: List<String>): Map<String, String> {
    val params =
      BazelResolveLocalToRemoteParams(
        localPaths = localPaths,
      )
    return BazelWorkspaceResolveService
      .getInstance(project)
      .withEndpointProxy { it.resolveLocalToRemote(params) }
      .resolvedPaths
  }

  private suspend fun resolveRemoteToLocalOnServer(remotePaths: List<String>): Map<String, String> {
    val params =
      BazelResolveRemoteToLocalParams(
        remotePaths = remotePaths,
        goRoot = goRoot,
      )
    return BazelWorkspaceResolveService
      .getInstance(project)
      .withEndpointProxy { it.resolveRemoteToLocal(params) }
      .resolvedPaths
  }
}
