package org.jetbrains.bazel.jvm.sync

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
import org.jetbrains.bazel.android.AndroidSdk
import org.jetbrains.bazel.android.AndroidSdkGetterExtension
import org.jetbrains.bazel.android.androidSdkGetterExtension
import org.jetbrains.bazel.android.androidSdkGetterExtensionExists
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.bazelProjectName
import org.jetbrains.bazel.config.defaultJdkName
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.extensionPoints.shouldImportJvmBinaryJars
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.findNameProvider
import org.jetbrains.bazel.magicmetamodel.impl.TargetIdToModuleEntitiesMap
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.WorkspaceModelUpdaterImpl
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.LibraryGraph
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModulesToCompiledSourceCodeInsideJarExcludeTransformer
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.androidJarToAndroidSdkName
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.scalaVersionToScalaSdkName
import org.jetbrains.bazel.magicmetamodel.orDefault
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.scala.sdk.ScalaSdk
import org.jetbrains.bazel.scala.sdk.scalaSdkExtension
import org.jetbrains.bazel.scala.sdk.scalaSdkExtensionExists
import org.jetbrains.bazel.sdkcompat.isSharedSourceSupportEnabled
import org.jetbrains.bazel.server.client.IMPORT_SUBTASK_ID
import org.jetbrains.bazel.sync.BaseTargetInfo
import org.jetbrains.bazel.sync.BaseTargetInfos
import org.jetbrains.bazel.sync.scope.FullProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.task.asyncQueryIf
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.task.queryIf
import org.jetbrains.bazel.target.calculateFileToTarget
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier
import org.jetbrains.bazel.utils.isSourceFile
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bazel.workspacemodel.entities.includesJava
import org.jetbrains.bazel.workspacemodel.entities.includesScala
import org.jetbrains.bazel.workspacemodel.entities.toBuildTargetInfo
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.ScalacOptionsParams
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import org.jetbrains.bsp.protocol.utils.extractAndroidBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractScalaBuildTarget
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTopics
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import kotlin.io.path.toPath

class CollectProjectDetailsTask(
  private val project: Project,
  private val taskId: String,
  private val diff: MutableEntityStorage,
) {
  private var uniqueJavaHomes: Set<String>? = null

  private lateinit var javacOptions: Map<String, String>

  private var scalaSdks: Set<ScalaSdk>? = null

  private var androidSdks: Set<AndroidSdk>? = null

  suspend fun execute(
    project: Project,
    server: JoinedBuildServer,
    syncScope: ProjectSyncScope,
    progressReporter: SequentialProgressReporter,
    baseTargetInfos: BaseTargetInfos,
  ) {
    val projectDetails =
      progressReporter.sizedStep(workSize = 50, text = BazelPluginBundle.message("progress.bar.collect.project.details")) {
        collectModel(project, server, baseTargetInfos)
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
      progressReporter.indeterminateStep(text = "Calculating all unique scala sdk infos") {
        calculateAllScalaSdkInfosSubtask(projectDetails)
      }
    }

    if (BazelFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists()) {
      progressReporter.indeterminateStep(text = BazelPluginBundle.message("progress.bar.calculate.android.sdk.infos")) {
        calculateAllAndroidSdkInfosSubtask(projectDetails)
      }
    }

    progressReporter.sizedStep(workSize = 25, text = BazelPluginBundle.message("progress.bar.update.internal.model")) {
      updateInternalModelSubtask(projectDetails, syncScope)
    }
  }

  private suspend fun collectModel(
    project: Project,
    server: JoinedBuildServer,
    baseTargetInfos: BaseTargetInfos,
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
      message = BazelPluginBundle.message("console.task.model.calculate.jdks.infos"),
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
      BazelPluginBundle.message("progress.bar.calculate.android.sdk.infos"),
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
      BazelPluginBundle.message("console.task.model.calculate.structure"),
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

        val fileToTarget: Map<URI, List<Label>> = calculateFileToTarget(targetIdToModuleDetails)

        val targetIdToModuleEntitiesMap =
          bspTracer.spanBuilder("create.target.id.to.module.entities.map.ms").use {
            val syncedTargetIdToTargetInfo =
              (projectDetails.targets).associate {
                it.id to
                  it.toBuildTargetInfo()
              }

            val targetIdToTargetInfo =
              if (syncScope is FullProjectSync) {
                syncedTargetIdToTargetInfo
              } else {
                project.targetUtils.labelToTargetInfo.mapKeys { it.key } +
                  syncedTargetIdToTargetInfo
              }
            val targetIdToModuleEntityMap =
              TargetIdToModuleEntitiesMap(
                projectDetails = projectDetails,
                targetIdToModuleDetails = targetIdToModuleDetails,
                targetIdToTargetInfo = targetIdToTargetInfo,
                fileToTarget = fileToTarget,
                projectBasePath = projectBasePath,
                project = project,
                nameProvider = nameProvider,
                isAndroidSupportEnabled = BazelFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
              )

            if (syncScope is FullProjectSync) {
              project.targetUtils.saveTargets(
                targetIdToTargetInfo,
                targetIdToModuleEntityMap,
                fileToTarget,
                projectDetails.libraries,
                libraryModules,
                nameProvider,
              )
            }
            targetIdToModuleEntityMap
          }

        val modulesToLoad = targetIdToModuleEntitiesMap.values.flatten().distinctBy { module -> module.getModuleName() }

        val compiledSourceCodeInsideJarToExclude =
          bspTracer.spanBuilder("calculate.non.generated.class.files.to.exclude").use {
            if (BazelFeatureFlags.excludeCompiledSourceCodeInsideJars) {
              ModulesToCompiledSourceCodeInsideJarExcludeTransformer().transform(targetIdToModuleDetails.values)
            } else {
              null
            }
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
              isAndroidSupportEnabled = BazelFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
            )

          workspaceModelUpdater.loadModules(modulesToLoad + libraryModules)
          workspaceModelUpdater.loadLibraries(libraries)
          compiledSourceCodeInsideJarToExclude?.let { workspaceModelUpdater.loadCompiledSourceCodeInsideJarExclude(it) }
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
    project.targetUtils.fireSyncListeners(targetListChanged)
    SdkUtils.cleanUpInvalidJdks(project.bazelProjectName)
    addBspFetchedJdks()
    addBspFetchedJavacOptions()
    addBspFetchedScalaSdks()

    if (BazelFeatureFlags.isAndroidSupportEnabled) {
      addBspFetchedAndroidSdks()
    }

    VirtualFileManager.getInstance().asyncRefresh()
    project.refreshKotlinHighlighting()
    checkSharedSources()
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

  private suspend fun addBspFetchedAndroidSdks() {
    androidSdkGetterExtension()?.let { extension ->
      project.syncConsole.withSubtask(
        taskId,
        "add-bsp-fetched-android-sdks",
        BazelPluginBundle.message("console.task.model.add.android.fetched.sdks"),
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
      try {
        analysisMessageBus.syncPublisher(KotlinModificationTopics.GLOBAL_MODULE_STATE_MODIFICATION).onModification()
      } catch (_: NoClassDefFoundError) {
        // TODO: the above method was removed in 252 master.
        //  Replace with `project.publishGlobalModuleStateModificationEvent()` once it's available in the next 252 EAP
        val utilsKt = Class.forName("org.jetbrains.kotlin.analysis.api.platform.modification.UtilsKt")
        val method = utilsKt.getDeclaredMethod("publishGlobalModuleStateModificationEvent", Project::class.java)
        method.invoke(utilsKt, project)
      }
    }

  private fun checkSharedSources() {
    if (project.isSharedSourceSupportEnabled) return
    val fileToTarget = project.targetUtils.fileToTarget
    for ((file, targets) in fileToTarget) {
      if (targets.size <= 1) continue
      if (!file.isSourceFile()) continue
      if (IGNORED_NAMES_FOR_OVERLAPPING_SOURCES.any { file.path.endsWith(it) }) continue
      warnOverlappingSources(targets[0], targets[1], file)
      break
    }
  }

  private fun warnOverlappingSources(
    firstTarget: Label,
    secondTarget: Label,
    source: URI,
  ) {
    BazelBalloonNotifier.warn(
      BazelPluginBundle.message("widget.collect.targets.overlapping.sources.title"),
      BazelPluginBundle.message(
        "widget.collect.targets.overlapping.sources.message",
        firstTarget.toString(),
        secondTarget.toString(),
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
  project: Project,
  server: JoinedBuildServer,
  baseTargetInfos: BaseTargetInfos,
): ProjectDetails =
  coroutineScope {
    try {
      val javaTargetIds = baseTargetInfos.infos.calculateJavaTargetIds()
      val scalaTargetIds = baseTargetInfos.infos.calculateScalaTargetIds()
      val libraries: WorkspaceLibrariesResult =
        query("workspace/libraries") {
          server.workspaceLibraries()
        }

      val jvmBinaryJarsResult =
        queryIf(
          javaTargetIds.isNotEmpty() &&
            project.shouldImportJvmBinaryJars(),
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
        asyncQueryIf(javaTargetIds.isNotEmpty(), "buildTarget/javacOptions") {
          server.buildTargetJavacOptions(JavacOptionsParams(javaTargetIds))
        }

      // Same for Scala
      val scalacOptionsResult =
        // TODO: Son
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
        javacOptions = javacOptionsResult.await()?.items ?: emptyList(),
        // TODO: Son
        scalacOptions = scalacOptionsResult?.await()?.items ?: emptyList(),
        libraries = libraries.libraries,
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

private fun List<BaseTargetInfo>.calculateJavaTargetIds(): List<Label> =
  filter { it.target.languageIds.includesJava() }.map { it.target.id }

private fun List<BaseTargetInfo>.calculateScalaTargetIds(): List<Label> =
  filter { it.target.languageIds.includesScala() }.map { it.target.id }
