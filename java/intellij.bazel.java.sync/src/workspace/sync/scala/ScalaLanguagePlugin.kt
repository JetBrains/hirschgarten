package org.jetbrains.bazel.sync.workspace.languages.scala

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.jvm.ScalaBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bsp.protocol.BuildTargetData
import kotlin.reflect.KClass

@ApiStatus.Internal
class ScalaLanguagePlugin : LanguagePlugin {
  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(ScalaBuildTarget::class)

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(JavaLanguageClass.SCALA)

  override fun collectUsedLanguages(target: TargetIdeInfo): List<LanguageClass> =
    if (target.hasScalaTargetInfo()) listOf(JavaLanguageClass.SCALA) else emptyList()

  override suspend fun mapBuildTargetData(
    server: BazelServerFacade,
    target: TargetIdeInfo,
    repoMapping: RepoMapping,
  ): List<BuildTargetData> {
    if (!target.hasScalaTargetInfo()) {
      return emptyList()
    }
    val scalaSdkResolver = ScalaSdkResolver(server.bazelPathsResolver)
    val sdk = scalaSdkResolver.resolveSdk(target, repoMapping.getLocalRepositories()) ?: return emptyList()
    return listOf(
      ScalaBuildTarget(
        scalaVersion = sdk.version,
        sdkJars = SourceFileCollectionBuilder.build(server.outFileHardLinks.createOutputFileHardLinks(sdk.compilerJars)),
        scalacOptions = target.scalaTargetInfo.scalacOptsList,
        scalatestClasspathTargets = target.scalaTargetInfo.scalatestClasspathTargetsList.map { Label.parse(it) },
      ),
    )
  }
}
