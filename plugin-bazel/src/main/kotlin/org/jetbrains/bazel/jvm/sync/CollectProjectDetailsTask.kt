package org.jetbrains.bazel.jvm.sync

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.codeInsight.multiverse.isSharedSourceSupportEnabled
import com.intellij.compiler.impl.javaCompiler.javac.JavacConfiguration
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.util.PathUtilRt
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.bazelProjectName
import org.jetbrains.bazel.config.defaultJdkName
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.TargetIdToModuleEntitiesMap
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.WorkspaceModelUpdater
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.CompiledSourceCodeInsideJarExcludeTransformer
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.LibraryGraph
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.scalaVersionToScalaSdkName
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.scala.sdk.ScalaSdk
import org.jetbrains.bazel.scala.sdk.scalaSdkExtension
import org.jetbrains.bazel.scala.sdk.scalaSdkExtensionExists
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.Module
import org.jetbrains.bazel.server.client.IMPORT_SUBTASK_ID
import org.jetbrains.bazel.sync.scope.FullProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.target.sync.projectStructure.TargetUtilsProjectStructureDiff
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier
import org.jetbrains.bazel.utils.SourceType
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractScalaBuildTarget
import java.nio.file.Path
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

class CollectProjectDetailsTask(
  private val project: Project,
  private val taskId: String,
  private val diff: MutableEntityStorage,
  private val targetUtilsDiff: TargetUtilsProjectStructureDiff,
) {
  private var uniqueJavaHomes: Set<Path>? = null

  private var javacOptions: Map<String, String>? = null

  private var scalaSdks: Set<ScalaSdk>? = null

  suspend fun execute(
    project: Project,
    server: JoinedBuildServer,
    syncScope: ProjectSyncScope,
    progressReporter: SequentialProgressReporter,
  ) {
    val projectDetails =
      progressReporter.sizedStep(workSize = 50, text = BazelPluginBundle.message("progress.bar.collect.project.details")) {
        collectModel(project, server, syncScope)
      }

    progressReporter.indeterminateStep(text = BazelPluginBundle.message("progress.bar.calculate.jdk.infos")) {
      calculateAllUniqueJdkInfosSubtask(projectDetails)
      uniqueJavaHomes.orEmpty().also {
        if (it.isNotEmpty()) {
          projectDetails.defaultJdkName = project.bazelProjectName.projectNameToJdkName(it.first())
        } else {
          projectDetails.defaultJdkName = SdkUtils.getProjectJdkOrMostRecentJdk(project)?.name
        }
      }
      project.defaultJdkName = projectDetails.defaultJdkName
    }

    if (scalaSdkExtensionExists()) {
      progressReporter.indeterminateStep(text = BazelPluginBundle.message("progress.reporter.calculating.scala.sdk.info")) {
        calculateAllScalaSdkInfosSubtask(projectDetails)
      }
    }

    progressReporter.sizedStep(workSize = 25, text = BazelPluginBundle.message("progress.bar.update.internal.model")) {
      updateInternalModelSubtask(projectDetails, syncScope)
    }
  }

  private suspend fun collectModel(
    project: Project,
    server: JoinedBuildServer,
    syncScope: ProjectSyncScope,
  ): ProjectDetails =
    try {
      project.syncConsole.startSubtask(
        this.taskId,
        IMPORT_SUBTASK_ID,
        BazelPluginBundle.message("console.task.model.collect"),
      )

      val projectDetails =
        calculateProjectDetailsWithCapabilities(
          project = project,
          server = server,
          syncScope = syncScope,
          taskId = taskId,
        )

      project.syncConsole.finishSubtask(IMPORT_SUBTASK_ID)

      projectDetails
    } catch (e: Exception) {
      if (e is CancellationException) {
        project.syncConsole.finishSubtask(
          IMPORT_SUBTASK_ID,
          null,
          FailureResultImpl(),
        )
      } else {
        project.syncConsole.finishSubtask(
          IMPORT_SUBTASK_ID,
          null,
          FailureResultImpl(e),
        )
      }
      throw e
    }

  private suspend fun calculateAllUniqueJdkInfosSubtask(projectDetails: ProjectDetails) =
    project.syncConsole.withSubtask(
      taskId = taskId,
      subtaskId = "calculate-all-unique-jdk-infos",
      message = BazelPluginBundle.message("console.task.model.calculate.jdks.infos"),
    ) {
      uniqueJavaHomes =
        bspTracer.spanBuilder("calculate.all.unique.jdk.infos.ms").use {
          calculateAllUniqueJavaHomes(projectDetails)
        }
    }

  private fun calculateAllUniqueJavaHomes(projectDetails: ProjectDetails): Set<Path> =
    projectDetails.targets
      .mapNotNull(::extractJvmBuildTarget)
      .map { requireNotNull(it.javaHome) { "javaHome is null but expected not to be null for $it" } }
      .toSet()

  private suspend fun calculateAllScalaSdkInfosSubtask(projectDetails: ProjectDetails) =
    project.syncConsole.withSubtask(
      taskId = taskId,
      subtaskId = "calculate-all-scala-sdk-infos",
      message = BazelPluginBundle.message("console.task.model.calculate.scala.sdk.infos"),
    ) {
      scalaSdks =
        bspTracer.spanBuilder("calculate.all.scala.sdk.infos.ms").use {
          calculateAllScalaSdkInfos(projectDetails)
        }
    }

  private fun calculateAllScalaSdkInfos(projectDetails: ProjectDetails): Set<ScalaSdk> =
    projectDetails.targets
      .mapNotNull {
        createScalaSdk(it)
      }.toSet()

  private fun createScalaSdk(target: BuildTarget): ScalaSdk? =
    extractScalaBuildTarget(target)
      ?.let { scalaBuildTarget ->
        ScalaSdk(
          name = scalaBuildTarget.scalaVersion.scalaVersionToScalaSdkName(),
          scalaVersion = scalaBuildTarget.scalaVersion,
          sdkJars = scalaBuildTarget.sdkJars.map { path -> path.toUri() },
        )
      }

  private suspend fun updateInternalModelSubtask(projectDetails: ProjectDetails, syncScope: ProjectSyncScope) {
    project.syncConsole.withSubtask(
      taskId,
      "calculate-project-structure",
      BazelPluginBundle.message("console.task.model.calculate.structure"),
    ) {
      coroutineScope {
        val projectBasePath = project.rootDir.toNioPath()
        val libraryGraph = LibraryGraph(projectDetails.libraries.orEmpty())

        val libraries =
          bspTracer.spanBuilder("create.libraries.ms").use {
            libraryGraph.createLibraries(project)
          }

        val libraryModules =
          bspTracer.spanBuilder("create.library.modules.ms").use {
            libraryGraph.createLibraryModules(project, projectDetails.defaultJdkName)
          }

        val targetIdToModuleDetails =
          bspTracer.spanBuilder("create.module.details.ms").use {
            val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, libraryGraph)
            projectDetails.targetIds.associateWith { transformer.moduleDetailsForTargetId(it) }
          }

        val targetIdToModuleEntitiesMap =
          bspTracer.spanBuilder("create.target.id.to.module.entities.map.ms").use {
            val syncedTargetIdToTargetInfo =
              (projectDetails.targets).associateBy { it.id }

            val targetIdToTargetInfo =
              if (syncScope is FullProjectSync) {
                syncedTargetIdToTargetInfo
              } else {
                project.targetUtils.computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo)
              }
            val targetIdToModuleEntityMap =
              TargetIdToModuleEntitiesMap(
                projectDetails = projectDetails,
                targetIdToModuleDetails = targetIdToModuleDetails,
                targetIdToTargetInfo = targetIdToTargetInfo,
                // TODO: remove usage, https://youtrack.jetbrains.com/issue/BAZEL-2015
                fileToTargetWithoutLowPrioritySharedSources = targetUtilsDiff.fileToTargetWithoutLowPrioritySharedSources,
                projectBasePath = projectBasePath,
                project = project,
              )
            targetIdToModuleEntityMap
          }

        // TODO: remove this: https://youtrack.jetbrains.com/issue/BAZEL-2015/
        targetUtilsDiff.libraryItems = projectDetails.libraries

        val modulesToLoad = targetIdToModuleEntitiesMap.values.flatten().distinctBy { module -> module.getModuleName() }

        val compiledSourceCodeInsideJarToExclude =
          bspTracer.spanBuilder("calculate.non.generated.class.files.to.exclude").use {
            if (BazelFeatureFlags.excludeCompiledSourceCodeInsideJars) {
              CompiledSourceCodeInsideJarExcludeTransformer().transform(
                targetIdToModuleDetails.values,
                projectDetails.libraries.orEmpty(),
              )
            } else {
              null
            }
          }

        bspTracer.spanBuilder("load.modules.ms").use {
          val workspaceModel = project.serviceAsync<WorkspaceModel>()
          val virtualFileUrlManager = workspaceModel.getVirtualFileUrlManager()

          val workspaceModelUpdater =
            WorkspaceModelUpdater(
              workspaceEntityStorageBuilder = diff,
              virtualFileUrlManager = virtualFileUrlManager,
              projectBasePath = projectBasePath,
              project = project,
              importIjars = projectDetails.workspaceContext?.importIjars ?: false,
            )

          workspaceModelUpdater.load(modulesToLoad, libraries, libraryModules)
          compiledSourceCodeInsideJarToExclude?.let { workspaceModelUpdater.loadCompiledSourceCodeInsideJarExclude(it) }
          javacOptions = calculateAllJavacOptions(modulesToLoad)
        }
      }
    }
  }

  suspend fun postprocessingSubtask(targetUtilsDiff: TargetUtilsProjectStructureDiff) {
    // This order is strict as now SDKs also use the workspace model,
    // updating jdks before applying the project model will render the action to fail.
    // This will be handled properly after this ticket:
    // https://youtrack.jetbrains.com/issue/BAZEL-426/Configure-JDK-using-workspace-model-API-instead-of-ProjectJdkTable
    SdkUtils.cleanUpInvalidJdks(project)
    addBspFetchedJdks()
    JavacConfiguration.getOptions(project, JavacConfiguration::class.java).ADDITIONAL_OPTIONS_OVERRIDE =
      requireNotNull(this.javacOptions) {
        "javacOptions is null but expected to be computed"
      }
    addBspFetchedScalaSdks()

    VirtualFileManager.getInstance().asyncRefresh()
    checkSharedSources(targetUtilsDiff.fileToTargetWithoutLowPrioritySharedSources)
  }

  private suspend fun addBspFetchedJdks() =
    project.syncConsole.withSubtask(
      taskId,
      "add-bsp-fetched-jdks",
      BazelPluginBundle.message("console.task.model.add.fetched.jdks"),
    ) {
      bspTracer.spanBuilder("add.bsp.fetched.jdks.ms").useWithScope {
        uniqueJavaHomes?.forEach {
          SdkUtils.addJdkIfNeeded(
            projectName = project.bazelProjectName,
            javaHome = it,
          )
        }
      }
    }

  private suspend fun addBspFetchedScalaSdks() {
    scalaSdkExtension()?.let { extension ->
      project.syncConsole.withSubtask(
        taskId,
        "add-bsp-fetched-scala-sdks",
        BazelPluginBundle.message("console.task.model.add.scala.fetched.sdks"),
      ) {
        val modifiableProvider = IdeModifiableModelsProviderImpl(project)

        writeAction {
          scalaSdks?.forEach { extension.addScalaSdk(it, modifiableProvider) }
          modifiableProvider.commit()
        }
      }
    }
  }

  private fun checkSharedSources(fileToTargetWithoutLowPrioritySharedSources: Map<Path, List<Label>>) {
    if (isSharedSourceSupportEnabled(project)) return
    if (!BazelFeatureFlags.checkSharedSources) return
    for ((file, labels) in fileToTargetWithoutLowPrioritySharedSources) {
      if (labels.size <= 1) {
        continue
      }

      val fileName = PathUtilRt.getFileName(file.toString())
      if (!SourceType.hasSourceFileExtension(fileName)) {
        continue
      }
      if (IGNORED_NAMES_FOR_OVERLAPPING_SOURCES.any { fileName.endsWith(it) }) {
        continue
      }
      warnOverlappingSources(firstTarget = labels[0], secondTarget = labels[1], fileName = fileName)
      break
    }
  }

  private fun warnOverlappingSources(
    firstTarget: Label,
    secondTarget: Label,
    fileName: String,
  ) {
    BazelBalloonNotifier.warn(
      BazelPluginBundle.message("widget.collect.targets.overlapping.sources.title"),
      BazelPluginBundle.message(
        "widget.collect.targets.overlapping.sources.message",
        firstTarget.toString(),
        secondTarget.toString(),
        fileName,
      ),
    )
  }

  private companion object {
    private val IGNORED_NAMES_FOR_OVERLAPPING_SOURCES = arrayOf("empty.kt")
  }
}

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
suspend fun calculateProjectDetailsWithCapabilities(
  project: Project,
  server: JoinedBuildServer,
  syncScope: ProjectSyncScope,
  taskId: String,
): ProjectDetails =
  coroutineScope {
    try {
      val service = BazelWorkspaceResolveService.getInstance(project)
      val workspace = service.getOrFetchResolvedWorkspace(syncScope, taskId)
      val targets = workspace.targets

      val workspaceContext =
        query("workspace/context") {
          server.workspaceContext()
        }

      ProjectDetails(
        targetIds = targets.getTargetIDs().toList(),
        targets = targets.getTargets().toSet(),
        libraries = workspace.libraries,
        workspaceContext = workspaceContext,
      )
    } catch (e: Exception) {
      // TODO the type xd
      if (e is CancellationException || e is ExecutionException && e.cause is CancellationException) {
        thisLogger().debug("calculateProjectDetailsWithCapabilities has been cancelled", e)
      } else {
        thisLogger().error("calculateProjectDetailsWithCapabilities has failed", e)
      }

      throw e
    }
  }

// TODO: fix imports -- my build has broken sdkcompat
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
