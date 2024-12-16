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
 *
 * Original Java code taken from https://github.com/bazelbuild, converted to Kotlin and modified.
 */
package org.jetbrains.plugins.bsp.golang.debug

import com.google.common.base.Objects
import java.io.File

/**
 * An absolute or relative path returned from Bazel. If it is a relative path, it is relative to the
 * execution root.
 */
class ExecutionRootPath(path: String) {
  val absoluteOrRelativeFile: File

  init {
    this.absoluteOrRelativeFile = File(path)
  }

  val isAbsolute: Boolean
    get() = absoluteOrRelativeFile.isAbsolute()

  fun getFileRootedAt(absoluteRoot: File): File =
    if (absoluteOrRelativeFile.isAbsolute()) {
      this.absoluteOrRelativeFile
    } else {
      File(
        absoluteRoot,
        absoluteOrRelativeFile.getPath(),
      )
    }

  override fun equals(o: Any?): Boolean {
    if (this === o) {
      return true
    }
    if (o == null || javaClass != o.javaClass) {
      return false
    }
    val that = o as ExecutionRootPath
    return Objects.equal(this.absoluteOrRelativeFile, that.absoluteOrRelativeFile)
  }

  override fun hashCode(): Int = Objects.hashCode(this.absoluteOrRelativeFile)

  override fun toString(): String = "ExecutionRootPath{" + "path='" + this.absoluteOrRelativeFile + '\'' + '}'
}
