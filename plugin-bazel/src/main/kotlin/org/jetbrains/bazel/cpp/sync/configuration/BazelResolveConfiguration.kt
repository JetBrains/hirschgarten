package org.jetbrains.bazel.cpp.sync.configuration

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCFileTypeHelpers
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import org.jetbrains.bazel.commons.ExecutionRootPath
import org.jetbrains.bazel.commons.TargetKey
import org.jetbrains.bazel.cpp.sync.BazelCompilerSettings
import org.jetbrains.bazel.cpp.sync.OCSourceFileFinder
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.sync.task.bazelProject

/** A clustering of "equivalent" Bazel targets for creating [OCResolveConfiguration].
 * See com.google.idea.blaze.cpp.BlazeResolveConfiguration
 * */
data class BazelResolveConfiguration(
  val project: Project,
  val configurationData: BazelResolveConfigurationData,
  val displayName: String,
  val targets: List<TargetKey>,
  val targetSources: Map<TargetKey, List<VirtualFile>>,
) {
  fun isEquivalentConfigurations(other: BazelResolveConfiguration): Boolean =
    configurationData == other.configurationData &&
      this.displayName == other.displayName &&
      targets == other.targets &&
      targetSources == other.targetSources

  fun getDeclaredLanguageKind(sourceOrHeaderFile: VirtualFile): OCLanguageKind? {
    val fileName = sourceOrHeaderFile.name
    if (OCFileTypeHelpers.isSourceFile(fileName)) {
      return getLanguageKind(sourceOrHeaderFile)
    }

    if (OCFileTypeHelpers.isHeaderFile(fileName)) {
      return getLanguageKind(OCSourceFileFinder.getSourceFileForHeaderFile(project, sourceOrHeaderFile))
    }

    return null
  }

  private fun getLanguageKind(sourceFile: VirtualFile?): OCLanguageKind? {
    if (sourceFile == null) return maximumLanguageKind

    val kind = OCFileTypeHelpers.getLanguageKind(sourceFile.name)
    return kind ?: maximumLanguageKind
  }

  val libraryHeadersRootsInternal: List<ExecutionRootPath>
    get() {
      return listOf(
        configurationData.transitiveQuoteIncludeDirectories,
        configurationData.transitiveIncludeDirectories,
        configurationData.transitiveSystemIncludeDirectories,
      ).flatten()
    }

  val targetCopts
    get() = configurationData.localCopts

  val compilerSettings: BazelCompilerSettings
    get() = configurationData.compilerSettings

  fun getSources(targetKey: TargetKey): List<VirtualFile> = targetSources[targetKey] ?: emptyList()

  companion object {
    fun createForTargets(
      project: Project,
      configurationData: BazelResolveConfigurationData,
      targets: List<TargetKey>,
    ): BazelResolveConfiguration =
      BazelResolveConfiguration(
        project,
        configurationData,
        computeDisplayName(targets),
        targets,
        computeTargetToSources(project, targets),
      )

    private fun computeDisplayName(targets: List<TargetKey>): String {
      val minTargetKey = targets.minWith { obj: TargetKey, other: TargetKey -> obj.compareTo(other) }
      val minTarget = minTargetKey.toString()
      if (targets.size == 1) {
        return minTarget
      } else {
        return String.format("%s and %d other target(s)", minTarget, targets.size - 1)
      }
    }

    private val maximumLanguageKind: OCLanguageKind
      get() = CLanguageKind.CPP

    private fun computeTargetToSources(project: Project, targets: List<TargetKey>): Map<TargetKey, List<VirtualFile>> {
      val targetSourcesBuilder: MutableMap<TargetKey, List<VirtualFile>> = mutableMapOf()

      for (targetKey in targets) {
        targetSourcesBuilder.put(targetKey, computeSources(project, targetKey))
      }
      return targetSourcesBuilder
    }

    private fun computeSources(project: Project, targetKey: TargetKey): List<VirtualFile> {
      val bazelPathResolver = BazelPathsResolver(project.bazelProject.bazelInfo)
      val builder = mutableListOf<VirtualFile>()
      val targetIdeInfo = project.bazelProject.targets[targetKey.label] ?: return emptyList()
      for (sourceArtifact in targetIdeInfo.sourcesList) {
        val file = bazelPathResolver.resolve(sourceArtifact)
        val vf: VirtualFile = VirtualFileManager.getInstance().findFileByNioPath(file) ?: continue
        if (!OCFileTypeHelpers.isSourceFile(vf.name)) {
          continue
        }
        builder.add(vf)
      }
      return builder
    }
  }
}
