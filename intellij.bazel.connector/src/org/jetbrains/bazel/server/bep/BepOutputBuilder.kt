package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.NamedSetOfFiles
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.OutputGroup
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

@ApiStatus.Internal
data class TextProtoDepSet(val files: Collection<Path>, val children: Collection<String>)

internal class BepOutputBuilder(private val bazelPathsResolver: BazelPathsResolver) {
  private val outputGroups: MutableMap<String, MutableSet<String>> = HashMap()
  private val textProtoFileSets: MutableMap<String, TextProtoDepSet> = HashMap()
  private val rootTargets: MutableSet<WorkspaceTargetKey> = HashSet()
  private val options: MutableList<String> = mutableListOf()
  private val configurations: MutableMap<WorkspaceConfigurationId, WorkspaceConfiguration> = mutableMapOf()
  var buildToolVersion: BazelRelease = BazelRelease.FALLBACK_VERSION

  fun storeNamedSet(id: String, namedSetOfFiles: NamedSetOfFiles) {
    val textProtoDepSet =
      TextProtoDepSet(
        files =
          namedSetOfFiles
            .filesList
            .map { it.toLocalPath() },
        children = namedSetOfFiles.fileSetsList.map { it.id },
      )

    textProtoFileSets[id] = textProtoDepSet
  }

  private fun BuildEventStreamProtos.File.toLocalPath(): Path {
    val mergedPathPrefix = Path(pathPrefixList.joinToString(File.separator))
    val bazelOutputRelativePath = mergedPathPrefix.resolve(name)

    return bazelPathsResolver.resolveOutput(bazelOutputRelativePath)
  }

  fun storeTargetOutputGroups(target: WorkspaceTargetKey, outputGroups: List<OutputGroup>) {
    rootTargets.add(target)

    for (group in outputGroups) {
      val fileSets = group.fileSetsList.map { it.id }
      this.outputGroups.computeIfAbsent(group.name) { HashSet() }.addAll(fileSets)
    }
  }

  fun storeOptions(cmdline: List<String>) {
    options.addAll(cmdline)
  }

  fun storeConfiguration(configuration: WorkspaceConfiguration) {
    configurations[configuration.id] = configuration
  }

  fun clear() {
    outputGroups.clear()
    textProtoFileSets.clear()
    rootTargets.clear()
    options.clear()
    buildToolVersion = BazelRelease.FALLBACK_VERSION
  }

  fun build(): BepOutput = BepOutput(
    outputGroups,
    textProtoFileSets,
    rootTargets,
    options,
    configurations,
    buildToolVersion
  )
}
