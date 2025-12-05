package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.legacy

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.JvmModuleEntity
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.JvmResourceId
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.LegacyJvmTargetData
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.LegacyKotlinTargetData
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem

class LegacyModelConverter(
  private val storage: IncrementalEntityStore<JvmResourceId, JvmModuleEntity>,
) {
  fun convert(ctx: SyncContext): LegacyImportData {
    val targets = mutableListOf<RawBuildTarget>()
    val libraries = mutableListOf<LibraryItem>()
    for (entity in storage.getAllEntities()) {
      when (entity) {
        is JvmModuleEntity.LegacyLibraryModule -> {
          libraries += convertLibraryModule(entity)
        }

        is JvmModuleEntity.LegacySourceModule -> {
          targets += convertSourceModule(entity)
        }

        else -> {}
      }
    }
    return LegacyImportData(
      targets = targets,
      libraries = libraries
    )
  }

  private fun convertLibraryModule(module: JvmModuleEntity.LegacyLibraryModule): LibraryItem {
    val vertexDepsEntity = storage.getEntity(JvmResourceId.VertexDeps(label = module.label))
      as? JvmModuleEntity.VertexDeps
    // TODO: properly handle runtime deps
    val dependencies = (module.dependencies + (vertexDepsEntity?.deps ?: emptySet()))
      .map { DependencyLabel(it) }
    return LibraryItem(
      id = module.label,
      dependencies = dependencies,
      ijars = module.interfaceJars.toList(),
      jars = module.classJars.toList(),
      sourceJars = module.sourceJars.toList(),
      mavenCoordinates = null, // TODO
      isFromInternalTarget = module.isFromInternalTarget,
      isLowPriority = module.isLowPriority,
    )
  }

  private fun convertSourceModule(module: JvmModuleEntity.LegacySourceModule): RawBuildTarget {
    val vertexDepsEntity = storage.getEntity(JvmResourceId.VertexDeps(label = module.label))
      as? JvmModuleEntity.VertexDeps
    // TODO: properly handle runtime deps
    val dependencies = (module.dependencies + (vertexDepsEntity?.deps ?: emptySet()))
      .map { DependencyLabel(it) }
    val languageClasses = buildSet {
      if (module.legacyJvmData != null) {
        add(LanguageClass.JAVA)
      }
      if (module.legacyKotlinData != null) {
        add(LanguageClass.KOTLIN)
      }
    }
    val sources = module.sources
      .map {
        SourceItem(
          path = it.path,
          generated = it.generated,
          jvmPackagePrefix = it.jvmPackagePrefix,
        )
      }
    val data = when {
      module.legacyKotlinData != null -> {
        val jvmData = module.legacyJvmData ?: error("kotlin target without jvm data ${module.label}")
        convertLegacyKotlinData(module.legacyKotlinData, jvmData)
      }

      module.legacyJvmData != null -> convertLegacyJvmData(module.legacyJvmData)
      else -> null
    }
    return RawBuildTarget(
      id = module.label,
      tags = emptyList(),
      dependencies = dependencies,
      kind = TargetKind("unused", languageClasses, RuleType.LIBRARY),
      sources = sources,
      resources = module.resources,
      baseDirectory = module.baseDirectory,
      noBuild = false,
      data = data,
      lowPrioritySharedSources = listOf(),
    )
  }

  private fun convertLegacyJvmData(data: LegacyJvmTargetData): JvmBuildTarget {
    return JvmBuildTarget(
      javaHome = data.javaHome,
      javaVersion = data.javaVersion,
      javacOpts = data.javacOpts,
      binaryOutputs = data.binaryOutputs,
    )
  }

  private fun convertLegacyKotlinData(kotlinData: LegacyKotlinTargetData, jvmData: LegacyJvmTargetData): KotlinBuildTarget {
    return KotlinBuildTarget(
      languageVersion = kotlinData.languageVersion,
      apiVersion = kotlinData.apiVersion,
      kotlincOptions = kotlinData.kotlincOptions,
      associates = kotlinData.associates,
      jvmBuildTarget = convertLegacyJvmData(jvmData),
    )
  }
}
