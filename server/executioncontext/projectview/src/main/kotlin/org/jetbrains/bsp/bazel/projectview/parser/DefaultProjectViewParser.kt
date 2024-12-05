package org.jetbrains.bsp.bazel.projectview.parser

import org.apache.logging.log4j.LogManager
import org.jetbrains.bsp.bazel.commons.escapeNewLines
import org.jetbrains.bsp.bazel.projectview.model.ProjectView
import org.jetbrains.bsp.bazel.projectview.parser.sections.AndroidMinSdkSectionParser
import org.jetbrains.bsp.bazel.projectview.parser.sections.EnableNativeAndroidRulesParser
import org.jetbrains.bsp.bazel.projectview.parser.sections.ExperimentalAddTransitiveCompileTimeJarsParser
import org.jetbrains.bsp.bazel.projectview.parser.sections.ProjectViewAllowManualTargetsSyncSectionParser
import org.jetbrains.bsp.bazel.projectview.parser.sections.ProjectViewBazelBinarySectionParser
import org.jetbrains.bsp.bazel.projectview.parser.sections.ProjectViewBuildFlagsSectionParser
import org.jetbrains.bsp.bazel.projectview.parser.sections.ProjectViewDeriveTargetsFromDirectoriesSectionParser
import org.jetbrains.bsp.bazel.projectview.parser.sections.ProjectViewDirectoriesSectionParser
import org.jetbrains.bsp.bazel.projectview.parser.sections.ProjectViewEnabledRulesSectionParser
import org.jetbrains.bsp.bazel.projectview.parser.sections.ProjectViewIdeJavaHomeOverrideSectionParser
import org.jetbrains.bsp.bazel.projectview.parser.sections.ProjectViewImportDepthSectionParser
import org.jetbrains.bsp.bazel.projectview.parser.sections.ProjectViewTargetsSectionParser
import org.jetbrains.bsp.bazel.projectview.parser.splitter.ProjectViewRawSections
import org.jetbrains.bsp.bazel.projectview.parser.splitter.ProjectViewSectionSplitter
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Default implementation of ProjectViewParser.
 *
 * @see ProjectViewParser
 * @see ProjectViewSectionSplitter
 */
open class DefaultProjectViewParser(private val workspaceRoot: Path? = null) : ProjectViewParser {
  private val log = LogManager.getLogger(DefaultProjectViewParser::class.java)

  override fun parse(projectViewFileContent: String): ProjectView {
    log.trace("Parsing project view for the content: '{}'", projectViewFileContent.escapeNewLines())

    val rawSections = ProjectViewSectionSplitter.getRawSectionsForFileContent(projectViewFileContent)

    return ProjectView
      .Builder(
        imports = findImportedProjectViews(rawSections) + findTryImportedProjectViews(rawSections),
        targets = ProjectViewTargetsSectionParser.parse(rawSections),
        bazelBinary = ProjectViewBazelBinarySectionParser.parse(rawSections),
        buildFlags = ProjectViewBuildFlagsSectionParser.parse(rawSections),
        allowManualTargetsSync = ProjectViewAllowManualTargetsSyncSectionParser.parse(rawSections),
        directories = ProjectViewDirectoriesSectionParser.parse(rawSections),
        deriveTargetsFromDirectories = ProjectViewDeriveTargetsFromDirectoriesSectionParser.parse(rawSections),
        importDepth = ProjectViewImportDepthSectionParser.parse(rawSections),
        enabledRules = ProjectViewEnabledRulesSectionParser.parse(rawSections),
        ideJavaHomeOverride = ProjectViewIdeJavaHomeOverrideSectionParser.parse(rawSections),
        addTransitiveCompileTimeJars = ExperimentalAddTransitiveCompileTimeJarsParser.parse(rawSections),
        enableNativeAndroidRules = EnableNativeAndroidRulesParser.parse(rawSections),
        androidMinSdkSection = AndroidMinSdkSectionParser.parse(rawSections),
      ).build()
  }

  private fun findImportedProjectViews(rawSections: ProjectViewRawSections): List<ProjectView> =
    rawSections
      .getAllWithName(IMPORT_STATEMENT)
      .asSequence()
      .map { it.sectionBody }
      .map(String::trim)
      .map(::toProjectViewPath)
      .onEach { log.debug("Parsing imported file {}.", it) }
      .map(this::parse)
      .toList()

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
