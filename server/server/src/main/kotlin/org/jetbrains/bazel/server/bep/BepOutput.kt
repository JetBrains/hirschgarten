package org.jetbrains.bazel.server.bep

import com.google.common.collect.Queues
import org.jetbrains.bazel.label.CanonicalLabel
import java.nio.file.Path

class BepOutput(
  private val outputGroups: Map<String, Set<String>> = emptyMap(),
  private val textProtoFileSets: Map<String, TextProtoDepSet> = emptyMap(),
  private val rootTargets: Set<CanonicalLabel> = emptySet(),
) {
  fun rootTargets(): Set<CanonicalLabel> = rootTargets

  fun filesByOutputGroupNameTransitive(outputGroup: String): Set<Path> {
    val rootIds = outputGroups.getOrDefault(outputGroup, emptySet())
    if (rootIds.isEmpty()) {
      return emptySet()
    }
    val result = HashSet<Path>(rootIds.size)
    val toVisit = Queues.newArrayDeque(rootIds)
    val visited = HashSet<String>(rootIds)
    while (!toVisit.isEmpty()) {
      val fileSetId = toVisit.remove()
      val fileSet = textProtoFileSets[fileSetId]
      result.addAll(fileSet!!.files)
      val children = fileSet.children
      children
        .asSequence()
        .filter { child: String -> !visited.contains(child) }
        .forEach { e: String ->
          visited.add(e)
          toVisit.add(e)
        }
    }
    return result
  }

  fun merge(anotherBepOutput: BepOutput): BepOutput =
    BepOutput(
      outputGroups + anotherBepOutput.outputGroups,
      (textProtoFileSets.keys + anotherBepOutput.textProtoFileSets.keys).associateWith { k ->
        val left = textProtoFileSets[k] ?: TextProtoDepSet(emptyList(), emptyList())
        val right = anotherBepOutput.textProtoFileSets[k] ?: TextProtoDepSet(emptyList(), emptyList())
        TextProtoDepSet(left.files + right.files, left.children + right.children)
      },
      rootTargets + anotherBepOutput.rootTargets,
    )
}
