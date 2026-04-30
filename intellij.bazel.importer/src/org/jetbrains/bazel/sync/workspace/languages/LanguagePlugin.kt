package org.jetbrains.bazel.sync.workspace.languages

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bsp.protocol.BazelServerFacade
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.SourceItem

@ApiStatus.Internal
interface LanguagePlugin {
  fun getSupportedLanguages(): Set<LanguageClass>
  fun createProjectMapper(project: Project, server: BazelServerFacade): Mapper

  interface Mapper {
    suspend fun prepareSync(
      graph: DependencyGraph,
      targetsToImport: Map<Label, TargetInfo>,
      repoMapping: RepoMapping,
    ) {}

    suspend fun createBuildTargetData(
      target: TargetInfo,
      targetsToImport: Map<Label, TargetInfo>,
      graph: DependencyGraph,
      repoMapping: RepoMapping,
    ): List<BuildTargetData>
  }

  companion object {
    private val EP = ExtensionPointName<LanguagePlugin>("org.jetbrains.bazel.languagePlugin")
    fun all(): List<LanguagePlugin> = EP.extensionList
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
  LanguagePlugin.all().forEach { plugin ->
    val mapper = plugin.createProjectMapper(project, server)
    plugin.getSupportedLanguages().forEach { languageClass ->
      if (result.containsKey(languageClass))
        throw IncorrectOperationException("Language class $languageClass is already mapped by another plugin")
      result[languageClass] = mapper
    }
  }
  return LanguageProjectMappers(result)
}
