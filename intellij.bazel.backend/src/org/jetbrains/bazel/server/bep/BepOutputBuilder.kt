package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.OutputGroup
import com.intellij.aspect.lib.OutputGroups
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import java.nio.file.Path

@ApiStatus.Internal
data class TextProtoDepSet(val files: Collection<Path>, val children: Collection<String>)

internal class BepOutputBuilder {
  private val infoOutputGroupFileSets: MutableSet<String> = HashSet()
  private val textProtoFileSets: MutableMap<String, TextProtoDepSet> = HashMap()
  private val rootTargets: MutableSet<WorkspaceTargetKey> = HashSet()
  private val options: MutableList<String> = mutableListOf()
  val configurations: MutableMap<WorkspaceConfigurationId, WorkspaceConfiguration> = mutableMapOf()
  var buildToolVersion: BazelRelease = BazelRelease.FALLBACK_VERSION

  fun storeNamedSet(id: String, textProtoDepSet: TextProtoDepSet) {
    textProtoFileSets[id] = textProtoDepSet
  }

  fun storeTargetOutputGroups(target: WorkspaceTargetKey, outputGroups: List<OutputGroup>) {
    rootTargets.add(target)
    val infoGroup = outputGroups.firstOrNull { it.name == OutputGroups.INFO.groupName } ?: return
    val fileSets = infoGroup.fileSetsList.map { it.id }
    infoOutputGroupFileSets.addAll(fileSets)
  }

  fun storeOptions(cmdline: List<String>) {
    options.addAll(cmdline)
  }

  fun storeConfiguration(configuration: WorkspaceConfiguration) {
    configurations[configuration.id] = configuration
  }

  fun add(output: BepOutput) {
    infoOutputGroupFileSets.addAll(output.infoOutputGroupFileSets)
    for ((id, depSet) in output.textProtoFileSets) {
      textProtoFileSets.merge(id, depSet) { first, second -> TextProtoDepSet(first.files + second.files, first.children + second.children) }
    }
    rootTargets.addAll(output.rootTargets())
    options.addAll(output.options)
    configurations.putAll(output.configurations)
    if (buildToolVersion == BazelRelease.FALLBACK_VERSION) {
      buildToolVersion = output.buildToolVersion
    }
  }

  fun clear() {
    infoOutputGroupFileSets.clear()
    textProtoFileSets.clear()
    rootTargets.clear()
    options.clear()
    buildToolVersion = BazelRelease.FALLBACK_VERSION
  }

  fun build(): BepOutput = BepOutput(
    infoOutputGroupFileSets,
    textProtoFileSets,
    rootTargets,
    options,
    configurations,
    buildToolVersion,
  )
}
