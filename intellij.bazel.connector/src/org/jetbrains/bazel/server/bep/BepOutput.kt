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

  private val configIdCache: ConcurrentMap<String, WorkspaceConfiguration> = ConcurrentHashMap()

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
   * Find according [WorkspaceConfiguration] by either full checksum of short id
   *
   * @param id Full checksum or short id (`ctx.configuration.short_id`)
   *
   * @return Matching [WorkspaceConfiguration]
   */
  fun findConfigurationByChecksum(id: String): WorkspaceConfiguration? {
    // full configuration checksum
    configurations[WorkspaceConfigurationId.of(id)]
      ?.let { return it }

    // try matching short checksum
    return configIdCache.computeIfAbsent(id) {
      configurations.values.firstOrNull { cfg -> cfg.id.configurationChecksum?.startsWith(id) == true }
    }
  }

  fun merge(anotherBepOutput: BepOutput): BepOutput =
    BepOutput(
      outputGroups = outputGroups + anotherBepOutput.outputGroups,
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
