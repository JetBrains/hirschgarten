package org.jetbrains.bazel.projectview.parser

import org.jetbrains.bazel.languages.projectview.language.ALLOW_MANUAL_TARGETS_SYNC
import org.jetbrains.bazel.languages.projectview.language.ALLOW_MANUAL_TARGETS_SYNC_TYPE
import org.jetbrains.bazel.languages.projectview.language.ANDROID_MIN_SDK
import org.jetbrains.bazel.languages.projectview.language.ANDROID_MIN_SDK_TYPE
import org.jetbrains.bazel.languages.projectview.language.BAZEL_BINARY
import org.jetbrains.bazel.languages.projectview.language.BAZEL_BINARY_TYPE
import org.jetbrains.bazel.languages.projectview.language.BUILD_FLAGS
import org.jetbrains.bazel.languages.projectview.language.BUILD_FLAGS_TYPE
import org.jetbrains.bazel.languages.projectview.language.DEBUG_FLAGS
import org.jetbrains.bazel.languages.projectview.language.DEBUG_FLAGS_TYPE
import org.jetbrains.bazel.languages.projectview.language.DERIVE_TARGETS_FROM_DIRECTORIES
import org.jetbrains.bazel.languages.projectview.language.DERIVE_TARGETS_FROM_DIRECTORIES_TYPE
import org.jetbrains.bazel.languages.projectview.language.DIRECTORIES
import org.jetbrains.bazel.languages.projectview.language.DIRECTORIES_INNER_TYPE
import org.jetbrains.bazel.languages.projectview.language.DIRECTORIES_TYPE
import org.jetbrains.bazel.languages.projectview.language.ENABLED_RULES
import org.jetbrains.bazel.languages.projectview.language.ENABLED_RULES_TYPE
import org.jetbrains.bazel.languages.projectview.language.ENABLE_NATIVE_ANDROID_RULES
import org.jetbrains.bazel.languages.projectview.language.ENABLE_NATIVE_ANDROID_RULES_TYPE
import org.jetbrains.bazel.languages.projectview.language.EXPERIMENTAL_ADD_TRANSITIVE_COMPILE_TIME_JARS
import org.jetbrains.bazel.languages.projectview.language.EXPERIMENTAL_ADD_TRANSITIVE_COMPILE_TIME_JARS_TYPE
import org.jetbrains.bazel.languages.projectview.language.EXPERIMENTAL_NO_PRUNE_TRANSITIVE_COMPILE_TIME_JARS_PATTERNS
import org.jetbrains.bazel.languages.projectview.language.EXPERIMENTAL_NO_PRUNE_TRANSITIVE_COMPILE_TIME_JARS_PATTERNS_TYPE
import org.jetbrains.bazel.languages.projectview.language.EXPERIMENTAL_PRIORITIZE_LIBRARIES_OVER_MODULES_TARGET_KINDS
import org.jetbrains.bazel.languages.projectview.language.EXPERIMENTAL_PRIORITIZE_LIBRARIES_OVER_MODULES_TARGET_KINDS_TYPE
import org.jetbrains.bazel.languages.projectview.language.EXPERIMENTAL_TRANSITIVE_COMPILE_TIME_JARS_TARGET_KINDS
import org.jetbrains.bazel.languages.projectview.language.EXPERIMENTAL_TRANSITIVE_COMPILE_TIME_JARS_TARGET_KINDS_TYPE
import org.jetbrains.bazel.languages.projectview.language.GAZELLE_TARGET
import org.jetbrains.bazel.languages.projectview.language.GAZELLE_TARGET_TYPE
import org.jetbrains.bazel.languages.projectview.language.IDE_JAVA_HOME_OVERRIDE
import org.jetbrains.bazel.languages.projectview.language.IDE_JAVA_HOME_OVERRIDE_TYPE
import org.jetbrains.bazel.languages.projectview.language.IMPORT_DEPTH
import org.jetbrains.bazel.languages.projectview.language.IMPORT_DEPTH_TYPE
import org.jetbrains.bazel.languages.projectview.language.IMPORT_IJARS
import org.jetbrains.bazel.languages.projectview.language.IMPORT_IJARS_TYPE
import org.jetbrains.bazel.languages.projectview.language.IMPORT_RUN_CONFIGURATIONS
import org.jetbrains.bazel.languages.projectview.language.IMPORT_RUN_CONFIGURATIONS_TYPE
import org.jetbrains.bazel.languages.projectview.language.INDEX_ALL_FILES_IN_DIRECTORIES
import org.jetbrains.bazel.languages.projectview.language.INDEX_ALL_FILES_IN_DIRECTORIES_TYPE
import org.jetbrains.bazel.languages.projectview.language.PYTHON_CODE_GENERATOR_RULE_NAMES
import org.jetbrains.bazel.languages.projectview.language.PYTHON_CODE_GENERATOR_RULE_NAMES_TYPE
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.language.SHARD_APPROACH
import org.jetbrains.bazel.languages.projectview.language.SHARD_APPROACH_TYPE
import org.jetbrains.bazel.languages.projectview.language.SHARD_SYNC
import org.jetbrains.bazel.languages.projectview.language.SHARD_SYNC_TYPE
import org.jetbrains.bazel.languages.projectview.language.SYNC_FLAGS
import org.jetbrains.bazel.languages.projectview.language.SYNC_FLAGS_TYPE
import org.jetbrains.bazel.languages.projectview.language.TARGETS
import org.jetbrains.bazel.languages.projectview.language.TARGETS_INNER_TYPE
import org.jetbrains.bazel.languages.projectview.language.TARGETS_TYPE
import org.jetbrains.bazel.languages.projectview.language.TARGET_SHARD_SIZE
import org.jetbrains.bazel.languages.projectview.language.TARGET_SHARD_SIZE_TYPE
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.AndroidMinSdkSection
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
import org.jetbrains.bazel.projectview.model.sections.ProjectViewIdeJavaHomeOverrideSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewImportDepthSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewSyncFlagsSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewTargetsSection
import org.jetbrains.bazel.projectview.model.sections.PythonCodeGeneratorRuleNamesSection
import org.jetbrains.bazel.projectview.model.sections.ShardSyncSection
import org.jetbrains.bazel.projectview.model.sections.ShardingApproachSection
import org.jetbrains.bazel.projectview.model.sections.TargetShardSizeSection
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
open class DefaultProjectViewParser(private val workspaceRoot: Path? = null) : ProjectViewParser {
  private val log = LoggerFactory.getLogger(DefaultProjectViewParser::class.java)

  override fun parse(projectViewFileContent: String): ProjectView {
    log.trace("Parsing project view for the content: '{}'", projectViewFileContent.escapeNewLines())

    val rawSections = ProjectViewSectionSplitter.getRawSectionsForFileContent(projectViewFileContent)
    val imports = findImportedProjectViews(rawSections) + findTryImportedProjectViews(rawSections)
    return PsiProjectViewParser(rawSections, imports).buildProjectView()
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

class PsiProjectViewParser(private val rawProjectViewSections: ProjectViewRawSections, private val imports: List<ProjectView>) {
  // TODO silent failing is a bad idea here.
  // private fun getSectionsFromPsiFile(): Map<String, List<String>> {
  //  val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(projectViewPath!!) ?: error("cannot find file $projectViewPath")
  //  val psiFile = virtualFile.findPsiFile(project!!) as? ProjectViewPsiFile
  //  val sections =
  //    psiFile
  //      ?.getSections()
  //      ?.map {
  //        val name = it.getKeyword()?.text ?: return@map null
  //        val items = it.getItems().map { item -> item.text }
  //        Pair(name, items)
  //      }?.filterNotNull()
  //  if (sections.isNullOrEmpty()) return emptyMap()
  //  return sections.toMap()
  // }toMap

  fun buildProjectView(): ProjectView =
    ProjectView
      .Builder(
        imports = imports,
        allowManualTargetsSync = extractAllowManualTargetsSync(),
        targets = extractTargets(),
        bazelBinary = extractBazelBinary(),
        buildFlags = extractBuildFlags(),
        syncFlags = extractSyncFlags(),
        debugFlags = extractDebugFlags(),
        directories = extractDirectories(),
        deriveTargetsFromDirectories = extractDeriveTargetsFromDirectories(),
        importDepth = extractImportDepth(),
        enabledRules = extractEnabledRules(),
        ideJavaHomeOverride = extractIdeJavaHomeOverride(),
        addTransitiveCompileTimeJars = extractAddTransitiveCompileTimeJars(),
        transitiveCompileTimeJarsTargetKinds = extractTransitiveCompileTimeJarsTargetKinds(),
        noPruneTransitiveCompileTimeJarsPatterns = extractNoPruneTransitiveCompileTimeJarsPatterns(),
        prioritizeLibrariesOverModulesTargetKinds = extractPrioritizeLibrariesOverModulesTargetKinds(),
        enableNativeAndroidRules = extractEnableNativeAndroidRules(),
        androidMinSdkSection = extractAndroidMinSdkSection(),
        shardSync = extractShardSync(),
        targetShardSize = extractTargetShardSize(),
        shardingApproach = extractShardingApproach(),
        importRunConfigurations = extractImportRunConfigurations(),
        gazelleTarget = extractGazelleTarget(),
        indexAllFilesInDirectories = extractIndexAllFilesInDirectories(),
        pythonCodeGeneratorRuleNamesSection = extractPythonCodeGeneratorRuleNamesSection(),
        importIjars = extractImportIjars(),
      ).build()

  private inline fun <reified T> getValue(sectionName: String): T? {
    val parser = ProjectViewSection.KEYWORD_MAP[sectionName]?.sectionValueParser ?: return null
    val rawSection = rawProjectViewSections.getLastSectionWithName(sectionName) ?: return null
    val values =
      rawSection.sectionBody
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val value = values.firstOrNull() ?: return null
    return parser.parse(value) as? T
  }

  private inline fun <reified T> getValues(sectionName: String): List<T> {
    val parser = ProjectViewSection.KEYWORD_MAP[sectionName]?.sectionValueParser ?: return emptyList()
    val rawSection = rawProjectViewSections.getLastSectionWithName(sectionName) ?: return emptyList()
    val values =
      rawSection.sectionBody
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    return values.mapNotNull {
      parser.parse(it) as? T
    }
  }

  private inline fun <reified T> unpackIncludedExcluded(vals: List<ProjectViewSection.Value<T>>): Pair<List<T>, List<T>> {
    val included =
      vals
        .filter { it is ProjectViewSection.Value.Included<*> }
        .mapNotNull { (it as? ProjectViewSection.Value.Included<*>)?.value as? T }
    val excluded =
      vals
        .filter { it is ProjectViewSection.Value.Excluded<*> }
        .mapNotNull { (it as? ProjectViewSection.Value.Excluded<*>)?.value as? T }
    return Pair(included, excluded)
  }

  private fun extractTargets(): ProjectViewTargetsSection {
    val targets = getValues<TARGETS_TYPE>(TARGETS)
    val (included, excluded) = unpackIncludedExcluded<TARGETS_INNER_TYPE>(targets)
    return ProjectViewTargetsSection(included, excluded)
  }

  private fun extractAllowManualTargetsSync(): ProjectViewAllowManualTargetsSyncSection? {
    val allowManualTargetsSync = getValue<ALLOW_MANUAL_TARGETS_SYNC_TYPE>(ALLOW_MANUAL_TARGETS_SYNC)
    return allowManualTargetsSync?.let { ProjectViewAllowManualTargetsSyncSection(it) }
  }

  private fun extractBazelBinary(): ProjectViewBazelBinarySection? {
    val path = getValue<BAZEL_BINARY_TYPE>(BAZEL_BINARY) ?: return null
    return ProjectViewBazelBinarySection(path)
  }

  private fun extractBuildFlags(): ProjectViewBuildFlagsSection {
    val flags = getValues<BUILD_FLAGS_TYPE>(BUILD_FLAGS)
    return ProjectViewBuildFlagsSection(flags)
  }

  private fun extractSyncFlags(): ProjectViewSyncFlagsSection {
    val flags = getValues<SYNC_FLAGS_TYPE>(SYNC_FLAGS)
    return ProjectViewSyncFlagsSection(flags)
  }

  private fun extractDebugFlags(): ProjectViewDebugFlagsSection {
    val flags = getValues<DEBUG_FLAGS_TYPE>(DEBUG_FLAGS)
    return ProjectViewDebugFlagsSection(flags)
  }

  private fun extractDirectories(): ProjectViewDirectoriesSection {
    val dirs = getValues<DIRECTORIES_TYPE>(DIRECTORIES)
    val (included, excluded) = unpackIncludedExcluded<DIRECTORIES_INNER_TYPE>(dirs)
    return ProjectViewDirectoriesSection(included, excluded)
  }

  private fun extractDeriveTargetsFromDirectories(): ProjectViewDeriveTargetsFromDirectoriesSection? {
    val deriveTargetsFromDirectories = getValue<DERIVE_TARGETS_FROM_DIRECTORIES_TYPE>(DERIVE_TARGETS_FROM_DIRECTORIES)
    return deriveTargetsFromDirectories?.let { ProjectViewDeriveTargetsFromDirectoriesSection(it) }
  }

  private fun extractImportDepth(): ProjectViewImportDepthSection? {
    val importDepth = getValue<IMPORT_DEPTH_TYPE>(IMPORT_DEPTH)
    return importDepth?.let { ProjectViewImportDepthSection(it) }
  }

  private fun extractEnabledRules(): ProjectViewEnabledRulesSection {
    val rules = getValues<ENABLED_RULES_TYPE>(ENABLED_RULES)
    return ProjectViewEnabledRulesSection(rules)
  }

  private fun extractIdeJavaHomeOverride(): ProjectViewIdeJavaHomeOverrideSection? {
    val newPath = getValue<IDE_JAVA_HOME_OVERRIDE_TYPE>(IDE_JAVA_HOME_OVERRIDE)
    return newPath?.let { ProjectViewIdeJavaHomeOverrideSection(it) }
  }

  private fun extractAddTransitiveCompileTimeJars(): ExperimentalAddTransitiveCompileTimeJarsSection? {
    val shouldAdd = getValue<EXPERIMENTAL_ADD_TRANSITIVE_COMPILE_TIME_JARS_TYPE>(EXPERIMENTAL_ADD_TRANSITIVE_COMPILE_TIME_JARS)
    return shouldAdd?.let { ExperimentalAddTransitiveCompileTimeJarsSection(it) }
  }

  private fun extractTransitiveCompileTimeJarsTargetKinds(): ExperimentalTransitiveCompileTimeJarsTargetKindsSection? {
    val targetKinds =
      getValues<EXPERIMENTAL_TRANSITIVE_COMPILE_TIME_JARS_TARGET_KINDS_TYPE>(
        EXPERIMENTAL_TRANSITIVE_COMPILE_TIME_JARS_TARGET_KINDS,
      )
    return targetKinds.takeIf { it.isNotEmpty() }?.let { ExperimentalTransitiveCompileTimeJarsTargetKindsSection(it) }
  }

  private fun extractNoPruneTransitiveCompileTimeJarsPatterns(): ExperimentalNoPruneTransitiveCompileTimeJarsPatternsSection? {
    val patterns =
      getValues<EXPERIMENTAL_NO_PRUNE_TRANSITIVE_COMPILE_TIME_JARS_PATTERNS_TYPE>(
        EXPERIMENTAL_NO_PRUNE_TRANSITIVE_COMPILE_TIME_JARS_PATTERNS,
      )
    return patterns.takeIf { it.isNotEmpty() }?.let { ExperimentalNoPruneTransitiveCompileTimeJarsPatternsSection(it) }
  }

  private fun extractPrioritizeLibrariesOverModulesTargetKinds(): ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection? {
    val values =
      getValues<EXPERIMENTAL_PRIORITIZE_LIBRARIES_OVER_MODULES_TARGET_KINDS_TYPE>(
        EXPERIMENTAL_PRIORITIZE_LIBRARIES_OVER_MODULES_TARGET_KINDS,
      )
    return values.takeIf { it.isNotEmpty() }?.let { ExperimentalPrioritizeLibrariesOverModulesTargetKindsSection(it) }
  }

  private fun extractEnableNativeAndroidRules(): EnableNativeAndroidRulesSection? {
    val enable = getValue<ENABLE_NATIVE_ANDROID_RULES_TYPE>(ENABLE_NATIVE_ANDROID_RULES)
    return enable?.let { EnableNativeAndroidRulesSection(it) }
  }

  private fun extractAndroidMinSdkSection(): AndroidMinSdkSection? {
    val minSdk = getValue<ANDROID_MIN_SDK_TYPE>(ANDROID_MIN_SDK)
    return minSdk?.let { AndroidMinSdkSection(it) }
  }

  private fun extractShardSync(): ShardSyncSection? {
    val shardSync = getValue<SHARD_SYNC_TYPE>(SHARD_SYNC)
    return shardSync?.let { ShardSyncSection(it) }
  }

  private fun extractTargetShardSize(): TargetShardSizeSection? {
    val shardSize = getValue<TARGET_SHARD_SIZE_TYPE>(TARGET_SHARD_SIZE)
    return shardSize?.let { TargetShardSizeSection(it) }
  }

  private fun extractShardingApproach(): ShardingApproachSection? {
    val approach = getValue<SHARD_APPROACH_TYPE>(SHARD_APPROACH)
    return approach?.let { ShardingApproachSection(it) }
  }

  private fun extractImportRunConfigurations(): ImportRunConfigurationsSection? {
    val imports = getValues<IMPORT_RUN_CONFIGURATIONS_TYPE>(IMPORT_RUN_CONFIGURATIONS)
    return imports.takeIf { it.isNotEmpty() }?.let { ImportRunConfigurationsSection(it.map { path -> path.toString() }) }
  }

  private fun extractGazelleTarget(): GazelleTargetSection? {
    val target = getValue<GAZELLE_TARGET_TYPE>(GAZELLE_TARGET)
    return target?.let { GazelleTargetSection(it) }
  }

  private fun extractIndexAllFilesInDirectories(): IndexAllFilesInDirectoriesSection? {
    val index = getValue<INDEX_ALL_FILES_IN_DIRECTORIES_TYPE>(INDEX_ALL_FILES_IN_DIRECTORIES)
    return index?.let { IndexAllFilesInDirectoriesSection(it) }
  }

  private fun extractPythonCodeGeneratorRuleNamesSection(): PythonCodeGeneratorRuleNamesSection? {
    val names = getValues<PYTHON_CODE_GENERATOR_RULE_NAMES_TYPE>(PYTHON_CODE_GENERATOR_RULE_NAMES)
    return names.takeIf { it.isNotEmpty() }?.let { PythonCodeGeneratorRuleNamesSection(it) }
  }

  private fun extractImportIjars(): ImportIjarsSection? {
    val shouldImport = getValue<IMPORT_IJARS_TYPE>(IMPORT_IJARS)
    return shouldImport?.let { ImportIjarsSection(it) }
  }
}
