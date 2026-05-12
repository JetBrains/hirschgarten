package org.jetbrains.bazel.workspace.importer

import com.intellij.compiler.impl.javaCompiler.javac.JavacConfiguration
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.entities
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelJavaBackendBundle
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.TargetIdToModuleEntitiesMap
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.WorkspaceModelUpdater
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.CompiledSourceCodeInsideJarExcludeTransformer
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.environment.bazelProjectName
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import org.jetbrains.bazel.sync.workspace.languages.java.JavaWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.DefaultJvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import java.nio.file.Path
import kotlin.collections.first
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString

// TODO: fix this JDK table mess, I'm sure this can be done in better way,
//  wait if we have multiple JDKs in single workspace for some case???
// RC: for now this just wraps 'purified' MMM
internal class JavaBazelWorkspaceImporter : BazelWorkspaceImporter, BazelWorkspaceImporter.Named {
  private var javacOptions: Map<String, String>? = null
  private lateinit var projectDetails: ProjectDetails
  private lateinit var uniqueJavaHomes: Set<Path>
  private lateinit var commonSyncConfig: CommonWorkspaceSyncConfig
  private lateinit var javaSyncConfig: JavaWorkspaceSyncConfig

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
    // TODO: move is somewhere else
    if (!context.project.projectCtx.avoidExternalSystem) {
      // store generated IML files outside the project directory
      ExternalProjectsManagerImpl.getInstance(context.project).setStoreExternally(true)
    }

    commonSyncConfig = snapshot.syncConfigs.filterIsInstance<CommonWorkspaceSyncConfig>()
                         .firstOrNull() ?: throw IllegalStateException()

    javaSyncConfig = snapshot.syncConfigs.filterIsInstance<JavaWorkspaceSyncConfig>()
                       .firstOrNull() ?: throw IllegalStateException()

    val targets = snapshot.targets.map { (_, target) -> target.rawBuildTarget }

    // init project details
    projectDetails = ProjectDetails(
      targetIds = targets.map { it.id },
      targets = targets.toSet(),
      libraries = targets.mapNotNull { extractJvmBuildTarget(it) }
        .flatMap { it.libraries }
        .distinctBy { it.id },
    )

    // TODO: check why is this even needed, cannot I just a SdkEntity to project workspace model
    //  and avoid global global JDK table at all?
    // compute JDKs
    uniqueJavaHomes = projectDetails.targets
      .mapNotNull(::extractJvmBuildTarget)
      .map { requireNotNull(it.javaHome) { "javaHome is null but expected not to be null for $it" } }
      .toSet()
    uniqueJavaHomes.also {
      if (it.isNotEmpty()) {
        projectDetails.defaultJdkName = context.project.bazelProjectName.projectNameToJdkName(it.first())
      }
      else {
        projectDetails.defaultJdkName = SdkUtils.getProjectJdkOrMostRecentJdk(context.project)?.name
      }
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
      updateInternalModelSubtask(context, projectDetails, snapshot, builder, entitySource)
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
    projectDetails: ProjectDetails,
    snapshot: WorkspaceSnapshot,
    builder: MutableEntityStorage,
    entitySource: EntitySource,
  ) = coroutineScope {
    val libraries = projectDetails.libraries.map { Library.fromLibraryItem(snapshot.repoMapping, it, context.project) }

    val targetIdToModuleDetails =
      bspTracer.spanBuilder("create.module.details.ms").use {
        val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails)
        projectDetails.targetIds.associateWith { transformer.moduleDetailsForTargetId(it) }
      }

    val jvmPackagePrefixes = DefaultJvmPackagePrefixCalculator(
      sourceRootOptimizationMode = javaSyncConfig.sourceRootOptimizationMode,
    ).also {
      it.calculate(projectDetails.targets)
    }

    val targetIdToModuleEntitiesMap =
      bspTracer.spanBuilder("create.target.id.to.module.entities.map.ms").use {
        val targetIdToTargetInfo =
          (projectDetails.targets).associateBy { it.id }
        val targetIdToModuleEntityMap =
          TargetIdToModuleEntitiesMap(
            projectDetails = projectDetails,
            targetIdToModuleDetails = targetIdToModuleDetails,
            targetIdToTargetInfo = targetIdToTargetInfo,
            // TODO: remove usage, https://youtrack.jetbrains.com/issue/BAZEL-2015
            // MAYBE RC: why is it needed?
            fileToTargets = snapshot.fileToTarget.mapValues { (_, v) -> v.map { it.label } },
            projectBasePath = commonSyncConfig.projectRootDir,
            repoMapping = snapshot.repoMapping,
            projectName = commonSyncConfig.projectName,
            testSourcesGlob = ProjectViewGlobSet(commonSyncConfig.projectRootDir, javaSyncConfig.testSourcesPatterns),
            packagePrefixes = jvmPackagePrefixes,
          )
        targetIdToModuleEntityMap
      }

    val modulesToLoad = targetIdToModuleEntitiesMap.values.flatten().distinctBy { module -> module.getModuleName() }

    val compiledSourceCodeInsideJarToExclude =
      bspTracer.spanBuilder("calculate.non.generated.class.files.to.exclude").use {
        if (BazelFeatureFlags.excludeCompiledSourceCodeInsideJars) {
          CompiledSourceCodeInsideJarExcludeTransformer().transform(
            targetIdToModuleDetails.values,
            projectDetails.libraries,
            packagePrefixes = jvmPackagePrefixes,
          )
        }
        else {
          null
        }
      }

    bspTracer.spanBuilder("load.modules.ms").use {
      val workspaceModelUpdater =
        WorkspaceModelUpdater(
          workspaceEntityStorageBuilder = builder,
          virtualFileUrlManager = context.vfuManager,
          projectBasePath = commonSyncConfig.projectRootDir,
          importIjars = javaSyncConfig.importIjars,
          entitySource = entitySource,
          currentExcludeEntity = context.currentSnapshot
            .entities<CompiledSourceCodeInsideJarExcludeEntity>()
            .firstOrNull(),
        )

      workspaceModelUpdater.load(modulesToLoad, libraries)
      compiledSourceCodeInsideJarToExclude?.let { workspaceModelUpdater.loadCompiledSourceCodeInsideJarExclude(it) }
      javacOptions = calculateAllJavacOptions(modulesToLoad)
    }
  }

  private fun calculateAllJavacOptions(modulesToLoad: List<Module>): HashMap<String, String> {
    val javacOptions = HashMap<String, String>()
    for (module in modulesToLoad) {
      if (module is JavaModule) {
        val options = module.javaAddendum?.javacOptions
        if (options != null && options.isNotEmpty()) {
          if (options.size == 1 && options[0] == "-proc:none") {
            continue
          }
          javacOptions[module.getModuleName()] = options.joinToString(" ")
        }
      }
    }
    return javacOptions
  }
}
