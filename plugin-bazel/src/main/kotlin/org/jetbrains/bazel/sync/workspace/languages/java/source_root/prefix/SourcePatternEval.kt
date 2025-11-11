package org.jetbrains.bazel.sync.workspace.languages.java.source_root.prefix

import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path
import kotlin.io.path.relativeTo

object SourcePatternEval {
  data class PatternEvalResult<T>(val included: List<T>, val excluded: List<T>)

  fun <T, R> eval(
    items: Iterable<T>,
    transform: (item: T) -> R,
    includes: List<(item: R) -> Boolean>,
    excludes: List<(item: R) -> Boolean>,
  ): PatternEvalResult<T> {
    val matched = mutableListOf<T>()
    val unmatched = mutableListOf<T>()
    for (item in items) {
      val transformed = transform(item)
      val excluded = if (includes.isEmpty()) {
        false
      } else {
        excludes.any { it(transformed) }
      }
      if (excluded) {
        unmatched.add(item)
      } else {
        if (includes.any { it(transformed) }) {
          matched.add(item)
        } else {
          unmatched.add(item)
        }
      }
    }
    return PatternEvalResult(matched, unmatched)
  }

  fun <T> evalIdentity(
    items: Iterable<T>,
    includes: List<(item: T) -> Boolean>,
    excludes: List<(item: T) -> Boolean>,
  ): PatternEvalResult<T> = eval(items, { it }, includes, excludes)

  fun evalSources(
    workspaceRoot: Path,
    sources: List<SourceItem>,
    includes: List<SourceRootPattern>,
    excludes: List<SourceRootPattern>,
  ): PatternEvalResult<SourceItem> = eval(
    items = sources,
    transform = { it.path.relativeTo(workspaceRoot).toString() },
    includes = includes.map { { pattern: String -> it.matches(pattern)} },
    excludes = excludes.map { { pattern: String -> it.matches(pattern)} },
  )
}
