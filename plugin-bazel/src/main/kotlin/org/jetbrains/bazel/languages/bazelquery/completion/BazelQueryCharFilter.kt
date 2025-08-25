package org.jetbrains.bazel.languages.bazelquery.completion

import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage

/**
 * BazelQueryCharFilter is needed to avoid hiding
 * the completions after typing special characters
 * like '/' or ':', which often appear in targets.
 */
class BazelQueryCharFilter : CharFilter() {
  override fun acceptChar(
    c: Char,
    prefixLength: Int,
    lookup: Lookup,
  ): Result? {
    if (lookup.psiFile?.language != BazelQueryLanguage) {
      return null
    }

    return Result.ADD_TO_PREFIX
  }
}
