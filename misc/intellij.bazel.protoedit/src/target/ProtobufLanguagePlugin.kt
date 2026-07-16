package org.jetbrains.bazel.protobuf

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.protobuf.target.ProtobufBuildTarget
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.BuildTargetData
import kotlin.io.path.absolutePathString
import kotlin.reflect.KClass

internal class ProtobufLanguagePlugin : LanguagePlugin {
  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(ProtobufBuildTarget::class)

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(ProtobufLanguageClass.PROTOBUF)
  override fun collectUsedLanguages(target: TargetIdeInfo): List<LanguageClass> {
    if (target.hasProtobufTargetInfo())
      return listOf(ProtobufLanguageClass.PROTOBUF)
    return emptyList()
  }
  override suspend fun mapBuildTargetData(
    server: BazelServerFacade,
    target: TargetIdeInfo,
    repoMapping: RepoMapping,
  ): List<BuildTargetData> {
    if (!target.hasProtobufTargetInfo()) {
      return emptyList()
    }
    val localRepositories = repoMapping.getLocalRepositories()
    val sources =
      target.protobufTargetInfo.sourceMappingsList
        .associate<IntellijIdeInfo.ProtobufSourceMapping, String, String> {
          it.importPath to server.bazelPathsResolver.resolve(it.protoFile, localRepositories).absolutePathString()
        }
    return listOf(
      ProtobufBuildTarget(
        sources = sources,
      ),
    )
  }
}
