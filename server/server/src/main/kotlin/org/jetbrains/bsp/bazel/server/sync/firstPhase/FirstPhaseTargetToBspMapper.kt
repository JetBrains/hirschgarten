package org.jetbrains.bsp.bazel.server.sync.firstPhase

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.bazel.server.model.BspMappings
import org.jetbrains.bsp.bazel.server.model.FirstPhaseProject
import org.jetbrains.bsp.bazel.server.model.Language
import org.jetbrains.bsp.bazel.server.sync.languages.JVMLanguagePluginParser
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.ResourcesParams
import org.jetbrains.bsp.protocol.ResourcesResult
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourceItemKind
import org.jetbrains.bsp.protocol.SourcesItem
import org.jetbrains.bsp.protocol.SourcesParams
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class FirstPhaseTargetToBspMapper(private val workspaceContextProvider: WorkspaceContextProvider, private val workspaceRoot: Path) {
  fun toWorkspaceBuildTargetsResult(project: FirstPhaseProject): WorkspaceBuildTargetsResult {
    val shouldSyncManualTargets = workspaceContextProvider.currentWorkspaceContext().allowManualTargetsSync.value

    val targets =
      project.modules
        .values
        .asSequence()
        .filter { it.isSupported() }
        .filter { shouldSyncManualTargets || !it.isManual }
        .filterNot { it.isNoIde }
        .map { it.toBspBuildTarget() }
        .toList()

    return WorkspaceBuildTargetsResult(targets)
  }

  private fun Target.toBspBuildTarget(): BuildTarget =
    BuildTarget(
      id = BuildTargetIdentifier(name),
      tags = inferTags(),
      languageIds = inferLanguages().map { it.id }.toList(),
      dependencies = interestingDeps.map { BuildTargetIdentifier(it) },
      capabilities = inferCapabilities(),
    )

  private fun Target.inferTags(): List<String> {
    val typeTag = inferTypeTagFromTargetKind()
    val manualTag = if (isManual) BuildTargetTag.MANUAL else null

    return listOfNotNull(typeTag, manualTag)
  }

  private fun Target.inferTypeTagFromTargetKind(): String =
    when {
      isBinary -> BuildTargetTag.APPLICATION
      isTest -> BuildTargetTag.TEST
      else -> BuildTargetTag.LIBRARY
    }

  private fun Target.inferLanguages(): Set<Language> {
    val languagesForTarget = Language.allOfKind(kind)
    val languagesForSources = srcs.flatMap { Language.allOfSource(it) }.toHashSet()
    return languagesForTarget + languagesForSources
  }

  private fun Target.inferCapabilities(): BuildTargetCapabilities =
    BuildTargetCapabilities(
      canCompile = !isManual,
      canRun = isBinary,
      canTest = isTest,
      canDebug = false,
    )

  private fun Target.isSupported(): Boolean {
    val isRuleSupported = Language.allOfKind(kind).isNotEmpty()
    val areSourcesSupported =
      srcs
        .map { it.substringAfterLast('.') }
        .any { Language.allOfSource(".$it").isNotEmpty() }

    return isRuleSupported || areSourcesSupported
  }

  fun toSourcesResult(project: FirstPhaseProject, sourcesParams: SourcesParams): SourcesResult {
    val items =
      project
        .lightweightModulesForTargets(sourcesParams.targets)
        .map { it.toBspSourcesItem(project) }

    return SourcesResult(items)
  }

  private fun Target.toBspSourcesItem(project: FirstPhaseProject): SourcesItem {
    val sourceFiles = srcs.calculateFiles()
    val sourceFilesAndData = sourceFiles.map { it to JVMLanguagePluginParser.calculateJVMSourceRootAndAdditionalData(it) }

    val itemsForSourcesReferencedViaTarget = srcs.calculateModuleDependencies(project).map { it.toBspSourcesItem(project) }
    val directItems =
      sourceFilesAndData.map {
        SourceItem(
          it.first.toUri().toString(),
          SourceItemKind.FILE,
          false,
          it.second.jvmPackagePrefix,
        )
      }
    val items = (directItems + itemsForSourcesReferencedViaTarget.flatMap { it.sources }).distinct()

    val directRoots = sourceFilesAndData.map { it.second.sourceRoot }.map { it.toUri().toString() }
    val roots = (directRoots + itemsForSourcesReferencedViaTarget.flatMap { it.roots }).distinct()

    return SourcesItem(BuildTargetIdentifier(name), sources = items, roots = roots)
  }

  fun toResourcesResult(project: FirstPhaseProject, resourcesParams: ResourcesParams): ResourcesResult {
    val items =
      project
        .lightweightModulesForTargets(resourcesParams.targets)
        .map { it.toBspResourcesItem(project) }

    return ResourcesResult(items)
  }

  private fun Target.toBspResourcesItem(project: FirstPhaseProject): ResourcesItem {
    val directResources = resources.calculateFiles().map { it.toUri().toString() }
    val resourcesReferencedViaTarget =
      resources
        .calculateModuleDependencies(project)
        .map { it.toBspResourcesItem(project) }
        .flatMap { it.resources }

    val items = (directResources + resourcesReferencedViaTarget).distinct()
    return ResourcesItem(BuildTargetIdentifier(name), items)
  }

  private fun List<String>.calculateModuleDependencies(project: FirstPhaseProject): List<Target> =
    mapNotNull { Label.parseOrNull(it) }.mapNotNull { project.modules[it] }

  private fun List<String>.calculateFiles(): List<Path> =
    map { it.bazelFileFormatToPath() }.filter { it.exists() }.filter { it.isRegularFile() }

  private fun String.bazelFileFormatToPath(): Path {
    val withoutColons = replace(':', '/')
    val withoutTargetPrefix = withoutColons.trimStart('/')
    val relativePath = Path(withoutTargetPrefix)

    return workspaceRoot.resolve(relativePath)
  }

  private fun FirstPhaseProject.lightweightModulesForTargets(targets: List<BuildTargetIdentifier>): List<Target> =
    BspMappings
      .toLabels(targets)
      .mapNotNull { modules[it] }
}
