package org.jetbrains.bazel.projectview.model

import org.jetbrains.bazel.projectview.model.sections.AndroidMinSdkSection
import org.jetbrains.bazel.projectview.model.sections.DeriveInstrumentationFilterFromTargetsSection
import org.jetbrains.bazel.projectview.model.sections.EnableNativeAndroidRulesSection
import org.jetbrains.bazel.projectview.model.sections.ExperimentalAddTransitiveCompileTimeJarsSection
import org.jetbrains.bazel.projectview.model.sections.ExperimentalNoPruneTransitiveCompileTimeJarsPatternsSection
import org.jetbrains.bazel.projectview.model.sections.ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection
import org.jetbrains.bazel.projectview.model.sections.ExperimentalTransitiveCompileTimeJarsTargetKindsSection
import org.jetbrains.bazel.projectview.model.sections.GazelleTargetSection
import org.jetbrains.bazel.projectview.model.sections.ImportIjarsSection
import org.jetbrains.bazel.projectview.model.sections.ImportRunConfigurationsSection
import org.jetbrains.bazel.projectview.model.sections.IndexAllFilesInDirectoriesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewAllowManualTargetsSyncSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewBazelBinarySection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewBuildFlagsSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDebugFlagsSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDeriveTargetsFromDirectoriesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDirectoriesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewEnabledRulesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewExcludableListSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewIdeJavaHomeOverrideSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewImportDepthSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewListSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewSingletonSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewSyncFlagsSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewTargetsSection
import org.jetbrains.bazel.projectview.model.sections.PythonCodeGeneratorRuleNamesSection
import org.jetbrains.bazel.projectview.model.sections.ShardSyncSection
import org.jetbrains.bazel.projectview.model.sections.ShardingApproachSection
import org.jetbrains.bazel.projectview.model.sections.TargetShardSizeSection
import org.slf4j.LoggerFactory

val supportedSections =
  setOf(
    ProjectViewTargetsSection.SECTION_NAME,
    ProjectViewBazelBinarySection.SECTION_NAME,
    ProjectViewBuildFlagsSection.SECTION_NAME,
    ProjectViewSyncFlagsSection.SECTION_NAME,
    ProjectViewAllowManualTargetsSyncSection.SECTION_NAME,
    ProjectViewDirectoriesSection.SECTION_NAME,
    ProjectViewDeriveTargetsFromDirectoriesSection.SECTION_NAME,
    ProjectViewImportDepthSection.SECTION_NAME,
    ProjectViewEnabledRulesSection.SECTION_NAME,
    ProjectViewIdeJavaHomeOverrideSection.SECTION_NAME,
    ExperimentalAddTransitiveCompileTimeJarsSection.SECTION_NAME,
    ExperimentalTransitiveCompileTimeJarsTargetKindsSection.SECTION_NAME,
    ExperimentalNoPruneTransitiveCompileTimeJarsPatternsSection.SECTION_NAME,
    ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection.SECTION_NAME,
    EnableNativeAndroidRulesSection.SECTION_NAME,
    AndroidMinSdkSection.SECTION_NAME,
    ShardSyncSection.SECTION_NAME,
    TargetShardSizeSection.SECTION_NAME,
    ShardingApproachSection.SECTION_NAME,
    ImportRunConfigurationsSection.SECTION_NAME,
    GazelleTargetSection.SECTION_NAME,
    IndexAllFilesInDirectoriesSection.SECTION_NAME,
    PythonCodeGeneratorRuleNamesSection.SECTION_NAME,
    ImportIjarsSection.SECTION_NAME,
    DeriveInstrumentationFilterFromTargetsSection.SECTION_NAME,
  )

/**
 * Representation of the project view file.
 * IMPORTANT!!! When adding support for a new section, make sure to update the list above.
 * @link https://ij.bazel.build/docs/project-views.html
 */
data class ProjectView(
  /** targets included and excluded from the project  */
  val targets: ProjectViewTargetsSection?,
  /** bazel path used to invoke bazel from the code  */
  val bazelBinary: ProjectViewBazelBinarySection?,
  /** bazel flags added to all bazel command invocations  */
  val buildFlags: ProjectViewBuildFlagsSection?,
  /** bazel flags added to sync invocation */
  val syncFlags: ProjectViewSyncFlagsSection?,
  /** bazel flags added to debug bazel run invocation */
  val debugFlags: ProjectViewDebugFlagsSection?,
  /** flag for building manual targets. */
  val allowManualTargetsSync: ProjectViewAllowManualTargetsSyncSection?,
  /** directories included and excluded from the project  */
  val directories: ProjectViewDirectoriesSection?,
  /** if set to true, relevant project targets will be automatically derived from the `directories` */
  val deriveTargetsFromDirectories: ProjectViewDeriveTargetsFromDirectoriesSection?,
  /** level of depth for importing inherited targets */
  val importDepth: ProjectViewImportDepthSection?,
  /** manually enabled rules to override the automatic rules detection mechanism */
  val enabledRules: ProjectViewEnabledRulesSection?,
  /** local java home path to override to use with IDE, e.g. IntelliJ IDEA */
  val ideJavaHomeOverride: ProjectViewIdeJavaHomeOverrideSection?,
  /** add transitive compile time jars to compensate for possible missing classpaths */
  val addTransitiveCompileTimeJars: ExperimentalAddTransitiveCompileTimeJarsSection? = null,
  /** used alongside with [addTransitiveCompileTimeJars] with the list of custom JVM target kinds */
  val transitiveCompileTimeJarsTargetKinds: ExperimentalTransitiveCompileTimeJarsTargetKindsSection? = null,
  /** used alongside with [addTransitiveCompileTimeJars] with the list of transitive compile time jars patterns to not prune */
  val noPruneTransitiveCompileTimeJarsPatternsSection: ExperimentalNoPruneTransitiveCompileTimeJarsPatternsSection? = null,
  /** libraries get higher precedence in the IDE than modules (JVM-related) */
  val prioritizeLibrariesOverModulesTargetKindsSection: ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection? = null,
  /** enable native (non-starlarkified) Android rules */
  val enableNativeAndroidRules: EnableNativeAndroidRulesSection? = null,
  /** Override the minimum Android SDK version globally for the whole project */
  val androidMinSdkSection: AndroidMinSdkSection? = null,
  /** enable sharded sync */
  val shardSync: ShardSyncSection? = null,
  /** number of targets per build shard */
  val targetShardSize: TargetShardSizeSection? = null,
  /** sharding approach */
  val shardingApproach: ShardingApproachSection? = null,
  /** See https://ij.bazel.build/docs/project-views.html#import_run_configurations */
  val importRunConfigurations: ImportRunConfigurationsSection? = null,
  /** gazelle target */
  val gazelleTarget: GazelleTargetSection? = null,
  /** Whether to all index files inside [ProjectViewDirectoriesSection] or just sources of targets */
  val indexAllFilesInDirectories: IndexAllFilesInDirectoriesSection? = null,
  val pythonCodeGeneratorRuleNamesSection: PythonCodeGeneratorRuleNamesSection? = null,
  val importIjars: ImportIjarsSection? = null,
  val deriveInstrumentationFilterFromTargets: DeriveInstrumentationFilterFromTargetsSection? = null,
) {
  data class Builder(
    private val imports: List<ProjectView> = emptyList(),
    private val targets: ProjectViewTargetsSection? = null,
    private val bazelBinary: ProjectViewBazelBinarySection? = null,
    private val buildFlags: ProjectViewBuildFlagsSection? = null,
    private val syncFlags: ProjectViewSyncFlagsSection? = null,
    private val debugFlags: ProjectViewDebugFlagsSection? = null,
    private val allowManualTargetsSync: ProjectViewAllowManualTargetsSyncSection? = null,
    private val directories: ProjectViewDirectoriesSection? = null,
    private val deriveTargetsFromDirectories: ProjectViewDeriveTargetsFromDirectoriesSection? = null,
    private val importDepth: ProjectViewImportDepthSection? = null,
    private val enabledRules: ProjectViewEnabledRulesSection? = null,
    private val ideJavaHomeOverride: ProjectViewIdeJavaHomeOverrideSection? = null,
    private val addTransitiveCompileTimeJars: ExperimentalAddTransitiveCompileTimeJarsSection? = null,
    private val transitiveCompileTimeJarsTargetKinds: ExperimentalTransitiveCompileTimeJarsTargetKindsSection? = null,
    private val noPruneTransitiveCompileTimeJarsPatterns: ExperimentalNoPruneTransitiveCompileTimeJarsPatternsSection? = null,
    private val prioritizeLibrariesOverModulesTargetKinds: ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection? = null,
    private val enableNativeAndroidRules: EnableNativeAndroidRulesSection? = null,
    private val androidMinSdkSection: AndroidMinSdkSection? = null,
    private val shardSync: ShardSyncSection? = null,
    private val targetShardSize: TargetShardSizeSection? = null,
    private val shardingApproach: ShardingApproachSection? = null,
    private val importRunConfigurations: ImportRunConfigurationsSection? = null,
    private val gazelleTarget: GazelleTargetSection? = null,
    private val indexAllFilesInDirectories: IndexAllFilesInDirectoriesSection? = null,
    private val pythonCodeGeneratorRuleNamesSection: PythonCodeGeneratorRuleNamesSection? = null,
    private val importIjars: ImportIjarsSection? = null,
    private val deriveInstrumentationFilterFromTargets: DeriveInstrumentationFilterFromTargetsSection? = null,
  ) {
    fun build(): ProjectView {
      log.debug("Building project view for: {}", this)

      return buildWithImports(imports)
    }

    private fun buildWithImports(importedProjectViews: List<ProjectView>): ProjectView {
      val targets = combineTargetsSection(importedProjectViews)
      val bazelBinary = combineBazelBinarySection(importedProjectViews)
      val buildFlags = combineBuildFlagsSection(importedProjectViews)
      val syncFlags = combineSyncFlagsSection(importedProjectViews)
      val debugFlags = combineDebugFlagsSection(importedProjectViews)
      val allowManualTargetsSync = combineManualTargetsSection(importedProjectViews)
      val directories = combineDirectoriesSection(importedProjectViews)
      val deriveTargetsFromDirectories = combineDeriveTargetFlagSection(importedProjectViews)
      val importDepth = combineImportDepthSection(importedProjectViews)
      val enabledRules = combineEnabledRulesSection(importedProjectViews)
      val ideJavaHomeOverride = combineIdeJavaHomeOverrideSection(importedProjectViews)
      val addTransitiveCompileTimeJars = combineAddTransitiveCompileTimeJarsSection(importedProjectViews)
      val transitiveCompileTimeJarsTargetKinds = combineTransitiveCompileTimeJarsTargetKindsSection(importedProjectViews)
      val noPruneTransitiveCompileTimeJarsPatterns = combineNoPruneTransitiveCompileTimeJarsPatternsSection(importedProjectViews)
      val prioritizeLibrariesOverModulesTargetKinds = combinePrioritizeLibrariesOverModulesTargetKindsSection(importedProjectViews)
      val enableNativeAndroidRules = combineEnableNativeAndroidRulesSection(importedProjectViews)
      val androidMinSdkSection = combineAndroidMinSdkSection(importedProjectViews)
      val shardSyncSection = combineShardSyncSection(importedProjectViews)
      val targetShardSizeSection = combineTargetShardSizeSection(importedProjectViews)
      val shardingApproachSection = combineShardingApproachSection(importedProjectViews)
      val importRunConfigurationsSection = combineImportRunConfigurationsSection(importedProjectViews)
      val gazelleTarget = combineGazelleTargetSection(importedProjectViews)
      val indexAllFilesInDirectories = combineIndexAllFilesInDirectoriesSection(importedProjectViews)
      val pythonCodeGeneratorRuleNamesSection = combinePythonCodeGeneratorRuleNamesSection(importedProjectViews)
      val importIjars = combineImportIjarsSection(importedProjectViews)
      val deriveInstrumentationFilterFromTargets = combineDeriveInstrumentationFilterFromTargetsSection(importedProjectViews)

      return ProjectView(
        targets,
        bazelBinary,
        buildFlags,
        syncFlags,
        debugFlags,
        allowManualTargetsSync,
        directories,
        deriveTargetsFromDirectories,
        importDepth,
        enabledRules,
        ideJavaHomeOverride,
        addTransitiveCompileTimeJars,
        transitiveCompileTimeJarsTargetKinds,
        noPruneTransitiveCompileTimeJarsPatterns,
        prioritizeLibrariesOverModulesTargetKinds,
        enableNativeAndroidRules,
        androidMinSdkSection,
        shardSyncSection,
        targetShardSizeSection,
        shardingApproachSection,
        importRunConfigurationsSection,
        gazelleTarget,
        indexAllFilesInDirectories,
        pythonCodeGeneratorRuleNamesSection,
        importIjars,
        deriveInstrumentationFilterFromTargets,
      )
    }

    private fun combineAddTransitiveCompileTimeJarsSection(
      importedProjectViews: List<ProjectView>,
    ): ExperimentalAddTransitiveCompileTimeJarsSection? =
      addTransitiveCompileTimeJars ?: getLastImportedSingletonValue(
        importedProjectViews,
        ProjectView::addTransitiveCompileTimeJars,
      )

    private fun combineTransitiveCompileTimeJarsTargetKindsSection(
      importedProjectViews: List<ProjectView>,
    ): ExperimentalTransitiveCompileTimeJarsTargetKindsSection? {
      val targetKinds =
        combineListValuesWithImported(
          importedProjectViews,
          transitiveCompileTimeJarsTargetKinds,
          ProjectView::transitiveCompileTimeJarsTargetKinds,
          ExperimentalTransitiveCompileTimeJarsTargetKindsSection::values,
        )
      return createInstanceOfListSectionOrNull(targetKinds, ::ExperimentalTransitiveCompileTimeJarsTargetKindsSection)
    }

    private fun combineNoPruneTransitiveCompileTimeJarsPatternsSection(
      importedProjectViews: List<ProjectView>,
    ): ExperimentalNoPruneTransitiveCompileTimeJarsPatternsSection? {
      val patterns =
        combineListValuesWithImported(
          importedProjectViews,
          noPruneTransitiveCompileTimeJarsPatterns,
          ProjectView::noPruneTransitiveCompileTimeJarsPatternsSection,
          ExperimentalNoPruneTransitiveCompileTimeJarsPatternsSection::values,
        )
      return createInstanceOfListSectionOrNull(patterns, ::ExperimentalNoPruneTransitiveCompileTimeJarsPatternsSection)
    }

    private fun combinePrioritizeLibrariesOverModulesTargetKindsSection(
      importedProjectViews: List<ProjectView>,
    ): ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection? {
      val targetKinds =
        combineListValuesWithImported(
          importedProjectViews,
          prioritizeLibrariesOverModulesTargetKinds,
          ProjectView::prioritizeLibrariesOverModulesTargetKindsSection,
          ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection::values,
        )
      return createInstanceOfListSectionOrNull(targetKinds, ::ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection)
    }

    private fun combineEnableNativeAndroidRulesSection(importedProjectViews: List<ProjectView>): EnableNativeAndroidRulesSection? =
      enableNativeAndroidRules ?: getLastImportedSingletonValue(
        importedProjectViews,
        ProjectView::enableNativeAndroidRules,
      )

    private fun combineAndroidMinSdkSection(importedProjectViews: List<ProjectView>): AndroidMinSdkSection? =
      androidMinSdkSection ?: getLastImportedSingletonValue(
        importedProjectViews,
        ProjectView::androidMinSdkSection,
      )

    private fun combineShardSyncSection(importedProjectViews: List<ProjectView>): ShardSyncSection? =
      shardSync ?: getLastImportedSingletonValue(
        importedProjectViews,
        ProjectView::shardSync,
      )

    private fun combineTargetShardSizeSection(importedProjectViews: List<ProjectView>): TargetShardSizeSection? =
      targetShardSize ?: getLastImportedSingletonValue(
        importedProjectViews,
        ProjectView::targetShardSize,
      )

    private fun combineGazelleTargetSection(importedProjectViews: List<ProjectView>): GazelleTargetSection? =
      gazelleTarget ?: getLastImportedSingletonValue(
        importedProjectViews,
        ProjectView::gazelleTarget,
      )

    private fun combineShardingApproachSection(importedProjectViews: List<ProjectView>): ShardingApproachSection? =
      shardingApproach ?: getLastImportedSingletonValue(
        importedProjectViews,
        ProjectView::shardingApproach,
      )

    private fun combineImportRunConfigurationsSection(importedProjectViews: List<ProjectView>): ImportRunConfigurationsSection? {
      val importRunConfigurations =
        combineListValuesWithImported(
          importedProjectViews,
          importRunConfigurations,
          ProjectView::importRunConfigurations,
          ImportRunConfigurationsSection::values,
        )
      return createInstanceOfListSectionOrNull(importRunConfigurations, ::ImportRunConfigurationsSection)
    }

    private fun combineIndexAllFilesInDirectoriesSection(importedProjectViews: List<ProjectView>): IndexAllFilesInDirectoriesSection? =
      indexAllFilesInDirectories ?: getLastImportedSingletonValue(
        importedProjectViews,
        ProjectView::indexAllFilesInDirectories,
      )

    private fun combinePythonCodeGeneratorRuleNamesSection(importedProjectViews: List<ProjectView>): PythonCodeGeneratorRuleNamesSection? {
      val importRunConfigurations =
        combineListValuesWithImported(
          importedProjectViews,
          pythonCodeGeneratorRuleNamesSection,
          ProjectView::pythonCodeGeneratorRuleNamesSection,
          PythonCodeGeneratorRuleNamesSection::values,
        )
      return createInstanceOfListSectionOrNull(importRunConfigurations, ::PythonCodeGeneratorRuleNamesSection)
    }

    private fun combineImportIjarsSection(importedProjectViews: List<ProjectView>): ImportIjarsSection? =
      importIjars ?: getLastImportedSingletonValue(
        importedProjectViews,
        ProjectView::importIjars,
      )

    private fun combineDeriveInstrumentationFilterFromTargetsSection(
      importedProjectViews: List<ProjectView>,
    ): DeriveInstrumentationFilterFromTargetsSection? =
      deriveInstrumentationFilterFromTargets ?: getLastImportedSingletonValue(
        importedProjectViews,
        ProjectView::deriveInstrumentationFilterFromTargets,
      )

    private fun combineTargetsSection(importedProjectViews: List<ProjectView>): ProjectViewTargetsSection? {
      val includedTargets =
        combineListValuesWithImported(
          importedProjectViews,
          targets,
          ProjectView::targets,
          ProjectViewTargetsSection::values,
        )
      val excludedTargets =
        combineListValuesWithImported(
          importedProjectViews,
          targets,
          ProjectView::targets,
          ProjectViewTargetsSection::excludedValues,
        )
      return createInstanceOfExcludableListSectionOrNull(
        includedTargets,
        excludedTargets,
        ::ProjectViewTargetsSection,
      )
    }

    private fun combineBuildFlagsSection(importedProjectViews: List<ProjectView>): ProjectViewBuildFlagsSection? {
      val flags =
        combineListValuesWithImported(
          importedProjectViews,
          buildFlags,
          ProjectView::buildFlags,
          ProjectViewBuildFlagsSection::values,
        )

      return createInstanceOfListSectionOrNull(flags, ::ProjectViewBuildFlagsSection)
    }

    private fun combineSyncFlagsSection(importedProjectViews: List<ProjectView>): ProjectViewSyncFlagsSection? {
      val flags =
        combineListValuesWithImported(
          importedProjectViews,
          syncFlags,
          ProjectView::syncFlags,
          ProjectViewSyncFlagsSection::values,
        )

      return createInstanceOfListSectionOrNull(flags, ::ProjectViewSyncFlagsSection)
    }

    private fun combineDebugFlagsSection(importedProjectViews: List<ProjectView>): ProjectViewDebugFlagsSection? {
      val flags =
        combineListValuesWithImported(
          importedProjectViews,
          debugFlags,
          ProjectView::debugFlags,
          ProjectViewDebugFlagsSection::values,
        )

      return createInstanceOfListSectionOrNull(flags, ::ProjectViewDebugFlagsSection)
    }

    private fun combineDirectoriesSection(importedProjectViews: List<ProjectView>): ProjectViewDirectoriesSection? {
      val includedTargets =
        combineListValuesWithImported(
          importedProjectViews,
          directories,
          ProjectView::directories,
          ProjectViewDirectoriesSection::values,
        )
      val excludedTargets =
        combineListValuesWithImported(
          importedProjectViews,
          directories,
          ProjectView::directories,
          ProjectViewDirectoriesSection::excludedValues,
        )
      return createInstanceOfExcludableListSectionOrNull(
        includedTargets,
        excludedTargets,
        ::ProjectViewDirectoriesSection,
      )
    }

    private fun <V, T : ProjectViewListSection<V>> combineListValuesWithImported(
      importedProjectViews: List<ProjectView>,
      section: T?,
      sectionGetter: (ProjectView) -> T?,
      valuesGetter: (T) -> List<V>,
    ): List<V> {
      val sectionValues =
        section
          ?.let(valuesGetter)
          .orEmpty()

      val importedValues =
        importedProjectViews
          .mapNotNull(sectionGetter)
          .flatMap(valuesGetter)

      return importedValues + sectionValues
    }

    private fun <V, T : ProjectViewExcludableListSection<V>?> createInstanceOfExcludableListSectionOrNull(
      includedElements: List<V>,
      excludedElements: List<V>,
      constructor: (List<V>, List<V>) -> T,
    ): T? =
      if (includedElements.isEmpty() && excludedElements.isEmpty()) {
        null
      } else {
        constructor(
          includedElements,
          excludedElements,
        )
      }

    private fun <V, T : ProjectViewListSection<V>?> createInstanceOfListSectionOrNull(values: List<V>, constructor: (List<V>) -> T): T? =
      if (values.isEmpty()) null else constructor(values)

    private fun combineBazelBinarySection(importedProjectViews: List<ProjectView>): ProjectViewBazelBinarySection? =
      bazelBinary ?: getLastImportedSingletonValue(importedProjectViews, ProjectView::bazelBinary)

    private fun combineManualTargetsSection(importedProjectViews: List<ProjectView>): ProjectViewAllowManualTargetsSyncSection? =
      allowManualTargetsSync
        ?: getLastImportedSingletonValue(importedProjectViews, ProjectView::allowManualTargetsSync)

    private fun combineDeriveTargetFlagSection(importedProjectViews: List<ProjectView>): ProjectViewDeriveTargetsFromDirectoriesSection? =
      deriveTargetsFromDirectories ?: getLastImportedSingletonValue(
        importedProjectViews,
        ProjectView::deriveTargetsFromDirectories,
      )

    private fun combineImportDepthSection(importedProjectViews: List<ProjectView>): ProjectViewImportDepthSection? =
      importDepth ?: getLastImportedSingletonValue(importedProjectViews, ProjectView::importDepth)

    private fun combineEnabledRulesSection(importedProjectViews: List<ProjectView>): ProjectViewEnabledRulesSection? {
      val rules =
        combineListValuesWithImported(
          importedProjectViews,
          enabledRules,
          ProjectView::enabledRules,
          ProjectViewEnabledRulesSection::values,
        )
      return createInstanceOfListSectionOrNull(rules, ::ProjectViewEnabledRulesSection)
    }

    private fun combineIdeJavaHomeOverrideSection(importedProjectViews: List<ProjectView>): ProjectViewIdeJavaHomeOverrideSection? =
      ideJavaHomeOverride ?: getLastImportedSingletonValue(importedProjectViews, ProjectView::ideJavaHomeOverride)

    private fun <T : ProjectViewSingletonSection<*>> getLastImportedSingletonValue(
      importedProjectViews: List<ProjectView>,
      sectionGetter: (ProjectView) -> T?,
    ): T? =
      importedProjectViews
        .asReversed()
        .asSequence()
        .mapNotNull(sectionGetter)
        .firstOrNull()
  }

  companion object {
    private val log = LoggerFactory.getLogger(ProjectView::class.java)
  }
}
