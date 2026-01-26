package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync_new.bridge.LegacySyncTargetInfo
import org.jetbrains.bazel.sync_new.flow.universe_expand.SyncExpandService
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.impl.BazelGenericTargetData
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetDependency
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetEdge
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetResourceFile
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetSourceFile
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex
import org.jetbrains.bazel.sync_new.graph.impl.DependencyType
import org.jetbrains.bazel.sync_new.graph.impl.PRIORITY_NORMAL
import org.jetbrains.bazel.sync_new.graph.impl.toBazelPath
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDataBuilder
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDetector
import org.jetbrains.bazel.sync_new.lang.SyncLanguagePlugin
import org.jetbrains.bazel.sync_new.lang.SyncTargetData
import kotlin.collections.set
import kotlin.to

class SyncTargetBuilder(
  private val project: Project,
  private val pathsResolver: BazelPathsResolver,
  private val tagsBuilder: SyncTagsBuilder = SyncTagsBuilder(),
  private val legacyRepoMapping: RepoMapping
) {
  private data class BakedLanguagePlugin<T : SyncTargetData>(
    val plugin: SyncLanguagePlugin<T>,
    val detector: SyncLanguageDetector,
    val builder: SyncLanguageDataBuilder<T>,
  )

  private val expandService: SyncExpandService = project.service<SyncExpandService>()

  private suspend fun <T : SyncTargetData> bakeLanguagePlugin(ctx: SyncContext, plugin: SyncLanguagePlugin<T>): BakedLanguagePlugin<T> {
    val builder = plugin.createSyncDataBuilder(ctx)
    builder.init(ctx)
    return BakedLanguagePlugin(
      plugin = plugin,
      detector = plugin.createLanguageDetector(ctx),
      builder = builder,
    )
  }

  suspend fun buildAllChangedTargetVertices(ctx: SyncContext, targets: Collection<LegacySyncTargetInfo>): List<Pair<LegacySyncTargetInfo, BazelTargetVertex>> {
    val bakedPlugins = SyncLanguagePlugin.ep.extensionList
      .filter { it.isEnabled(ctx) }
      .map { bakeLanguagePlugin(ctx, it) }
      .toTypedArray()
    return withContext(Dispatchers.Default) {
      targets.map {
        async {
          it to buildTargetVertex(ctx, it, bakedPlugins)
        }
      }.awaitAll()
    }
  }

  private suspend fun buildTargetVertex(ctx: SyncContext, raw: LegacySyncTargetInfo, plugins: Array<BakedLanguagePlugin<*>>): BazelTargetVertex {
    val genericData = buildGeneralTargetData(raw)
    val targetData = Long2ObjectOpenHashMap<SyncTargetData>()
    val languageTags = LongOpenHashSet()
    for (plugin in plugins) {
      if (plugin.detector.detect(ctx, raw)) {
        val data = plugin.builder.buildTargetData(ctx, raw) ?: continue
        val serialId = plugin.plugin.language.serialId
        targetData[serialId] = data
        languageTags.add(serialId)
      }
    }
    return BazelTargetVertex(
      label = raw.target.label(),
      genericData = genericData,
      languageTags = languageTags,
      targetData = targetData,
      baseDirectory = pathsResolver.toDirectoryPath(raw.target.label().assumeResolved(), legacyRepoMapping),
      kind = raw.target.kind,
    )
  }

  fun buildTargetEdge(ctx: SyncContext, from: ID, to: ID, dependency: BspTargetInfo.Dependency): BazelTargetEdge {
    val edgeId = ctx.graph.getNextEdgeId()
    val type = when (dependency.dependencyType) {
      BspTargetInfo.Dependency.DependencyType.RUNTIME -> DependencyType.RUNTIME
      else -> DependencyType.COMPILE
    }
    return BazelTargetEdge(
      edgeId = edgeId,
      from = from,
      to = to,
      type = type,
    )
  }

  private fun buildGeneralTargetData(raw: LegacySyncTargetInfo): BazelGenericTargetData {
    return BazelGenericTargetData(
      tags = tagsBuilder.build(raw),
      directDependencies = buildDependencyList(raw),
      sources = buildSourceList(raw),
      resources = buildResourceList(raw),
      isUniverseTarget = expandService.isWithinUniverseScope(raw.target.label()),
    )
  }

  private fun buildDependencyList(raw: LegacySyncTargetInfo): List<BazelTargetDependency> {
    return raw.target.dependenciesList
      .map { BazelTargetDependency(it.label()) }
  }

  private fun buildSourceList(raw: LegacySyncTargetInfo): List<BazelTargetSourceFile> {
    return raw.target.sourcesList
      .map {
        BazelTargetSourceFile(
          path = it.toBazelPath(),
          priority = PRIORITY_NORMAL,
        )
      }
  }

  private fun buildResourceList(raw: LegacySyncTargetInfo): List<BazelTargetResourceFile> {
    return raw.target.resourcesList
      .map {
        BazelTargetResourceFile(
          path = it.toBazelPath(),
        )
      }
  }
}
