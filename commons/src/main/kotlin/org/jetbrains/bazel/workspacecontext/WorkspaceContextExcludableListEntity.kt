package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

/**
 * Base list-based `WorkspaceContext` entity class - you need to extend it if you want to
 * create your list-based with excluded values entity.
 */
abstract class WorkspaceContextExcludableListEntity<T> : WorkspaceContextListEntity<T>() {
  abstract val excludedValues: List<T>
}

data class DirectoriesSpec(override val values: List<Path>, override val excludedValues: List<Path>) :
  WorkspaceContextExcludableListEntity<Path>()

class IllegalTargetsSizeException(message: String) : IllegalArgumentException(message)

data class TargetsSpec(override val values: List<TargetPattern>, override val excludedValues: List<TargetPattern>) :
  WorkspaceContextExcludableListEntity<TargetPattern>() {
  fun halve(): List<TargetsSpec> {
    val valueSize = values.size
    if (valueSize <= 1) throw IllegalTargetsSizeException("Cannot halve further, size is $valueSize")
    val firstHalfSize = valueSize / 2
    val firstHalf = TargetsSpec(values.subList(0, firstHalfSize), excludedValues)
    val secondHalf = TargetsSpec(values.subList(firstHalfSize, valueSize), excludedValues)
    return listOf(firstHalf, secondHalf)
  }
}
