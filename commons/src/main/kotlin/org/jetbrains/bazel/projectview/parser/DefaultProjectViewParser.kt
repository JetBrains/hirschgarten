package org.jetbrains.bazel.projectview.parser

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.parser.sections.AndroidMinSdkSectionParser
import org.jetbrains.bazel.projectview.parser.sections.EnableNativeAndroidRulesParser
import org.jetbrains.bazel.projectview.parser.sections.ExperimentalAddTransitiveCompileTimeJarsParser
import org.jetbrains.bazel.projectview.parser.sections.ExperimentalNoPruneTransitiveCompileTimeJarsPatternsSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ExperimentalPrioritizeLibrariesOverModulesTargetKindsSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ExperimentalTransitiveCompileTimeJarsTargetKindsSectionParser
import org.jetbrains.bazel.projectview.parser.sections.GazelleTargetParser
import org.jetbrains.bazel.projectview.parser.sections.ImportIjarsSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ImportRunConfigurationsSectionParser
import org.jetbrains.bazel.projectview.parser.sections.IndexAllFilesInDirectoriesSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ProjectViewAllowManualTargetsSyncSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ProjectViewBazelBinarySectionParser
import org.jetbrains.bazel.projectview.parser.sections.ProjectViewBuildFlagsSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ProjectViewDebugFlagsSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ProjectViewDeriveTargetsFromDirectoriesSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ProjectViewDirectoriesSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ProjectViewEnabledRulesSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ProjectViewIdeJavaHomeOverrideSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ProjectViewImportDepthSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ProjectViewSyncFlagsSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ProjectViewTargetsSectionParser
import org.jetbrains.bazel.projectview.parser.sections.PythonCodeGeneratorRuleNamesSectionParser
import org.jetbrains.bazel.projectview.parser.sections.ShardSyncParser
import org.jetbrains.bazel.projectview.parser.sections.ShardingApproachParser
import org.jetbrains.bazel.projectview.parser.sections.TargetShardSizeParser
import org.jetbrains.bazel.projectview.parser.splitter.ProjectViewRawSections
import org.jetbrains.bazel.projectview.parser.splitter.ProjectViewSectionSplitter
import org.slf4j.LoggerFactory
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Default implementation of ProjectViewParser.
 *
 * @see ProjectViewParser
 * @see ProjectViewSectionSplitter
 */
open class DefaultProjectViewParser(
  private val workspaceRoot: Path? = null,
  private val rawSectionsProvided: ProjectViewRawSections? = null,
) : ProjectViewParser {
  private val log = LoggerFactory.getLogger(DefaultProjectViewParser::class.java)

  override fun parse(projectViewFileContent: String): ProjectView {
    log.trace("Parsing project view for the content: '{}'", projectViewFileContent.escapeNewLines())

    val rawSections = rawSectionsProvided ?: ProjectViewSectionSplitter.getRawSectionsForFileContent(projectViewFileContent)

    return ProjectView
      .Builder(
        imports = findImportedProjectViews(rawSections) + findTryImportedProjectViews(rawSections),
        targets = ProjectViewTargetsSectionParser.parse(rawSections),
        bazelBinary = ProjectViewBazelBinarySectionParser.parse(rawSections),
        buildFlags = ProjectViewBuildFlagsSectionParser.parse(rawSections),
        syncFlags = ProjectViewSyncFlagsSectionParser.parse(rawSections),
        debugFlags = ProjectViewDebugFlagsSectionParser.parse(rawSections),
        allowManualTargetsSync = ProjectViewAllowManualTargetsSyncSectionParser.parse(rawSections),
        directories = ProjectViewDirectoriesSectionParser.parse(rawSections),
        deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSectionParser.parse(rawSections),
        importDepth = ProjectViewImportDepthSectionParser.parse(rawSections),
        enabledRules = ProjectViewEnabledRulesSectionParser.parse(rawSections),
        ideJavaHomeOverride = ProjectViewIdeJavaHomeOverrideSectionParser.parse(rawSections),
        addTransitiveCompileTimeJars = ExperimentalAddTransitiveCompileTimeJarsParser.parse(rawSections),
        transitiveCompileTimeJarsTargetKinds = ExperimentalTransitiveCompileTimeJarsTargetKindsSectionParser.parse(rawSections),
        noPruneTransitiveCompileTimeJarsPatterns = ExperimentalNoPruneTransitiveCompileTimeJarsPatternsSectionParser.parse(rawSections),
        prioritizeLibrariesOverModulesTargetKinds = ExperimentalPrioritizeLibrariesOverModulesTargetKindsSectionParser.parse(rawSections),
        enableNativeAndroidRules = EnableNativeAndroidRulesParser.parse(rawSections),
        androidMinSdkSection = AndroidMinSdkSectionParser.parse(rawSections),
        shardSync = ShardSyncParser.parse(rawSections),
        targetShardSize = TargetShardSizeParser.parse(rawSections),
        shardingApproach = ShardingApproachParser.parse(rawSections),
        importRunConfigurations = ImportRunConfigurationsSectionParser.parse(rawSections),
        gazelleTarget = GazelleTargetParser.parse(rawSections),
        indexAllFilesInDirectories = IndexAllFilesInDirectoriesSectionParser.parse(rawSections),
        pythonCodeGeneratorRuleNamesSection = PythonCodeGeneratorRuleNamesSectionParser.parse(rawSections),
        importIjars = ImportIjarsSectionParser.parse(rawSections),
      ).build()
  }

  private fun String.escapeNewLines(): String = this.replace("\n", "\\n")

  private fun findImportedProjectViews(rawSections: ProjectViewRawSections): List<ProjectView> =
    rawSections
      .getAllWithName(IMPORT_STATEMENT)
      .asSequence()
      .map { it.sectionBody }
      .map(String::trim)
      .map(::toProjectViewPath)
      .onEach { log.debug("Parsing imported file {}.", it) }
      .map {
        try {
          parse(it)
        } catch (_: NoSuchFileException) {
          throw ProjectViewParser.ImportNotFound(it)
        }
      }.toList()

  private fun findTryImportedProjectViews(rawSections: ProjectViewRawSections): List<ProjectView> =
    rawSections
      .getAllWithName(TRY_IMPORT_STATEMENT)
      .asSequence()
      .map { it.sectionBody }
      .map(String::trim)
      .map(::toProjectViewPath)
      .onEach { log.debug("Parsing try_imported file {}.", it) }
      .mapNotNull(this::tryParse)
      .toList()

  private fun toProjectViewPath(projectViewPathStr: String): Path {
    val currentPath = Path(projectViewPathStr)
    return when {
      currentPath.isAbsolute -> currentPath
      workspaceRoot != null -> workspaceRoot.resolve(currentPath)
      else -> currentPath
    }
  }

  companion object {
    private const val IMPORT_STATEMENT = "import"
    private const val TRY_IMPORT_STATEMENT = "try_import"
  }
}
