package org.jetbrains.bazel.commons

import org.jetbrains.bazel.label.Label

/**
 * Utility class for working with collections of includable and excludable targets.
 * This replaces the old TargetsSpec class.
 */
data class TargetCollection(
  val values: List<Label>,
  val excludedValues: List<Label> = emptyList()
) {
  companion object {
    fun fromExcludableList(targets: List<ExcludableValue<Label>>): TargetCollection {
      val included = mutableListOf<Label>()
      val excluded = mutableListOf<Label>()
      
      targets.forEach { excludableTarget ->
        if (excludableTarget.isIncluded()) {
          included.add(excludableTarget.value)
        } else {
          excluded.add(excludableTarget.value)
        }
      }
      
      return TargetCollection(included, excluded)
    }
  }
}