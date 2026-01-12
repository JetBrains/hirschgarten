package org.jetbrains.bazel.sync_new.flow.index.target_utils

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetTag
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex
import org.jetbrains.bazel.sync_new.graph.impl.resolve
import org.jetbrains.bazel.sync_new.index.SyncIndexUpdater
import org.jetbrains.bazel.sync_new.lang.getLangData
import org.jetbrains.bazel.sync_new.lang.hasLang
import org.jetbrains.bazel.sync_new.languages_impl.jvm.JvmSyncLanguage
import org.jetbrains.bazel.sync_new.languages_impl.kotlin.KotlinSyncLanguage
import org.jetbrains.bazel.sync_new.storage.DefaultStorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.hash.putLabel
import org.jetbrains.bazel.sync_new.storage.storageContext
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.PartialBuildTarget

@Service(Service.Level.PROJECT)
class TargetUtilsIndexService(
  private val project: Project,
) : SyncIndexUpdater {

  private val myGlobalTargetStorage =
    project.storageContext.createFlatStore<GlobalTargetStorage>("bazel.sync.index.targetUtils.globalStorage", DefaultStorageHints.USE_IN_MEMORY)
      .withCreator { GlobalTargetStorage() }
      .withCodec { ofKryo() }
      .build()

  private val myLegacyTargetStorage =
    project.storageContext.createKVStore<HashValue128, LegacyBuildTarget>(
      "bazel.sync.index.targetUtils.targetStorage",
      DefaultStorageHints.USE_PAGED_STORE,
    )
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofKryo() }
      .build()

  // TODO: profile it, when take too much time - make it incremental
  override suspend fun updateIndexes(ctx: SyncContext, diff: SyncDiff) {
    val allTargets = mutableListOf<Label>()
    val allTargetsLabels = mutableListOf<String>()
    val allExecutableTargetsLabels = mutableListOf<String>()
    for (compact in ctx.graph.getAllVertexCompacts()) {
      allTargets.add(compact.label)
      allTargetsLabels.add(compact.label.toShortString(ctx.project))
      if (compact.isExecutable) {
        allExecutableTargetsLabels.add(compact.label.toShortString(ctx.project))
      }
    }
    myGlobalTargetStorage.modify {
      GlobalTargetStorage(
        allTargets = allTargets,
        allTargetsLabels = allTargetsLabels,
        allExecutableTargetsLabels = allExecutableTargetsLabels,
      )
    }
    updateLegacyTargets(ctx, diff)
  }

  private fun updateLegacyTargets(ctx: SyncContext, diff: SyncDiff) {
    val (changed, removed) = diff.split
    for (removed in removed) {
      myLegacyTargetStorage.remove(hash { putLabel(removed.label) })
    }
    for (changed in changed) {
      val target = changed.getBuildTarget() ?: continue
      val legacyTarget = createLegacyBuildTarget(ctx, target)
      myLegacyTargetStorage.put(hash { putLabel(target.label) }, legacyTarget)
    }
  }

  private fun createLegacyBuildTarget(ctx: SyncContext, vertex: BazelTargetVertex): LegacyBuildTarget {
    return LegacyBuildTarget(
      label = vertex.label,
      tags = buildList {
        val tags = vertex.genericData.tags
        if (BazelTargetTag.NO_IDE in tags) {
          add(LegacyBuildTargetTags.NO_IDE)
        }
        if (BazelTargetTag.MANUAL in tags) {
          add(LegacyBuildTargetTags.MANUAL)
        }
        if (BazelTargetTag.INTELLIJ_PLUGIN in tags) {
          add(LegacyBuildTargetTags.INTELLIJ_PLUGIN)
        }
      },
      targetKind = LegacyTargetKind(
        kind = vertex.kind,
        languageClasses = buildSet {
          if (JvmSyncLanguage.hasLang(vertex)) {
            add(LegacyLanguageClass.JAVA)
          }
          if (KotlinSyncLanguage.hasLang(vertex)) {
            add(LegacyLanguageClass.KOTLIN)
          }
        },
        ruleType = when {
          BazelTargetTag.TEST in vertex.genericData.tags -> LegacyRuleType.TEST
          BazelTargetTag.EXECUTABLE in vertex.genericData.tags -> LegacyRuleType.BINARY
          BazelTargetTag.LIBRARY in vertex.genericData.tags -> LegacyRuleType.LIBRARY
          else -> LegacyRuleType.LIBRARY
        },
      ),
      baseDirectory = vertex.baseDirectory,
      data = createLegacyTargetData(ctx, vertex),
      noBuild = BazelTargetTag.NO_BUILD in vertex.genericData.tags,
    )
  }

  private fun createLegacyTargetData(ctx: SyncContext, vertex: BazelTargetVertex): LegacyBuildTargetData? {
    val jvmData = JvmSyncLanguage.getLangData(vertex)?.let {
      LegacyJvmTargetData(
        javaHome = it.compilerOptions.javaHome ?: it.toolchain?.javaHome,
        javaVersion = it.compilerOptions.javaVersion.orEmpty(),
        javacOptions = it.compilerOptions.javacOpts,
        binaryOutputs = (it.outputs.classJars + it.generatedOutputs.classJars + it.outputs.iJars + it.generatedOutputs.iJars)
          .map { ctx.pathsResolver.resolve(it) }
          .toMutableList(),
      )
    }
    val kotlinData = KotlinSyncLanguage.getLangData(vertex)
    return when {
      kotlinData != null -> {
        LegacyKotlinTargetData(
          languageVersion = kotlinData.languageVersion,
          apiVersion = kotlinData.apiVersion,
          kotlincOptions = kotlinData.kotlincOptions,
          associates = kotlinData.associates,
          jvmBuildTarget = jvmData,
        )
      }

      else -> jvmData
    }
  }

  private fun LegacyBuildTarget.toBspTarget(): PartialBuildTarget {
    return PartialBuildTarget(
      id = this.label,
      tags = this.tags,
      kind = TargetKind(
        kindString = this.targetKind.kind,
        languageClasses = this.targetKind.languageClasses
          .map { it.toBspLanguageClass() }
          .toSet(),
        ruleType = this.targetKind.ruleType.toBspRuleType(),
      ),
      baseDirectory = this.baseDirectory,
      data = this.data?.toBsp(),
      noBuild = this.noBuild
    )
  }

  private fun LegacyBuildTargetData.toBsp(): BuildTargetData {
    return when (this) {
      is LegacyJvmTargetData -> JvmBuildTarget(
        javaHome = this.javaHome,
        javaVersion = this.javaVersion,
        javacOpts = this.javacOptions,
        binaryOutputs = this.binaryOutputs,
      )
      is LegacyKotlinTargetData -> KotlinBuildTarget(
        languageVersion = this.languageVersion,
        apiVersion = this.apiVersion,
        kotlincOptions = this.kotlincOptions,
        associates = this.associates,
        jvmBuildTarget = this.jvmBuildTarget?.toBsp() as? JvmBuildTarget?,
      )
    }
  }

  private fun LegacyLanguageClass.toBspLanguageClass(): LanguageClass {
    return when (this) {
      LegacyLanguageClass.JAVA -> LanguageClass.JAVA
      LegacyLanguageClass.KOTLIN -> LanguageClass.KOTLIN
    }
  }

  private fun LegacyRuleType.toBspRuleType(): RuleType {
    return when (this) {
      LegacyRuleType.TEST -> RuleType.TEST
      LegacyRuleType.BINARY -> RuleType.BINARY
      LegacyRuleType.LIBRARY -> RuleType.LIBRARY
      else -> RuleType.UNKNOWN
    }
  }

  fun getAllTargets(): List<Label> = myGlobalTargetStorage.get().allTargets
  fun getAllTargetsLabels(): List<String> = myGlobalTargetStorage.get().allTargetsLabels
  fun getAllExecutableTargetsLabels(): List<String> = myGlobalTargetStorage.get().allExecutableTargetsLabels

  internal fun getLegacyBuildTarget(label: Label): LegacyBuildTarget? = myLegacyTargetStorage[hash { putLabel(label) }]
  fun getBspBuildTarget(label: Label): PartialBuildTarget? = getLegacyBuildTarget(label)?.toBspTarget()

}

