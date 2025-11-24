package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.project.Project
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.impl.BazelGenericTargetData
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetDependency
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetEdge
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetResourceFile
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetSourceFile
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex
import org.jetbrains.bazel.sync_new.graph.impl.DependencyType
import org.jetbrains.bazel.sync_new.graph.impl.toBazelPath
import org.jetbrains.bazel.sync_new.lang.SyncTargetData
import org.jetbrains.bsp.protocol.RawAspectTarget

class SyncTargetBuilder(
  private val project: Project,
  private val pathsResolver: BazelPathsResolver,
  private val tagsBuilder: SyncTagsBuilder = SyncTagsBuilder(),
) {
  suspend fun buildTargetVertex(ctx: SyncContext, raw: RawAspectTarget): BazelTargetVertex {
    val genericData = buildGeneralTargetData(raw)
    val targetData = Long2ObjectOpenHashMap<SyncTargetData>()
    val languageTags = LongOpenHashSet()
    for (detector in ctx.languageService.languageDetectors) {
      for (language in detector.detect(ctx, raw)) {
        languageTags.add(language.serialId)

        for (plugin in ctx.languageService.getPluginsByLanguage(language)) {
          val languageData = plugin.createTargetData(ctx, raw)
          val tag = ctx.languageService.getTagByType(languageData.javaClass)
          if (tag == 0L) {
            error("Tag is not found for ${languageData.javaClass}")
          }
          targetData[tag] = languageData
        }
      }
    }
    return BazelTargetVertex(
      label = raw.target.label(),
      genericData = genericData,
      languageTags = languageTags,
      targetData = targetData,
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

  private fun buildGeneralTargetData(raw: RawAspectTarget): BazelGenericTargetData {
    return BazelGenericTargetData(
      tags = tagsBuilder.build(raw),
      directDependencies = buildDependencyList(raw),
      sources = buildSourceList(raw),
      resources = buildResourceList(raw),
    )
  }

  private fun buildDependencyList(raw: RawAspectTarget): List<BazelTargetDependency> {
    return raw.target.dependenciesList
      .map { BazelTargetDependency(it.label()) }
  }

  private fun buildSourceList(raw: RawAspectTarget): List<BazelTargetSourceFile> {
    return raw.target.sourcesList
      .map {
        BazelTargetSourceFile(
          path = it.toBazelPath(),
          priority = 0,
        )
      }
  }

  private fun buildResourceList(raw: RawAspectTarget): List<BazelTargetResourceFile> {
    return raw.target.resourcesList
      .map {
        BazelTargetResourceFile(
          path = it.toBazelPath(),
        )
      }
  }
}
