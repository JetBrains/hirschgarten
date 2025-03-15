/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting
import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.config.isBazelProject
import java.util.regex.Matcher
import java.util.regex.Pattern

/** Adds hyperlinks to generic console output of the form 'path:line:column: ...'  */
internal class GenericFileMessageFilter(private val project: Project) : Filter {
  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    val matcher: Matcher = FILE_LINE_COLUMN.matcher(line)
    if (!matcher.find()) {
      return null
    }
    val filePath = matcher.group(1)
    if (filePath == null) {
      return null
    }
    val file = FileResolver.resolveToVirtualFile(project, filePath)
    if (file == null) {
      return null
    }
    val lineNumber: Int = parseNumber(matcher.group(2))
    val columnNumber: Int = parseNumber(matcher.group(3))
    val hyperlink: OpenFileHyperlinkInfo =
      CustomOpenFileHyperlinkInfo(project, file, lineNumber - 1, columnNumber - 1)

    val startIx = matcher.start(1)
    val endIx = matcher.end(3)
    val offset = entireLength - line.length
    return Filter.Result(startIx + offset, endIx + offset, hyperlink)
  }

  /** Provider for traceback filter  */
  internal class Provider : ConsoleFilterProvider {
    override fun getDefaultFilters(project: Project): Array<Filter> =
      if (project.isBazelProject) {
        arrayOf(GenericFileMessageFilter(project))
      } else {
        emptyArray()
      }
  }

  /**
   * A trivial wrapper class to allow interrogating file, line, column results in unit tests
   * (without setting up all the project, application services transitively required by
   * OpenFileHyperlinkInfo).
   */
  @VisibleForTesting
  internal class CustomOpenFileHyperlinkInfo private constructor(
    project: Project,
    val vf: VirtualFile,
    val line: Int,
    val column: Int,
  ) : OpenFileHyperlinkInfo(
      project,
      vf,
      line,
      column,
    )

  companion object {
    private val FILE_LINE_COLUMN: Pattern = Pattern.compile("^([^:\\s]+):([0-9]+):([0-9]+): ")

    /** defaults to -1 if no number can be parsed.  */
    private fun parseNumber(string: String?): Int =
      try {
        string?.toInt() ?: -1
      } catch (e: NumberFormatException) {
        -1
      }
  }
}
