package org.jetbrains.bazel.workspace.importer

import com.intellij.compiler.impl.javaCompiler.javac.JavacConfiguration
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.entities
import org.jetbrains.bazel.config.BazelJavaBackendBundle
import org.jetbrains.bazel.config.bazelProjectName
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import org.jetbrains.bazel.sync.workspace.languages.java.JavaWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.DefaultJvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.jvm.extractJvmBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

internal class JavaBazelWorkspaceImporter : BazelWorkspaceImporter, BazelWorkspaceImporter.Named {
  private var javacOptions: Map<String, String>? = null
  private lateinit var moduleTargets: List<WorkspaceTarget>
  private lateinit var targets: List<RawBuildTarget>
  private lateinit var jvmResolved: Map<WorkspaceTargetKey, JvmResolvedTarget>
  private lateinit var uniqueJavaHomes: Set<Path>
  private lateinit var commonSyncConfig: CommonWorkspaceSyncConfig
  private lateinit var javaSyncConfig: JavaWorkspaceSyncConfig
  private var defaultJdkName: String? = null

  override val importerName: @NlsContexts.ProgressTitle String
    get() = BazelJavaBackendBundle.message("workspace.java.importer.name")

  override suspend fun import(
    context: WorkspaceImporterContext,
    phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): Result<WorkspaceImporterResult> = runCatching {
    when (phase) {
      WorkspaceImporterPhase.Initialize -> onInitialize(context, snapshot)
      is WorkspaceImporterPhase.WorkspaceApply -> onWorkspaceApply(context, snapshot, phase.builder, phase.entitySource)
      WorkspaceImporterPhase.Finalize -> onFinalize(context, snapshot)
      WorkspaceImporterPhase.PostProcessing -> onPostProcessing(context, snapshot)
    }
  }

  fun onInitialize(context: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): WorkspaceImporterResult {
    if (!context.project.projectCtx.avoidExternalSystem) {
      // store generated IML files outside the project directory
      ExternalProjectsManagerImpl.getInstance(context.project).setStoreExternally(true)
    }

    commonSyncConfig = snapshot.syncConfigs.filterIsInstance<CommonWorkspaceSyncConfig>()
                         .firstOrNull() ?: throw IllegalStateException()
    javaSyncConfig = snapshot.syncConfigs.filterIsInstance<JavaWorkspaceSyncConfig>()
                       .firstOrNull() ?: throw IllegalStateException()

    moduleTargets = snapshot.targetGraph.findAllTargetsAtDepth(
      maxDepth = commonSyncConfig.importDepth,
      useRelaxedDependencyExpansion = true,
    )
    targets = moduleTargets.map { it.rawBuildTarget }
    jvmResolved = JvmBuildTargetResolver(
      allTargets = snapshot.targets,
      targetsToImport = moduleTargets.associateBy { it.targetKey },
      javaSyncConfig = snapshot.syncConfigs.filterIsInstance<JavaWorkspaceSyncConfig>().first(),
    ).resolveAll()

    // TODO: check why is this even needed - can't we just write SdkEntity into the project workspace model
    //  and avoid the global JDK table altogether?
    uniqueJavaHomes = jvmResolved.values.mapNotNull { it.javaHome }.toSet()
    defaultJdkName = if (uniqueJavaHomes.isNotEmpty()) {
      context.project.bazelProjectName.projectNameToJdkName(uniqueJavaHomes.first())
    }
    else {
      SdkUtils.getProjectJdkOrMostRecentJdk(context.project)?.name
    }
    return WorkspaceImporterResult.Success
  }

  suspend fun onWorkspaceApply(
    context: WorkspaceImporterContext, snapshot: WorkspaceSnapshot,
    builder: MutableEntityStorage, entitySource: EntitySource,
  ): WorkspaceImporterResult {
    context.taskConsole.withSubtask(
      context.taskId.subTask("update-internal-model"),
      BazelJavaBackendBundle.message("workspace.java.importer.update.internal.model"),
    ) {
      updateInternalModelSubtask(context, snapshot, builder, entitySource)
    }
    return WorkspaceImporterResult.Success
  }

  fun onFinalize(context: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): WorkspaceImporterResult {
    return WorkspaceImporterResult.Success
  }

  suspend fun onPostProcessing(context: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): WorkspaceImporterResult {
    SdkUtils.cleanUpInvalidJdks(context.project)
    uniqueJavaHomes.forEach {
      SdkUtils.addJdkIfNeeded(
        projectName = context.project.bazelProjectName,
        javaHome = it,
      )
    }

    JavacConfiguration.getOptions(context.project, JavacConfiguration::class.java).ADDITIONAL_OPTIONS_OVERRIDE =
      requireNotNull(this.javacOptions) {
        "javacOptions is null but expected to be computed"
      }
    return WorkspaceImporterResult.Success
  }

  private suspend fun updateInternalModelSubtask(
    context: WorkspaceImporterContext,
    snapshot: WorkspaceSnapshot,
    builder: MutableEntityStorage,
    entitySource: EntitySource,
  ) {
    val packagePrefixes = DefaultJvmPackagePrefixCalculator(
      sourceRootOptimizationMode = javaSyncConfig.sourceRootOptimizationMode,
    ).also { it.calculate(targets) }

    val testTargets = TestTargetClassifier.calculateTargetsToMarkAsTest(targets)

    val importContext = ImportContext(
      targets = moduleTargets,
      jvmResolved = jvmResolved,
      repoMapping = snapshot.repoMapping,
      projectName = commonSyncConfig.projectName,
      projectBasePath = commonSyncConfig.projectRootDir,
      defaultJdkName = defaultJdkName,
      testSourcesGlob = ProjectViewGlobSet(commonSyncConfig.projectRootDir, javaSyncConfig.testSourcesPatterns),
      testTargets = testTargets,
      packagePrefixes = packagePrefixes,
      fileToTargets = snapshot.fileToTarget,
      virtualFileUrlManager = context.vfuManager,
      importIJars = javaSyncConfig.importIjars,
      entitySource = entitySource,
      excludeCompiledSourceCodeInsideJars = javaSyncConfig.excludeCompiledSourceCodeInsideJars,
      currentCompiledSourceExcludeEntity = context.currentSnapshot
        .entities<CompiledSourceCodeInsideJarExcludeEntity>()
        .firstOrNull(),
    )

    bspTracer.spanBuilder("load.modules.ms").use {
      JvmTargetEntitiesBuilder(importContext).writeAll(builder)
    }

    javacOptions = calculateAllJavacOptions(snapshot)
  }

  private fun calculateAllJavacOptions(snapshot: WorkspaceSnapshot): HashMap<String, String> {
    val result = HashMap<String, String>()
    for (target in targets) {
      val jvm = extractJvmBuildTarget(target) ?: continue
      val options = jvm.javacOpts
      if (options.isEmpty()) {
        continue
      }
      if (options.size == 1 && options[0] == "-proc:none") {
        continue
      }
      result[target.id.formatAsModuleName(snapshot.repoMapping)] = options.joinToString(" ")
    }
    return result
  }
}
