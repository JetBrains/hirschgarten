package org.jetbrains.bazel.cpp.sync.configuration

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.TargetKey
import org.jetbrains.bazel.cpp.sync.BazelCompilerSettings
import org.jetbrains.bazel.cpp.sync.CFileExtensions
import org.jetbrains.bazel.cpp.sync.ExecutionRootPathResolver
import org.jetbrains.bazel.cpp.sync.xcode.XCodeCompilerSettingProvider
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.toPath
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.sync.task.bazelProject
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path

// todo: fill in aspect ids
fun Project.targetMap() = bazelProject.targets.map { it -> TargetKey(it.key, listOf()) to it.value }.toMap()

// See com.google.idea.blaze.cpp.BlazeConfigurationResolver
class BazelConfigurationResolver(private val project: Project, private val workspaceContext: WorkspaceContext) {
  val bazelRunner = BazelRunner(null, project.bazelProject.bazelInfo.workspaceRoot)
  val pathResolver = BazelPathsResolver(project.bazelProject.bazelInfo)

  fun update(oldResult: BazelConfigurationResolverResult): BazelConfigurationResolverResult {
    val executionRootPathResolver =
      ExecutionRootPathResolver(
        project.bazelProject.bazelInfo,
        project.targetMap(),
      )
    val toolchainLookupMap =
      BazelConfigurationToolchainResolver.buildToolchainLookupMap(
        project.targetMap(),
      )
    val xcodeSettings =
      XCodeCompilerSettingProvider.getInstance(project)?.fromContext(bazelRunner, workspaceContext)
    val compilerSettings: Map<BspTargetInfo.CToolchainInfo, BazelCompilerSettings> =
      BazelConfigurationToolchainResolver.buildCompilerSettingsMap(
        project,
        toolchainLookupMap,
        executionRootPathResolver,
        oldResult.compilerSettings,
        xcodeSettings,
      )
    val targetFilter = getTargetFilter()
    val builder: BazelConfigurationResolverResult.Builder = BazelConfigurationResolverResult.Builder()
    buildBazelConfigurationData(
      targetMap = project.targetMap().filter { targetFilter(it.value) },
      toolchainLookupMap,
      compilerSettings,
      builder,
    )
    builder.compilerSettings = compilerSettings
    val validHeaderRoots =
      HeaderRootTrimmer.getValidRoots(
        project,
        toolchainLookupMap,
        executionRootPathResolver,
      )
    builder.validHeaderRoots = validHeaderRoots
    builder.xcodeSettings = xcodeSettings

    return builder.build()
  }

  private fun buildBazelConfigurationData(
    targetMap: Map<TargetKey, BspTargetInfo.TargetInfo>,
    toolchainLookupMap: Map<TargetKey, BspTargetInfo.CToolchainInfo>,
    compilerSettings: Map<BspTargetInfo.CToolchainInfo, BazelCompilerSettings>,
    builder: BazelConfigurationResolverResult.Builder,
  ) {
    // Type specification needed to avoid incorrect type inference during command line build.
    val targetToData: Map<TargetKey, BazelResolveConfigurationData> =
      targetMap
        .map {
          it.key to
            createResolveConfiguration(
              it.value,
              toolchainLookupMap,
              compilerSettings,
            )
        }.filter { it.second != null }
        .associate { it.first to it.second!! }

    findEquivalenceClasses(project, targetToData, builder)
  }

  private fun createResolveConfiguration(
    target: BspTargetInfo.TargetInfo,
    toolchainLookupMap: Map<TargetKey, BspTargetInfo.CToolchainInfo>,
    compilerSettingsMap: Map<BspTargetInfo.CToolchainInfo, BazelCompilerSettings>,
  ): BazelResolveConfigurationData? {
    val targetKey = TargetKey(target.label(), listOf())
    val cIdeInfo = target.cppTargetInfo ?: return null

    val toolchainIdeInfo: BspTargetInfo.CToolchainInfo = toolchainLookupMap[targetKey] ?: return null

    val compilerSettings: BazelCompilerSettings = compilerSettingsMap[toolchainIdeInfo] ?: return null
    return BazelResolveConfigurationData(compilerSettings, cIdeInfo, toolchainIdeInfo)
  }

  private fun isSubPath(parent: Path, child: Path): Boolean = pathResolver.resolve(child).startsWith(pathResolver.resolve(parent))

  private fun targetInProjectView(label: Label): Boolean {
    // todo: for now we only generate configurations for targets in main workspaces
    if (!label.isMainWorkspace) {
      return false
    }

    val labelPath = label.packagePath.toPath()
    return workspaceContext.directories.values.any { isSubPath(it, labelPath) } &&
      !workspaceContext.directories.excludedValues.any { isSubPath(it, labelPath) } ||
      workspaceContext.targets.values.contains(label) &&
      !workspaceContext.targets.excludedValues.contains(label)
  }

  private fun getTargetFilter(): (BspTargetInfo.TargetInfo) -> Boolean =
    { targetInfo ->
      val label = targetInfo.label()
      label.isMainWorkspace &&
        targetInProjectView(label) &&
        containsCompiledSources(targetInfo)
    }

  companion object {
    private fun containsCompiledSources(target: BspTargetInfo.TargetInfo): Boolean =
      target.hasCppTargetInfo() &&
        target.sourcesList
          .filter { it.isSource }
          .any { location ->
            val locationExtension: String = FileUtilRt.getExtension(location.getRelativePath())
            CFileExtensions.SOURCE_EXTENSIONS.contains(locationExtension)
          }

    private val logger: Logger = Logger.getInstance(BazelConfigurationResolver::class.java)

    private fun findEquivalenceClasses(
      project: Project,
      targetToData: Map<TargetKey, BazelResolveConfigurationData>,
      builder: BazelConfigurationResolverResult.Builder,
    ) {
      val dataEquivalenceClasses: Multimap<BazelResolveConfigurationData, TargetKey> =
        ArrayListMultimap.create()
      for (entry in targetToData.entries) {
        val target = entry.key
        val data: BazelResolveConfigurationData? = entry.value
        dataEquivalenceClasses.put(data, target)
      }

      val dataToConfiguration = mutableMapOf<BazelResolveConfigurationData, BazelResolveConfiguration>()
      for (entry in dataEquivalenceClasses.asMap()) {
        val data: BazelResolveConfigurationData = entry.key ?: continue
        val targets: MutableCollection<TargetKey> = entry.value
        dataToConfiguration.put(
          data,
          BazelResolveConfiguration.createForTargets(project, data, targets.toList()),
        )
      }

      builder.uniqueConfigurations = dataToConfiguration
    }
  }
}
