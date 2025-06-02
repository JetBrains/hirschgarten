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

import com.google.common.annotations.VisibleForTesting
import com.google.idea.blaze.base.issueparser.NonProblemHyperlinkInfo
import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ui.UIUtil
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import java.awt.Font
import java.util.regex.Matcher
import java.util.regex.Pattern

/** Parse blaze targets in streamed output.  */
class BlazeTargetFilter : Filter {
  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    val matcher: Matcher = TARGET_PATTERN.matcher(line)
    val results = ArrayList<Filter.ResultItem>()
    while (matcher.find()) {
      val prefixLength = matcher.group(1)?.length ?: 0

      val labelString = matcher.group().substring(prefixLength)
      // for performance reasons, don't resolve until the user clicks the link
      val link: NonProblemHyperlinkInfo =
        NonProblemHyperlinkInfo { project ->
          val label: Label? = LabelUtils.createLabelFromString( /* blazePackage= */null, labelString)
          if (label == null) {
            Logger
              .getInstance(this.javaClass)
              .error(IllegalStateException("Parsing returned illegal label: $labelString"))
          }
          val psi: PsiElement? = BuildReferenceManager.getInstance(project).resolveLabel(label)
          if (psi is NavigatablePsiElement) {
            psi.navigate(true)
          }
        }
      val offset = entireLength - line.length
      results.add(
        Filter.ResultItem( // highlightStartOffset=
          matcher.start() + offset + prefixLength, // highlightEndOffset=
          matcher.end() + offset,
          link,
          this.highlightAttributes,
        ),
      )
    }
    return if (results.isEmpty()) null else Filter.Result(results)
  }

  private val highlightAttributes: TextAttributes
    get() = // Avoid a sea of blue in the console: just add a grey underline to navigable targets.
      TextAttributes(
        UIUtil.getActiveTextColor(), // backgroundColor=
        null,
        UIUtil.getInactiveTextColor(),
        EffectType.LINE_UNDERSCORE,
        Font.PLAIN,
      )

  /** Provider for [BlazeTargetFilter]  */
  internal class Provider : ConsoleFilterProvider {
    override fun getDefaultFilters(project: Project): Array<Filter> =
      if (project.isBazelProject) arrayOf(BlazeTargetFilter()) else emptyArray()
  }

  companion object {
    // See Bazel's LabelValidator class. Whitespace character and ' intentionally not included here.
    private const val PACKAGE_NAME_CHARS = "a-zA-Z0-9/\\-\\._$()@"
    private const val TARGET_CHARS = "a-zA-Z0-9!%@^_\"#$&()*\\-+,;<=>?\\[\\]{|}~/\\."

    // ignore '//' preceded by text (e.g. https://...)
    // format: ([@external_workspace]//package:rule)
    private val TARGET_REGEX: String? =
      String.format(
        "(^|[ '\"=])(@[%s]*)?//[%s]+(:[%s]+)?",
        PACKAGE_NAME_CHARS,
        PACKAGE_NAME_CHARS,
        TARGET_CHARS,
      )

    @VisibleForTesting
    val TARGET_PATTERN: Pattern = Pattern.compile(TARGET_REGEX)
  }
}
