package org.jetbrains.bazel.projectview.parser.sections

import org.jetbrains.bazel.projectview.model.sections.ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection
import org.jetbrains.bazel.projectview.model.sections.ImportRunConfigurationsSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewBuildFlagsSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDebugFlagsSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewEnabledRulesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewListSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewSyncFlagsSection
import org.jetbrains.bazel.projectview.model.sections.PythonCodeGeneratorRuleNamesSection
import org.jetbrains.bazel.projectview.parser.splitter.ProjectViewRawSections
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

/**
 * Implementation of list section parser.
 *
 *
 * It takes a raw section and search for entries - they are split by whitespaces, no entry is
 * excluded even if it starts with '-'.
 *
 * @param <T> type of parsed list section
</T> */
abstract class ProjectViewListSectionParser<V, T : ProjectViewListSection<V>> protected constructor(override val sectionName: String) :
  ProjectViewSectionParser<T>() {
    override fun parse(rawSections: ProjectViewRawSections): T? =
      try {
        parseAllSectionsAndMerge(rawSections)?.also { log.debug("Parsed '{}' section. Result:\n{}", sectionName, it) }
      } catch (e: Exception) {
        log.error("Failed to parse '$sectionName' section.", e)
        null
      }

    private fun parseAllSectionsAndMerge(rawSections: ProjectViewRawSections): T? =
      rawSections
        .getAllWithName(sectionName)
        .mapNotNull { parse(it) }
        .reduceOrNull(::concatSectionsItems)

    protected open fun concatSectionsItems(section1: T, section2: T): T = createInstance(section1.values + section2.values)

    override fun parse(sectionBody: String): T? {
      val allEntries = splitSectionEntries(sectionBody)

      return parse(allEntries)
    }

    private fun splitSectionEntries(sectionBody: String): List<String> = WHITESPACE_CHAR_REGEX.split(sectionBody).filter { it.isNotBlank() }

    protected open fun parse(allEntries: List<String>): T? {
      val values = allEntries.map { mapRawValues(it) }
      return createInstanceOrNull(values)
    }

    protected abstract fun mapRawValues(rawValue: String): V

    private fun createInstanceOrNull(values: List<V>): T? = values.ifEmpty { null }?.let { createInstance(values) }

    protected abstract fun createInstance(values: List<V>): T

    companion object {
      private val log = LoggerFactory.getLogger(ProjectViewListSectionParser::class.java)
      private val WHITESPACE_CHAR_REGEX = Pattern.compile("[ \n\t]+")
    }
  }

object ProjectViewBuildFlagsSectionParser :
  ProjectViewListSectionParser<String, ProjectViewBuildFlagsSection>(ProjectViewBuildFlagsSection.SECTION_NAME) {
  override fun mapRawValues(rawValue: String): String = rawValue

  override fun createInstance(values: List<String>): ProjectViewBuildFlagsSection = ProjectViewBuildFlagsSection(values)
}

object ProjectViewSyncFlagsSectionParser :
  ProjectViewListSectionParser<String, ProjectViewSyncFlagsSection>(ProjectViewSyncFlagsSection.SECTION_NAME) {
  override fun mapRawValues(rawValue: String): String = rawValue

  override fun createInstance(values: List<String>): ProjectViewSyncFlagsSection = ProjectViewSyncFlagsSection(values)
}

object ProjectViewDebugFlagsSectionParser :
  ProjectViewListSectionParser<String, ProjectViewDebugFlagsSection>(ProjectViewDebugFlagsSection.SECTION_NAME) {
  override fun mapRawValues(rawValue: String): String = rawValue

  override fun createInstance(values: List<String>): ProjectViewDebugFlagsSection = ProjectViewDebugFlagsSection(values)
}

object ProjectViewEnabledRulesSectionParser :
  ProjectViewListSectionParser<String, ProjectViewEnabledRulesSection>(ProjectViewEnabledRulesSection.SECTION_NAME) {
  override fun mapRawValues(rawValue: String): String = rawValue

  override fun createInstance(values: List<String>): ProjectViewEnabledRulesSection = ProjectViewEnabledRulesSection(values)
}

object ExperimentalPrioritizeLibrariesOverModulesTargetKindsSectionParser :
  ProjectViewListSectionParser<String, ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection>(
    ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection.SECTION_NAME,
  ) {
  override fun mapRawValues(rawValue: String): String = rawValue

  override fun createInstance(values: List<String>): ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection =
    ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection(values)
}

object ImportRunConfigurationsSectionParser :
  ProjectViewListSectionParser<String, ImportRunConfigurationsSection>(
    ImportRunConfigurationsSection.SECTION_NAME,
  ) {
  override fun mapRawValues(rawValue: String): String = rawValue

  override fun createInstance(values: List<String>): ImportRunConfigurationsSection = ImportRunConfigurationsSection(values)
}

object PythonCodeGeneratorRuleNamesSectionParser :
  ProjectViewListSectionParser<String, PythonCodeGeneratorRuleNamesSection>(
    PythonCodeGeneratorRuleNamesSection.SECTION_NAME,
  ) {
  override fun mapRawValues(rawValue: String): String = rawValue

  override fun createInstance(values: List<String>): PythonCodeGeneratorRuleNamesSection = PythonCodeGeneratorRuleNamesSection(values)
}
