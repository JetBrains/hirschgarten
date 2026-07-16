package org.jetbrains.bazel.languages.projectview

import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.commons.ShardingApproach
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

// Extension properties to provide convenient access to ProjectView sections

@Internal
val BUILD_FLAGS_KEY: SectionKey<List<String>> = SectionKey("build_flags", emptyList())
val ProjectView.buildFlags: List<String>
  @Internal
  get() = getSection(BUILD_FLAGS_KEY)

@Internal
val SYNC_FLAGS_KEY: SectionKey<List<String>> = SectionKey("sync_flags", emptyList())
val ProjectView.syncFlags: List<String>
  @Internal
  get() = getSection(SYNC_FLAGS_KEY)

@Internal
val DEBUG_FLAGS_KEY: SectionKey<List<String>> = SectionKey("debug_flags", emptyList())
val ProjectView.debugFlags: List<String>
  @Internal
  get() = getSection(DEBUG_FLAGS_KEY) + if (BazelFeatureFlags.isPythonSupportEnabled) pythonDebugFlags else emptyList()

@Internal
val TEST_FLAGS_KEY: SectionKey<List<String>> = SectionKey("test_flags", emptyList())
val ProjectView.testFlags: List<String>
  @Internal
  get() = getSection(TEST_FLAGS_KEY)

@Internal
val BAZEL_BINARY_KEY: SectionKey<Path?> = SectionKey("bazel_binary", null)
val ProjectView.bazelBinary: Path?
  @Internal
  get() = getSection(BAZEL_BINARY_KEY)

@Internal
val ALLOW_MANUAL_TARGETS_SYNC_KEY: SectionKey<Boolean> = SectionKey("allow_manual_targets_sync", false)
val ProjectView.allowManualTargetsSync: Boolean
  get() = getSection(ALLOW_MANUAL_TARGETS_SYNC_KEY)

@Internal
val DERIVE_TARGETS_FROM_DIRECTORIES_KEY: SectionKey<Boolean> = SectionKey("derive_targets_from_directories", false)
val ProjectView.deriveTargetsFromDirectories: Boolean
  get() = getSection(DERIVE_TARGETS_FROM_DIRECTORIES_KEY)

@Internal
val IMPORT_DEPTH_KEY: SectionKey<Int> = SectionKey("import_depth", -1)
val ProjectView.importDepth: Int
  @Internal
  get() = getSection(IMPORT_DEPTH_KEY)

@Internal
val IDE_JAVA_HOME_OVERRIDE_KEY: SectionKey<Path?> = SectionKey("ide_java_home_override", null)
val ProjectView.ideJavaHomeOverride: Path?
  @Internal
  get() = getSection(IDE_JAVA_HOME_OVERRIDE_KEY)

@Internal
val SHARD_SYNC_KEY: SectionKey<Boolean> = SectionKey("shard_sync", false)
val ProjectView.shardSync: Boolean
  @Internal
  get() = getSection(SHARD_SYNC_KEY)

@Internal
val TARGET_SHARD_SIZE_KEY: SectionKey<Int> = SectionKey("target_shard_size", 1000)
val ProjectView.targetShardSize: Int
  @Internal
  get() = getSection(TARGET_SHARD_SIZE_KEY)

@Internal
val SHARDING_APPROACH_KEY: SectionKey<ShardingApproach?> = SectionKey("sharding_approach", null)
val ProjectView.shardingApproach: String?
  @Internal
  get() = getSection(SHARDING_APPROACH_KEY)?.name?.lowercase()

@Internal
val IMPORT_RUN_CONFIGURATIONS_KEY: SectionKey<List<Path>> = SectionKey("import_run_configurations", emptyList())
val ProjectView.importRunConfigurations: List<Path>
  @Internal
  get() = getSection(IMPORT_RUN_CONFIGURATIONS_KEY)

@Internal
val GAZELLE_TARGET_KEY: SectionKey<Label?> = SectionKey("gazelle_target", null)
val ProjectView.gazelleTarget: Label?
  @Internal
  get() = getSection(GAZELLE_TARGET_KEY)

@Internal
val INDEX_ALL_FILES_IN_DIRECTORIES_KEY: SectionKey<Boolean> = SectionKey("index_all_files_in_directories", false)
val ProjectView.indexAllFilesInDirectories: Boolean
  @Internal
  get() = getSection(INDEX_ALL_FILES_IN_DIRECTORIES_KEY)

@Internal
val PYTHON_DEBUG_FLAGS_KEY: SectionKey<List<String>> = SectionKey("python_debug_flags", emptyList())
val ProjectView.pythonDebugFlags: List<String>
  @Internal
  get() = getSection(PYTHON_DEBUG_FLAGS_KEY)

@Internal
val IMPORT_IJARS_KEY: SectionKey<Boolean> = SectionKey("import_ijars", false)
val ProjectView.importIjars: Boolean
  @Internal
  get() = getSection(IMPORT_IJARS_KEY)

@Internal
val DERIVE_INSTRUMENTATION_FILTER_FROM_TARGETS_KEY: SectionKey<Boolean> = SectionKey("derive_instrumentation_filter_from_targets", true)
val ProjectView.deriveInstrumentationFilterFromTargets: Boolean
  @Internal
  get() = getSection(DERIVE_INSTRUMENTATION_FILTER_FROM_TARGETS_KEY)

@Internal
val INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY: SectionKey<List<String>> = SectionKey("index_additional_files_in_directories", emptyList())
val ProjectView.indexAdditionalFilesInDirectories: List<String>
  @Internal
  get() = getSection(INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY)

@Internal
val USE_JETBRAINS_TEST_RUNNER_KEY: SectionKey<Boolean> = SectionKey("use_jetbrains_test_runner", false)
val ProjectView.useJetBrainsTestRunner: Boolean
  @Internal
  get() = getSection(USE_JETBRAINS_TEST_RUNNER_KEY)

@Internal
val PREFER_CLASS_JARS_OVER_SOURCELESS_JARS_KEY: SectionKey<Boolean> = SectionKey("prefer_class_jars_over_sourceless_jars", true)
val ProjectView.preferClassJarsOverSourcelessJars: Boolean
  @Internal
  get() = getSection(PREFER_CLASS_JARS_OVER_SOURCELESS_JARS_KEY)

@Internal
val RUN_CONFIG_RUN_WITH_BAZEL_KEY: SectionKey<Boolean> = SectionKey("run_config_run_with_bazel", BazelFeatureFlags.runConfigRunWithBazel)
val ProjectView.runConfigRunWithBazel: Boolean
  @Internal
  get() = getSection(RUN_CONFIG_RUN_WITH_BAZEL_KEY)

@Internal
val TEST_SOURCES_KEY: SectionKey<List<String>> = SectionKey("test_sources", emptyList())
val ProjectView.testSources: List<String>
  @Internal
  get() = getSection(TEST_SOURCES_KEY)

@Internal
val DOT_IDEA_DIRECTORY_LOCATION_KEY: SectionKey<Path?> = SectionKey("dot_idea_directory_location", null)
val ProjectView.dotIdeaDirectoryLocation: Path?
  @Internal
  get() = getSection(DOT_IDEA_DIRECTORY_LOCATION_KEY)
