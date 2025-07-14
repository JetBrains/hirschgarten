package org.jetbrains.bazel.languages.bazelquery.completion

import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage

class BazelQueryCharFilter : CharFilter() {
  override fun acceptChar(
    c: Char,
    prefixLength: Int,
    lookup: Lookup,
  ): Result? {
    if (lookup.psiFile?.language != BazelQueryLanguage) {
      return null
    }

    return when {
      c == '(' -> Result.SELECT_ITEM_AND_FINISH_LOOKUP
      c == ' ' -> Result.HIDE_LOOKUP
      else -> Result.ADD_TO_PREFIX
    }
  }
}
