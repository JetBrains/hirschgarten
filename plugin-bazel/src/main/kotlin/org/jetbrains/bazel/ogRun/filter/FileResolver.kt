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
package org.jetbrains.bazel.ogRun.filter

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.ogRun.other.VirtualFileSystemProvider
import java.io.File

/** Parses file strings in blaze/bazel output.  */
interface FileResolver {
  fun resolve(project: Project, fileString: String): File?

  companion object {
    /**
     * Iterates through all available [FileResolver]s, returning the first successful result.
     */
    @JvmStatic
    fun resolveToVirtualFile(project: Project, fileString: String): VirtualFile? =
      EP_NAME.extensions
        .asSequence()
        .mapNotNull { it.resolve(project, fileString) }
        .mapNotNull { file ->
          VirtualFileSystemProvider.instance.system.findFileByPath(file.path)
        }
        .firstOrNull()

    /**
     * Iterates through all available [FileResolver]s, returning the first successful result.
     */
    fun resolveToFile(project: Project, fileString: String): File? {
      return EP_NAME.extensions
        .asSequence()
        .mapNotNull { it.resolve(project, fileString) }
        .firstOrNull()
    }

    val EP_NAME: ExtensionPointName<FileResolver> =
      ExtensionPointName.create("com.google.idea.blaze.FileStringParser")
  }
}
