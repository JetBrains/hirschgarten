package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextExcludableListEntity
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.TargetPattern

class IllegalTargetsSizeException(message: String) : IllegalArgumentException(message)

data class TargetsSpec(override val values: List<TargetPattern>, override val excludedValues: List<TargetPattern>) :
  ExecutionContextExcludableListEntity<TargetPattern>() {
  fun halve(): List<TargetsSpec> {
    val valueSize = values.size
    if (valueSize <= 1) throw IllegalTargetsSizeException("Cannot halve further, size is $valueSize")
    val firstHalfSize = valueSize / 2
    val firstHalf = TargetsSpec(values.subList(0, firstHalfSize), excludedValues)
    val secondHalf = TargetsSpec(values.subList(firstHalfSize, valueSize), excludedValues)
    return listOf(firstHalf, secondHalf)
  }
}
