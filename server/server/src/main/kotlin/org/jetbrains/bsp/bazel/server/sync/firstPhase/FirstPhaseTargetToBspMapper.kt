package org.jetbrains.bsp.bazel.server.sync.firstPhase

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.BuildTargetTag
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.server.model.BspMappings
import org.jetbrains.bsp.bazel.server.model.FirstPhaseProject
import org.jetbrains.bsp.bazel.server.model.Language
import org.jetbrains.bsp.bazel.server.sync.languages.JVMLanguagePluginParser
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.EnhancedSourceItem
import java.nio.file.Path
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.collections.mapNotNull
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
      BuildTargetIdentifier(name),
      inferTags(),
      inferLanguages().map { it.id }.toList(),
      interestingDeps.map { BuildTargetIdentifier(it) },
      inferCapabilities(),
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
    BuildTargetCapabilities().apply {
      canCompile = !isManual
      canRun = isBinary
      canTest = isTest
      canDebug = false
    }

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1557
  fun Target.isSupported(): Boolean = Language.allOfKind(kind).isNotEmpty()

  fun toSourcesResult(project: FirstPhaseProject, sourcesParams: SourcesParams): SourcesResult {
    val items =
      project
        .lightweightModulesForTargets(sourcesParams.targets)
        .map { it.toBspSourcesItem(project) }

    return SourcesResult(items)
  }

  private fun Target.toBspSourcesItem(project: FirstPhaseProject): SourcesItem {
    val sourceFiles = srcs.map { it.bazelFileFormatToPath() }.filter { it.exists() }.filter { it.isRegularFile() }
    val sourceFilesAndData = sourceFiles.map { it to JVMLanguagePluginParser.calculateJVMSourceRootAndAdditionalData(it) }

    val itemsForSourcesReferencedViaTarget =
      srcs
        .mapNotNull { Label.parseOrNull(it) }
        .mapNotNull { project.modules[it] }
        .map { it.toBspSourcesItem(project) }
    val directItems = sourceFilesAndData.map { EnhancedSourceItem(it.first.toUri().toString(), SourceItemKind.FILE, false, it.second.data) }
    val items = (directItems + itemsForSourcesReferencedViaTarget.flatMap { it.sources }).distinct()

    val directRoots = sourceFilesAndData.map { it.second.sourceRoot }.map { it.toUri().toString() }
    val roots = (directRoots + itemsForSourcesReferencedViaTarget.flatMap { it.roots }).distinct()

    return SourcesItem(BuildTargetIdentifier(name), items).apply {
      this.roots = roots
    }
  }

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1549/
  fun toResourcesResult(project: FirstPhaseProject, resourcesParams: ResourcesParams): ResourcesResult {
    val items =
      project
        .lightweightModulesForTargets(resourcesParams.targets)
        .map { it.toBspResourcesItem() }

    return ResourcesResult(items)
  }

  private fun Target.toBspResourcesItem(): ResourcesItem =
    ResourcesItem(
      BuildTargetIdentifier(name),
      resources.map { it.bazelFileFormatToPath() }.filter { it.exists() }.map { it.toUri().toString() },
    )

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
