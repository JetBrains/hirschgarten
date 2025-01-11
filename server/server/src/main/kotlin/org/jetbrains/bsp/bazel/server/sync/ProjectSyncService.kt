package org.jetbrains.bsp.bazel.server.sync

import ch.epfl.scala.bsp4j.BuildClientCapabilities
import ch.epfl.scala.bsp4j.CppOptionsParams
import ch.epfl.scala.bsp4j.CppOptionsResult
import ch.epfl.scala.bsp4j.DependencyModulesParams
import ch.epfl.scala.bsp4j.DependencyModulesResult
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.InitializeBuildResult
import ch.epfl.scala.bsp4j.InverseSourcesParams
import ch.epfl.scala.bsp4j.InverseSourcesResult
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.JavacOptionsResult
import ch.epfl.scala.bsp4j.JvmCompileClasspathParams
import ch.epfl.scala.bsp4j.JvmCompileClasspathResult
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult
import ch.epfl.scala.bsp4j.OutputPathsParams
import ch.epfl.scala.bsp4j.OutputPathsResult
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.PythonOptionsResult
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.RustWorkspaceParams
import ch.epfl.scala.bsp4j.RustWorkspaceResult
import ch.epfl.scala.bsp4j.ScalaMainClassesParams
import ch.epfl.scala.bsp4j.ScalaMainClassesResult
import ch.epfl.scala.bsp4j.ScalaTestClassesParams
import ch.epfl.scala.bsp4j.ScalaTestClassesResult
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import ch.epfl.scala.bsp4j.ScalacOptionsResult
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.server.model.AspectSyncProject
import org.jetbrains.bsp.bazel.server.model.FirstPhaseProject
import org.jetbrains.bsp.bazel.server.model.Language
import org.jetbrains.bsp.bazel.server.model.Project
import org.jetbrains.bsp.bazel.server.sync.firstPhase.FirstPhaseTargetToBspMapper
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult

/** A facade for all project sync related methods  */
class ProjectSyncService(
  private val bspMapper: BspProjectMapper,
  private val firstPhaseTargetToBspMapper: FirstPhaseTargetToBspMapper,
  private val projectProvider: ProjectProvider,
  private val clientCapabilities: BuildClientCapabilities,
) {
  fun initialize(): InitializeBuildResult = bspMapper.initializeServer(Language.all())

  // TODO https://youtrack.jetbrains.com/issue/BAZEL-639
  // We might consider doing the actual project reload in this endpoint
  // i.e. just run projectProvider.refreshAndGet() and in workspaceBuildTargets
  // just run projectProvider.get() although current approach seems to work
  // correctly, so I am not changing anything.
  fun workspaceReload(cancelChecker: CancelChecker): Any = Any()

  fun workspaceBuildTargets(cancelChecker: CancelChecker, build: Boolean): WorkspaceBuildTargetsResult {
    val project = projectProvider.refreshAndGet(cancelChecker, build = build)
    return bspMapper.workspaceTargets(project)
  }

  fun workspaceBuildTargetsPartial(cancelChecker: CancelChecker, targetsToSync: List<Label>): WorkspaceBuildTargetsResult {
    val project =
      projectProvider.updateAndGet(
        cancelChecker = cancelChecker,
        targetsToSync = targetsToSync,
      )
    return bspMapper.workspaceTargets(project)
  }

  fun workspaceBuildFirstPhase(cancelChecker: CancelChecker, params: WorkspaceBuildTargetsFirstPhaseParams): WorkspaceBuildTargetsResult {
    val project = projectProvider.bazelQueryRefreshAndGet(cancelChecker, params.originId)
    return firstPhaseTargetToBspMapper.toWorkspaceBuildTargetsResult(project)
  }

  fun workspaceBuildLibraries(cancelChecker: CancelChecker): WorkspaceLibrariesResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.workspaceLibraries(project)
  }

  fun workspaceBuildGoLibraries(cancelChecker: CancelChecker): WorkspaceGoLibrariesResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.workspaceGoLibraries(project)
  }

  fun workspaceNonModuleTargets(cancelChecker: CancelChecker): NonModuleTargetsResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.workspaceNonModuleTargets(project)
  }

  fun workspaceDirectories(cancelChecker: CancelChecker): WorkspaceDirectoriesResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.workspaceDirectories(project)
  }

  fun workspaceInvalidTargets(cancelChecker: CancelChecker): WorkspaceInvalidTargetsResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.workspaceInvalidTargets(project)
  }

  fun workspaceBazelRepoMapping(cancelChecker: CancelChecker): WorkspaceBazelRepoMappingResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.workspaceBazelRepoMapping(project)
  }

  fun buildTargetSources(cancelChecker: CancelChecker, sourcesParams: SourcesParams): SourcesResult {
    val project = projectProvider.get(cancelChecker)
    return when (project) {
      is AspectSyncProject -> bspMapper.sources(project, sourcesParams)
      is FirstPhaseProject -> firstPhaseTargetToBspMapper.toSourcesResult(project, sourcesParams)
    }
  }

  fun buildTargetResources(cancelChecker: CancelChecker, resourcesParams: ResourcesParams): ResourcesResult {
    val project = projectProvider.get(cancelChecker)
    return when (project) {
      is AspectSyncProject -> bspMapper.resources(project, resourcesParams)
      is FirstPhaseProject -> firstPhaseTargetToBspMapper.toResourcesResult(project, resourcesParams)
    }
  }

  fun buildTargetInverseSources(cancelChecker: CancelChecker, inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.inverseSources(project, inverseSourcesParams, cancelChecker)
  }

  fun buildTargetDependencySources(
    cancelChecker: CancelChecker,
    dependencySourcesParams: DependencySourcesParams,
  ): DependencySourcesResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.dependencySources(project, dependencySourcesParams)
  }

  fun buildTargetOutputPaths(cancelChecker: CancelChecker, params: OutputPathsParams): OutputPathsResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.outputPaths(project, params)
  }

  fun jvmRunEnvironment(cancelChecker: CancelChecker, params: JvmRunEnvironmentParams): JvmRunEnvironmentResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.jvmRunEnvironment(project, params, cancelChecker)
  }

  fun jvmTestEnvironment(cancelChecker: CancelChecker, params: JvmTestEnvironmentParams): JvmTestEnvironmentResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.jvmTestEnvironment(project, params, cancelChecker)
  }

  fun jvmBinaryJars(cancelChecker: CancelChecker, params: JvmBinaryJarsParams): JvmBinaryJarsResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.jvmBinaryJars(project, params)
  }

  fun jvmCompileClasspath(cancelChecker: CancelChecker, params: JvmCompileClasspathParams): JvmCompileClasspathResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.jvmCompileClasspath(project, params, cancelChecker)
  }

  fun buildTargetJavacOptions(cancelChecker: CancelChecker, params: JavacOptionsParams): JavacOptionsResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    val includeClasspath = clientCapabilities.jvmCompileClasspathReceiver == false
    return bspMapper.buildTargetJavacOptions(project, params, includeClasspath, cancelChecker)
  }

  fun buildTargetCppOptions(cancelChecker: CancelChecker, params: CppOptionsParams): CppOptionsResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.buildTargetCppOptions(project, params)
  }

  fun buildTargetPythonOptions(cancelChecker: CancelChecker, params: PythonOptionsParams): PythonOptionsResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.buildTargetPythonOptions(project, params)
  }

  fun buildTargetScalacOptions(cancelChecker: CancelChecker, params: ScalacOptionsParams): ScalacOptionsResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    val includeClasspath = clientCapabilities.jvmCompileClasspathReceiver == false
    return bspMapper.buildTargetScalacOptions(project, params, includeClasspath, cancelChecker)
  }

  fun buildTargetScalaTestClasses(cancelChecker: CancelChecker, params: ScalaTestClassesParams): ScalaTestClassesResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.buildTargetScalaTestClasses(project, params)
  }

  fun buildTargetScalaMainClasses(cancelChecker: CancelChecker, params: ScalaMainClassesParams): ScalaMainClassesResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.buildTargetScalaMainClasses(project, params)
  }

  fun buildTargetDependencyModules(cancelChecker: CancelChecker, params: DependencyModulesParams): DependencyModulesResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.buildDependencyModules(project, params)
  }

  fun rustWorkspace(cancelChecker: CancelChecker, params: RustWorkspaceParams): RustWorkspaceResult {
    val project = projectProvider.get(cancelChecker).toAspectSyncProjectOrThrow()
    return bspMapper.rustWorkspace(project, params)
  }

  private fun Project.toAspectSyncProjectOrThrow(): AspectSyncProject =
    this as? AspectSyncProject ?: error("Project is not an aspect sync project. Endpoint has been called before executing aspects sync.")
}
