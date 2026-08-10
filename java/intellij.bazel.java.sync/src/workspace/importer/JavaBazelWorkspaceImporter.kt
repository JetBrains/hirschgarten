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
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContextBuilder
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import org.jetbrains.bazel.sync.workspace.languages.java.JavaWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.DefaultJvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.jvm.extractJvmBuildTarget
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.id
import java.nio.file.Path

internal class JavaBazelWorkspaceImporter : BazelWorkspaceImporter, BazelWorkspaceImporter.Named {
  private var javacOptions: Map<String, String>? = null
  private lateinit var moduleTargets: List<BuildTarget>
  private lateinit var targets: List<BuildTarget>
  private lateinit var jvmResolved: Map<WorkspaceTargetKey, JvmResolvedTarget>
  private lateinit var plan: JvmImportPlan
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
      is WorkspaceImporterPhase.Initialize -> onInitialize(context, snapshot, phase.naming)
      is WorkspaceImporterPhase.WorkspaceApply -> onWorkspaceApply(context, snapshot, phase.builder, phase.entitySource, phase.naming)
      WorkspaceImporterPhase.Finalize -> onFinalize(context, snapshot)
      WorkspaceImporterPhase.PostProcessing -> onPostProcessing(context, snapshot)
    }
  }

  fun onInitialize(
    context: WorkspaceImporterContext,
    snapshot: WorkspaceSnapshot,
    naming: GlobalNamingContextBuilder,
  ): WorkspaceImporterResult {
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
    ).mapNotNull { it.load(snapshot.targets, TargetLoadOptions.ALL) }.toList()
    targets = moduleTargets
    jvmResolved = JvmBuildTargetResolver(
      allTargets = snapshot.targets.allTargets().associateBy { it.key },
      targetsToImport = moduleTargets.associateBy { it.key },
      javaSyncConfig = snapshot.syncConfigs.filterIsInstance<JavaWorkspaceSyncConfig>().first(),
    ).resolveAll()

    plan = JvmImportPlan(rawTargets = moduleTargets, jvmResolved = jvmResolved)
    plan.declareNames(naming)

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
    naming: GlobalNamingContext,
  ): WorkspaceImporterResult {
    context.taskConsole.withSubtask(
      context.taskId.subTask("update-internal-model"),
      BazelJavaBackendBundle.message("workspace.java.importer.update.internal.model"),
    ) {
      updateInternalModelSubtask(context, snapshot, builder, entitySource, naming)
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
    naming: GlobalNamingContext,
  ) {
    val packagePrefixes = DefaultJvmPackagePrefixCalculator(
      sourceRootOptimizationMode = javaSyncConfig.sourceRootOptimizationMode,
    ).also { it.calculate(targets) }

    val importContext = ImportContext(
      plan = plan,
      naming = naming,
      jvmResolved = jvmResolved,
      projectName = commonSyncConfig.projectName,
      projectBasePath = commonSyncConfig.projectRootDir,
      defaultJdkName = defaultJdkName,
      testSourcesGlob = ProjectViewGlobSet(commonSyncConfig.projectRootDir, javaSyncConfig.testSourcesPatterns),
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

    javacOptions = calculateAllJavacOptions(importContext)
  }

  private fun calculateAllJavacOptions(ctx: ImportContext): HashMap<String, String> {
    val result = HashMap<String, String>()
    for (target in ctx.targets) {
      val jvm = extractJvmBuildTarget(target) ?: continue
      val options = jvm.javacOpts
      if (options.isEmpty()) {
        continue
      }
      if (options.size == 1 && options[0] == "-proc:none") {
        continue
      }
      val moduleName = ctx.moduleNamesByKey[target.key] ?: continue
      result[moduleName] = options.joinToString(" ")
    }
    return result
  }
}
