package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.commons.ShardingApproach
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.completion.DirectoriesCompletionProvider
import java.nio.file.Path

@ApiStatus.Internal
interface ProjectViewSectionProvider {
  val sections: List<ProjectViewSection<*>>

  companion object {
    val EP_NAME: ExtensionPointName<ProjectViewSectionProvider> = ExtensionPointName("org.jetbrains.bazel.projectViewSectionProvider")
  }
}

internal class DefaultProjectViewSectionProvider : ProjectViewSectionProvider {
  override val sections: List<ProjectViewSection<*>> =
    listOf(
      AllowManualTargetsSyncSection,
      BazelBinarySection,
      BuildFlagsSection,
      DebugFlagsSection,
      DeriveTargetsFromDirectoriesSection,
      DirectoriesSection,
      DotIdeaDirectoryLocationSection,
      EnabledRulesSection,
      GazelleTargetSection,
      ImportDepthSection,
      ImportRunConfigurationsSection,
      IndexAdditionalFilesInDirectoriesSection,
      IndexAllFilesInDirectoriesSection,
      PythonDebugFlagsSection,
      RunConfigRunWithBazelSection,
      ShardSyncSection,
      ShardingApproachSection,
      SyncFlagsSection,
      TargetShardSizeSection,
      TargetsSection,
      TestFlagsSection,
      TestSourcesSection,
    )
}

private val AllowManualTargetsSyncSection: ProjectViewSection<Boolean> = ProjectViewSection(
  key = ALLOW_MANUAL_TARGETS_SYNC_KEY,
  type = ProjectViewSectionType.boolean,
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.allow_manual_targets_sync"),
)

private val BazelBinarySection: ProjectViewSection<Path?> = ProjectViewSection(
  key = BAZEL_BINARY_KEY,
  type = ProjectViewSectionType.path(existing = true),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.bazel_binary"),
)

private val BuildFlagsSection: ProjectViewSection<List<String>> = ProjectViewSection(
  key = BUILD_FLAGS_KEY,
  type = ProjectViewSectionType.flag("build").list(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.build_flags"),
)

private val DebugFlagsSection: ProjectViewSection<List<String>> = ProjectViewSection(
  key = DEBUG_FLAGS_KEY,
  type = ProjectViewSectionType.flag("run", "test").list(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.debug_flags"),
)

private val DeriveTargetsFromDirectoriesSection: ProjectViewSection<Boolean> = ProjectViewSection(
  key = DERIVE_TARGETS_FROM_DIRECTORIES_KEY,
  type = ProjectViewSectionType.boolean,
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.derive_targets_from_directories"),
)

private val DirectoriesSection: ProjectViewSection<List<ExcludableValue<Path>>> = ProjectViewSection(
  key = DIRECTORIES_KEY,
  type = ProjectViewSectionType.path(completionProvider = DirectoriesCompletionProvider()).excludable().list(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.directories"),
)

private val DotIdeaDirectoryLocationSection: ProjectViewSection<Path?> = ProjectViewSection(
  key = DOT_IDEA_DIRECTORY_LOCATION_KEY,
  type = ProjectViewSectionType.path(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.dot_idea_directory_location"),
)

private val EnabledRulesSection: ProjectViewSection<List<String>> = ProjectViewSection(
  key = ENABLED_RULES_KEY,
  type = ProjectViewSectionType.string().list(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.enabled_rules"),
)

private val GazelleTargetSection: ProjectViewSection<Label?> = ProjectViewSection(
  key = GAZELLE_TARGET_KEY,
  type = ProjectViewSectionType.label,
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.gazelle_target"),
)

private val ImportDepthSection: ProjectViewSection<Int> = ProjectViewSection(
  key = IMPORT_DEPTH_KEY,
  type = ProjectViewSectionType.int,
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.import_depth"),
)

private val ImportRunConfigurationsSection: ProjectViewSection<List<Path>> = ProjectViewSection(
  key = IMPORT_RUN_CONFIGURATIONS_KEY,
  type = ProjectViewSectionType.file(extension = ".xml").list(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.import_run_configurations"),
)

private val IndexAdditionalFilesInDirectoriesSection: ProjectViewSection<List<String>> = ProjectViewSection(
  key = INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY,
  type = ProjectViewSectionType.string().list(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.index_additional_files_in_directories"),
)

private val IndexAllFilesInDirectoriesSection: ProjectViewSection<Boolean> = ProjectViewSection(
  key = INDEX_ALL_FILES_IN_DIRECTORIES_KEY,
  type = ProjectViewSectionType.boolean,
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.index_all_files_in_directories"),
)

private val RunConfigRunWithBazelSection: ProjectViewSection<Boolean> = ProjectViewSection(
  key = RUN_CONFIG_RUN_WITH_BAZEL_KEY,
  type = ProjectViewSectionType.boolean,
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.run_config_run_with_bazel"),
)

private val ShardSyncSection: ProjectViewSection<Boolean> = ProjectViewSection(
  key = SHARD_SYNC_KEY,
  type = ProjectViewSectionType.boolean,
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.shard_sync"),
)

private val ShardingApproachSection: ProjectViewSection<ShardingApproach?> = ProjectViewSection(
  key = SHARDING_APPROACH_KEY,
  type = ProjectViewSectionType.enum<ShardingApproach>(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.sharding_approach"),
)

private val SyncFlagsSection: ProjectViewSection<List<String>> = ProjectViewSection(
  key = SYNC_FLAGS_KEY,
  type = ProjectViewSectionType.flag("sync").list(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.sync_flags"),
)

private val TargetShardSizeSection: ProjectViewSection<Int> = ProjectViewSection(
  key = TARGET_SHARD_SIZE_KEY,
  type = ProjectViewSectionType.int,
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.target_shard_size"),
)

private val TargetsSection: ProjectViewSection<List<ExcludableValue<Label>>> = ProjectViewSection(
  key = TARGETS_KEY,
  type = ProjectViewSectionType.label.excludable().list(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.targets"),
)

private val TestFlagsSection: ProjectViewSection<List<String>> = ProjectViewSection(
  key = TEST_FLAGS_KEY,
  type = ProjectViewSectionType.flag("test").list(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.test_flags"),
)

// deprecated sections

private val PythonDebugFlagsSection: ProjectViewSection<List<String>> = ProjectViewSection(
  key = PYTHON_DEBUG_FLAGS_KEY,
  type = ProjectViewSectionType.flag("run", "test").list(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.python_debug_flags"),
  deprecation = ProjectViewSection.Deprecation.withMergeQuickFix(DEBUG_FLAGS_KEY)
)

private val TestSourcesSection: ProjectViewSection<List<String>> = ProjectViewSection(
  key = TEST_SOURCES_KEY,
  type = ProjectViewSectionType.string().list(),
  documentation = BazelProjectViewBundle.message("bazel.language.projectview.docs.test_sources"),
  deprecation = ProjectViewSection.Deprecation(BazelProjectViewBundle.message("annotator.deprecated.section.test_sources")),
)
