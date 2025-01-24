package org.jetbrains.plugins.bsp.projectDetails

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.compiler.impl.javaCompiler.javac.JavacConfiguration
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.storage.MutableEntityStorage
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bsp.protocol.BazelBuildServer
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import org.jetbrains.bsp.protocol.utils.extractAndroidBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractScalaBuildTarget
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTopics
import org.jetbrains.plugins.bsp.android.AndroidSdk
import org.jetbrains.plugins.bsp.android.AndroidSdkGetterExtension
import org.jetbrains.plugins.bsp.android.androidSdkGetterExtension
import org.jetbrains.plugins.bsp.android.androidSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.building.syncConsole
import org.jetbrains.plugins.bsp.building.withSubtask
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.bspProjectName
import org.jetbrains.plugins.bsp.config.defaultJdkName
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.impl.flow.sync.BaseTargetInfo
import org.jetbrains.plugins.bsp.impl.flow.sync.BaseTargetInfos
import org.jetbrains.plugins.bsp.impl.flow.sync.FullProjectSync
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncScope
import org.jetbrains.plugins.bsp.impl.flow.sync.asyncQueryIf
import org.jetbrains.plugins.bsp.impl.flow.sync.queryIf
import org.jetbrains.plugins.bsp.impl.server.client.IMPORT_SUBTASK_ID
import org.jetbrains.plugins.bsp.impl.server.tasks.BspServerTask
import org.jetbrains.plugins.bsp.impl.server.tasks.SdkUtils
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.findNameProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.TargetIdToModuleEntitiesMap
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.WorkspaceModelUpdaterImpl
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.LibraryGraph
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.androidJarToAndroidSdkName
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.scalaVersionToScalaSdkName
import org.jetbrains.plugins.bsp.magicmetamodel.orDefault
import org.jetbrains.plugins.bsp.performance.bspTracer
import org.jetbrains.plugins.bsp.scala.sdk.ScalaSdk
import org.jetbrains.plugins.bsp.scala.sdk.scalaSdkExtension
import org.jetbrains.plugins.bsp.scala.sdk.scalaSdkExtensionExists
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.notifications.BspBalloonNotifier
import org.jetbrains.plugins.bsp.utils.isSourceFile
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaModule
import org.jetbrains.plugins.bsp.workspacemodel.entities.Module
import org.jetbrains.plugins.bsp.workspacemodel.entities.includesJava
import org.jetbrains.plugins.bsp.workspacemodel.entities.includesScala
import org.jetbrains.plugins.bsp.workspacemodel.entities.toBuildTargetInfo
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.io.path.toPath

class CollectProjectDetailsTask(
  project: Project,
  private val taskId: String,
  private val diff: MutableEntityStorage,
) : BspServerTask<ProjectDetails>("collect project details", project) {
  private var uniqueJavaHomes: Set<String>? = null

  private lateinit var javacOptions: Map<String, String>

  private var scalaSdks: Set<ScalaSdk>? = null

  private var androidSdks: Set<AndroidSdk>? = null

  suspend fun execute(
    server: JoinedBuildServer,
    syncScope: ProjectSyncScope,
    capabilities: BazelBuildServerCapabilities,
    progressReporter: SequentialProgressReporter,
    baseTargetInfos: BaseTargetInfos,
  ) {
    val projectDetails =
      progressReporter.sizedStep(workSize = 50, text = BspPluginBundle.message("progress.bar.collect.project.details")) {
        collectModel(server, capabilities, baseTargetInfos)
      }

    progressReporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.jdk.infos")) {
      calculateAllUniqueJdkInfosSubtask(projectDetails)
      uniqueJavaHomes.orEmpty().also {
        if (it.isNotEmpty()) {
          projectDetails.defaultJdkName = project.bspProjectName.projectNameToJdkName(it.first())
        } else {
          projectDetails.defaultJdkName = SdkUtils.getProjectJdkOrMostRecentJdk(project)?.name
        }
      }
      project.defaultJdkName = projectDetails.defaultJdkName
    }

    if (scalaSdkExtensionExists()) {
      progressReporter.indeterminateStep(text = "Calculating all unique scala sdk infos") {
        calculateAllScalaSdkInfosSubtask(projectDetails)
      }
    }

    if (BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists()) {
      progressReporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.android.sdk.infos")) {
        calculateAllAndroidSdkInfosSubtask(projectDetails)
      }
    }

    progressReporter.sizedStep(workSize = 25, text = BspPluginBundle.message("progress.bar.update.internal.model")) {
      updateInternalModelSubtask(projectDetails, syncScope)
    }
  }

  private suspend fun collectModel(
    server: JoinedBuildServer,
    capabilities: BazelBuildServerCapabilities,
    baseTargetInfos: BaseTargetInfos,
  ): ProjectDetails =
    try {
      project.syncConsole.startSubtask(
        this.taskId,
        IMPORT_SUBTASK_ID,
        BspPluginBundle.message("console.task.model.collect"),
      )

      val projectDetails =
        calculateProjectDetailsWithCapabilities(
          server = server,
          buildServerCapabilities = capabilities,
          baseTargetInfos = baseTargetInfos,
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
      message = BspPluginBundle.message("console.task.model.calculate.jdks.infos"),
    ) {
      uniqueJavaHomes =
        bspTracer.spanBuilder("calculate.all.unique.jdk.infos.ms").use {
          calculateAllUniqueJavaHomes(projectDetails)
        }
    }

  private fun calculateAllUniqueJavaHomes(projectDetails: ProjectDetails): Set<String> =
    projectDetails.targets
      .mapNotNull(::extractJvmBuildTarget)
      .map { it.javaHome }
      .toSet()

  private suspend fun calculateAllScalaSdkInfosSubtask(projectDetails: ProjectDetails) =
    project.syncConsole.withSubtask(
      taskId = taskId,
      subtaskId = "calculate-all-scala-sdk-infos",
      message = BspPluginBundle.message("console.task.model.calculate.scala.sdk.infos"),
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
      ?.let {
        ScalaSdk(
          name = it.scalaVersion.scalaVersionToScalaSdkName(),
          scalaVersion = it.scalaVersion,
          sdkJars = it.jars,
        )
      }

  private suspend fun calculateAllAndroidSdkInfosSubtask(projectDetails: ProjectDetails) =
    project.syncConsole.withSubtask(
      taskId,
      "calculate-all-android-sdk-infos",
      BspPluginBundle.message("progress.bar.calculate.android.sdk.infos"),
    ) {
      androidSdks =
        bspTracer.spanBuilder("calculate.all.android.sdk.infos.ms").use {
          calculateAllAndroidSdkInfos(projectDetails)
        }
    }

  private fun calculateAllAndroidSdkInfos(projectDetails: ProjectDetails): Set<AndroidSdk> =
    projectDetails.targets
      .mapNotNull { createAndroidSdk(it) }
      .toSet()

  private fun createAndroidSdk(target: BuildTarget): AndroidSdk? =
    extractAndroidBuildTarget(target)?.androidJar?.let { androidJar ->
      AndroidSdk(
        name = androidJar.androidJarToAndroidSdkName(),
        androidJar = androidJar,
      )
    }

  private suspend fun updateInternalModelSubtask(projectDetails: ProjectDetails, syncScope: ProjectSyncScope) {
    project.syncConsole.withSubtask(
      taskId,
      "calculate-project-structure",
      BspPluginBundle.message("console.task.model.calculate.structure"),
    ) {
      coroutineScope {
        val projectBasePath = project.rootDir.toNioPath()
        val nameProvider = project.findNameProvider().orDefault()
        val libraryGraph = LibraryGraph(projectDetails.libraries.orEmpty())

        val libraries =
          bspTracer.spanBuilder("create.libraries.ms").use {
            libraryGraph.createLibraries(nameProvider)
          }

        val libraryModules =
          bspTracer.spanBuilder("create.library.modules.ms").use {
            libraryGraph.createLibraryModules(nameProvider, projectDetails.defaultJdkName)
          }

        val targetIdToModuleDetails =
          bspTracer.spanBuilder("create.module.details.ms").use {
            val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, libraryGraph)
            projectDetails.targetIds.associateWith { transformer.moduleDetailsForTargetId(it) }
          }

        val targetIdToModuleEntitiesMap =
          bspTracer.spanBuilder("create.target.id.to.module.entities.map.ms").use {
            // Filter out non-module targets which cannot be run, as they are just cluttering the ui
            val usefulNonModuleTargets = projectDetails.nonModuleTargets.filter { it.capabilities.canRun }

            val syncedTargetIdToTargetInfo =
              (projectDetails.targets + usefulNonModuleTargets).associate {
                it.id to
                  it.toBuildTargetInfo()
              }

            val targetIdToTargetInfo =
              if (syncScope is FullProjectSync) {
                syncedTargetIdToTargetInfo
              } else {
                project.temporaryTargetUtils.targetIdToTargetInfo +
                  syncedTargetIdToTargetInfo
              }
            val targetIdToModuleEntityMap =
              TargetIdToModuleEntitiesMap(
                projectDetails = projectDetails,
                targetIdToModuleDetails = targetIdToModuleDetails,
                targetIdToTargetInfo = targetIdToTargetInfo,
                projectBasePath = projectBasePath,
                project = project,
                nameProvider = nameProvider,
                isAndroidSupportEnabled = BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
              )

            if (syncScope is FullProjectSync) {
              project.temporaryTargetUtils.saveTargets(
                targetIdToTargetInfo,
                targetIdToModuleEntityMap,
                targetIdToModuleDetails,
                projectDetails.libraries,
                libraryModules,
              )
            }
            targetIdToModuleEntityMap
          }

        bspTracer.spanBuilder("load.modules.ms").use {
          val workspaceModel = WorkspaceModel.getInstance(project)
          val virtualFileUrlManager = workspaceModel.getVirtualFileUrlManager()

          val workspaceModelUpdater =
            WorkspaceModelUpdaterImpl(
              workspaceEntityStorageBuilder = diff,
              virtualFileUrlManager = virtualFileUrlManager,
              projectBasePath = projectBasePath,
              project = project,
              isAndroidSupportEnabled = BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
            )

          val modulesToLoad = targetIdToModuleEntitiesMap.values.toList()

          workspaceModelUpdater.loadModules(modulesToLoad + libraryModules)
          workspaceModelUpdater.loadLibraries(libraries)
          calculateAllJavacOptions(modulesToLoad)
        }
      }
    }
  }

  private fun calculateAllJavacOptions(modulesToLoad: List<Module>) {
    javacOptions =
      modulesToLoad
        .asSequence()
        .filterIsInstance<JavaModule>()
        .mapNotNull { module ->
          module.javaAddendum?.javacOptions?.takeIf { it.isNotEmpty() }?.let { javacOptions ->
            module.getModuleName() to javacOptions.joinToString(" ")
          }
        }.toMap()
  }

  suspend fun postprocessingSubtask(targetListChanged: Boolean) {
    // This order is strict as now SDKs also use the workspace model,
    // updating jdks before applying the project model will render the action to fail.
    // This will be handled properly after this ticket:
    // https://youtrack.jetbrains.com/issue/BAZEL-426/Configure-JDK-using-workspace-model-API-instead-of-ProjectJdkTable
    project.temporaryTargetUtils.fireSyncListeners(targetListChanged)
    SdkUtils.cleanUpInvalidJdks(project.bspProjectName)
    addBspFetchedJdks()
    addBspFetchedJavacOptions()
    addBspFetchedScalaSdks()

    if (BspFeatureFlags.isAndroidSupportEnabled) {
      addBspFetchedAndroidSdks()
    }

    VirtualFileManager.getInstance().asyncRefresh()
    project.refreshKotlinHighlighting()
    checkOverlappingSources()
  }

  private suspend fun addBspFetchedJdks() =
    project.syncConsole.withSubtask(
      taskId,
      "add-bsp-fetched-jdks",
      BspPluginBundle.message("console.task.model.add.fetched.jdks"),
    ) {
      bspTracer.spanBuilder("add.bsp.fetched.jdks.ms").useWithScope {
        uniqueJavaHomes?.forEach {
          SdkUtils.addJdkIfNeeded(
            projectName = project.bspProjectName,
            javaHomeUri = it,
          )
        }
      }
    }

  private suspend fun addBspFetchedScalaSdks() {
    scalaSdkExtension()?.let { extension ->
      project.syncConsole.withSubtask(
        taskId,
        "add-bsp-fetched-scala-sdks",
        BspPluginBundle.message("console.task.model.add.scala.fetched.sdks"),
      ) {
        val modifiableProvider = IdeModifiableModelsProviderImpl(project)

        writeAction {
          scalaSdks?.forEach { extension.addScalaSdk(it, modifiableProvider) }
          modifiableProvider.commit()
        }
      }
    }
  }

  private suspend fun addBspFetchedAndroidSdks() {
    androidSdkGetterExtension()?.let { extension ->
      project.syncConsole.withSubtask(
        taskId,
        "add-bsp-fetched-android-sdks",
        BspPluginBundle.message("console.task.model.add.android.fetched.sdks"),
      ) {
        bspTracer.spanBuilder("add.bsp.fetched.android.sdks.ms").useWithScope {
          androidSdks?.forEach { addAndroidSdkIfNeeded(it, extension) }
        }
      }
    }
  }

  private suspend fun addAndroidSdkIfNeeded(androidSdk: AndroidSdk, androidSdkGetterExtension: AndroidSdkGetterExtension) {
    val sdk = writeAction { androidSdkGetterExtension.getAndroidSdk(androidSdk) } ?: return
    SdkUtils.addSdkIfNeeded(sdk)
  }

  private suspend fun addBspFetchedJavacOptions() =
    writeAction {
      val javacOptions = JavacConfiguration.getOptions(project, JavacConfiguration::class.java)
      javacOptions.ADDITIONAL_OPTIONS_OVERRIDE = this.javacOptions
    }

  /**
   * Workaround for https://youtrack.jetbrains.com/issue/KT-70632
   */
  private suspend fun Project.refreshKotlinHighlighting() =
    writeAction {
      analysisMessageBus.syncPublisher(KotlinModificationTopics.GLOBAL_MODULE_STATE_MODIFICATION).onModification()
    }

  private fun checkOverlappingSources() {
    val fileToTargetId = project.temporaryTargetUtils.fileToTargetId
    for ((file, targetIds) in fileToTargetId) {
      if (targetIds.size <= 1) continue
      if (!file.isSourceFile()) continue
      if (IGNORED_NAMES_FOR_OVERLAPPING_SOURCES.any { file.path.endsWith(it) }) continue
      warnOverlappingSources(targetIds[0], targetIds[1], file)
      break
    }
  }

  private fun warnOverlappingSources(
    firstTargetId: BuildTargetIdentifier,
    secondTargetId: BuildTargetIdentifier,
    source: URI,
  ) {
    BspBalloonNotifier.warn(
      BspPluginBundle.message("widget.collect.targets.overlapping.sources.title"),
      BspPluginBundle.message(
        "widget.collect.targets.overlapping.sources.message",
        firstTargetId.uri,
        secondTargetId.uri,
        source.toPath().fileName,
      ),
    )
  }

  private companion object {
    private val IGNORED_NAMES_FOR_OVERLAPPING_SOURCES = listOf("empty.kt")
  }
}

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
suspend fun calculateProjectDetailsWithCapabilities(
  server: JoinedBuildServer,
  buildServerCapabilities: BazelBuildServerCapabilities,
  baseTargetInfos: BaseTargetInfos,
): ProjectDetails =
  coroutineScope {
    try {
      val javaTargetIds = baseTargetInfos.infos.calculateJavaTargetIds()
      val scalaTargetIds = baseTargetInfos.infos.calculateScalaTargetIds()
      val libraries: WorkspaceLibrariesResult? =
        queryIf(buildServerCapabilities.workspaceLibrariesProvider, "workspace/libraries") {
          (server as BazelBuildServer).workspaceLibraries()
        }

      val dependencySourcesResult =
        asyncQueryIf(buildServerCapabilities.dependencySourcesProvider == true, "buildTarget/dependencySources") {
          val dependencySourcesTargetIds =
            if (libraries == null) {
              baseTargetInfos.allTargetIds
            } else {
              emptyList()
            }
          if (dependencySourcesTargetIds.isNotEmpty()) {
            server.buildTargetDependencySources(DependencySourcesParams(dependencySourcesTargetIds))
          } else {
            CompletableFuture.completedFuture(DependencySourcesResult(emptyList()))
          }
        }

      val nonModuleTargets =
        queryIf(buildServerCapabilities.workspaceNonModuleTargetsProvider, "workspace/nonModuleTargets") {
          (server as BazelBuildServer).workspaceNonModuleTargets()
        }

      val jvmBinaryJarsResult =
        queryIf(
          BspFeatureFlags.isAndroidSupportEnabled &&
            buildServerCapabilities.jvmBinaryJarsProvider &&
            javaTargetIds.isNotEmpty(),
          "buildTarget/jvmBinaryJars",
        ) {
          server.buildTargetJvmBinaryJars(JvmBinaryJarsParams(javaTargetIds))
        }

      // We use javacOptions only to build the dependency tree based on the classpath.
      // If the workspace/libraries endpoint is NOT available (like SBT), we need to retrieve it.
      // If a server supports buildTarget/jvmCompileClasspath, then the classpath won't be passed via this endpoint
      // (see https://build-server-protocol.github.io/docs/extensions/java#javacoptionsitem).
      // In this case we can use this request to retrieve the javac options without the overhead of passing the whole classpath.
      // There's no capability for javacOptions.
      val javacOptionsResult =
        if (libraries == null || buildServerCapabilities.jvmCompileClasspathProvider) {
          asyncQueryIf(javaTargetIds.isNotEmpty(), "buildTarget/javacOptions") {
            server.buildTargetJavacOptions(JavacOptionsParams(javaTargetIds))
          }
        } else {
          null
        }

      // Same for Scala
      val scalacOptionsResult =
        if (libraries == null) {
          asyncQueryIf(scalaTargetIds.isNotEmpty(), "buildTarget/scalacOptions") {
            server.buildTargetScalacOptions(ScalacOptionsParams(scalaTargetIds))
          }
        } else {
          null
        }

      ProjectDetails(
        targetIds = baseTargetInfos.allTargetIds,
        targets = baseTargetInfos.infos.map { it.target }.toSet(),
        sources = baseTargetInfos.infos.flatMap { it.sources },
        resources = baseTargetInfos.infos.flatMap { it.resources },
        dependenciesSources = dependencySourcesResult.await()?.items ?: emptyList(),
        javacOptions = javacOptionsResult?.await()?.items ?: emptyList(),
        scalacOptions = scalacOptionsResult?.await()?.items ?: emptyList(),
        libraries = libraries?.libraries,
        nonModuleTargets = nonModuleTargets?.nonModuleTargets ?: emptyList(),
        jvmBinaryJars = jvmBinaryJarsResult?.items ?: emptyList(),
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

private fun List<BaseTargetInfo>.calculateJavaTargetIds(): List<BuildTargetIdentifier> =
  filter { it.target.languageIds.includesJava() }.map { it.target.id }

private fun List<BaseTargetInfo>.calculateScalaTargetIds(): List<BuildTargetIdentifier> =
  filter { it.target.languageIds.includesScala() }.map { it.target.id }
