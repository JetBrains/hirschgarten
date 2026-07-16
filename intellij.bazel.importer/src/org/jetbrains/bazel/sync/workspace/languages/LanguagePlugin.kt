package org.jetbrains.bazel.sync.workspace.languages

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTargetData
import kotlin.reflect.KClass

@ApiStatus.Internal
interface LanguagePlugin {
  val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>

  fun getSupportedLanguages(): Set<LanguageClass>
  fun collectUsedLanguages(target: IntellijIdeInfo.TargetIdeInfo): List<LanguageClass> = emptyList()
  fun createProjectMapper(project: Project, server: BazelServerFacade): Mapper

  /**
   * Create a set of sync configurations that shall be later consumed by [BazelWorkspaceImporter].
   * This is the only time when [BazelWorkspaceImporter] can indirectly access [ProjectView].
   * Or any other external data outside [ProjectView]
   *
   * @param project [Project] that can be used as a data source
   * @param projectView current project view
   */
  suspend fun createSyncConfigs(project: Project, projectView: ProjectView): List<WorkspaceSyncConfig> = listOf()

  interface Mapper {
    val langPlugin: LanguagePlugin

    suspend fun prepareSync(
      graph: DependencyGraph,
      targetsToImport: Map<WorkspaceTargetKey, IntellijIdeInfo.TargetIdeInfo>,
      repoMapping: RepoMapping,
    ) {
    }

    suspend fun createBuildTargetData(
      target: IntellijIdeInfo.TargetIdeInfo,
      targetsToImport: Map<WorkspaceTargetKey, IntellijIdeInfo.TargetIdeInfo>,
      graph: DependencyGraph,
      repoMapping: RepoMapping,
    ): List<BuildTargetData>
  }

  companion object {
    val EP_NAME = ExtensionPointName<LanguagePlugin>("org.jetbrains.bazel.languagePlugin")
  }
}

@ApiStatus.Internal
class LanguageProjectMappers(val registry: Map<LanguageClass, LanguagePlugin.Mapper>) {
  fun all(): Collection<LanguagePlugin.Mapper> = registry.values.distinct()
  fun get(lang: LanguageClass): LanguagePlugin.Mapper = registry.getValue(lang)
}

@ApiStatus.Internal
fun createLanguageProjectMappers(project: Project, server: BazelServerFacade): LanguageProjectMappers {
  val result = HashMap<LanguageClass, LanguagePlugin.Mapper>()
  LanguagePlugin.EP_NAME.forEachExtensionSafe { plugin ->
    val mapper = plugin.createProjectMapper(project, server)
    plugin.getSupportedLanguages().forEach { languageClass ->
      if (result.containsKey(languageClass))
        throw IncorrectOperationException("Language class $languageClass is already mapped by another plugin")
      result[languageClass] = mapper
    }
  }
  return LanguageProjectMappers(result)
}
