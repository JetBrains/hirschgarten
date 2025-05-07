package org.jetbrains.bazel.commons

import com.google.common.collect.ComparisonChain
import com.google.common.collect.Ordering
import org.jetbrains.bazel.label.Label

/** A key that uniquely identifies a target in the target map */
data class TargetKey(val label: Label, val aspectIds: List<String> = listOf()) : Comparable<TargetKey> {
  override fun compareTo(other: TargetKey): Int =
    ComparisonChain
      .start()
      .compare(label, other.label)
      .compare(aspectIds, other.aspectIds, Ordering.natural<String>().lexicographical())
      .result()

  val isPlainTarget: Boolean = aspectIds.isEmpty()
}
