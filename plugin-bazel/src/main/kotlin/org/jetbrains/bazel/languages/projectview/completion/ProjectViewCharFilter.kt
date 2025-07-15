package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

class ProjectViewCharFilter : CharFilter() {
  override fun acceptChar(
    c: Char,
    prefixLength: Int,
    lookup: Lookup?,
  ): Result? {
    if (lookup?.psiFile?.language != ProjectViewLanguage) {
      return null
    }
    if (c == ' ') return Result.HIDE_LOOKUP
    return Result.ADD_TO_PREFIX
  }
}
