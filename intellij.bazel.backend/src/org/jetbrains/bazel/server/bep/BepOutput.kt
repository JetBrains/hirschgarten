package org.jetbrains.bazel.server.bep

import com.google.common.collect.Queues
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@ApiStatus.Internal
class BepOutput(
  private val outputGroups: Map<String, Set<String>> = emptyMap(),
  private val textProtoFileSets: Map<String, TextProtoDepSet> = emptyMap(),
  private val rootTargets: Set<WorkspaceTargetKey> = emptySet(),
  val options: List<String> = emptyList(),
  val configurations: Map<WorkspaceConfigurationId, WorkspaceConfiguration> = emptyMap(),
  var buildToolVersion: BazelRelease = BazelRelease.FALLBACK_VERSION
) {

  fun rootTargets(): Set<WorkspaceTargetKey> = rootTargets

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

  /**
   * Rename all namedSetOfFiles occurring in the BepOutput by encoding the run number.
   */
  fun renameNamedSets(runNumber: Int) : BepOutput  {
    val renameSetName = { name : String -> "${runNumber}.${name}" }
    return BepOutput(
      outputGroups = outputGroups.mapValues {  (_, entries) -> entries.map(renameSetName).toSet() },
      textProtoFileSets = textProtoFileSets.map { (key, entry) -> renameSetName(key) to TextProtoDepSet(entry.files, entry.children.map(renameSetName) ) }.toMap(),
      rootTargets = rootTargets,
      options = options,
      configurations = configurations,
      buildToolVersion = buildToolVersion,
    )
 }



  fun merge(anotherBepOutput: BepOutput): BepOutput =
    BepOutput(
      outputGroups = (outputGroups.keys + anotherBepOutput.outputGroups.keys).associateWith { k ->
        (outputGroups[k] ?: emptySet()) + (anotherBepOutput.outputGroups[k] ?: emptySet())
      },
      textProtoFileSets = (textProtoFileSets.keys + anotherBepOutput.textProtoFileSets.keys).associateWith { k ->
        val left = textProtoFileSets[k] ?: TextProtoDepSet(emptyList(), emptyList())
        val right = anotherBepOutput.textProtoFileSets[k] ?: TextProtoDepSet(emptyList(), emptyList())
        TextProtoDepSet(left.files + right.files, left.children + right.children)
      },
      rootTargets = rootTargets + anotherBepOutput.rootTargets,
      options = options + anotherBepOutput.options,
      configurations = configurations + anotherBepOutput.configurations,

      // pick first that isn't `BazelRelease.FALLBACK_VERSION`
      buildToolVersion = buildToolVersion.takeIf { it != BazelRelease.FALLBACK_VERSION }
                         ?: anotherBepOutput.buildToolVersion.takeIf { it != BazelRelease.FALLBACK_VERSION }
                         ?: BazelRelease.FALLBACK_VERSION,
    )
}
