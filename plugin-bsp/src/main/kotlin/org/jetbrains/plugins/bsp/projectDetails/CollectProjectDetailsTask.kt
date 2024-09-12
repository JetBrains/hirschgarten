package org.jetbrains.plugins.bsp.projectDetails

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.PythonOptionsParams
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
import kotlinx.coroutines.runInterruptible
import org.jetbrains.bsp.protocol.BazelBuildServer
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import org.jetbrains.bsp.protocol.utils.extractAndroidBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
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
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.impl.flow.sync.BaseTargetInfo
import org.jetbrains.plugins.bsp.impl.flow.sync.BaseTargetInfos
import org.jetbrains.plugins.bsp.impl.flow.sync.asyncQueryIf
import org.jetbrains.plugins.bsp.impl.flow.sync.queryIf
import org.jetbrains.plugins.bsp.impl.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.TargetIdToModuleEntitiesMap
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.LibraryGraph
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.androidJarToAndroidSdkName
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.scalaVersionToScalaSdkName
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.includesJava
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.includesPython
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.includesScala
import org.jetbrains.plugins.bsp.impl.server.client.IMPORT_SUBTASK_ID
import org.jetbrains.plugins.bsp.impl.server.tasks.BspServerTask
import org.jetbrains.plugins.bsp.impl.server.tasks.SdkUtils
import org.jetbrains.plugins.bsp.impl.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.impl.utils.findLibraryNameProvider
import org.jetbrains.plugins.bsp.impl.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.impl.utils.orDefault
import org.jetbrains.plugins.bsp.performance.bspTracer
import org.jetbrains.plugins.bsp.python.PythonSdk
import org.jetbrains.plugins.bsp.python.PythonSdkGetterExtension
import org.jetbrains.plugins.bsp.python.pythonSdkGetterExtension
import org.jetbrains.plugins.bsp.python.pythonSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.scala.sdk.ScalaSdk
import org.jetbrains.plugins.bsp.scala.sdk.scalaSdkExtension
import org.jetbrains.plugins.bsp.scala.sdk.scalaSdkExtensionExists
import org.jetbrains.plugins.bsp.ui.notifications.BspBalloonNotifier
import org.jetbrains.plugins.bsp.utils.isSourceFile
import org.jetbrains.plugins.bsp.workspacemodel.entities.Module
import org.jetbrains.plugins.bsp.workspacemodel.entities.toBuildTargetInfo
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.io.path.Path
import kotlin.io.path.toPath

class CollectProjectDetailsTask(
  project: Project,
  private val taskId: String,
  private val diff: MutableEntityStorage,
) : BspServerTask<ProjectDetails>("collect project details", project) {
  private var uniqueJavaHomes: Set<String>? = null

  private lateinit var javacOptions: Map<String, String>

  private var pythonSdks: Set<PythonSdk>? = null

  private var scalaSdks: Set<ScalaSdk>? = null

  private var androidSdks: Set<AndroidSdk>? = null

  suspend fun execute(
    server: JoinedBuildServer,
    capabilities: BazelBuildServerCapabilities,
    progressReporter: SequentialProgressReporter,
    baseTargetInfos: BaseTargetInfos,
  ) {
    val projectDetails =
      progressReporter.sizedStep(workSize = 50, text = BspPluginBundle.message("progress.bar.collect.project.details")) {
        collectModel(server, capabilities, baseTargetInfos)
      } ?: return

    progressReporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.jdk.infos")) {
      calculateAllUniqueJdkInfosSubtask(projectDetails)
      uniqueJavaHomes.orEmpty().also {
        if (it.isNotEmpty()) {
          projectDetails.defaultJdkName = project.name.projectNameToJdkName(it.first())
        } else {
          projectDetails.defaultJdkName = SdkUtils.getProjectJdkOrMostRecentJdk(project)?.name
        }
      }
    }

    if (BspFeatureFlags.isPythonSupportEnabled && pythonSdkGetterExtensionExists()) {
      progressReporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.python.sdk.infos")) {
        calculateAllPythonSdkInfosSubtask(projectDetails)
      }
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
      updateInternalModelSubtask(projectDetails)
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
        BspPluginBundle.message("console.task.model.collect.in.progress"),
      )

      val projectDetails =
        calculateProjectDetailsWithCapabilities(
          project = project,
          server = server,
          buildServerCapabilities = capabilities,
          baseTargetInfos = baseTargetInfos,
        )

      project.syncConsole.finishSubtask(IMPORT_SUBTASK_ID, BspPluginBundle.message("console.task.model.collect.success"))

      projectDetails
    } catch (e: Exception) {
      if (e is CancellationException) {
        project.syncConsole.finishSubtask(
          IMPORT_SUBTASK_ID,
          BspPluginBundle.message("console.task.model.collect.cancelled"),
          FailureResultImpl(),
        )
      } else {
        project.syncConsole.finishSubtask(
          IMPORT_SUBTASK_ID,
          BspPluginBundle.message("console.task.model.collect.failed"),
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

  private suspend fun calculateAllPythonSdkInfosSubtask(projectDetails: ProjectDetails) =
    project.syncConsole.withSubtask(
      taskId = taskId,
      subtaskId = "calculate-all-python-sdk-infos",
      message = BspPluginBundle.message("console.task.model.calculate.python.sdks.done"),
    ) {
      runInterruptible {
        pythonSdks =
          bspTracer.spanBuilder("calculate.all.python.sdk.infos.ms").use {
            calculateAllPythonSdkInfos(projectDetails)
          }
      }
    }

  private fun createPythonSdk(target: BuildTarget, dependenciesSources: List<DependencySourcesItem>): PythonSdk? =
    extractPythonBuildTarget(target)?.let {
      if (it.interpreter != null && it.version != null) {
        PythonSdk(
          name = "${target.id.uri}-${it.version}",
          interpreterUri = it.interpreter,
          dependencies = dependenciesSources,
        )
      } else {
        pythonSdkGetterExtension()
          ?.getSystemSdk()
          ?.let { sdk ->
            PythonSdk(
              name = "${target.id.uri}-detected-PY3",
              interpreterUri = Path(sdk.homePath!!).toUri().toString(),
              dependencies = dependenciesSources,
            )
          }
      }
    }

  private fun calculateAllPythonSdkInfos(projectDetails: ProjectDetails): Set<PythonSdk> =
    projectDetails.targets
      .mapNotNull {
        createPythonSdk(it, projectDetails.dependenciesSources.filter { a -> a.target.uri == it.id.uri })
      }.toSet()

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

  private suspend fun updateInternalModelSubtask(projectDetails: ProjectDetails) {
    project.syncConsole.withSubtask(
      taskId,
      "calculate-project-structure",
      BspPluginBundle.message("console.task.model.calculate.structure"),
    ) {
      runInterruptible {
        val projectBasePath = project.rootDir.toNioPath()
        val moduleNameProvider = project.findModuleNameProvider().orDefault()
        val libraryNameProvider = project.findLibraryNameProvider().orDefault()
        val libraryGraph = LibraryGraph(projectDetails.libraries.orEmpty())

        val libraries =
          bspTracer.spanBuilder("create.libraries.ms").use {
            libraryGraph.createLibraries(libraryNameProvider)
          }

        val libraryModules =
          bspTracer.spanBuilder("create.library.modules.ms").use {
            libraryGraph.createLibraryModules(libraryNameProvider, projectDetails.defaultJdkName)
          }

        val targetIdToModuleDetails =
          bspTracer.spanBuilder("create.module.details.ms").use {
            val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, libraryGraph)
            projectDetails.targetIds.associateWith { transformer.moduleDetailsForTargetId(it) }
          }

        val targetIdToModuleEntitiesMap =
          bspTracer.spanBuilder("create.target.id.to.module.entities.map.ms").use {
            val targetIdToTargetInfo =
              (projectDetails.targets + projectDetails.nonModuleTargets).associate {
                it.id to
                  it.toBuildTargetInfo()
              }
            val targetIdToModuleEntityMap =
              TargetIdToModuleEntitiesMap(
                projectDetails = projectDetails,
                targetIdToModuleDetails = targetIdToModuleDetails,
                targetIdToTargetInfo = targetIdToTargetInfo,
                projectBasePath = projectBasePath,
                moduleNameProvider = moduleNameProvider,
                libraryNameProvider = libraryNameProvider,
                hasDefaultPythonInterpreter = BspFeatureFlags.isPythonSupportEnabled,
                isPythonSupportEnabled = BspFeatureFlags.isPythonSupportEnabled,
                isAndroidSupportEnabled = BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
              )

            project.temporaryTargetUtils.saveTargets(
              targetIdToTargetInfo,
              targetIdToModuleEntityMap,
              targetIdToModuleDetails,
              libraries,
              libraryModules,
            )

            targetIdToModuleEntityMap
          }

        bspTracer.spanBuilder("load.modules.ms").use {
          val workspaceModel = WorkspaceModel.getInstance(project)
          val virtualFileUrlManager = workspaceModel.getVirtualFileUrlManager()

          val workspaceModelUpdater =
            WorkspaceModelUpdater.create(
              diff,
              virtualFileUrlManager,
              projectBasePath,
              project,
              BspFeatureFlags.isPythonSupportEnabled,
              BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
            )

          val modulesToLoad = targetIdToModuleEntitiesMap.values.toList()

          workspaceModelUpdater.loadModules(modulesToLoad + project.temporaryTargetUtils.getAllLibraryModules())
          workspaceModelUpdater.loadLibraries(project.temporaryTargetUtils.getAllLibraries())
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

  suspend fun postprocessingSubtask() {
    // This order is strict as now SDKs also use the workspace model,
    // updating jdks before applying the project model will render the action to fail.
    // This will be handled properly after this ticket:
    // https://youtrack.jetbrains.com/issue/BAZEL-426/Configure-JDK-using-workspace-model-API-instead-of-ProjectJdkTable
    project.temporaryTargetUtils.fireListeners()
    addBspFetchedJdks()
    addBspFetchedJavacOptions()
    addBspFetchedScalaSdks()

    if (BspFeatureFlags.isPythonSupportEnabled) {
      addBspFetchedPythonSdks()
    }

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
            projectName = project.name,
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

  private suspend fun addBspFetchedPythonSdks() {
    pythonSdkGetterExtension()?.let { extension ->
      project.syncConsole.withSubtask(
        taskId,
        "add-bsp-fetched-python-sdks",
        BspPluginBundle.message("console.task.model.add.python.fetched.sdks"),
      ) {
        bspTracer.spanBuilder("add.bsp.fetched.python.sdks.ms").useWithScope {
          pythonSdks?.forEach { addPythonSdkIfNeeded(it, extension) }
        }
      }
    }
  }

  private suspend fun addPythonSdkIfNeeded(pythonSdk: PythonSdk, pythonSdkGetterExtension: PythonSdkGetterExtension) {
    val sdk =
      runInterruptible {
        pythonSdkGetterExtension.getPythonSdk(
          pythonSdk,
          WorkspaceModel.getInstance(project).getVirtualFileUrlManager(),
        )
      }

    SdkUtils.addSdkIfNeeded(sdk)
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
}

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
public suspend fun calculateProjectDetailsWithCapabilities(
  project: Project,
  server: JoinedBuildServer,
  buildServerCapabilities: BazelBuildServerCapabilities,
  baseTargetInfos: BaseTargetInfos,
): ProjectDetails =
  coroutineScope {
    try {
      val javaTargetIds = baseTargetInfos.infos.calculateJavaTargetIds()
      val scalaTargetIds = baseTargetInfos.infos.calculateScalaTargetIds()
      val pythonTargetsIds = baseTargetInfos.infos.calculatePythonTargetsIds()
      val libraries: WorkspaceLibrariesResult? =
        queryIf(buildServerCapabilities.workspaceLibrariesProvider, "workspace/libraries") {
          (server as BazelBuildServer).workspaceLibraries()
        }

      val dependencySourcesResult =
        asyncQueryIf(buildServerCapabilities.dependencySourcesProvider == true, "buildTarget/dependencySources") {
          val dependencySourcesTargetIds =
            if (libraries == null) {
              baseTargetInfos.allTargetIds
            } else if (BspFeatureFlags.isPythonSupportEnabled) {
              pythonTargetsIds
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

      val pythonOptionsResult =
        asyncQueryIf(pythonTargetsIds.isNotEmpty() && BspFeatureFlags.isPythonSupportEnabled, "buildTarget/pythonOptions") {
          server.buildTargetPythonOptions(PythonOptionsParams(pythonTargetsIds))
        }

      ProjectDetails(
        targetIds = baseTargetInfos.allTargetIds,
        targets = baseTargetInfos.infos.map { it.target }.toSet(),
        sources = baseTargetInfos.infos.flatMap { it.sources },
        resources = baseTargetInfos.infos.flatMap { it.resources },
        dependenciesSources = dependencySourcesResult.await()?.items ?: emptyList(),
        javacOptions = javacOptionsResult?.await()?.items ?: emptyList(),
        scalacOptions = scalacOptionsResult?.await()?.items ?: emptyList(),
        pythonOptions = pythonOptionsResult.await()?.items ?: emptyList(),
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

private fun List<BaseTargetInfo>.calculatePythonTargetsIds(): List<BuildTargetIdentifier> =
  filter { it.target.languageIds.includesPython() }.map { it.target.id }
