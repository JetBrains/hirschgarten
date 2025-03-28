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
package org.jetbrains.bazel.commons

import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute

/**
 * An absolute or relative path returned from Bazel. If it is a relative path, it is relative to the
 * execution root.
 */
data class ExecutionRootPath(val absoluteOrRelativePath: Path) {
  constructor(path: String) : this(Path(path))

  constructor(file: File) : this(file.toPath())

  val isAbsolute: Boolean
    get() = absoluteOrRelativePath.isAbsolute

  fun getPathRootedAt(absoluteRoot: Path): Path =
    if (absoluteOrRelativePath.isAbsolute) {
      absoluteOrRelativePath
    } else {
      absoluteRoot.resolve(absoluteOrRelativePath)
    }

  fun getPathRootedAt(absoluteRoot: File): Path = getPathRootedAt(absoluteRoot.toPath())

  fun getFileRootedAt(absoluteRoot: File?): File =
    if (absoluteOrRelativePath.isAbsolute) {
      absoluteOrRelativePath.toFile()
    } else {
      absoluteRoot?.let { File(it, absoluteOrRelativePath.toString()) }
        ?: absoluteOrRelativePath.toFile()
    }

  companion object {
    /**
     * Returns the relative [ExecutionRootPath] if `root` is an ancestor of `path`
     * otherwise returns null.
     */
    fun createAncestorRelativePath(root: Path, path: Path): ExecutionRootPath? {
      // We cannot find the relative path between an absolute and relative path
      if (root.isAbsolute != path.isAbsolute) {
        return null
      }

      val rootAbs = root.absolute()
      val pathAbs = path.absolute()

      if (!isAncestor(rootAbs.toString(), pathAbs.toString(), false)) {
        return null
      }

      val relativePath = rootAbs.relativize(pathAbs)
      return ExecutionRootPath(relativePath)
    }

    fun createAncestorRelativePath(root: File, path: File): ExecutionRootPath? = createAncestorRelativePath(root.toPath(), path.toPath())

    /**
     * @param possibleParent
     * @param possibleChild
     * @param strict if `false` then this method returns `true` if `possibleParent`
     * equals to `possibleChild`.
     */
    fun isAncestor(
      possibleParent: ExecutionRootPath,
      possibleChild: ExecutionRootPath,
      strict: Boolean,
    ): Boolean =
      isAncestor(
        possibleParent.absoluteOrRelativePath.toString(),
        possibleChild.absoluteOrRelativePath.toString(),
        strict,
      )

    /**
     * @param possibleParentPath
     * @param possibleChild
     * @param strict if `false` then this method returns `true` if `possibleParent`
     * equals to `possibleChild`.
     */
    fun isAncestor(
      possibleParentPath: String,
      possibleChild: ExecutionRootPath,
      strict: Boolean,
    ): Boolean =
      isAncestor(
        possibleParentPath,
        possibleChild.absoluteOrRelativePath.toString(),
        strict,
      )

    /**
     * @param possibleParent
     * @param possibleChildPath
     * @param strict if `false` then this method returns `true` if `possibleParent`
     * equals to `possibleChild`.
     */
    fun isAncestor(
      possibleParent: ExecutionRootPath,
      possibleChildPath: String,
      strict: Boolean,
    ): Boolean =
      isAncestor(
        possibleParent.absoluteOrRelativePath.toString(),
        possibleChildPath,
        strict,
      )

    /**
     * @param possibleParentPath
     * @param possibleChildPath
     * @param strict if `false` then this method returns `true` if `possibleParent`
     * equals to `possibleChild`.
     */
    fun isAncestor(
      possibleParentPath: String,
      possibleChildPath: String,
      strict: Boolean,
    ): Boolean = FileUtil.isAncestor(possibleParentPath, possibleChildPath, strict)
  }
}
