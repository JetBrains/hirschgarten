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

import com.google.idea.blaze.base.settings.Blaze
import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.isBazelProject
import java.util.regex.Matcher
import java.util.regex.Pattern

/** Hyperlinks test logs in the streamed output.  */
internal class TestLogFilter(private val project: Project) : Filter {
  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    var matcher: Matcher = OLD_REGEX.matcher(line)
    if (!matcher.matches()) {
      matcher = NEW_REGEX.matcher(line)
    }
    if (!matcher.matches()) {
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
    val offset = entireLength - line.length
    return Filter.Result(
      matcher.start(1) + offset,
      matcher.end(1) + offset,
      OpenFileHyperlinkInfo(project, file, /* line= */0),
    )
  }

  /** Provider for traceback filter  */
  internal class Provider : ConsoleFilterProvider {
    override fun getDefaultFilters(project: Project): Array<Filter> =
      if (project.isBazelProject) {
        arrayOf(TestLogFilter(project))
      } else {
        emptyArray()
      }
  }

  companion object {
    private val OLD_REGEX: Pattern = Pattern.compile("^\\s*(/[^:\\s]+/testlogs/[^:\\s]+/test\\.log)\\s*$")

    private val NEW_REGEX: Pattern = Pattern.compile(".*\\(see (/[^:\\s]+/testlogs/[^:\\s]+/test\\.log)\\)\\s*")
  }
}
