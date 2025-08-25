package org.jetbrains.bazel.projectview.parser.sections

import org.jetbrains.bazel.projectview.model.sections.AndroidMinSdkSection
import org.jetbrains.bazel.projectview.model.sections.DeriveInstrumentationFilterFromTargetsSection
import org.jetbrains.bazel.projectview.model.sections.EnableNativeAndroidRulesSection
import org.jetbrains.bazel.projectview.model.sections.GazelleTargetSection
import org.jetbrains.bazel.projectview.model.sections.ImportIjarsSection
import org.jetbrains.bazel.projectview.model.sections.IndexAllFilesInDirectoriesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewAllowManualTargetsSyncSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewBazelBinarySection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDeriveTargetsFromDirectoriesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewIdeJavaHomeOverrideSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewImportDepthSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewSingletonSection
import org.jetbrains.bazel.projectview.model.sections.ShardSyncSection
import org.jetbrains.bazel.projectview.model.sections.ShardingApproachSection
import org.jetbrains.bazel.projectview.model.sections.TargetShardSizeSection
import org.jetbrains.bazel.projectview.parser.splitter.ProjectViewRawSections
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Implementation of single value section parser.
 *
 *
 * It takes a raw section and trims the content.
 *
 * @param <T> type of parsed single value section
</T> */
abstract class ProjectViewSingletonSectionParser<V, T : ProjectViewSingletonSection<V>> protected constructor(
  override val sectionName: String,
) : ProjectViewSectionParser<T>() {
  override fun parse(rawSections: ProjectViewRawSections): T? =
    rawSections
      .getLastSectionWithName(sectionName)
      ?.let { parse(it) }
      .also { log.debug("Parsed '{}' section. Result:\n{}", sectionName, it) }

  override fun parse(sectionBody: String): T? =
    sectionBody
      .trim()
      .ifBlank { null }
      ?.let { mapRawValue(it) }
      ?.let { createInstance(it) }

  protected abstract fun mapRawValue(rawValue: String): V

  protected abstract fun createInstance(value: V): T

  companion object {
    private val log = LoggerFactory.getLogger(ProjectViewSingletonSectionParser::class.java)
  }
}

object ProjectViewBazelBinarySectionParser :
  ProjectViewSingletonSectionParser<Path, ProjectViewBazelBinarySection>(ProjectViewBazelBinarySection.SECTION_NAME) {
  override fun mapRawValue(rawValue: String): Path = Path(rawValue)

  override fun createInstance(value: Path): ProjectViewBazelBinarySection = ProjectViewBazelBinarySection(value)
}

object ProjectViewDeriveTargetsFromDirectoriesSectionParser :
  ProjectViewSingletonSectionParser<Boolean, ProjectViewDeriveTargetsFromDirectoriesSection>(
    ProjectViewDeriveTargetsFromDirectoriesSection.SECTION_NAME,
  ) {
  override fun mapRawValue(rawValue: String): Boolean = rawValue.toBoolean()

  override fun createInstance(value: Boolean): ProjectViewDeriveTargetsFromDirectoriesSection =
    ProjectViewDeriveTargetsFromDirectoriesSection(value)
}

object ProjectViewAllowManualTargetsSyncSectionParser :
  ProjectViewSingletonSectionParser<Boolean, ProjectViewAllowManualTargetsSyncSection>(
    ProjectViewAllowManualTargetsSyncSection.SECTION_NAME,
  ) {
  override fun mapRawValue(rawValue: String): Boolean = rawValue.toBoolean()

  override fun createInstance(value: Boolean): ProjectViewAllowManualTargetsSyncSection = ProjectViewAllowManualTargetsSyncSection(value)
}

object ProjectViewImportDepthSectionParser :
  ProjectViewSingletonSectionParser<Int, ProjectViewImportDepthSection>(ProjectViewImportDepthSection.SECTION_NAME) {
  override fun mapRawValue(rawValue: String): Int = rawValue.toInt()

  override fun createInstance(value: Int): ProjectViewImportDepthSection = ProjectViewImportDepthSection(value)
}

object ProjectViewIdeJavaHomeOverrideSectionParser :
  ProjectViewSingletonSectionParser<Path, ProjectViewIdeJavaHomeOverrideSection>(ProjectViewIdeJavaHomeOverrideSection.SECTION_NAME) {
  override fun mapRawValue(rawValue: String): Path = Path(rawValue)

  override fun createInstance(value: Path): ProjectViewIdeJavaHomeOverrideSection = ProjectViewIdeJavaHomeOverrideSection(value)
}

object EnableNativeAndroidRulesParser :
  ProjectViewSingletonSectionParser<Boolean, EnableNativeAndroidRulesSection>(
    EnableNativeAndroidRulesSection.SECTION_NAME,
  ) {
  override fun mapRawValue(rawValue: String): Boolean = rawValue.toBoolean()

  override fun createInstance(value: Boolean): EnableNativeAndroidRulesSection = EnableNativeAndroidRulesSection(value)
}

object AndroidMinSdkSectionParser :
  ProjectViewSingletonSectionParser<Int, AndroidMinSdkSection>(
    AndroidMinSdkSection.SECTION_NAME,
  ) {
  override fun mapRawValue(rawValue: String): Int = rawValue.toInt()

  override fun createInstance(value: Int): AndroidMinSdkSection = AndroidMinSdkSection(value)
}

object ShardSyncParser : ProjectViewSingletonSectionParser<Boolean, ShardSyncSection>(
  ShardSyncSection.SECTION_NAME,
) {
  override fun mapRawValue(rawValue: String): Boolean = rawValue.toBoolean()

  override fun createInstance(value: Boolean): ShardSyncSection = ShardSyncSection(value)
}

object TargetShardSizeParser : ProjectViewSingletonSectionParser<Int, TargetShardSizeSection>(
  TargetShardSizeSection.SECTION_NAME,
) {
  override fun mapRawValue(rawValue: String): Int = rawValue.toInt()

  override fun createInstance(value: Int): TargetShardSizeSection = TargetShardSizeSection(value)
}

object ShardingApproachParser : ProjectViewSingletonSectionParser<String, ShardingApproachSection>(
  ShardingApproachSection.SECTION_NAME,
) {
  override fun mapRawValue(rawValue: String): String = rawValue

  override fun createInstance(value: String): ShardingApproachSection = ShardingApproachSection(value)
}

object GazelleTargetParser : ProjectViewSingletonSectionParser<String, GazelleTargetSection>(
  GazelleTargetSection.SECTION_NAME,
) {
  override fun mapRawValue(rawValue: String): String = rawValue

  override fun createInstance(value: String): GazelleTargetSection = GazelleTargetSection(value)
}

object IndexAllFilesInDirectoriesSectionParser : ProjectViewSingletonSectionParser<Boolean, IndexAllFilesInDirectoriesSection>(
  IndexAllFilesInDirectoriesSection.SECTION_NAME,
) {
  override fun mapRawValue(rawValue: String): Boolean = rawValue.toBoolean()

  override fun createInstance(value: Boolean): IndexAllFilesInDirectoriesSection = IndexAllFilesInDirectoriesSection(value)
}

object ImportIjarsSectionParser : ProjectViewSingletonSectionParser<Boolean, ImportIjarsSection>(
  ImportIjarsSection.SECTION_NAME,
) {
  override fun mapRawValue(rawValue: String): Boolean = rawValue.toBoolean()

  override fun createInstance(value: Boolean): ImportIjarsSection = ImportIjarsSection(value)
}

object DeriveInstrumentationFilterFromTargetsSectionParser :
  ProjectViewSingletonSectionParser<Boolean, DeriveInstrumentationFilterFromTargetsSection>(
    DeriveInstrumentationFilterFromTargetsSection.SECTION_NAME,
  ) {
  override fun mapRawValue(rawValue: String): Boolean = rawValue.toBoolean()

  override fun createInstance(value: Boolean): DeriveInstrumentationFilterFromTargetsSection =
    DeriveInstrumentationFilterFromTargetsSection(value)
}
