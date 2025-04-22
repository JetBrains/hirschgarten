package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextExcludableListEntity
import org.jetbrains.bazel.label.Label

class IllegalTargetsSizeException(message: String) : IllegalArgumentException(message)

data class TargetsSpec(override val values: List<Label>, override val excludedValues: List<Label>) :
  ExecutionContextExcludableListEntity<Label>() {
  fun halve(): List<TargetsSpec> {
    val valueSize = values.size
    if (valueSize <= 1) throw IllegalTargetsSizeException("Cannot halve further, size is $valueSize")
    val firstHalfSize = valueSize / 2
    val firstHalf = TargetsSpec(values.subList(0, firstHalfSize), excludedValues)
    val secondHalf = TargetsSpec(values.subList(firstHalfSize, valueSize), excludedValues)
    return listOf(firstHalf, secondHalf)
  }
}
