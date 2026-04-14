package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.SourceItem

@ApiStatus.Internal
object SourcePatternEval {
  data class PatternEvalResult<T>(val included: List<T>, val excluded: List<T>)

  fun <T> eval(
    items: Iterable<T>,
    includes: List<(item: T) -> Boolean>,
    excludes: List<(item: T) -> Boolean>,
  ): PatternEvalResult<T> {
    val matched = mutableListOf<T>()
    val unmatched = mutableListOf<T>()
    for (item in items) {
      val excluded = if (includes.isEmpty()) {
        false
      } else {
        excludes.any { it(item) }
      }
      if (excluded) {
        unmatched.add(item)
      } else {
        if (includes.any { it(item) }) {
          matched.add(item)
        } else {
          unmatched.add(item)
        }
      }
    }
    return PatternEvalResult(matched, unmatched)
  }

  fun evalSources(
    sources: List<SourceItem>,
    includes: List<SourceRootPattern>,
    excludes: List<SourceRootPattern>,
  ): PatternEvalResult<SourceItem> = eval(
    items = sources,
    includes = includes.map { { sourceItem: SourceItem -> it.matches(sourceItem.path) } },
    excludes = excludes.map { { sourceItem: SourceItem -> it.matches(sourceItem.path) } },
  )
}
