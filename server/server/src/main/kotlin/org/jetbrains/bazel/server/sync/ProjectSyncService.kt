package org.jetbrains.bazel.server.sync

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.FirstPhaseProject
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseTargetToBspMapper
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.CppOptionsParams
import org.jetbrains.bsp.protocol.CppOptionsResult
import org.jetbrains.bsp.protocol.DependencyModulesParams
import org.jetbrains.bsp.protocol.DependencyModulesResult
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.JvmCompileClasspathParams
import org.jetbrains.bsp.protocol.JvmCompileClasspathResult
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import org.jetbrains.bsp.protocol.OutputPathsParams
import org.jetbrains.bsp.protocol.OutputPathsResult
import org.jetbrains.bsp.protocol.PythonOptionsParams
import org.jetbrains.bsp.protocol.PythonOptionsResult
import org.jetbrains.bsp.protocol.ResourcesParams
import org.jetbrains.bsp.protocol.ResourcesResult
import org.jetbrains.bsp.protocol.RustWorkspaceParams
import org.jetbrains.bsp.protocol.RustWorkspaceResult
import org.jetbrains.bsp.protocol.ScalaMainClassesParams
import org.jetbrains.bsp.protocol.ScalaMainClassesResult
import org.jetbrains.bsp.protocol.ScalaTestClassesParams
import org.jetbrains.bsp.protocol.ScalaTestClassesResult
import org.jetbrains.bsp.protocol.ScalacOptionsParams
import org.jetbrains.bsp.protocol.ScalacOptionsResult
import org.jetbrains.bsp.protocol.SourcesParams
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBazelBinPathResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult

/** A facade for all project sync related methods  */
class ProjectSyncService(
  private val bspMapper: BspProjectMapper,
  private val firstPhaseTargetToBspMapper: FirstPhaseTargetToBspMapper,
  private val projectProvider: ProjectProvider,
  private val bazelInfo: BazelInfo,
) {
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
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return WorkspaceLibrariesResult(emptyList())
    return bspMapper.workspaceLibraries(project)
  }

  fun workspaceBuildGoLibraries(cancelChecker: CancelChecker): WorkspaceGoLibrariesResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return WorkspaceGoLibrariesResult(emptyList())
    return bspMapper.workspaceGoLibraries(project)
  }

  fun workspaceNonModuleTargets(cancelChecker: CancelChecker): NonModuleTargetsResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return NonModuleTargetsResult(emptyList())
    return bspMapper.workspaceNonModuleTargets(project)
  }

  fun workspaceDirectories(cancelChecker: CancelChecker): WorkspaceDirectoriesResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.workspaceDirectories(project)
  }

  fun workspaceInvalidTargets(cancelChecker: CancelChecker): WorkspaceInvalidTargetsResult {
    // TODO: BAZEL-1644
    return WorkspaceInvalidTargetsResult(emptyList())
//    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return WorkspaceInvalidTargetsResult(emptyList())
//    return bspMapper.workspaceInvalidTargets(project)
  }

  fun workspaceBazelRepoMapping(cancelChecker: CancelChecker): WorkspaceBazelRepoMappingResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.workspaceBazelRepoMapping(project)
  }

  fun workspaceBazelBinPath(cancelChecker: CancelChecker): WorkspaceBazelBinPathResult =
    WorkspaceBazelBinPathResult(bazelInfo.bazelBin.toString())

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
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return InverseSourcesResult(emptyList())
    return bspMapper.inverseSources(project, inverseSourcesParams, cancelChecker)
  }

  fun buildTargetDependencySources(
    cancelChecker: CancelChecker,
    dependencySourcesParams: DependencySourcesParams,
  ): DependencySourcesResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return DependencySourcesResult(emptyList())
    return bspMapper.dependencySources(project, dependencySourcesParams)
  }

  fun buildTargetOutputPaths(cancelChecker: CancelChecker, params: OutputPathsParams): OutputPathsResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return OutputPathsResult(emptyList())
    return bspMapper.outputPaths(project, params)
  }

  fun jvmRunEnvironment(cancelChecker: CancelChecker, params: JvmRunEnvironmentParams): JvmRunEnvironmentResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return JvmRunEnvironmentResult(emptyList())
    return bspMapper.jvmRunEnvironment(project, params, cancelChecker)
  }

  fun jvmTestEnvironment(cancelChecker: CancelChecker, params: JvmTestEnvironmentParams): JvmTestEnvironmentResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return JvmTestEnvironmentResult(emptyList())
    return bspMapper.jvmTestEnvironment(project, params, cancelChecker)
  }

  fun jvmBinaryJars(cancelChecker: CancelChecker, params: JvmBinaryJarsParams): JvmBinaryJarsResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return JvmBinaryJarsResult(emptyList())
    return bspMapper.jvmBinaryJars(project, params)
  }

  fun jvmCompileClasspath(cancelChecker: CancelChecker, params: JvmCompileClasspathParams): JvmCompileClasspathResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return JvmCompileClasspathResult(emptyList())
    return bspMapper.jvmCompileClasspath(project, params, cancelChecker)
  }

  fun buildTargetJavacOptions(cancelChecker: CancelChecker, params: JavacOptionsParams): JavacOptionsResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return JavacOptionsResult(emptyList())
    return bspMapper.buildTargetJavacOptions(project, params, cancelChecker)
  }

  fun buildTargetCppOptions(cancelChecker: CancelChecker, params: CppOptionsParams): CppOptionsResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return CppOptionsResult(emptyList())
    return bspMapper.buildTargetCppOptions(project, params)
  }

  fun buildTargetPythonOptions(cancelChecker: CancelChecker, params: PythonOptionsParams): PythonOptionsResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return PythonOptionsResult(emptyList())
    return bspMapper.buildTargetPythonOptions(project, params)
  }

  fun buildTargetScalacOptions(cancelChecker: CancelChecker, params: ScalacOptionsParams): ScalacOptionsResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return ScalacOptionsResult(emptyList())
    return bspMapper.buildTargetScalacOptions(project, params, cancelChecker)
  }

  fun buildTargetScalaTestClasses(cancelChecker: CancelChecker, params: ScalaTestClassesParams): ScalaTestClassesResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return ScalaTestClassesResult(emptyList())
    return bspMapper.buildTargetScalaTestClasses(project, params)
  }

  fun buildTargetScalaMainClasses(cancelChecker: CancelChecker, params: ScalaMainClassesParams): ScalaMainClassesResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return ScalaMainClassesResult(emptyList())
    return bspMapper.buildTargetScalaMainClasses(project, params)
  }

  fun buildTargetDependencyModules(cancelChecker: CancelChecker, params: DependencyModulesParams): DependencyModulesResult {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return DependencyModulesResult(emptyList())
    return bspMapper.buildDependencyModules(project, params)
  }

  fun rustWorkspace(cancelChecker: CancelChecker, params: RustWorkspaceParams): RustWorkspaceResult {
    val project =
      projectProvider.get(cancelChecker) as? AspectSyncProject
        ?: return RustWorkspaceResult(emptyList(), emptyMap(), emptyMap(), emptyList())
    return bspMapper.rustWorkspace(project, params)
  }

  fun resolveLocalToRemote(cancelChecker: CancelChecker, params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult =
    bspMapper.resolveLocalToRemote(cancelChecker, params)

  fun resolveRemoteToLocal(cancelChecker: CancelChecker, params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult =
    bspMapper.resolveRemoteToLocal(cancelChecker, params)
}
