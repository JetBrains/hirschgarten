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
package org.jetbrains.plugins.bsp.golang.debug

import com.goide.dlv.location.DlvPositionConverter
import com.google.common.collect.Maps
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class BspDlvPositionConverter(
    private val root: WorkspaceRoot,
    private val goRoot: String,
    private val resolver: ExecutionRootPathResolver,
    remotePaths: Set<String>,
    private val cgoTrimmedPathsHandler: CgoTrimmedPathsHandler?
) : DlvPositionConverter {
    private val localToRemote: MutableMap<VirtualFile, String>
    private val normalizedToLocal: MutableMap<String, VirtualFile>

    init {
        this.localToRemote = Maps.newHashMapWithExpectedSize<VirtualFile, String>(remotePaths.size)
        this.normalizedToLocal = Maps.newHashMapWithExpectedSize<String, VirtualFile>(remotePaths.size)

        for (path in remotePaths) {
            val normalized = normalizePath(path)
            if (normalizedToLocal.containsKey(normalized)) {
                continue
            }
            val localFile = resolve(normalized)
            if (localFile != null) {
                if (remotePaths.contains(normalized)) {
                    localToRemote.put(localFile, normalized)
                } else {
                    localToRemote.put(localFile, path)
                }
                normalizedToLocal.put(normalized, localFile)
            } else {
                logger.warn("Unable to find local file for debug path: " + path)
            }
        }
    }

    override fun toRemotePath(localFile: VirtualFile): String {
        var remotePath = localToRemote.get(localFile)
        if (remotePath != null) {
            return remotePath
        }
        remotePath =
            if (root.isInWorkspace(localFile))
                root.workspacePathFor(localFile).relativePath()
            else
                localFile.getPath()
        localToRemote.put(localFile, remotePath)
        return remotePath
    }

    override fun toLocalFile(remotePath: String): VirtualFile? {
        val normalized = normalizePath(remotePath)
        var localFile = normalizedToLocal.get(normalized)
        if (localFile == null || !localFile.isValid()) {
            localFile = resolve(normalized)
            if (localFile != null) {
                normalizedToLocal.put(normalized, localFile)
            }
        }
        return localFile
    }

    private fun resolve(normalizedPath: String): VirtualFile? {
        return resolveVirtualFile(
            resolver.resolveExecutionRootPath(ExecutionRootPath(normalizedPath)),  /* refreshIfNeeded= */
            false
        )
    }

    private fun normalizePath(path: String): String {
        if (path.startsWith("/build/work/")) {
            // /build/work/<hash>/<project>/actual/path
            return afterNthSlash(path, 5)
        } else if (path.startsWith("/tmp/go-build-release/buildroot/")) {
            return afterNthSlash(path, 4)
        } else if (path.startsWith("GOROOT/")) {
            return goRoot + '/' + afterNthSlash(path, 1)
        } else if (cgoTrimmedPathsHandler != null && cgoTrimmedPathsHandler.matchesCgoTrimmedPath(path)) {
            return cgoTrimmedPathsHandler.normalizeCgoTrimmedPath(path)
        }
        return path
    }

    companion object {
        private val logger = Logger.getInstance(BspDlvPositionConverter::class.java)

        /**
         * @return path substring after nth slash, if path contains at least n slashes, return path
         * unchanged otherwise.
         */
        fun afterNthSlash(path: String, n: Int): String {
            var index = 0
            for (i in 0..<n) {
                index = path.indexOf('/', index) + 1
                if (index == 0) { // -1 + 1
                    return path
                }
            }
            return path.substring(index)
        }


        /**
         * Attempts to resolve the given file path to a [VirtualFile].
         *
         *
         * WARNING: Refreshing files on the EDT may freeze the IDE.
         *
         * @param refreshIfNeeded whether to refresh the file in the VFS, if it is not already cached.
         * Will only refresh if called on the EDT.
         */
        fun resolveVirtualFile(file: File, refreshIfNeeded: Boolean): VirtualFile? {
            val fileSystem = LocalFileSystem.getInstance()
            var vf = fileSystem.findFileByPathIfCached(file.getPath())
            if (vf != null) {
                return vf
            }
            vf = fileSystem.findFileByIoFile(file)
            if (vf != null && vf.isValid()) {
                return vf
            }
            val shouldRefresh = refreshIfNeeded && ApplicationManager.getApplication().isDispatchThread()
            return if (shouldRefresh) fileSystem.refreshAndFindFileByIoFile(file) else null
        }
    }
}
