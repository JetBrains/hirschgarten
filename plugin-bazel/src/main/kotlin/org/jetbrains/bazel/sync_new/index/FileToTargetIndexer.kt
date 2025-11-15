package org.jetbrains.bazel.sync_new.index

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

data class Target(
  val label: Label,
  val sources: List<Path>,
)

data class Diff(
  val new: List<Target>,
  val updated: List<Target>,
  val removed: List<Target>,
)

class FileToTargetIndexer {
  fun index(index: One2ManyIndex<Path, Label>, diff: Diff, labelToTarget: (label: Label) -> Target) {
    val affectedTargets = mutableListOf<Label>()
    for (removed in diff.removed + diff.updated) {
      for (source in removed.sources) {
        affectedTargets += index.invalidateByKey(source)
      }
    }

    val removedTargets = diff.removed.map { it.label }.toSet()
    val toReindex = (affectedTargets - removedTargets) + diff.updated.map { it.label } + diff.new.map { it.label }

    for (label in toReindex) {
      val target = labelToTarget(label)
      for (source in target.sources) {
        index.add(source, listOf(target.label))
      }
    }
  }
}
