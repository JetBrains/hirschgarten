/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
 */
package org.jetbrains.bazel.run2.filter

import com.google.idea.blaze.base.io.VirtualFileSystemProvider
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.Objects

/** Parses file strings in blaze/bazel output.  */
interface FileResolver {
  fun resolve(project: Project, fileString: String): File?

  companion object {
    /**
     * Iterates through all available [FileResolver]s, returning the first successful result.
     */
    @JvmStatic
    fun resolveToVirtualFile(project: Project, fileString: String): VirtualFile? =
      EP_NAME.extensionList
        .map { it.resolve(project, fileString) }
        .filterNotNull()
        .map { VirtualFileSystemProvider.instance.system.findFileByPath(it.path) }
        .filterNotNull()
        .firstOrNull()

    /**
     * Iterates through all available [FileResolver]s, returning the first successful result.
     */
    fun resolveToFile(project: Project, fileString: String): File? =
      EP_NAME.extensionList
        .map { it.resolve(project, fileString) }
        .firstOrNull { obj: File? -> Objects.nonNull(obj) }

    val EP_NAME: ExtensionPointName<FileResolver> =
      ExtensionPointName.create("com.google.idea.blaze.FileStringParser")
  }
}
