package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.slf4j.LoggerFactory
import java.nio.file.Path

class WorkspaceContextConstructor(
  private val workspaceRoot: Path,
  private val dotBazelBspDirPath: Path,
  projectViewPath: Path,
) {
  private val log = LoggerFactory.getLogger(WorkspaceContextConstructor::class.java)

  fun construct(projectView: ProjectView): WorkspaceContext {
    log.info("Constructing workspace context for: {}.", projectView)

    return WorkspaceContext(
      targets = convertTargets(projectView),
      directories = convertDirectories(projectView),
      buildFlags = convertFlags(projectView.buildFlags),
      syncFlags = convertFlags(projectView.syncFlags),
      debugFlags = convertFlags(projectView.debugFlags),
      bazelBinary = projectView.bazelBinary?.value,
      allowManualTargetsSync = projectView.allowManualTargetsSync?.value ?: false,
      dotBazelBspDirPath = dotBazelBspDirPath,
      importDepth = projectView.importDepth?.value ?: 1,
      enabledRules = projectView.enabledRules?.values ?: emptyList(),
      ideJavaHomeOverride = projectView.ideJavaHomeOverride?.value,
      shardSync = projectView.shardSync?.value ?: false,
      targetShardSize = projectView.targetShardSize?.value ?: 1000,
      shardingApproach = projectView.shardingApproach?.value?.lowercase(),
      importRunConfigurations = projectView.importRunConfigurations?.values?.map { it.toString() } ?: emptyList(),
      gazelleTarget = projectView.gazelleTarget?.value?.let { Label.parse(it) },
      indexAllFilesInDirectories = projectView.indexAllFilesInDirectories?.value ?: false,
      pythonCodeGeneratorRuleNames = projectView.pythonCodeGeneratorRuleNamesSection?.values ?: emptyList(),
      importIjars = projectView.importIjars?.value ?: false,
      deriveInstrumentationFilterFromTargets = projectView.deriveInstrumentationFilterFromTargets?.value ?: false,
    )
  }

  private fun convertTargets(projectView: ProjectView): List<ExcludableValue<Label>> {
    val targetsSection = projectView.targets ?: return emptyList()
    val result = mutableListOf<ExcludableValue<Label>>()
    
    targetsSection.values.forEach { label ->
      result.add(ExcludableValue.included(label))
    }
    targetsSection.excludedValues.forEach { label ->
      result.add(ExcludableValue.excluded(label))
    }
    
    return result
  }

  private fun convertDirectories(projectView: ProjectView): List<ExcludableValue<Path>> {
    val directoriesSection = projectView.directories
    if (directoriesSection == null || (directoriesSection.values.isEmpty() && directoriesSection.excludedValues.isEmpty())) {
      // Default to whole project if no directories specified
      return listOf(ExcludableValue.included(workspaceRoot))
    }
    
    val result = mutableListOf<ExcludableValue<Path>>()
    
    directoriesSection.values.forEach { path ->
      result.add(ExcludableValue.included(path))
    }
    directoriesSection.excludedValues.forEach { path ->
      result.add(ExcludableValue.excluded(path))
    }
    
    return result
  }

  private fun convertFlags(flagsSection: org.jetbrains.bazel.projectview.model.sections.ProjectViewListSection<String>?): List<String> {
    return flagsSection?.values ?: emptyList()
  }
}