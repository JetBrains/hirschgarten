package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.flow.universe_expand.SyncExpandService
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex
import org.jetbrains.bazel.sync_new.graph.impl.resolve
import org.jetbrains.bazel.sync_new.lang.getLangData
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.languages_impl.jvm.JvmSyncLanguage
import org.jetbrains.bazel.sync_new.languages_impl.jvm.JvmSyncTargetData
import org.jetbrains.bazel.sync_new.languages_impl.kotlin.KotlinSyncLanguage
import java.nio.file.Path

class SourceModuleProcessor(
  private val project: Project,
  private val storage: IncrementalEntityStore<JvmResourceId, JvmModuleEntity>,
) {
  private val expandService = project.service<SyncExpandService>()

  suspend fun computeSourceModules(ctx: SyncContext, diff: SyncDiff) {
    val (added, _) = diff.split
    for (added in added) {
      val target = added.getBuildTarget() ?: continue
      val jvmData = JvmSyncLanguage.getLangData(target) ?: continue

      // if the target should be treated as a library
      if (expandService.isWithinUniverseScope(target.label)) {
        addSourceModuleEntity(ctx, target, jvmData)
      } else {
        addLibraryModuleEntity(ctx, target, jvmData)
      }
    }
  }

  private fun addSourceModuleEntity(
    ctx: SyncContext,
    target: BazelTargetVertex,
    jvmData: JvmSyncTargetData,
  ) {
    val resourceId = JvmResourceId.VertexReference(vertexId = target.vertexId)
    storage.createEntity(resourceId) {
      JvmModuleEntity.LegacySourceModule(
        resourceId = it,
        baseDirectory = target.baseDirectory,
        label = target.label,
        dependencies = computeDirectDependencies(ctx, target),
        sources = computeSources(ctx, jvmData),
        resources = computeResources(ctx, target).toMutableList(),
        legacyKotlinData = computeLegacyKotlinData(ctx, target),
        legacyJvmData = computeLegacyJvmData(ctx, jvmData),
      )
    }
  }

  private fun addLibraryModuleEntity(
    ctx: SyncContext,
    target: BazelTargetVertex,
    jvmData: JvmSyncTargetData,
  ) {
    val resourceId = JvmResourceId.VertexReference(vertexId = target.vertexId)
    storage.createEntity(resourceId) {
      JvmModuleEntity.LegacyLibraryModule(
        resourceId = it,
        label = target.label,
        dependencies = computeDirectDependencies(ctx, target),
        interfaceJars = jvmData.outputs.iJars
          .map { ctx.pathsResolver.resolve(it) }
          .toHashSet(),
        classJars = jvmData.outputs.classJars
          .map { ctx.pathsResolver.resolve(it) }
          .toHashSet(),
        sourceJars = jvmData.outputs.srcJars
          .map { ctx.pathsResolver.resolve(it) }
          .toHashSet(),
        isFromInternalTarget = target.label.isMainWorkspace,
        isLowPriority = false,
      )
    }
  }

  private fun computeDirectDependencies(
    ctx: SyncContext,
    target: BazelTargetVertex,
  ): Set<Label> {
    return ctx.graph.getSuccessors(target.vertexId)
      .asSequence()
      .mapNotNull { ctx.graph.getLabelByVertexId(it) }
      .toSet()
  }

  private fun computeSources(
    ctx: SyncContext,
    jvmData: JvmSyncTargetData,
  ): List<JvmSourceItem> {
    return jvmData.jvmTarget.sources
      .sortedBy { it.priority }
      .map {
        JvmSourceItem(
          path = ctx.pathsResolver.resolve(it.path),
          generated = it.generated,
          jvmPackagePrefix = it.jvmPackagePrefix,
        )
      }
  }

  private fun computeResources(ctx: SyncContext, target: BazelTargetVertex): List<Path> {
    return target.genericData.resources.map { ctx.pathsResolver.resolve(it.path) }
  }

  private fun computeLegacyKotlinData(ctx: SyncContext, target: BazelTargetVertex): LegacyKotlinTargetData? {
    val language = KotlinSyncLanguage.getLangData(target) ?: return null
    return LegacyKotlinTargetData(
      languageVersion = language.languageVersion,
      apiVersion = language.apiVersion,
      kotlincOptions = language.kotlincOptions,
      associates = language.associates,
    )
  }

  private fun computeLegacyJvmData(ctx: SyncContext, jvmData: JvmSyncTargetData): LegacyJvmTargetData {
    val compilerOpts = jvmData.compilerOptions
    val outputs = jvmData.outputs
    val generatedOutputs = jvmData.generatedOutputs
    return LegacyJvmTargetData(
      javaHome = compilerOpts.javaHome ?: jvmData.toolchain?.javaHome,
      javaVersion = compilerOpts.javaVersion.orEmpty(),
      javacOpts = compilerOpts.javacOpts,
      binaryOutputs = (outputs.classJars + generatedOutputs.classJars + outputs.iJars + generatedOutputs.iJars)
        .map { ctx.pathsResolver.resolve(it) }
        .toMutableList(),
      toolchain = jvmData.toolchain,
    )
  }
}
