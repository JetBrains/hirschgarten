package org.jetbrains.bazel.workspace.importer

import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntityBuilder
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.isTestTarget
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

@ApiStatus.Internal
val JAVA_SOURCE_ROOT_TYPE: SourceRootTypeId = SourceRootTypeId("java-source")

@ApiStatus.Internal
val JAVA_TEST_SOURCE_ROOT_TYPE: SourceRootTypeId = SourceRootTypeId("java-test")

@ApiStatus.Internal
val JAVA_RESOURCE_ROOT_TYPE: SourceRootTypeId = SourceRootTypeId("java-resource")

@ApiStatus.Internal
val JAVA_TEST_RESOURCE_ROOT_TYPE: SourceRootTypeId = SourceRootTypeId("java-test-resource")

// RC: replaces `SourcesItemToJavaSourceRootTransformer` + `SourceItemToSourceRootTransformer` +
// `JavaSourceEntityUpdater` + the source-side of `ContentRootEntityUpdater`;
// the `JavaSourceRoot` / `ContentRoot` / `GenericSourceRoot` wrappers are dropped
@ApiStatus.Internal
object SourceRootBuilder {
  data class ResolvedSourceRoot(
    val sourcePath: Path,
    val generated: Boolean,
    val packagePrefix: String,
    val rootType: SourceRootTypeId,
  )

  fun resolve(
    target: BuildTarget,
    testSourcesGlob: ProjectViewGlobSet,
    packagePrefixes: JvmPackagePrefixCalculator,
  ): List<ResolvedSourceRoot> {
    val prefixes = packagePrefixes.get(target)
    fun Path.convert(generated: Boolean) =
      ResolvedSourceRoot(
        sourcePath = this,
        generated = generated,
        packagePrefix = prefixes[this] ?: "",
        rootType = when {
          target.isTestTarget() -> JAVA_TEST_SOURCE_ROOT_TYPE
          testSourcesGlob.matches(this) -> JAVA_TEST_SOURCE_ROOT_TYPE
          else -> JAVA_SOURCE_ROOT_TYPE
        },
      )

    // KSP is not a compiler plugin, it uses kotlin analysis API to analyze
    // user code and expose results using KSP API then generate code (that's why it's separate bazel actions),
    // the issue is that rules_kotlin treat KSP outputs in the same way as normal compiler artifacts.
    // Correct way of representing ksp sources is to put then side-by-side to real sources
    // that allow our plugin to correctly handle things like two-way references, or access to internal members.
    val kspSrcJars = target.extractData<KotlinBuildTarget>()?.kspSourceJars ?: SourceFileCollection.EMPTY

    return (target.sources.getFiles().map { it.convert(generated = false) } +
            target.generatedSources.getFiles().map { it.convert(generated = true) }).toList() +
           kspSrcJars.getFiles().map { it.convert(generated = true) }
  }

  /**
   * writes a (ContentRootEntity, SourceRootEntity, JavaSourceRootPropertiesEntity) triple per source root.
   *
   * if [singleContentRoot] is true (the hirschgarten workspacemodel-module special case), a single content
   * root is created at the parent directory of the first source root and shared by all source roots.
   */
  fun write(
    sourceRoots: List<ResolvedSourceRoot>,
    parentModuleEntity: ModuleEntity,
    projectBasePath: Path,
    virtualFileUrlManager: VirtualFileUrlManager,
    storage: MutableEntityStorage,
  ) {
    if (sourceRoots.isEmpty()) {
      return
    }

    val entitySource = parentModuleEntity.entitySource
    val contentRootEntities = sourceRoots.groupBy { root ->
      if (BazelFeatureFlags.mergeSourceRoots || // sources were merged earlier, now we just need to create a source root for each path
          root.sourcePath.parent == projectBasePath) { // don't create a content root for project root dir to avoid excessive indexing
        root.sourcePath
      }
      else root.sourcePath.parent
    }.map { (commonParentDir, sourceRoots) ->
      ContentRootEntity(
        url = commonParentDir.toResolvedVirtualFileUrl(virtualFileUrlManager),
        excludedPatterns = emptyList(),
        entitySource = entitySource,
      ) {
        this.sourceRoots = sourceRoots.map { sourceRootEntity(it, entitySource, virtualFileUrlManager) }
      }
    }

    storage.modifyModuleEntity(parentModuleEntity) {
      contentRoots += contentRootEntities
    }
  }

  private fun sourceRootEntity(
    sourceRoot: ResolvedSourceRoot,
    entitySource: EntitySource,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): SourceRootEntityBuilder =
    SourceRootEntity(
      url = sourceRoot.sourcePath.toJarUrlString().toResolvedVirtualFileUrl(virtualFileUrlManager),
      rootTypeId = sourceRoot.rootType,
      entitySource = entitySource,
    ) {
      this.javaSourceRoots = listOf(
        JavaSourceRootPropertiesEntity(
          generated = sourceRoot.generated,
          packagePrefix = sourceRoot.packagePrefix,
          entitySource = entitySource,
        ),
      )
    }
}
