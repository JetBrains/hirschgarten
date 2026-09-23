package org.jetbrains.bazel.server.bep

import com.google.common.collect.Queues
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import java.nio.file.Path

@ApiStatus.Internal
class BepOutput(
  private val infoOutputGroupFileSets: Set<String> = emptySet(),
  private val textProtoFileSets: Map<String, TextProtoDepSet> = emptyMap(),
  private val rootTargets: Set<WorkspaceTargetKey> = emptySet(),
  val options: List<String> = emptyList(),
  val configurations: Map<WorkspaceConfigurationId, WorkspaceConfiguration> = emptyMap(),
  var buildToolVersion: BazelRelease = BazelRelease.FALLBACK_VERSION,
) {

  fun rootTargets(): Set<WorkspaceTargetKey> = rootTargets

  fun filesByInfoOutputGroup(): Set<Path> {
    val rootIds = infoOutputGroupFileSets
    if (rootIds.isEmpty()) {
      return emptySet()
    }
    val result = HashSet<Path>(rootIds.size)
    val toVisit = Queues.newArrayDeque(rootIds)
    val visited = HashSet<String>(rootIds)
    while (!toVisit.isEmpty()) {
      val fileSetId = toVisit.remove()
      val fileSet = textProtoFileSets[fileSetId] ?: run {
        logger.error("Missing textproto for $fileSetId")
        continue
      }
      result.addAll(fileSet.files)
      for (child in fileSet.children) {
        if (visited.add(child)) {
          toVisit.add(child)
        }
      }
    }
    return result
  }

  /**
   * Rename all namedSetOfFiles occurring in the BepOutput by encoding the run number.
   */
  fun renameNamedSets(runNumber: Int): BepOutput {
    val renameSetName = { name: String -> "${runNumber}.${name}" }
    return BepOutput(
      infoOutputGroupFileSets = infoOutputGroupFileSets.map(renameSetName).toSet(),
      textProtoFileSets = textProtoFileSets.map { (key, entry) -> renameSetName(key) to TextProtoDepSet(entry.files, entry.children.map(renameSetName) ) }.toMap(),
      rootTargets = rootTargets,
      options = options,
      configurations = configurations,
      buildToolVersion = buildToolVersion,
    )
  }

  fun merge(anotherBepOutput: BepOutput): BepOutput = mergeAll(listOf(this, anotherBepOutput))

  class Accumulator {
    private class MutableDepSet {
      val files: MutableList<Path> = ArrayList()
      val children: MutableList<String> = ArrayList()
    }

    private val infoOutputGroupFileSets: MutableSet<String> = LinkedHashSet()
    private val textProtoFileSets: MutableMap<String, MutableDepSet> = LinkedHashMap()
    private val rootTargets: MutableSet<WorkspaceTargetKey> = LinkedHashSet()
    private val options: MutableList<String> = ArrayList()
    private val configurations: MutableMap<WorkspaceConfigurationId, WorkspaceConfiguration> = LinkedHashMap()
    private var buildToolVersion: BazelRelease = BazelRelease.FALLBACK_VERSION

    fun add(output: BepOutput): Accumulator {
      infoOutputGroupFileSets.addAll(output.infoOutputGroupFileSets)
      for ((id, depSet) in output.textProtoFileSets) {
        val merged = textProtoFileSets.getOrPut(id) { MutableDepSet() }
        merged.files.addAll(depSet.files)
        merged.children.addAll(depSet.children)
      }
      rootTargets.addAll(output.rootTargets)
      options.addAll(output.options)
      configurations.putAll(output.configurations)
      if (buildToolVersion == BazelRelease.FALLBACK_VERSION) {
        buildToolVersion = output.buildToolVersion
      }
      return this
    }

    fun build(): BepOutput = BepOutput(
      infoOutputGroupFileSets = infoOutputGroupFileSets.toSet(),
      textProtoFileSets = textProtoFileSets.mapValues { (_, depSet) -> TextProtoDepSet(depSet.files.toList(), depSet.children.toList()) },
      rootTargets = rootTargets.toSet(),
      options = options.toList(),
      configurations = configurations.toMap(),
      buildToolVersion = buildToolVersion,
    )
  }

  companion object {
    private val logger = logger<BepOutput>()

    fun mergeAll(outputs: Iterable<BepOutput>): BepOutput = Accumulator().apply {
      outputs.forEach { add(it) }
    }.build()
  }
}
