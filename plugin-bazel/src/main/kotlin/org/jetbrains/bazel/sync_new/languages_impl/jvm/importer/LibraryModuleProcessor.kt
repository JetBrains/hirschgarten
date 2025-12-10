package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetTag
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex
import org.jetbrains.bazel.sync_new.graph.impl.getIncompletePath
import org.jetbrains.bazel.sync_new.graph.impl.resolve
import org.jetbrains.bazel.sync_new.lang.getLangData
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.lang.store.modifyEntityTyped
import org.jetbrains.bazel.sync_new.languages_impl.jvm.JvmSyncLanguage
import org.jetbrains.bazel.sync_new.languages_impl.jvm.JvmSyncTargetData
import kotlin.io.path.extension

class LibraryModuleProcessor(
  private val storage: IncrementalEntityStore<JvmResourceId, JvmModuleEntity>,
) {
  companion object {
    // TODO: make it extensible
    private val KNOWN_JVM_EXTENSIONS = setOf("java", "kt", "scala")
    private val WORKSPACE_TARGET_KINDS =
      setOf(
        "java_library",
        "java_binary",
        "java_test",
        "kt_jvm_library",
        "kt_jvm_binary",
        "kt_jvm_test",
        "jvm_library",
        "jvm_binary",
        "jvm_resources",
        "scala_library",
        "scala_binary",
        "scala_test",
        "intellij_plugin_debug_target",
        //"go_proto_library",
        //"go_library",
        //"go_binary",
        //"go_test",
      )
  }

  suspend fun computeLibraryModules(ctx: SyncContext, diff: SyncDiff) {
    val (added, _) = diff.split
    for (added in added) {
      val target = added.getBuildTarget() ?: continue
      val jvmData = JvmSyncLanguage.getLangData(target) ?: continue

      if (shouldCreateCompiledLibrary(target, jvmData)) {
        createCompiledLibrary(ctx, target, jvmData)
      }
    }
  }

  private fun createCompiledLibrary(ctx: SyncContext, target: BazelTargetVertex, jvmData: JvmSyncTargetData) {
    val resourceId = JvmResourceId.CompiledLibrary(owner = target.vertexId, name = "output_jars")
    val syntheticLabel = Label.synthetic(target.label.toString() + "_output_jars")
    storage.createEntity(resourceId) { resourceId ->
      JvmModuleEntity.LegacyLibraryModule(
        resourceId = resourceId,
        label = syntheticLabel,
        dependencies = emptySet(),
        interfaceJars = emptySet(),
        classJars = jvmData.outputs.classJars
          .map { ctx.pathsResolver.resolve(it) }
          .toSet(),
        sourceJars = jvmData.outputs.srcJars
          .map { ctx.pathsResolver.resolve(it) }
          .toSet(),
        isFromInternalTarget = true,
        isLowPriority = false,
      )
    }
    storage.addDependency(JvmResourceId.VertexReference(vertexId = target.vertexId), resourceId)
    storage.modifyEntityTyped(JvmResourceId.VertexDeps(label = target.label)) { deps: JvmModuleEntity.VertexDeps ->
      deps.copy(deps = deps.deps + syntheticLabel)
    }
  }

  private fun shouldCreateCompiledLibrary(target: BazelTargetVertex, jvmData: JvmSyncTargetData): Boolean {
    if (target.kind.endsWith("_resources")) {
      return false
    }
    val hasGeneratedSourceJars = jvmData.jvmTarget.generatedSources
      .any { it.getIncompletePath().endsWith(".srcjar") }
    if (hasGeneratedSourceJars) {
      return true
    }
    val hasNoJvmSources = jvmData.jvmTarget.sources.isNotEmpty() && !hasKnownJvmSources(jvmData)
    if (hasNoJvmSources) {
      return true
    }
    val isUnknownSourcelessTarget = jvmData.jvmTarget.sources.isEmpty()
      && target.kind !in WORKSPACE_TARGET_KINDS
      && !target.genericData.tags.contains(BazelTargetTag.EXECUTABLE)
    if (isUnknownSourcelessTarget) {
      return true
    }
    return jvmData.jvmTarget.hasApiGeneratingPlugin
  }

  private fun hasKnownJvmSources(jvmData: JvmSyncTargetData): Boolean {
    val target = jvmData.jvmTarget
    if (target.sources.isEmpty()) {
      return false
    }
    for (source in target.sources) {
      val path = source.path.getIncompletePath()
      if (KNOWN_JVM_EXTENSIONS.contains(path.extension)) {
        return true
      }
    }
    return false
  }
}
