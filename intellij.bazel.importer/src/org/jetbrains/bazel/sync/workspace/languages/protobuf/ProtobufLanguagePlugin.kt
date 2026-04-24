package org.jetbrains.bazel.sync.workspace.languages.protobuf

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.BazelServerFacade
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.ProtobufBuildTarget
import kotlin.io.path.absolutePathString

internal class ProtobufLanguagePlugin : LanguagePlugin {
  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.PROTOBUF)
  override fun createProjectMapper(project: Project, server: BazelServerFacade) = Mapper(server)

  class Mapper(private val server: BazelServerFacade) : LanguagePlugin.Mapper {

    override suspend fun createBuildTargetData(
      target: TargetInfo,
      targetsToImport: Map<Label, TargetInfo>,
      graph: DependencyGraph,
      repoMapping: RepoMapping,
    ): List<BuildTargetData> {
      if (!target.hasProtobufTargetInfo()) {
        return emptyList()
      }
      val localRepositories = repoMapping.getLocalRepositories()
      val sources =
        target.protobufTargetInfo.sourceMappingsList
          .associate<BspTargetInfo.ProtobufSourceMapping, String, String> {
            it.importPath to server.bazelPathsResolver.resolve(it.protoFile, localRepositories).absolutePathString()
          }
      return listOf(
        ProtobufBuildTarget(
          sources = sources,
        ),
      )
    }
  }
}
