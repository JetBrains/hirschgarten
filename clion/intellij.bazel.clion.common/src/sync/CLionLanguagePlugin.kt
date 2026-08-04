package org.jetbrains.bazel.clion.sync

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.BuildTargetData
import kotlin.reflect.KClass

@ApiStatus.Internal
class CLionLanguagePlugin : LanguagePlugin {
  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(CLionBuildTarget::class)

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(CLionLanguageClass.CPP)

  override fun collectUsedLanguages(target: TargetIdeInfo): List<LanguageClass> {
    return emptyList()
  }

  override suspend fun mapBuildTargetData(
    server: BazelServerFacade,
    target: TargetIdeInfo,
    repoMapping: RepoMapping,
  ): List<BuildTargetData> {
      return emptyList()
  }


  companion object {
    private val logger: Logger = logger<CLionLanguagePlugin>()
  }
}
